package com.zl.demo.fence.context;

import java.util.UUID;

/**
 * 单次请求的围栏上下文（贯穿 鉴权→匹配→脱敏→日志 全链路）
 */
public class FenceContext {

    private final String requestId;
    private final String route;      // 请求路由（如 /v1/chat/completions）
    private final String consumer;   // ZL-OPENID
    private final String model;
    private final long startTimeMs;

    public FenceContext(String route, String consumer, String model) {
        this.requestId = UUID.randomUUID().toString().replace("-", "");
        this.route = route;
        this.consumer = consumer;
        this.model = model;
        this.startTimeMs = System.currentTimeMillis();
    }

    /** 兼容旧构造（无 route） */
    public FenceContext(String consumer, String model) {
        this("/v1/chat/completions", consumer, model);
    }

    public String getRequestId() { return requestId; }
    public String getRoute() { return route; }
    public String getConsumer() { return consumer; }
    public String getModel() { return model; }
    public long getStartTimeMs() { return startTimeMs; }

    /** 从请求开始到当前的耗时（ms） */
    public long elapsed() { return System.currentTimeMillis() - startTimeMs; }
}
