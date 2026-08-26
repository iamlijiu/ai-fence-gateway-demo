package com.zl.demo.fence.entity;

import java.time.LocalDateTime;

/**
 * 违规记录实体类
 * <p>对应表：violation_log（二期启用，表先建）</p>
 */
public class ViolationLog {

    /** 违规记录唯一标识 */
    private String violationId;

    /** 请求唯一标识 */
    private String requestId;

    /** 模型名称 */
    private String model;

    /** 请求路由 */
    private String route;

    /** 调用方标识 */
    private String consumer;

    /** 违规类型：sensitive_word/script_injection/politics/porn/terror */
    private String violationType;

    /** 风险等级：高/中/低 */
    private String riskLevel;

    /** 违规内容哈希（不存明文） */
    private String contentHash;

    /** 脱敏后的命中片段（限长512字符） */
    private String hitSnippet;

    /** 检测模式：rule（规则）/semantic（语义） */
    private String detectMode;

    /** 反馈状态：pending/sent/failed */
    private String feedbackStatus;

    /** 创建时间 */
    private LocalDateTime createdAt;

    // ========== 构造方法 ==========

    public ViolationLog() {
    }

    public ViolationLog(String violationId, String requestId, String violationType) {
        this.violationId = violationId;
        this.requestId = requestId;
        this.violationType = violationType;
    }

    // ========== Getter/Setter ==========

    public String getViolationId() {
        return violationId;
    }

    public void setViolationId(String violationId) {
        this.violationId = violationId;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
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

    public String getViolationType() {
        return violationType;
    }

    public void setViolationType(String violationType) {
        this.violationType = violationType;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public String getContentHash() {
        return contentHash;
    }

    public void setContentHash(String contentHash) {
        this.contentHash = contentHash;
    }

    public String getHitSnippet() {
        return hitSnippet;
    }

    public void setHitSnippet(String hitSnippet) {
        this.hitSnippet = hitSnippet;
    }

    public String getDetectMode() {
        return detectMode;
    }

    public void setDetectMode(String detectMode) {
        this.detectMode = detectMode;
    }

    public String getFeedbackStatus() {
        return feedbackStatus;
    }

    public void setFeedbackStatus(String feedbackStatus) {
        this.feedbackStatus = feedbackStatus;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "ViolationLog{" +
                "violationId='" + violationId + '\'' +
                ", requestId='" + requestId + '\'' +
                ", violationType='" + violationType + '\'' +
                ", riskLevel='" + riskLevel + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
