package com.zl.demo.controller;

import java.io.IOException;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zl.demo.config.DemoProperties;
import com.zl.demo.fence.audit.AuditLogger;
import com.zl.demo.fence.context.FenceContext;
import com.zl.demo.fence.degrade.CircuitBreaker;
import com.zl.demo.fence.engine.RuleEngine;
import com.zl.demo.fence.engine.RuleEngine.EngineResult;
import com.zl.demo.fence.monitor.MetricsCounter;

import reactor.core.publisher.Flux;

/**
 * OpenAI 兼容网关入口（MVC 版 + 安全围栏管线）：POST /v1/chat/completions
 * <p>
 * 完整处理流程：
 * 0. 围栏总开关检查（enabled）
 * 1. 熔断器检查（circuit breaker）
 * 2. 鉴权（ZlOpenIdFilter 已完成，consumer 写入 Header）
 * 3. 生成请求上下文（request_id / consumer / model）
 * 4. 规则匹配 + 脱敏（RuleEngine）
 * 5. dry-run 模式判断（只检测不执行）
 * 6. 拦截检查（block → 403）
 * 7. 降级检查（异常降级：reject/passthrough）
 * 8. 转发上游（stream 分流：WebClient SSE / RestTemplate JSON）
 * 9. 审计日志（AuditLogger）+ 监控计数（MetricsCounter）
 * </p>
 */
@RestController
@RequestMapping("/v1")
public class ChatCompletionsController {

    private static final Logger log = LoggerFactory.getLogger(ChatCompletionsController.class);

    private final DemoProperties props;
    private final RestTemplate restTemplate;
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final RuleEngine ruleEngine;
    private final AuditLogger auditLogger;
    private final MetricsCounter metricsCounter;
    private final CircuitBreaker circuitBreaker;

    public ChatCompletionsController(DemoProperties props, RestTemplate restTemplate,
                                     WebClient webClient, ObjectMapper objectMapper,
                                     RuleEngine ruleEngine, AuditLogger auditLogger,
                                     MetricsCounter metricsCounter, CircuitBreaker circuitBreaker) {
        this.props = props;
        this.restTemplate = restTemplate;
        this.webClient = webClient;
        this.objectMapper = objectMapper;
        this.ruleEngine = ruleEngine;
        this.auditLogger = auditLogger;
        this.metricsCounter = metricsCounter;
        this.circuitBreaker = circuitBreaker;
    }

    /**
     * 统一入口。围栏管线 → stream 分流转发。
     */
    @PostMapping("/chat/completions")
    public Object chat(@RequestBody String rawBody, HttpServletRequest request, HttpServletResponse response) {
        long startTimeMs = System.currentTimeMillis();
        DemoProperties.Fence fenceConfig = props.getFence();

        // ① 围栏总开关检查
        if (!fenceConfig.isEnabled()) {
            log.debug("[围栏] 围栏已关闭，直接透传");
            return forwardByStreamFlag(rawBody, isStreamRequest(rawBody), response);
        }

        // ② 熔断器检查
        if (!circuitBreaker.allowRequest()) {
            log.warn("[围栏] 熔断器开启，走降级策略");
            metricsCounter.record(false, false, true, 0, System.currentTimeMillis() - startTimeMs);
            return handleDegradation("circuit_breaker_open", rawBody, response, startTimeMs);
        }

        // ③ 生成请求上下文
        String route = request.getRequestURI();
        String consumer = request.getHeader("ZL-OPENID");
        String model = extractModel(rawBody);
        boolean stream = isStreamRequest(rawBody);
        FenceContext ctx = new FenceContext(route, consumer, model);

        log.info("[围栏] request_id={} route={} consumer={} model={} stream={} dry_run={}",
                ctx.getRequestId(), route, consumer, model, stream, fenceConfig.isDryRun());

        // ④ 规则匹配 + 脱敏
        EngineResult result;
        try {
            result = ruleEngine.execute(rawBody, ctx);
        } catch (Exception e) {
            log.error("[围栏] request_id={} 规则引擎异常: {}", ctx.getRequestId(), e.getMessage(), e);
            circuitBreaker.record(false);
            metricsCounter.record(false, false, true, 0, System.currentTimeMillis() - startTimeMs);
            return handleDegradation("rule_engine_error", rawBody, response, startTimeMs);
        }

        // ⑤ dry-run 模式：只检测不执行处置
        if (fenceConfig.isDryRun()) {
            if (!result.getMatches().isEmpty()) {
                log.info("[围栏] request_id={} dry_run 模式，命中 {} 条规则（仅记录，不执行处置）: {}",
                        ctx.getRequestId(), result.getMatches().size(),
                        result.getMatches().stream()
                                .map(m -> m.getRule().getRuleId() + "(" + m.getRule().getDisposition() + ")")
                                .reduce((a, b) -> a + ", " + b).orElse(""));
            }
            // dry-run 模式下使用原始 body 转发
            auditLogger.log(ctx, result.getMatches(), false, "passthrough", rawBody.length());
            circuitBreaker.record(true);
            metricsCounter.record(false, false, false, result.getMatches().size(),
                    System.currentTimeMillis() - startTimeMs);
            return forwardByStreamFlag(rawBody, stream, response);
        }

        // ⑥ 拦截检查
        if (result.isBlocked()) {
            auditLogger.log(ctx, result.getMatches(), true, "blocked", rawBody.length());
            circuitBreaker.record(true); // 拦截是正常行为，不算异常
            metricsCounter.record(true, false, false, result.getMatches().size(),
                    System.currentTimeMillis() - startTimeMs);
            log.warn("[围栏] request_id={} 被拦截，命中 {} 条规则", ctx.getRequestId(), result.getMatches().size());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return "{\"code\":403,\"message\":\"请求包含敏感信息，已被安全围栏拦截\","
                    + "\"request_id\":\"" + ctx.getRequestId() + "\"}";
        }

        // ⑦ 使用处置后的 body 转发（脱敏 or 原样）
        String bodyToForward = result.getBody();
        boolean desensitized = result.isDesensitized();
        if (desensitized) {
            log.info("[围栏] request_id={} 已脱敏，命中 {} 条规则", ctx.getRequestId(), result.getMatches().size());
        }

        // ⑧ 审计日志
        String dispositionResult = desensitized ? "pass" : "pass";
        auditLogger.log(ctx, result.getMatches(), false, dispositionResult, bodyToForward.length());

        // ⑨ 监控计数
        long elapsed = System.currentTimeMillis() - startTimeMs;
        circuitBreaker.record(true);
        metricsCounter.record(false, desensitized, false, result.getMatches().size(), elapsed);

        // ⑩ stream 分流转发
        if (stream) {
            response.setContentType(MediaType.TEXT_EVENT_STREAM_VALUE);
            return forwardStream(bodyToForward);
        }
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        return forwardJson(bodyToForward);
    }

    /**
     * 处理降级场景
     *
     * @param reason    降级原因
     * @param rawBody   原始请求体
     * @param response  HTTP 响应
     * @param startTime 请求开始时间
     * @return 降级响应
     */
    private Object handleDegradation(String reason, String rawBody,
                                     HttpServletResponse response, long startTimeMs) {
        String strategy = props.getFence().getDegradeStrategy();
        boolean stream = isStreamRequest(rawBody);

        log.info("[围栏降级] reason={} strategy={}", reason, strategy);

        if ("reject".equals(strategy)) {
            // reject 策略：拒绝请求
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            return "{\"code\":503,\"message\":\"安全围栏服务降级，请稍后重试\","
                    + "\"degradation_reason\":\"" + reason + "\"}";
        }

        // passthrough 策略：透传放行（记录原因）
        log.warn("[围栏降级] passthrough 模式，reason={}，透传原始请求", reason);
        return forwardByStreamFlag(rawBody, stream, response);
    }

    /**
     * 按 stream 标志分流转发
     */
    private Object forwardByStreamFlag(String rawBody, boolean stream, HttpServletResponse response) {
        if (stream) {
            response.setContentType(MediaType.TEXT_EVENT_STREAM_VALUE);
            return forwardStream(rawBody);
        }
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        return forwardJson(rawBody);
    }

    /**
     * 流式：WebClient 转发上游，ResponseBodyEmitter 逐块返回 SSE。
     */
    private ResponseBodyEmitter forwardStream(String rawBody) {
        ResponseBodyEmitter emitter = new ResponseBodyEmitter();

        Flux<DataBuffer> upstream = webClient.post()
                .uri("/chat/completions")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .header(HttpHeaders.ACCEPT, MediaType.TEXT_EVENT_STREAM_VALUE)
                .syncBody(rawBody)
                .retrieve()
                .bodyToFlux(DataBuffer.class);

        upstream.subscribe(
                dataBuffer -> {
                    try {
                        byte[] bytes = new byte[dataBuffer.readableByteCount()];
                        dataBuffer.read(bytes);
                        emitter.send(bytes);
                    } catch (IOException e) {
                        log.error("emitter.send 异常", e);
                    } finally {
                        DataBufferUtils.release(dataBuffer);
                    }
                },
                error -> {
                    log.error("流式转发异常", error);
                    emitter.completeWithError(error);
                },
                emitter::complete
        );

        return emitter;
    }

    /**
     * 非流式：RestTemplate 阻塞转发，原样返回 JSON。
     */
    private ResponseEntity<String> forwardJson(String rawBody) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
        HttpEntity<String> entity = new HttpEntity<>(rawBody, headers);
        ResponseEntity<String> resp = restTemplate.postForEntity(
                props.getUpstreamBaseUrl() + "/chat/completions", entity, String.class);
        String body = resp.getBody() == null ? "" : resp.getBody();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(body);
    }

    /** 轻量解析 stream 标志 */
    private boolean isStreamRequest(String rawBody) {
        try {
            Map<?, ?> map = objectMapper.readValue(rawBody, Map.class);
            return Boolean.TRUE.equals(map.get("stream"));
        } catch (Exception e) {
            return false;
        }
    }

    /** 提取 model 字段（用于 scope 匹配） */
    private String extractModel(String rawBody) {
        try {
            Map<?, ?> map = objectMapper.readValue(rawBody, Map.class);
            Object model = map.get("model");
            return model != null ? model.toString() : "unknown";
        } catch (Exception e) {
            return "unknown";
        }
    }
}
