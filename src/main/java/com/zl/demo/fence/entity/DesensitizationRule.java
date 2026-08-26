package com.zl.demo.fence.entity;

import java.time.LocalDateTime;

/**
 * 脱敏规则实体类
 * <p>对应表：desensitization_rule</p>
 */
public class DesensitizationRule {

    /** 规则唯一标识 */
    private String ruleId;

    /** 规则名称 */
    private String ruleName;

    /** 匹配类型：regex/field_path/custom */
    private String matchType;

    /** 匹配模式（正则表达式） */
    private String matchPattern;

    /** JSON字段路径，如$.messages[*].content */
    private String fieldPath;

    /** 脱敏策略：mask_middle/replace/delete */
    private String maskStrategy;

    /** 脱敏配置JSON：keep_prefix/keep_suffix/mask_char */
    private String maskConfig;

    /** 处置策略：mask_pass/block/replace/delete */
    private String disposition;

    /** 优先级，数值越小越优先 */
    private Integer priority;

    /** 是否启用：1=启用，0=禁用 */
    private Integer enabled;

    /** 生效范围JSON：routes/consumers/models */
    private String scope;

    /** 规则版本号，用于灰度/回滚 */
    private Integer ruleVersion;

    /** 灰度范围JSON（空=全量生效） */
    private String grayScope;

    /** 规则说明 */
    private String description;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;

    /** 创建人 */
    private String createdBy;

    /** 更新人 */
    private String updatedBy;

    /** 逻辑删除：0=正常，1=已删除 */
    private Integer deleted;

    // ========== 构造方法 ==========

    public DesensitizationRule() {
    }

    public DesensitizationRule(String ruleId, String ruleName, String disposition) {
        this.ruleId = ruleId;
        this.ruleName = ruleName;
        this.disposition = disposition;
    }

    // ========== Getter/Setter ==========

    public String getRuleId() {
        return ruleId;
    }

    public void setRuleId(String ruleId) {
        this.ruleId = ruleId;
    }

    public String getRuleName() {
        return ruleName;
    }

    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
    }

    public String getMatchType() {
        return matchType;
    }

    public void setMatchType(String matchType) {
        this.matchType = matchType;
    }

    public String getMatchPattern() {
        return matchPattern;
    }

    public void setMatchPattern(String matchPattern) {
        this.matchPattern = matchPattern;
    }

    public String getFieldPath() {
        return fieldPath;
    }

    public void setFieldPath(String fieldPath) {
        this.fieldPath = fieldPath;
    }

    public String getMaskStrategy() {
        return maskStrategy;
    }

    public void setMaskStrategy(String maskStrategy) {
        this.maskStrategy = maskStrategy;
    }

    public String getMaskConfig() {
        return maskConfig;
    }

    public void setMaskConfig(String maskConfig) {
        this.maskConfig = maskConfig;
    }

    public String getDisposition() {
        return disposition;
    }

    public void setDisposition(String disposition) {
        this.disposition = disposition;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public Integer getEnabled() {
        return enabled;
    }

    public void setEnabled(Integer enabled) {
        this.enabled = enabled;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public Integer getRuleVersion() {
        return ruleVersion;
    }

    public void setRuleVersion(Integer ruleVersion) {
        this.ruleVersion = ruleVersion;
    }

    public String getGrayScope() {
        return grayScope;
    }

    public void setGrayScope(String grayScope) {
        this.grayScope = grayScope;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }

    // ========== 便捷方法 ==========

    public boolean isEnabled() {
        return enabled != null && enabled == 1;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled ? 1 : 0;
    }

    public boolean isDeleted() {
        return deleted != null && deleted == 1;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted ? 1 : 0;
    }

    @Override
    public String toString() {
        return "DesensitizationRule{" +
                "ruleId='" + ruleId + '\'' +
                ", ruleName='" + ruleName + '\'' +
                ", disposition='" + disposition + '\'' +
                ", priority=" + priority +
                ", enabled=" + enabled +
                '}';
    }
}
