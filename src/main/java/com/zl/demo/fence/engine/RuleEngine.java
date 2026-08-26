package com.zl.demo.fence.engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zl.demo.fence.context.FenceContext;
import com.zl.demo.fence.model.MatchResult;
import com.zl.demo.fence.model.Rule;
import com.zl.demo.fence.store.InMemoryRuleStore;

/**
 * 规则匹配引擎（P0-2）
 * <p>
 * 匹配流程：
 * 1. 从请求 JSON 中按 fieldPath 提取待检测文本节点
 * 2. 对每个节点用规则正则匹配
 * 3. 命中后按 disposition 执行处置（mask_pass/replace/block）
 * 4. 返回所有命中结果 + 处置后的 JSON
 * </p>
 */
@Component
public class RuleEngine {

    private static final Logger log = LoggerFactory.getLogger(RuleEngine.class);

    private final InMemoryRuleStore ruleStore;
    private final ObjectMapper objectMapper;

    /** 正则预编译缓存（避免每次匹配重新编译） */
    private final ConcurrentHashMap<String, Pattern> patternCache = new ConcurrentHashMap<>();

    public RuleEngine(InMemoryRuleStore ruleStore, ObjectMapper objectMapper) {
        this.ruleStore = ruleStore;
        this.objectMapper = objectMapper;
    }

    /**
     * 对请求体执行全量规则匹配与脱敏
     *
     * @param requestBody 原始请求 JSON
     * @param context     请求上下文
     * @return 匹配结果（含处置后的 JSON）
     */
    public EngineResult execute(String requestBody, FenceContext context) {
        List<Rule> rules = ruleStore.getEnabledSnapshot();
        if (rules.isEmpty()) {
            return EngineResult.pass(requestBody);
        }

        try {
            JsonNode root = objectMapper.readTree(requestBody);
            List<MatchResult> allMatches = new ArrayList<>();
            boolean blocked = false;

            // 按优先级遍历规则
            for (Rule rule : rules) {
                if (!rule.matchesScope(context.getRoute(), context.getConsumer(), context.getModel())) {
                    continue;
                }

                List<MatchResult> ruleMatches = matchRule(rule, root, context);
                for (MatchResult match : ruleMatches) {
                    if (match.isHit()) {
                        allMatches.add(match);
                        if ("block".equals(rule.getDisposition())) {
                            blocked = true;
                        }
                    }
                }
            }

            if (blocked) {
                return EngineResult.blocked(allMatches);
            }

            if (allMatches.isEmpty()) {
                return EngineResult.pass(requestBody);
            }

            // 序列化处置后的 JSON
            String desensitizedBody = objectMapper.writeValueAsString(root);
            return EngineResult.desensitized(desensitizedBody, allMatches);

        } catch (Exception e) {
            log.error("[围栏] 规则匹配异常，按降级策略放行: {}", e.getMessage(), e);
            return EngineResult.pass(requestBody);
        }
    }

    /**
     * 单条规则匹配：遍历 fieldPath 定位的文本节点，逐一正则匹配
     */
    private List<MatchResult> matchRule(Rule rule, JsonNode root, FenceContext context) {
        List<MatchResult> results = new ArrayList<>();

        // 解析 fieldPath 定位目标节点
        Map<String, JsonNode> targetNodes = resolveFieldPath(root, rule.getFieldPath());

        for (Map.Entry<String, JsonNode> entry : targetNodes.entrySet()) {
            String nodePath = entry.getKey();
            JsonNode node = entry.getValue();

            if (!node.isTextual()) continue;

            String text = node.asText();
            Pattern pattern = compilePattern(rule.getMatchPattern());
            if (pattern == null) continue;

            Matcher matcher = pattern.matcher(text);
            if (!matcher.find()) continue;

            // 命中 → 执行脱敏
            MatchResult match = new MatchResult(rule);
            String desensitized = applyDesensitization(text, pattern, rule);
            match.addHit(nodePath, text, desensitized);

            // 将脱敏后的值写回 JSON 树
            if (node.isValueNode()) {
                // 定位父节点并替换
                setNodeValue(root, nodePath, desensitized);
            }

            results.add(match);
        }

        return results;
    }

    /**
     * 解析 fieldPath，返回 {路径 → 节点} 映射
     * 支持：$.messages[*].content（数组通配）、$.field.subfield（嵌套）
     */
    private Map<String, JsonNode> resolveFieldPath(JsonNode root, String fieldPath) {
        Map<String, JsonNode> result = new LinkedHashMap<>();
        if (fieldPath == null || fieldPath.isEmpty() || "$".equals(fieldPath)) {
            // 无 fieldPath → 扫描全文本节点
            collectTextNodes(root, "$", result);
            return result;
        }

        // 解析 $.messages[*].content 格式
        String[] segments = fieldPath.replace("$.", "").split("\\.");
        resolveRecursive(root, "$", segments, 0, result);
        return result;
    }

    private void resolveRecursive(JsonNode current, String currentPath, String[] segments, int idx,
                                  Map<String, JsonNode> result) {
        if (idx >= segments.length) {
            if (current.isTextual()) {
                result.put(currentPath, current);
            }
            return;
        }

        String segment = segments[idx];

        if (segment.contains("[*]")) {
            // 数组通配：messages[*]
            String fieldName = segment.substring(0, segment.indexOf("[*]"));
            JsonNode arrayNode = current.get(fieldName);
            if (arrayNode != null && arrayNode.isArray()) {
                for (int i = 0; i < arrayNode.size(); i++) {
                    resolveRecursive(arrayNode.get(i),
                            currentPath + "." + fieldName + "[" + i + "]", segments, idx + 1, result);
                }
            }
        } else {
            JsonNode child = current.get(segment);
            if (child != null) {
                resolveRecursive(child, currentPath + "." + segment, segments, idx + 1, result);
            }
        }
    }

    /** 递归收集所有文本节点（无 fieldPath 时的全文扫描） */
    private void collectTextNodes(JsonNode node, String path, Map<String, JsonNode> result) {
        if (node.isTextual()) {
            result.put(path, node);
        } else if (node.isArray()) {
            for (int i = 0; i < node.size(); i++) {
                collectTextNodes(node.get(i), path + "[" + i + "]", result);
            }
        } else if (node.isObject()) {
            node.fields().forEachRemaining(entry ->
                    collectTextNodes(entry.getValue(), path + "." + entry.getKey(), result));
        }
    }

    /** 将脱敏后的值写回 JSON 树中指定路径的节点 */
    private void setNodeValue(JsonNode root, String path, String value) {
        String[] parts = path.replace("$.", "").split("\\.");
        JsonNode current = root;
        for (int i = 0; i < parts.length - 1; i++) {
            String part = parts[i];
            if (part.contains("[")) {
                String name = part.substring(0, part.indexOf("["));
                int idx = Integer.parseInt(part.substring(part.indexOf("[") + 1, part.indexOf("]")));
                current = current.get(name).get(idx);
            } else {
                current = current.get(part);
            }
        }
        if (current instanceof ObjectNode) {
            String lastPart = parts[parts.length - 1];
            ((ObjectNode) current).put(lastPart, value);
        }
    }

    /**
     * 对文本执行脱敏：找到正则匹配部分，按规则处置
     */
    private String applyDesensitization(String text, Pattern pattern, Rule rule) {
        String disposition = rule.getDisposition();

        if ("replace".equals(disposition)) {
            return pattern.matcher(text).replaceAll("[REDACTED]");
        }

        if ("mask_pass".equals(disposition)) {
            return applyMask(text, pattern, rule);
        }

        if ("delete".equals(disposition)) {
            // 删除脱敏：直接移除匹配到的敏感内容
            return pattern.matcher(text).replaceAll("");
        }

        // block 不修改文本（由调用方决定拦截）
        return text;
    }

    /**
     * 掩码脱敏：保留前 N 后 M 位，中间用 maskChar 替换
     * 例：13812341234 → 138****1234
     */
    private String applyMask(String text, Pattern pattern, Rule rule) {
        int keepPrefix = rule.getKeepPrefix();
        int keepSuffix = rule.getKeepSuffix();
        String maskChar = rule.getMaskChar();

        Matcher matcher = pattern.matcher(text);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String matched = matcher.group();
            int len = matched.length();

            if (len <= keepPrefix + keepSuffix) {
                // 太短，无法掩码，保持原样
                continue;
            }

            String prefix = matched.substring(0, keepPrefix);
            String suffix = matched.substring(len - keepSuffix);
            int maskLen = len - keepPrefix - keepSuffix;
            StringBuilder masked = new StringBuilder(prefix);
            for (int i = 0; i < maskLen; i++) masked.append(maskChar);
            masked.append(suffix);

            matcher.appendReplacement(sb, Matcher.quoteReplacement(masked.toString()));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private Pattern compilePattern(String regex) {
        if (regex == null) return null;
        return patternCache.computeIfAbsent(regex, r -> {
            try {
                return Pattern.compile(r);
            } catch (Exception e) {
                log.error("[围栏] 正则编译失败: {}", r, e);
                return null;
            }
        });
    }

    // ========== 引擎结果 ==========

    public static class EngineResult {
        private final String body;
        private final boolean blocked;
        private final boolean desensitized;
        private final List<MatchResult> matches;

        private EngineResult(String body, boolean blocked, boolean desensitized, List<MatchResult> matches) {
            this.body = body;
            this.blocked = blocked;
            this.desensitized = desensitized;
            this.matches = matches;
        }

        public static EngineResult pass(String body) {
            return new EngineResult(body, false, false, Collections.emptyList());
        }

        public static EngineResult blocked(List<MatchResult> matches) {
            return new EngineResult(null, true, false, matches);
        }

        public static EngineResult desensitized(String body, List<MatchResult> matches) {
            return new EngineResult(body, false, true, matches);
        }

        public String getBody() { return body; }
        public boolean isBlocked() { return blocked; }
        public boolean isDesensitized() { return desensitized; }
        public List<MatchResult> getMatches() { return matches; }
    }
}
