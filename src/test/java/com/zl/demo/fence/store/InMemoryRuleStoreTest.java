package com.zl.demo.fence.store;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.zl.demo.fence.model.Rule;

/**
 * InMemoryRuleStore 单元测试
 */
public class InMemoryRuleStoreTest {

    private InMemoryRuleStore ruleStore;

    @Before
    public void setUp() {
        ruleStore = new InMemoryRuleStore();
        ruleStore.init();
    }

    @Test
    public void testInit_loadPresetRules() {
        // 初始化后应加载预置规则
        List<Rule> rules = ruleStore.getAll();

        assertTrue("应加载预置规则", rules.size() > 0);
        // 应有 9 条预置规则
        assertEquals("应有 9 条预置规则", 9, rules.size());
    }

    @Test
    public void testGetById() {
        Rule rule = ruleStore.getById("rule_001");

        assertNotNull("应能获取 rule_001", rule);
        assertEquals("rule_001", rule.getRuleId());
        assertEquals("身份证号脱敏", rule.getRuleName());
        assertEquals("mask_pass", rule.getDisposition());
    }

    @Test
    public void testGetById_notFound() {
        Rule rule = ruleStore.getById("non_existent");

        assertNull("不存在的规则应返回 null", rule);
    }

    @Test
    public void testGetEnabledSnapshot_sorted() {
        List<Rule> snapshot = ruleStore.getEnabledSnapshot();

        // 应按优先级排序（priority 越小越优先）
        for (int i = 0; i < snapshot.size() - 1; i++) {
            assertTrue("应按优先级排序",
                    snapshot.get(i).getPriority() <= snapshot.get(i + 1).getPriority());
        }
    }

    @Test
    public void testPut_addNewRule() {
        Rule newRule = new Rule();
        newRule.setRuleId("test_rule");
        newRule.setRuleName("测试规则");
        newRule.setMatchPattern("test_pattern");
        newRule.setDisposition("mask_pass");
        newRule.setMaskStrategy("mask_middle");
        newRule.setPriority(100);

        ruleStore.put(newRule);

        Rule retrieved = ruleStore.getById("test_rule");
        assertNotNull("应能获取新添加的规则", retrieved);
        assertEquals("测试规则", retrieved.getRuleName());
    }

    @Test
    public void testPut_updateExistingRule() {
        Rule rule = ruleStore.getById("rule_001");
        assertNotNull(rule);

        // 修改规则名称
        rule.setRuleName("修改后的名称");
        ruleStore.put(rule);

        Rule updated = ruleStore.getById("rule_001");
        assertEquals("修改后的名称", updated.getRuleName());
    }

    @Test
    public void testRemove() {
        Rule removed = ruleStore.remove("rule_009");

        assertNotNull("应返回被删除的规则", removed);
        assertNull("删除后应无法获取", ruleStore.getById("rule_009"));
    }

    @Test
    public void testRemove_notFound() {
        Rule removed = ruleStore.remove("non_existent");

        assertNull("删除不存在的规则应返回 null", removed);
    }

    @Test
    public void testToggle_disable() {
        ruleStore.toggle("rule_001", false);

        Rule rule = ruleStore.getById("rule_001");
        assertFalse("规则应被禁用", rule.isEnabled());

        // 禁用后不应出现在快照中
        List<Rule> snapshot = ruleStore.getEnabledSnapshot();
        for (Rule r : snapshot) {
            assertNotEquals("禁用的规则不应在快照中", "rule_001", r.getRuleId());
        }
    }

    @Test
    public void testToggle_enable() {
        // 先禁用
        ruleStore.toggle("rule_001", false);
        assertFalse(ruleStore.getById("rule_001").isEnabled());

        // 再启用
        ruleStore.toggle("rule_001", true);
        assertTrue(ruleStore.getById("rule_001").isEnabled());
    }

    @Test
    public void testVersion_increment() {
        long version1 = ruleStore.getVersion();

        // 添加规则
        Rule newRule = new Rule();
        newRule.setRuleId("version_test");
        newRule.setRuleName("版本测试");
        newRule.setMatchPattern("test");
        newRule.setDisposition("mask_pass");
        ruleStore.put(newRule);

        long version2 = ruleStore.getVersion();
        assertTrue("版本号应递增", version2 > version1);
    }

    @Test
    public void testPresetRules_dispositions() {
        // 验证预置规则的处置类型
        assertNotNull("rule_001 应为 mask_pass", ruleStore.getById("rule_001"));
        assertEquals("mask_pass", ruleStore.getById("rule_001").getDisposition());

        assertNotNull("rule_005 应为 block", ruleStore.getById("rule_005"));
        assertEquals("block", ruleStore.getById("rule_005").getDisposition());

        assertNotNull("rule_006 应为 block", ruleStore.getById("rule_006"));
        assertEquals("block", ruleStore.getById("rule_006").getDisposition());

        assertNotNull("rule_007 应为 replace", ruleStore.getById("rule_007"));
        assertEquals("replace", ruleStore.getById("rule_007").getDisposition());

        assertNotNull("rule_009 应为 delete", ruleStore.getById("rule_009"));
        assertEquals("delete", ruleStore.getById("rule_009").getDisposition());
    }

    @Test
    public void testPresetRules_scope() {
        // 预置规则的 scope 应为空（全量生效）
        Rule rule = ruleStore.getById("rule_001");
        assertNotNull(rule);
        assertTrue("预置规则 scope 应为空", rule.getScope().isEmpty());
    }
}
