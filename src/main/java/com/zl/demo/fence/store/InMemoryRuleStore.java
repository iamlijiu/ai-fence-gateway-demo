package com.zl.demo.fence.store;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.zl.demo.fence.model.Rule;

/**
 * 规则存储（InMemory 实现，后续可替换为 Oracle）
 * <p>
 * 线程安全：ConcurrentHashMap + 不可变列表快照。
 * 规则变更后调用 refreshSnapshot() 重建排序快照，下一请求即生效（热加载）。
 * </p>
 */
@Component
public class InMemoryRuleStore {

    private static final Logger log = LoggerFactory.getLogger(InMemoryRuleStore.class);

    private final ConcurrentHashMap<String, Rule> rules = new ConcurrentHashMap<>();
    private volatile List<Rule> snapshot = Collections.emptyList();
    private final AtomicLong versionCounter = new AtomicLong(0);

    @PostConstruct
    public void init() {
        loadPresetRules();
        refreshSnapshot();
        log.info("规则存储初始化完成，共 {} 条规则", rules.size());
    }

    // ========== 查询 ==========

    /** 获取当前所有启用规则的排序快照（priority ASC），供匹配引擎使用 */
    public List<Rule> getEnabledSnapshot() {
        return snapshot;
    }

    public Rule getById(String ruleId) {
        return rules.get(ruleId);
    }

    public List<Rule> getAll() {
        return new ArrayList<>(rules.values());
    }

    // ========== 变更 ==========

    public void put(Rule rule) {
        rules.put(rule.getRuleId(), rule);
        refreshSnapshot();
        log.info("规则已更新: {} [{}] {}", rule.getRuleId(), rule.getDisposition(), rule.getRuleName());
    }

    public Rule remove(String ruleId) {
        Rule removed = rules.remove(ruleId);
        if (removed != null) refreshSnapshot();
        return removed;
    }

    public void toggle(String ruleId, boolean enabled) {
        Rule rule = rules.get(ruleId);
        if (rule != null) {
            rule.setEnabled(enabled);
            refreshSnapshot();
            log.info("规则 {} 已{}", ruleId, enabled ? "启用" : "禁用");
        }
    }

    public long getVersion() {
        return versionCounter.get();
    }

    private void refreshSnapshot() {
        this.snapshot = rules.values().stream()
                .filter(Rule::isEnabled)
                .sorted((a, b) -> Integer.compare(a.getPriority(), b.getPriority()))
                .collect(Collectors.collectingAndThen(Collectors.toList(), Collections::unmodifiableList));
        versionCounter.incrementAndGet();
    }

    // ========== 9 类预置规则（D4，金融租赁行业通用标准） ==========

    private void loadPresetRules() {
        // #1 个人身份识别 - 身份证号（优先级高于手机号，避免被手机号正则截取子串）
        addRule("rule_001", "身份证号脱敏", "regex",
                "\\d{17}[\\dXx]", "$.messages[*].content",
                "mask_pass", "mask_middle",
                map("keep_prefix", 6, "keep_suffix", 4, "mask_char", "*"),
                5);

        // #2 个人联系方式 - 手机号（(?<!\d) 避免匹配身份证号等更长数字串的子串）
        addRule("rule_002", "手机号脱敏", "regex",
                "(?<!\\d)1[3-9]\\d{9}(?!\\d)", "$.messages[*].content",
                "mask_pass", "mask_middle",
                map("keep_prefix", 3, "keep_suffix", 4, "mask_char", "*"),
                10);

        // #2 个人联系方式 - 邮箱
        addRule("rule_003", "邮箱脱敏", "regex",
                "[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}", "$.messages[*].content",
                "mask_pass", "mask_middle",
                map("keep_prefix", 1, "keep_suffix", 0, "mask_char", "*"),
                20);

        // #3 金融账户 - 银行卡号
        addRule("rule_004", "银行卡号脱敏", "regex",
                "[1-9]\\d{15,18}", "$.messages[*].content",
                "mask_pass", "mask_middle",
                map("keep_prefix", 4, "keep_suffix", 4, "mask_char", "*"),
                15);

        // #3 金融账户 - 密码/验证码（拦截）
        addRule("rule_005", "密码验证码拦截", "regex",
                "(密码|验证码|password|token)[：: =]+\\S+", "$.messages[*].content",
                "block", "mask_middle",
                map("keep_prefix", 0, "keep_suffix", 0, "mask_char", "*"),
                1);

        // #7 密钥与凭据 - API Key（拦截）
        addRule("rule_006", "API Key 拦截", "regex",
                "(sk\\-[A-Za-z0-9]{20,}|Bearer\\s+[A-Za-z0-9._~+/\\-]+=*)", "$.messages[*].content",
                "block", "mask_middle",
                map("keep_prefix", 0, "keep_suffix", 0, "mask_char", "*"),
                2);

        // #8 内部网络信息 - 内网 IP
        addRule("rule_007", "内网IP替换", "regex",
                "(10\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}|172\\.(1[6-9]|2\\d|3[01])\\.\\d{1,3}\\.\\d{1,3}|192\\.168\\.\\d{1,3}\\.\\d{1,3})",
                "$.messages[*].content",
                "replace", "replace",
                map("keep_prefix", 0, "keep_suffix", 0, "mask_char", "*"),
                30);

        // #5 企业身份 - 统一社会信用代码
        addRule("rule_008", "统一社会信用代码脱敏", "regex",
                "[0-9A-HJ-NPQRTUWXY]{18}", "$.messages[*].content",
                "mask_pass", "mask_middle",
                map("keep_prefix", 2, "keep_suffix", 4, "mask_char", "*"),
                25);

        // #9 删除脱敏示例 - HTML标签（删除处置）
        addRule("rule_009", "HTML标签删除", "regex",
                "<[^>]+>", "$.messages[*].content",
                "delete", "delete",
                map("keep_prefix", 0, "keep_suffix", 0, "mask_char", "*"),
                35);
    }

    private void addRule(String id, String name, String matchType, String pattern, String fieldPath,
                         String disposition, String maskStrategy, Map<String, Object> maskConfig, int priority) {
        Rule rule = new Rule(id, name, pattern, fieldPath, disposition, maskStrategy, maskConfig, priority);
        rule.setMatchType(matchType);
        rules.put(id, rule);
    }

    private static Map<String, Object> map(Object... kvs) {
        Map<String, Object> m = new java.util.LinkedHashMap<>();
        for (int i = 0; i < kvs.length; i += 2) m.put(kvs[i].toString(), kvs[i + 1]);
        return m;
    }
}
