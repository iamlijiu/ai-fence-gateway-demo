package com.zl.demo.integration;

import static org.junit.Assert.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * MockUpstreamController 集成测试
 * <p>
 * 验证Mock上游的JSON非流式返回
 * 注意：MockMvc不支持SSE流式响应测试，流式测试需要启动完整应用
 * </p>
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
public class MockUpstreamIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    // ========== 非流式请求测试 ==========

    @Test
    public void testMockJson_normal() throws Exception {
        // 非流式请求
        String body = buildChatBody("你好", false);

        mockMvc.perform(post("/mock/v1/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("mock-json"))
                .andExpect(jsonPath("$.model").value("qwen-max"))
                .andExpect(jsonPath("$.choices").isArray())
                .andExpect(jsonPath("$.choices[0].message.content").value("你好，这是非流式 Mock 响应"));
    }

    @Test
    public void testMockJson_customModel() throws Exception {
        // 自定义模型名称
        String body = buildChatBody("你好", false, "gpt-4");

        mockMvc.perform(post("/mock/v1/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.model").value("gpt-4"));
    }

    // ========== 流式请求测试 ==========

    @Test
    public void testMockStream_request() throws Exception {
        // 流式请求 - MockMvc不支持SSE，但可以验证请求被接受
        String body = buildChatBody("你好", true);

        // MockMvc会返回200，但不会真正发送SSE流
        mockMvc.perform(post("/mock/v1/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Accept", "text/event-stream")
                .content(body))
                .andExpect(status().isOk());
    }

    // ========== 错误处理测试 ==========

    @Test
    public void testMock_emptyBody() throws Exception {
        // 空请求体 - Mock上游会使用默认值
        mockMvc.perform(post("/mock/v1/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("mock-json"));
    }

    @Test
    public void testMock_getRequest_notAllowed() throws Exception {
        // GET请求不应该被允许
        mockMvc.perform(get("/mock/v1/chat/completions"))
                .andExpect(status().isMethodNotAllowed());
    }

    // ========== 辅助方法 ==========

    private String buildChatBody(String content, boolean stream) {
        return buildChatBody(content, stream, "qwen-max");
    }

    private String buildChatBody(String content, boolean stream, String model) {
        return String.format(
                "{\"model\":\"%s\",\"stream\":%s,\"messages\":[{\"role\":\"user\",\"content\":\"%s\"}]}",
                model,
                stream,
                content.replace("\"", "\\\""));
    }
}
