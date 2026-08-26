package com.zl.demo.fence.entity;

import java.time.LocalDateTime;

/**
 * 规则变更日志实体类
 * <p>对应表：rule_change_log</p>
 */
public class RuleChangeLog {

    /** 变更记录唯一标识 */
    private String changeId;

    /** 规则ID */
    private String ruleId;

    /** 操作类型：create/update/delete/enable/disable/gray/rollback */
    private String action;

    /** 规则版本号 */
    private Integer ruleVersion;

    /** 操作人 */
    private String operator;

    /** 变更详情JSON（变更前后快照） */
    private String changeDetail;

    /** 创建时间 */
    private LocalDateTime createdAt;

    // ========== 构造方法 ==========

    public RuleChangeLog() {
    }

    public RuleChangeLog(String changeId, String ruleId, String action, String operator) {
        this.changeId = changeId;
        this.ruleId = ruleId;
        this.action = action;
        this.operator = operator;
    }

    // ========== Getter/Setter ==========

    public String getChangeId() {
        return changeId;
    }

    public void setChangeId(String changeId) {
        this.changeId = changeId;
    }

    public String getRuleId() {
        return ruleId;
    }

    public void setRuleId(String ruleId) {
        this.ruleId = ruleId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public Integer getRuleVersion() {
        return ruleVersion;
    }

    public void setRuleVersion(Integer ruleVersion) {
        this.ruleVersion = ruleVersion;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public String getChangeDetail() {
        return changeDetail;
    }

    public void setChangeDetail(String changeDetail) {
        this.changeDetail = changeDetail;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "RuleChangeLog{" +
                "changeId='" + changeId + '\'' +
                ", ruleId='" + ruleId + '\'' +
                ", action='" + action + '\'' +
                ", operator='" + operator + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
