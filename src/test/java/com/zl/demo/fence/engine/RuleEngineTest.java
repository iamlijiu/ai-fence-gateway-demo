package com.zl.demo.fence.engine;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zl.demo.fence.context.FenceContext;
import com.zl.demo.fence.engine.RuleEngine.EngineResult;
import com.zl.demo.fence.model.MatchResult;
import com.zl.demo.fence.store.InMemoryRuleStore;

/**
 * RuleEngine 单元测试
 * <p>
 * 覆盖 9 类敏感信息的正常/变体/伪装样例，验证：
 * 1. 规则匹配正确性（命中/未命中）
 * 2. 脱敏效果（mask_pass/replace/delete/block）
 * 3. scope 匹配（route/consumer/model）
 * 4. 优先级顺序（高优先级规则先匹配）
 * </p>
 */
public class RuleEngineTest {

    private RuleEngine ruleEngine;
    private ObjectMapper objectMapper;
    private FenceContext defaultContext;

    @Before
    public void setUp() {
        objectMapper = new ObjectMapper();
        InMemoryRuleStore ruleStore = new InMemoryRuleStore();
        ruleStore.init(); // 加载预置规则
        ruleEngine = new RuleEngine(ruleStore, objectMapper);
        defaultContext = new FenceContext("/v1/chat/completions", "test_consumer", "qwen-max");
    }

    // ========== #1 身份证号脱敏（rule_001）==========

    @Test
    public void testIdCard_normal() {
        // 正常身份证号（18位，最后一位是X）
        String body = buildMessages("我的身份证号是110101199003076534");
        EngineResult result = ruleEngine.execute(body, defaultContext);

        assertFalse("不应被拦截", result.isBlocked());
        assertTrue("应被脱敏", result.isDesensitized());
        // keep_prefix=6, keep_suffix=4, 中间8位用*替换
        assertTrue("应包含掩码", result.getBody().contains("110101********6534"));
    }

    @Test
    public void testIdCard_withX() {
        // 身份证号最后一位是X
        String body = buildMessages("身份证：11010119900307653X");
        EngineResult result = ruleEngine.execute(body, defaultContext);

        assertFalse("不应被拦截", result.isBlocked());
        assertTrue("应被脱敏", result.isDesensitized());
        // keep_prefix=6, keep_suffix=4
        assertTrue("应包含掩码", result.getBody().contains("110101********653X"));
    }

    @Test
    public void testIdCard_lowercaseX() {
        // 身份证号最后一位是小写x
        String body = buildMessages("身份证：11010119900307653x");
        EngineResult result = ruleEngine.execute(body, defaultContext);

        assertFalse("不应被拦截", result.isBlocked());
        assertTrue("应被脱敏", result.isDesensitized());
    }

    @Test
    public void testIdCard_shortNumber_notMatch() {
        // 14位数字（银行卡和身份证都不匹配）
        // 银行卡正则 [1-9]\d{15,18} 要求16-19位
        // 身份证正则 \d{17}[\dXx] 要求18位
        String body = buildMessages("数字12345678901234");
        EngineResult result = ruleEngine.execute(body, defaultContext);

        // 14位数字不匹配任何规则
        assertFalse("不应被脱敏", result.isDesensitized());
    }

    // ========== #2 手机号脱敏（rule_002）==========

    @Test
    public void testPhone_normal() {
        // 正常手机号
        String body = buildMessages("我的手机号是13812341234");
        EngineResult result = ruleEngine.execute(body, defaultContext);

        assertFalse("不应被拦截", result.isBlocked());
        assertTrue("应被脱敏", result.isDesensitized());
        assertTrue("应包含掩码", result.getBody().contains("138****1234"));
    }

    @Test
    public void testPhone_withPrefix_notMatch() {
        // 手机号前面有数字（不应匹配，避免身份证子串误匹配）
        String body = buildMessages("数字1213812341234");
        EngineResult result = ruleEngine.execute(body, defaultContext);

        // 由于 (?<!\d) 负向前瞻，不应匹配
        assertFalse("不应被脱敏", result.isDesensitized());
    }

    @Test
    public void testPhone_invalidPrefix() {
        // 无效前缀（12开头）
        String body = buildMessages("手机号12012341234");
        EngineResult result = ruleEngine.execute(body, defaultContext);

        assertFalse("不应被脱敏", result.isDesensitized());
    }

    @Test
    public void testPhone_validPrefixes() {
        // 测试各运营商号段
        String[] validPhones = {"13800001111", "15000001111", "18600001111", "19900001111"};
        for (String phone : validPhones) {
            String body = buildMessages("手机号" + phone);
            EngineResult result = ruleEngine.execute(body, defaultContext);
            assertTrue("手机号 " + phone + " 应被脱敏", result.isDesensitized());
        }
    }

    // ========== #3 邮箱脱敏（rule_003）==========

    @Test
    public void testEmail_normal() {
        // 正常邮箱
        String body = buildMessages("我的邮箱是zhangsan@example.com");
        EngineResult result = ruleEngine.execute(body, defaultContext);

        assertFalse("不应被拦截", result.isBlocked());
        assertTrue("应被脱敏", result.isDesensitized());
        // 掩码策略：keep_prefix=1, keep_suffix=0
        // zhangsan@example.com (20字符) -> z + 19个* = z*******************
        assertTrue("应包含掩码", result.getBody().contains("z*******************"));
    }

    @Test
    public void testEmail_withDots() {
        // 带点号的邮箱
        String body = buildMessages("邮箱：test.name+tag@sub.domain.com");
        EngineResult result = ruleEngine.execute(body, defaultContext);

        assertTrue("应被脱敏", result.isDesensitized());
    }

    @Test
    public void testEmail_invalid_notMatch() {
        // 无效邮箱格式
        String body = buildMessages("这不是邮箱@");
        EngineResult result = ruleEngine.execute(body, defaultContext);

        assertFalse("不应被脱敏", result.isDesensitized());
    }

    // ========== #4 银行卡号脱敏（rule_004）==========

    @Test
    public void testBankCard_16digits() {
        // 16位银行卡号
        String body = buildMessages("卡号6222021234567890");
        EngineResult result = ruleEngine.execute(body, defaultContext);

        assertTrue("应被脱敏", result.isDesensitized());
        // keep_prefix=4, keep_suffix=4, 中间8位用*替换
        assertTrue("应包含掩码", result.getBody().contains("6222********7890"));
    }

    @Test
    public void testBankCard_19digits() {
        // 19位银行卡号
        String body = buildMessages("卡号6222021234567890123456");
        EngineResult result = ruleEngine.execute(body, defaultContext);

        assertTrue("应被脱敏", result.isDesensitized());
    }

    @Test
    public void testBankCard_short_notMatch() {
        // 15位数字（不是银行卡号）
        String body = buildMessages("数字123456789012345");
        EngineResult result = ruleEngine.execute(body, defaultContext);

        assertFalse("不应被脱敏", result.isDesensitized());
    }

    // ========== #5 密码验证码拦截（rule_005）==========

    @Test
    public void testPassword_block() {
        // 包含密码
        String body = buildMessages("密码: abc123");
        EngineResult result = ruleEngine.execute(body, defaultContext);

        assertTrue("应被拦截", result.isBlocked());
    }

    @Test
    public void testPassword_chinese() {
        // 中文密码 - 正则要求 "密码" 后跟 ":|：|=|空格"
        String body = buildMessages("密码是：mypassword");
        EngineResult result = ruleEngine.execute(body, defaultContext);

        // "密码是：" 不匹配正则，因为 "是" 在 "密码" 和 "：" 之间
        assertFalse("不应被拦截（正则不匹配'密码是：'）", result.isBlocked());
    }

    @Test
    public void testVerificationCode_block() {
        // 验证码
        String body = buildMessages("验证码: 123456");
        EngineResult result = ruleEngine.execute(body, defaultContext);

        assertTrue("应被拦截", result.isBlocked());
    }

    @Test
    public void testToken_block() {
        // token
        String body = buildMessages("token=abc123xyz");
        EngineResult result = ruleEngine.execute(body, defaultContext);

        assertTrue("应被拦截", result.isBlocked());
    }

    @Test
    public void testPassword_notInContent_notBlock() {
        // 密码不在敏感上下文中
        String body = buildMessages("密码学是一门有趣的学科");
        EngineResult result = ruleEngine.execute(body, defaultContext);

        // 这个会匹配"密码:"，但实际上"密码学"不会被匹配
        // 因为正则要求 "密码" 后面跟 ":|：|=|空格"
        assertFalse("不应被拦截", result.isBlocked());
    }

    // ========== #6 API Key 拦截（rule_006）==========

    @Test
    public void testApiKey_openai_block() {
        // OpenAI API Key
        String body = buildMessages("使用sk-abcdefghijklmnopqrstuvwxyz123456");
        EngineResult result = ruleEngine.execute(body, defaultContext);

        assertTrue("应被拦截", result.isBlocked());
    }

    @Test
    public void testApiKey_bearer_block() {
        // Bearer token
        String body = buildMessages("Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9");
        EngineResult result = ruleEngine.execute(body, defaultContext);

        assertTrue("应被拦截", result.isBlocked());
    }

    @Test
    public void testApiKey_short_notBlock() {
        // 短 key（不符合 sk- 20+ 字符规则）
        String body = buildMessages("key: sk-abc123");
        EngineResult result = ruleEngine.execute(body, defaultContext);

        assertFalse("不应被拦截", result.isBlocked());
    }

    // ========== #7 内网IP替换（rule_007）==========

    @Test
    public void testInternalIp_10x() {
        // 10.x 内网 IP
        String body = buildMessages("服务器地址10.0.0.1");
        EngineResult result = ruleEngine.execute(body, defaultContext);

        assertFalse("不应被拦截", result.isBlocked());
        assertTrue("应被脱敏", result.isDesensitized());
        assertTrue("应被替换为 [REDACTED]", result.getBody().contains("[REDACTED]"));
    }

    @Test
    public void testInternalIp_172x() {
        // 172.16.x 内网 IP
        String body = buildMessages("地址172.16.0.100");
        EngineResult result = ruleEngine.execute(body, defaultContext);

        assertTrue("应被脱敏", result.isDesensitized());
        assertTrue("应被替换", result.getBody().contains("[REDACTED]"));
    }

    @Test
    public void testInternalIp_192x() {
        // 192.168.x 内网 IP
        String body = buildMessages("地址192.168.1.1");
        EngineResult result = ruleEngine.execute(body, defaultContext);

        assertTrue("应被脱敏", result.isDesensitized());
    }

    @Test
    public void testExternalIp_notMatch() {
        // 外网 IP
        String body = buildMessages("地址8.8.8.8");
        EngineResult result = ruleEngine.execute(body, defaultContext);

        assertFalse("外网IP不应被脱敏", result.isDesensitized());
    }

    // ========== #8 统一社会信用代码脱敏（rule_008）==========

    @Test
    public void testCreditCode_normal() {
        // 正常统一社会信用代码（18位）
        String body = buildMessages("信用代码91310115MA1K4L1C0J");
        EngineResult result = ruleEngine.execute(body, defaultContext);

        assertTrue("应被脱敏", result.isDesensitized());
        // keep_prefix=2, keep_suffix=4, 中间12位用*替换
        assertTrue("应包含掩码", result.getBody().contains("91************1C0J"));
    }

    @Test
    public void testCreditCode_invalid_notMatch() {
        // 包含无效字符（I、O、S、V、Z 不在信用代码字符集中）
        String body = buildMessages("代码91310115MA1K4L1IOZ");
        EngineResult result = ruleEngine.execute(body, defaultContext);

        // 包含无效字符，不应匹配
        assertFalse("不应被脱敏", result.isDesensitized());
    }

    // ========== #9 HTML标签删除（rule_009）==========

    @Test
    public void testHtmlTag_delete() {
        // 包含 HTML 标签
        String body = buildMessages("内容：<div class='test'>hello</div>");
        EngineResult result = ruleEngine.execute(body, defaultContext);

        assertFalse("不应被拦截", result.isBlocked());
        assertTrue("应被脱敏", result.isDesensitized());
        assertFalse("不应包含 div 标签", result.getBody().contains("<div"));
        assertFalse("不应包含 /div 标签", result.getBody().contains("</div>"));
        assertTrue("应保留文本内容", result.getBody().contains("hello"));
    }

    @Test
    public void testHtmlTag_multiple() {
        // 多个 HTML 标签
        String body = buildMessages("<p>段落1</p><p>段落2</p>");
        EngineResult result = ruleEngine.execute(body, defaultContext);

        assertTrue("应被脱敏", result.isDesensitized());
        assertFalse("不应包含 p 标签", result.getBody().contains("<p>"));
        assertTrue("应保留文本", result.getBody().contains("段落1"));
        assertTrue("应保留文本", result.getBody().contains("段落2"));
    }

    // ========== 综合场景测试 ==========

    @Test
    public void testMultipleRules_multipleHits() {
        // 同时包含手机号和邮箱
        String body = buildMessages("手机13812341234，邮箱test@example.com");
        EngineResult result = ruleEngine.execute(body, defaultContext);

        assertTrue("应被脱敏", result.isDesensitized());
        assertTrue("应包含手机号掩码", result.getBody().contains("138****1234"));
        // 邮箱掩码：keep_prefix=1, keep_suffix=0 -> t***************
        assertTrue("应包含邮箱掩码", result.getBody().contains("t***************"));
    }

    @Test
    public void testBlockPriority_overDesensitize() {
        // 同时包含密码（block）和手机号（mask_pass）
        // block 应优先级更高
        String body = buildMessages("密码: abc123，手机13812341234");
        EngineResult result = ruleEngine.execute(body, defaultContext);

        assertTrue("应被拦截（block 优先）", result.isBlocked());
    }

    @Test
    public void testNoSensitiveData_pass() {
        // 不包含敏感信息
        String body = buildMessages("今天天气真好");
        EngineResult result = ruleEngine.execute(body, defaultContext);

        assertFalse("不应被拦截", result.isBlocked());
        assertFalse("不应被脱敏", result.isDesensitized());
        assertEquals("应原样返回", buildMessages("今天天气真好"), result.getBody());
    }

    @Test
    public void testEmptyContent_pass() {
        // 空内容
        String body = buildMessages("");
        EngineResult result = ruleEngine.execute(body, defaultContext);

        assertFalse("不应被拦截", result.isBlocked());
        assertFalse("不应被脱敏", result.isDesensitized());
    }

    // ========== scope 匹配测试 ==========

    @Test
    public void testScope_consumerMismatch_notMatch() {
        // 创建只有特定 consumer 的规则
        InMemoryRuleStore ruleStore = new InMemoryRuleStore();
        ruleStore.init();

        // 手动添加一个 scope 限定 consumer 的规则
        com.zl.demo.fence.model.Rule scopedRule = new com.zl.demo.fence.model.Rule();
        scopedRule.setRuleId("scoped_rule");
        scopedRule.setRuleName("限定consumer的规则");
        scopedRule.setMatchPattern("测试关键词");
        scopedRule.setDisposition("block");
        scopedRule.setPriority(0);
        java.util.Map<String, Object> scope = new java.util.HashMap<>();
        scope.put("consumers", java.util.Arrays.asList("other_consumer"));
        scopedRule.setScope(scope);
        ruleStore.put(scopedRule);

        RuleEngine engine = new RuleEngine(ruleStore, objectMapper);
        String body = buildMessages("包含测试关键词的内容");
        EngineResult result = engine.execute(body, defaultContext);

        // consumer 不匹配，不应被拦截
        assertFalse("consumer 不匹配时不应被拦截", result.isBlocked());
    }

    // ========== 辅助方法 ==========

    /**
     * 构建 OpenAI 格式的 messages JSON
     */
    private String buildMessages(String content) {
        return "{\"model\":\"qwen-max\",\"messages\":[{\"role\":\"user\",\"content\":\"" +
                content.replace("\"", "\\\"") + "\"}]}";
    }
}
