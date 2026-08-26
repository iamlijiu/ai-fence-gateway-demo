package com.zl.demo.integration;

import static org.junit.Assert.*;

import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zl.demo.config.DemoProperties;
import com.zl.demo.fence.context.FenceContext;
import com.zl.demo.fence.engine.RuleEngine;
import com.zl.demo.fence.engine.RuleEngine.EngineResult;
import com.zl.demo.fence.model.MatchResult;
import com.zl.demo.fence.model.Rule;
import com.zl.demo.fence.store.InMemoryRuleStore;
import com.zl.demo.fence.store.JsonRuleLoader;

/**
 * 端到端集成测试
 * <p>
 * 验证完整流程：
 * 1. 从JSON文件加载规则
 * 2. 规则引擎执行匹配
 * 3. 脱敏/拦截处置
 * 4. 审计日志记录
 * </p>
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
public class EndToEndIntegrationTest {

    @Autowired
    private RuleEngine ruleEngine;

    @Autowired
    private InMemoryRuleStore ruleStore;

    @Autowired
    private JsonRuleLoader jsonRuleLoader;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DemoProperties demoProperties;

    @Before
    public void setUp() {
        // 确保预置规则已加载
        ruleStore.init();
    }

    // ========== 规则加载测试 ==========

    @Test
    public void testRuleLoad_fromJson() {
        // 从JSON文件加载规则
        List<Rule> rules = jsonRuleLoader.loadFromClasspath("rules/preset-rules.json");

        assertNotNull("规则列表不应为空", rules);
        assertFalse("应加载到规则", rules.isEmpty());
        assertEquals("应加载9条规则", 9, rules.size());
    }

    @Test
    public void testRuleLoad_validate() {
        // 验证JSON格式
        boolean valid = jsonRuleLoader.validate("rules/preset-rules.json");
        assertTrue("JSON格式应有效", valid);
    }

    // ========== 身份证号脱敏端到端测试 ==========

    @Test
    public void testE2E_idCard_mask() {
        String body = buildMessages("我的身份证号是110101199003076534");
        FenceContext ctx = new FenceContext("/v1/chat/completions", "test", "qwen-max");

        EngineResult result = ruleEngine.execute(body, ctx);

        assertFalse("不应被拦截", result.isBlocked());
        assertTrue("应被脱敏", result.isDesensitized());
        assertTrue("应包含掩码", result.getBody().contains("110101********6534"));
    }

    // ========== 手机号脱敏端到端测试 ==========

    @Test
    public void testE2E_phoneNumber_mask() {
        String body = buildMessages("手机号13812341234");
        FenceContext ctx = new FenceContext("/v1/chat/completions", "test", "qwen-max");

        EngineResult result = ruleEngine.execute(body, ctx);

        assertTrue("应被脱敏", result.isDesensitized());
        assertTrue("应包含掩码", result.getBody().contains("138****1234"));
    }

    // ========== 邮箱脱敏端到端测试 ==========

    @Test
    public void testE2E_email_mask() {
        String body = buildMessages("邮箱test@example.com");
        FenceContext ctx = new FenceContext("/v1/chat/completions", "test", "qwen-max");

        EngineResult result = ruleEngine.execute(body, ctx);

        assertTrue("应被脱敏", result.isDesensitized());
        // 邮箱被掩码后应该包含*
        assertTrue("应包含掩码*", result.getBody().contains("*"));
        // 原始邮箱不应该完整保留
        assertFalse("不应完整保留原始邮箱", result.getBody().contains("test@example.com"));
    }

    // ========== 银行卡号脱敏端到端测试 ==========

    @Test
    public void testE2E_bankCard_mask() {
        String body = buildMessages("卡号6222021234567890");
        FenceContext ctx = new FenceContext("/v1/chat/completions", "test", "qwen-max");

        EngineResult result = ruleEngine.execute(body, ctx);

        assertTrue("应被脱敏", result.isDesensitized());
        assertTrue("应包含掩码", result.getBody().contains("6222********7890"));
    }

    // ========== 密码拦截端到端测试 ==========

    @Test
    public void testE2E_password_block() {
        String body = buildMessages("密码: abc123");
        FenceContext ctx = new FenceContext("/v1/chat/completions", "test", "qwen-max");

        EngineResult result = ruleEngine.execute(body, ctx);

        assertTrue("应被拦截", result.isBlocked());
        assertNotNull("应有命中记录", result.getMatches());
        assertFalse("应有命中规则", result.getMatches().isEmpty());
    }

    // ========== API Key拦截端到端测试 ==========

    @Test
    public void testE2E_apiKey_block() {
        String body = buildMessages("使用sk-abcdefghijklmnopqrstuvwxyz123456");
        FenceContext ctx = new FenceContext("/v1/chat/completions", "test", "qwen-max");

        EngineResult result = ruleEngine.execute(body, ctx);

        assertTrue("应被拦截", result.isBlocked());
    }

    // ========== 内网IP替换端到端测试 ==========

    @Test
    public void testE2E_internalIp_replace() {
        String body = buildMessages("服务器10.0.0.1");
        FenceContext ctx = new FenceContext("/v1/chat/completions", "test", "qwen-max");

        EngineResult result = ruleEngine.execute(body, ctx);

        assertTrue("应被脱敏", result.isDesensitized());
        assertTrue("应被替换为[REDACTED]", result.getBody().contains("[REDACTED]"));
    }

    // ========== HTML标签删除端到端测试 ==========

    @Test
    public void testE2E_htmlTag_delete() {
        String body = buildMessages("<div>内容</div>");
        FenceContext ctx = new FenceContext("/v1/chat/completions", "test", "qwen-max");

        EngineResult result = ruleEngine.execute(body, ctx);

        assertTrue("应被脱敏", result.isDesensitized());
        assertFalse("不应包含div标签", result.getBody().contains("<div>"));
        assertFalse("不应包含/div标签", result.getBody().contains("</div>"));
        assertTrue("应保留文本内容", result.getBody().contains("内容"));
    }

    // ========== 多规则命中端到端测试 ==========

    @Test
    public void testE2E_multipleRules() {
        // 同时包含手机号和邮箱
        String body = buildMessages("手机13812341234，邮箱test@example.com");
        FenceContext ctx = new FenceContext("/v1/chat/completions", "test", "qwen-max");

        EngineResult result = ruleEngine.execute(body, ctx);

        assertTrue("应被脱敏", result.isDesensitized());
        assertTrue("应包含手机号掩码", result.getBody().contains("138****1234"));
        // 邮箱被掩码后应该包含*
        assertTrue("应包含邮箱掩码", result.getBody().contains("*"));
        assertFalse("不应完整保留原始邮箱", result.getBody().contains("test@example.com"));
    }

    // ========== 优先级测试 ==========

    @Test
    public void testE2E_priority() {
        // 同时包含密码（block，优先级1）和手机号（mask_pass，优先级10）
        // 密码规则优先级更高，应被拦截
        String body = buildMessages("密码: abc123，手机13812341234");
        FenceContext ctx = new FenceContext("/v1/chat/completions", "test", "qwen-max");

        EngineResult result = ruleEngine.execute(body, ctx);

        assertTrue("应被拦截（密码规则优先）", result.isBlocked());
    }

    // ========== Scope匹配测试 ==========

    @Test
    public void testE2E_scope_consumerMismatch() {
        // 创建一个限定consumer的规则
        Rule scopedRule = new Rule();
        scopedRule.setRuleId("scoped_test");
        scopedRule.setRuleName("测试scope规则");
        scopedRule.setMatchPattern("测试关键词");
        scopedRule.setDisposition("block");
        scopedRule.setPriority(0);
        java.util.Map<String, Object> scope = new java.util.HashMap<>();
        scope.put("consumers", java.util.Arrays.asList("other_consumer"));
        scopedRule.setScope(scope);
        ruleStore.put(scopedRule);

        // 使用不匹配的consumer
        String body = buildMessages("包含测试关键词的内容");
        FenceContext ctx = new FenceContext("/v1/chat/completions", "test_consumer", "qwen-max");

        EngineResult result = ruleEngine.execute(body, ctx);

        // consumer不匹配，不应被拦截
        assertFalse("consumer不匹配时不应被拦截", result.isBlocked());

        // 清理测试规则
        ruleStore.remove("scoped_test");
    }

    // ========== 空内容测试 ==========

    @Test
    public void testE2E_emptyContent() {
        String body = buildMessages("");
        FenceContext ctx = new FenceContext("/v1/chat/completions", "test", "qwen-max");

        EngineResult result = ruleEngine.execute(body, ctx);

        assertFalse("不应被拦截", result.isBlocked());
        assertFalse("不应被脱敏", result.isDesensitized());
        assertEquals("应原样返回", body, result.getBody());
    }

    // ========== 无敏感数据测试 ==========

    @Test
    public void testE2E_noSensitiveData() {
        String body = buildMessages("今天天气真好");
        FenceContext ctx = new FenceContext("/v1/chat/completions", "test", "qwen-max");

        EngineResult result = ruleEngine.execute(body, ctx);

        assertFalse("不应被拦截", result.isBlocked());
        assertFalse("不应被脱敏", result.isDesensitized());
        assertEquals("应原样返回", body, result.getBody());
    }

    // ========== 围栏开关测试 ==========

    @Test
    public void testE2E_fenceDisabled() {
        // 关闭围栏
        demoProperties.getFence().setEnabled(false);

        try {
            String body = buildMessages("密码: abc123");
            FenceContext ctx = new FenceContext("/v1/chat/completions", "test", "qwen-max");

            // 围栏关闭时，规则引擎仍然会执行（在Controller层判断开关）
            // 这里测试规则引擎本身的行为
            EngineResult result = ruleEngine.execute(body, ctx);
            assertTrue("规则引擎仍会检测到密码", result.isBlocked());
        } finally {
            demoProperties.getFence().setEnabled(true);
        }
    }

    // ========== 辅助方法 ==========

    private String buildMessages(String content) {
        return String.format(
                "{\"model\":\"qwen-max\",\"messages\":[{\"role\":\"user\",\"content\":\"%s\"}]}",
                content.replace("\"", "\\\""));
    }
}
