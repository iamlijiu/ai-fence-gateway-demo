package com.zl.demo.fence.entity;

import java.time.LocalDateTime;

/**
 * 脱敏操作日志实体类
 * <p>对应表：desensitization_log</p>
 */
public class DesensitizationLog {

    /** 日志唯一标识 */
    private String logId;

    /** 请求唯一标识 */
    private String requestId;

    /** 命中规则ID */
    private String ruleId;

    /** 命中规则名称 */
    private String ruleName;

    /** 命中字段路径 */
    private String fieldPath;

    /** 脱敏策略 */
    private String maskStrategy;

    /** 规则配置的处置策略 */
    private String disposition;

    /** 实际处置结果：pass/blocked/degraded/passthrough */
    private String dispositionResult;

    /** 风险等级：高/中/低 */
    private String riskLevel;

    /** 原文SHA-256哈希（带盐） */
    private String originalHash;

    /** 脱敏后内容长度 */
    private Integer desensitizedLen;

    /** 处理耗时（毫秒） */
    private Long costMs;

    /** 请求路由 */
    private String route;

    /** 调用方标识（ZL-OPENID） */
    private String consumer;

    /** 模型名称 */
    private String model;

    /** 日志时间 */
    private LocalDateTime timestamp;

    // ========== 构造方法 ==========

    public DesensitizationLog() {
    }

    public DesensitizationLog(String logId, String requestId, String ruleId) {
        this.logId = logId;
        this.requestId = requestId;
        this.ruleId = ruleId;
    }

    // ========== Getter/Setter ==========

    public String getLogId() {
        return logId;
    }

    public void setLogId(String logId) {
        this.logId = logId;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

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

    public String getDisposition() {
        return disposition;
    }

    public void setDisposition(String disposition) {
        this.disposition = disposition;
    }

    public String getDispositionResult() {
        return dispositionResult;
    }

    public void setDispositionResult(String dispositionResult) {
        this.dispositionResult = dispositionResult;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public String getOriginalHash() {
        return originalHash;
    }

    public void setOriginalHash(String originalHash) {
        this.originalHash = originalHash;
    }

    public Integer getDesensitizedLen() {
        return desensitizedLen;
    }

    public void setDesensitizedLen(Integer desensitizedLen) {
        this.desensitizedLen = desensitizedLen;
    }

    public Long getCostMs() {
        return costMs;
    }

    public void setCostMs(Long costMs) {
        this.costMs = costMs;
    }

    public String getRoute() {
        return route;
    }

    public void setRoute(String route) {
        this.route = route;
    }

    public String getConsumer() {
        return consumer;
    }

    public void setConsumer(String consumer) {
        this.consumer = consumer;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "DesensitizationLog{" +
                "logId='" + logId + '\'' +
                ", requestId='" + requestId + '\'' +
                ", ruleId='" + ruleId + '\'' +
                ", dispositionResult='" + dispositionResult + '\'' +
                ", route='" + route + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
