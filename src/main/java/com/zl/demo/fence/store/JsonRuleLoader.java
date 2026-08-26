package com.zl.demo.fence.store;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zl.demo.fence.model.Rule;

/**
 * JSON规则加载器
 * <p>
 * 从JSON文件加载规则配置，支持：
 * 1. ClassPath资源加载
 * 2. 文件系统路径加载
 * 3. URL加载（预留）
 * </p>
 */
@Component
public class JsonRuleLoader {

    private static final Logger log = LoggerFactory.getLogger(JsonRuleLoader.class);
    private final ObjectMapper objectMapper;

    public JsonRuleLoader(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 从ClassPath加载规则
     *
     * @param classpath 资源路径，如 "rules/preset-rules.json"
     * @return 规则列表
     */
    public List<Rule> loadFromClasspath(String classpath) {
        try {
            ClassPathResource resource = new ClassPathResource(classpath);
            if (!resource.exists()) {
                log.warn("[规则加载] ClassPath资源不存在: {}", classpath);
                return new ArrayList<>();
            }
            return loadFromInputStream(resource.getInputStream());
        } catch (IOException e) {
            log.error("[规则加载] 从ClassPath加载失败: {}", classpath, e);
            return new ArrayList<>();
        }
    }

    /**
     * 从输入流加载规则
     *
     * @param inputStream 输入流
     * @return 规则列表
     */
    public List<Rule> loadFromInputStream(InputStream inputStream) {
        try {
            Map<String, Object> root = objectMapper.readValue(inputStream, new TypeReference<Map<String, Object>>() {});
            List<Map<String, Object>> ruleMaps = (List<Map<String, Object>>) root.get("rules");

            if (ruleMaps == null || ruleMaps.isEmpty()) {
                log.warn("[规则加载] JSON中未找到rules数组");
                return new ArrayList<>();
            }

            List<Rule> rules = new ArrayList<>();
            for (Map<String, Object> ruleMap : ruleMaps) {
                Rule rule = convertToRule(ruleMap);
                if (rule != null) {
                    rules.add(rule);
                }
            }

            log.info("[规则加载] 成功加载 {} 条规则", rules.size());
            return rules;
        } catch (IOException e) {
            log.error("[规则加载] 解析JSON失败", e);
            return new ArrayList<>();
        }
    }

    /**
     * 将Map转换为Rule对象
     */
    @SuppressWarnings("unchecked")
    private Rule convertToRule(Map<String, Object> map) {
        try {
            Rule rule = new Rule();
            rule.setRuleId((String) map.get("rule_id"));
            rule.setRuleName((String) map.get("rule_name"));
            rule.setMatchType((String) map.get("match_type"));
            rule.setMatchPattern((String) map.get("match_pattern"));
            rule.setFieldPath((String) map.get("field_path"));
            rule.setDisposition((String) map.get("disposition"));
            rule.setMaskStrategy((String) map.get("mask_strategy"));

            // 解析 mask_config
            Map<String, Object> maskConfig = (Map<String, Object>) map.get("mask_config");
            if (maskConfig != null) {
                rule.setMaskConfig(new HashMap<>(maskConfig));
            }

            // 解析 priority
            Object priority = map.get("priority");
            if (priority instanceof Number) {
                rule.setPriority(((Number) priority).intValue());
            }

            // 解析 enabled
            Object enabled = map.get("enabled");
            if (enabled instanceof Boolean) {
                rule.setEnabled((Boolean) enabled);
            }

            // 解析 scope
            Map<String, Object> scope = (Map<String, Object>) map.get("scope");
            if (scope != null) {
                rule.setScope(new HashMap<>(scope));
            }

            return rule;
        } catch (Exception e) {
            log.error("[规则加载] 转换规则失败: {}", map.get("rule_id"), e);
            return null;
        }
    }

    /**
     * 验证规则JSON格式
     *
     * @param classpath 资源路径
     * @return 验证结果（true=格式正确）
     */
    public boolean validate(String classpath) {
        try {
            List<Rule> rules = loadFromClasspath(classpath);
            if (rules.isEmpty()) {
                log.warn("[规则验证] 未加载到任何规则");
                return false;
            }

            // 验证每条规则
            for (Rule rule : rules) {
                if (rule.getRuleId() == null || rule.getRuleId().isEmpty()) {
                    log.error("[规则验证] 规则ID为空");
                    return false;
                }
                if (rule.getMatchPattern() == null || rule.getMatchPattern().isEmpty()) {
                    log.error("[规则验证] 规则 {} 的matchPattern为空", rule.getRuleId());
                    return false;
                }
                // 验证正则语法
                try {
                    java.util.regex.Pattern.compile(rule.getMatchPattern());
                } catch (java.util.regex.PatternSyntaxException e) {
                    log.error("[规则验证] 规则 {} 的正则语法错误: {}", rule.getRuleId(), e.getMessage());
                    return false;
                }
            }

            log.info("[规则验证] 格式验证通过，共 {} 条规则", rules.size());
            return true;
        } catch (Exception e) {
            log.error("[规则验证] 验证失败", e);
            return false;
        }
    }
}
