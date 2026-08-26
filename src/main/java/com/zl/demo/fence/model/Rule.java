package com.zl.demo.fence.model;

import java.util.HashMap;
import java.util.Map;

/**
 * 脱敏规则实体（对应 T2 规则模型 + T3 desensitization_rule 表）
 * <p>
 * disposition 决定顶层处置策略：
 * - mask_pass：脱敏后放行（mask_strategy 生效）
 * - replace：替换为 [REDACTED] 后放行
 * - block：直接拦截请求
 * </p>
 */
public class Rule {

    private String ruleId;
    private String ruleName;

    /** 匹配类型：regex（正则） / field_path（字段路径定位后正则） */
    private String matchType = "regex";

    /** 正则表达式 */
    private String matchPattern;

    /** JSON 字段路径，如 $.messages[*].content；为空则全文扫描 */
    private String fieldPath;

    /** 顶层处置：mask_pass / replace / block */
    private String disposition = "mask_pass";

    /** 脱敏手法：mask_middle / replace / delete（仅 disposition=mask_pass 时生效） */
    private String maskStrategy = "mask_middle";

    /** 脱敏配置：keep_prefix / keep_suffix / mask_char */
    private Map<String, Object> maskConfig = new HashMap<>();

    /** 优先级，数值越小越先匹配 */
    private int priority = 100;

    private boolean enabled = true;
    private int ruleVersion = 1;

    /** 生效范围：routes / consumers / models（为空表示全量生效） */
    private Map<String, Object> scope = new HashMap<>();

    // ========== 构造 ==========

    public Rule() {}

    public Rule(String ruleId, String ruleName, String matchPattern, String fieldPath,
                String disposition, String maskStrategy, Map<String, Object> maskConfig, int priority) {
        this.ruleId = ruleId;
        this.ruleName = ruleName;
        this.matchPattern = matchPattern;
        this.fieldPath = fieldPath;
        this.disposition = disposition;
        this.maskStrategy = maskStrategy;
        this.maskConfig = maskConfig;
        this.priority = priority;
    }

    // ========== getters/setters ==========

    public String getRuleId() { return ruleId; }
    public void setRuleId(String ruleId) { this.ruleId = ruleId; }

    public String getRuleName() { return ruleName; }
    public void setRuleName(String ruleName) { this.ruleName = ruleName; }

    public String getMatchType() { return matchType; }
    public void setMatchType(String matchType) { this.matchType = matchType; }

    public String getMatchPattern() { return matchPattern; }
    public void setMatchPattern(String matchPattern) { this.matchPattern = matchPattern; }

    public String getFieldPath() { return fieldPath; }
    public void setFieldPath(String fieldPath) { this.fieldPath = fieldPath; }

    public String getDisposition() { return disposition; }
    public void setDisposition(String disposition) { this.disposition = disposition; }

    public String getMaskStrategy() { return maskStrategy; }
    public void setMaskStrategy(String maskStrategy) { this.maskStrategy = maskStrategy; }

    public Map<String, Object> getMaskConfig() { return maskConfig; }
    public void setMaskConfig(Map<String, Object> maskConfig) { this.maskConfig = maskConfig; }

    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public int getRuleVersion() { return ruleVersion; }
    public void setRuleVersion(int ruleVersion) { this.ruleVersion = ruleVersion; }

    public Map<String, Object> getScope() { return scope; }
    public void setScope(Map<String, Object> scope) { this.scope = scope; }

    // ========== 便捷方法 ==========

    public int getKeepPrefix() {
        Object v = maskConfig.get("keep_prefix");
        return v instanceof Number ? ((Number) v).intValue() : 3;
    }

    public int getKeepSuffix() {
        Object v = maskConfig.get("keep_suffix");
        return v instanceof Number ? ((Number) v).intValue() : 4;
    }

    public String getMaskChar() {
        Object v = maskConfig.get("mask_char");
        return v != null ? v.toString() : "*";
    }

    /** 判断规则是否适用于指定的 route/consumer/model（scope 为空=全量生效） */
    public boolean matchesScope(String route, String consumer, String model) {
        if (scope == null || scope.isEmpty()) return true;
        @SuppressWarnings("unchecked")
        java.util.List<String> routes = (java.util.List<String>) scope.get("routes");
        if (routes != null && !routes.isEmpty() && !routes.contains(route)) return false;
        @SuppressWarnings("unchecked")
        java.util.List<String> consumers = (java.util.List<String>) scope.get("consumers");
        if (consumers != null && !consumers.isEmpty() && !consumers.contains(consumer)) return false;
        @SuppressWarnings("unchecked")
        java.util.List<String> models = (java.util.List<String>) scope.get("models");
        if (models != null && !models.isEmpty() && !models.contains(model)) return false;
        return true;
    }
}
