package com.zl.demo.integration;

import static org.junit.Assert.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import com.zl.demo.config.DemoProperties;

/**
 * ChatCompletionsController 集成测试
 * <p>
 * 注意：MockMvc无法真正调用RestTemplate/WebClient进行上游转发，
 * 因此本测试只验证鉴权、围栏开关、降级策略等前置逻辑。
 * 完整的请求流程测试请参考 EndToEndIntegrationTest。
 * </p>
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
public class ChatCompletionsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DemoProperties demoProperties;

    @Before
    public void setUp() {
        // 确保围栏功能开启
        demoProperties.getFence().setEnabled(true);
        demoProperties.getFence().setDryRun(false);
    }

    // ========== 鉴权测试 ==========

    @Test
    public void testAuth_missingOpenId() throws Exception {
        // 缺少ZL-OPENID头 → 403
        String body = buildChatBody("你好", false);

        mockMvc.perform(post("/v1/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.message").value(containsString("ZL-OPENID")));
    }

    @Test
    public void testAuth_invalidOpenId() throws Exception {
        // 无效的ZL-OPENID → 403
        String body = buildChatBody("你好", false);

        mockMvc.perform(post("/v1/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .header("ZL-OPENID", "invalid_id")
                .content(body))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }

    // ========== 拦截测试 ==========

    @Test
    public void testBlock_password() throws Exception {
        // 密码拦截 - 应返回403（不需要调用上游）
        String body = buildChatBody("密码: abc123", false);

        mockMvc.perform(post("/v1/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .header("ZL-OPENID", "sbzj_device")
                .content(body))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    public void testBlock_apiKey() throws Exception {
        // API Key拦截
        String body = buildChatBody("使用sk-abcdefghijklmnopqrstuvwxyz123456", false);

        mockMvc.perform(post("/v1/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .header("ZL-OPENID", "sbzj_device")
                .content(body))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    public void testBlock_token() throws Exception {
        // Token拦截
        String body = buildChatBody("token=abc123xyz", false);

        mockMvc.perform(post("/v1/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .header("ZL-OPENID", "sbzj_device")
                .content(body))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }

    // ========== 围栏开关测试 ==========

    @Test
    public void testFence_disabled_configChange() throws Exception {
        // 验证围栏配置可以被修改
        boolean originalEnabled = demoProperties.getFence().isEnabled();

        try {
            demoProperties.getFence().setEnabled(false);
            assertFalse("围栏应被关闭", demoProperties.getFence().isEnabled());

            demoProperties.getFence().setEnabled(true);
            assertTrue("围栏应被开启", demoProperties.getFence().isEnabled());
        } finally {
            demoProperties.getFence().setEnabled(originalEnabled);
        }
    }

    // ========== 监控API测试 ==========

    @Test
    public void testMetrics_api() throws Exception {
        // 查询监控统计
        mockMvc.perform(get("/api/v1/metrics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total_requests").isNumber());
    }

    @Test
    public void testCircuitBreaker_api() throws Exception {
        // 查询熔断器状态
        mockMvc.perform(get("/api/v1/circuit-breaker"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.state").value("CLOSED"));
    }

    @Test
    public void testFenceStatus_api() throws Exception {
        // 查询围栏配置状态
        mockMvc.perform(get("/api/v1/fence/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.enabled").value(true));
    }

    // ========== 规则管理API测试 ==========

    @Test
    public void testRuleList_api() throws Exception {
        // 查询规则列表
        mockMvc.perform(get("/api/v1/desensitization/rules"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(greaterThan(0)));
    }

    @Test
    public void testRuleExport_api() throws Exception {
        // 导出规则
        mockMvc.perform(get("/api/v1/desensitization/rules/export"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.rules").isArray())
                .andExpect(jsonPath("$.data.total").value(greaterThan(0)));
    }

    // ========== 辅助方法 ==========

    /**
     * 构建Chat Completions请求体
     */
    private String buildChatBody(String content, boolean stream) {
        return String.format(
                "{\"model\":\"qwen-max\",\"stream\":%s,\"messages\":[{\"role\":\"user\",\"content\":\"%s\"}]}",
                stream,
                content.replace("\"", "\\\""));
    }
}
