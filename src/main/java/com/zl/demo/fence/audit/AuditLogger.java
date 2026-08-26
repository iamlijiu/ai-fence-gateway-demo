package com.zl.demo.fence.audit;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.zl.demo.fence.context.FenceContext;
import com.zl.demo.fence.model.MatchResult;

/**
 * 审计日志（P0-5）
 * <p>
 * 记录每次脱敏操作的完整链路，日志格式对齐 desensitization_log 表结构。
 * 日志中不保存未脱敏的敏感数据明文（仅记录 original_hash）。
 * </p>
 *
 * <p>对齐字段：</p>
 * <ul>
 *   <li>log_id: 唯一日志ID</li>
 *   <li>request_id: 请求ID</li>
 *   <li>rule_id: 命中规则ID</li>
 *   <li>field_path: 命中字段路径</li>
 *   <li>mask_strategy: 脱敏策略</li>
 *   <li>disposition: 规则配置的处置策略</li>
 *   <li>disposition_result: 实际处置结果（pass/blocked/degraded/passthrough）</li>
 *   <li>risk_level: 风险等级（高/中/低）</li>
 *   <li>original_hash: 原文SHA-256哈希</li>
 *   <li>cost_ms: 处理耗时</li>
 *   <li>route: 请求路由</li>
 *   <li>consumer: 调用方标识</li>
 *   <li>model: 模型名称</li>
 *   <li>timestamp: 时间戳</li>
 * </ul>
 */
@Component
public class AuditLogger {

    private static final Logger log = LoggerFactory.getLogger(AuditLogger.class);
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String SALT = "ai-fence-v1-"; // 系统级盐值

    /**
     * 记录围栏处置结果（无命中时调用）
     *
     * @param context    请求上下文
     * @param matches    匹配结果列表（应为空）
     * @param blocked    是否被拦截
     * @param bodyLength 请求体长度
     */
    public void log(FenceContext context, List<MatchResult> matches, boolean blocked, int bodyLength) {
        log(context, matches, blocked, "pass", bodyLength);
    }

    /**
     * 记录围栏处置结果
     *
     * @param context           请求上下文
     * @param matches           匹配结果列表
     * @param blocked           是否被拦截
     * @param dispositionResult 实际处置结果（pass/blocked/degraded/passthrough）
     * @param bodyLength        请求体长度
     */
    public void log(FenceContext context, List<MatchResult> matches, boolean blocked,
                    String dispositionResult, int bodyLength) {
        if (matches.isEmpty()) {
            log.info("[围栏审计] log_id={} request_id={} route={} consumer={} model={} "
                    + "rule_id=- field=- disposition=- strategy=- disposition_result={} "
                    + "risk_level=- original_hash=- cost_ms={} timestamp={}",
                    generateLogId(),
                    context.getRequestId(),
                    context.getRoute(),
                    context.getConsumer(),
                    context.getModel(),
                    dispositionResult,
                    context.elapsed(),
                    Instant.now().toString());
            return;
        }

        for (MatchResult match : matches) {
            for (MatchResult.FieldHit hit : match.getHits()) {
                String originalHash = sha256(SALT + hit.getOriginalValue());
                String riskLevel = determineRiskLevel(match.getRule().getDisposition());

                log.info("[围栏审计] log_id={} request_id={} route={} consumer={} model={} "
                        + "rule_id={} rule_name={} field={} disposition={} strategy={} "
                        + "disposition_result={} risk_level={} original_hash={} "
                        + "desensitized_len={} cost_ms={} timestamp={}",
                        generateLogId(),
                        context.getRequestId(),
                        context.getRoute(),
                        context.getConsumer(),
                        context.getModel(),
                        match.getRule().getRuleId(),
                        match.getRule().getRuleName(),
                        hit.getFieldPath(),
                        match.getRule().getDisposition(),
                        match.getRule().getMaskStrategy(),
                        dispositionResult,
                        riskLevel,
                        originalHash,
                        hit.getDesensitizedValue().length(),
                        context.elapsed(),
                        Instant.now().toString());
            }
        }
    }

    /**
     * 根据处置策略确定风险等级
     */
    private String determineRiskLevel(String disposition) {
        if ("block".equals(disposition)) {
            return "高";
        }
        if ("replace".equals(disposition)) {
            return "中";
        }
        return "低";
    }

    /**
     * 生成唯一日志ID
     */
    private String generateLogId() {
        return System.currentTimeMillis() + "-" + RANDOM.nextInt(10000);
    }

    /**
     * SHA-256 哈希（带盐值，仅用于审计比对，不可逆）
     */
    private String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return "HASH_ERROR";
        }
    }
}
