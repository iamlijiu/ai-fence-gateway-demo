package com.zl.demo.controller;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 内置 Mock 上游（MVC 版，模拟 OpenAI 兼容的大模型供应商）
 * <p>
 * POST /mock/v1/chat/completions
 * 按 stream 标志返回不同格式（与真实供应商行为一致）。
 * </p>
 */
@RestController
@RequestMapping("/mock/v1")
public class MockUpstreamController {

    private final ObjectMapper objectMapper;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    public MockUpstreamController(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 统一入口：按 stream 标志分流返回。
     * <p>
     * stream=true  → ResponseBodyEmitter 逐块 SSE 返回 + [DONE]
     * stream=false → 一次性返回 JSON 字符串
     * </p>
     */
    @PostMapping("/chat/completions")
    public Object mockChat(@RequestBody String rawBody) {
        boolean stream = isStreamRequest(rawBody);
        Object model = readModel(rawBody);

        if (stream) {
            return mockStream(model);
        }
        return mockJson(model);
    }

    /** 流式 Mock：ResponseBodyEmitter 模拟 SSE 逐块返回 */
    private ResponseBodyEmitter mockStream(Object model) {
        ResponseBodyEmitter emitter = new ResponseBodyEmitter();

        // 异步发送 SSE 块（模拟上游流式响应）
        executor.submit(() -> {
            try {
                for (int i = 1; i <= 3; i++) {
                    String chunk = "data:{\"id\":\"mock-stream\",\"model\":\"" + model + "\","
                            + "\"choices\":[{\"delta\":{\"role\":\"assistant\","
                            + "\"content\":\"流式片段" + i + "\"}}]}\n\n";
                    emitter.send(chunk, MediaType.TEXT_EVENT_STREAM);
                    Thread.sleep(200);
                }
                // OpenAI 规范：以 [DONE] 结束
                emitter.send("data:[DONE]\n\n", MediaType.TEXT_EVENT_STREAM);
                emitter.complete();
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }

    /** 非流式 Mock：一次性返回 JSON */
    private String mockJson(Object model) {
        return "{\"id\":\"mock-json\",\"model\":\"" + model + "\","
                + "\"choices\":[{\"message\":{\"role\":\"assistant\","
                + "\"content\":\"你好，这是非流式 Mock 响应\"},"
                + "\"finish_reason\":\"stop\"}]}";
    }

    private boolean isStreamRequest(String rawBody) {
        try {
            Map<?, ?> map = objectMapper.readValue(rawBody, Map.class);
            return Boolean.TRUE.equals(map.get("stream"));
        } catch (Exception e) {
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    private Object readModel(String rawBody) {
        try {
            Map<String, Object> map = objectMapper.readValue(rawBody, Map.class);
            return map.getOrDefault("model", "unknown");
        } catch (Exception e) {
            return "unknown";
        }
    }
}
