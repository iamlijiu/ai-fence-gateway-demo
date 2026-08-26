package com.zl.demo.fence.api;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.zl.demo.fence.model.Rule;
import com.zl.demo.fence.store.InMemoryRuleStore;

/**
 * 规则管理 API（P0-6，最小形态：CRUD + 启停）
 * <p>
 * 对应接口设计：3.2 脱敏规则管理接口
 * </p>
 */
@RestController
@RequestMapping("/api/v1/desensitization/rules")
public class RuleController {

    private final InMemoryRuleStore ruleStore;

    public RuleController(InMemoryRuleStore ruleStore) {
        this.ruleStore = ruleStore;
    }

    /** 查询规则列表 */
    @GetMapping
    public ResponseEntity<List<Rule>> list() {
        return ResponseEntity.ok(ruleStore.getAll());
    }

    /** 查询单条规则 */
    @GetMapping("/{ruleId}")
    public ResponseEntity<Rule> get(@PathVariable String ruleId) {
        Rule rule = ruleStore.getById(ruleId);
        if (rule == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(rule);
    }

    /** 创建规则 */
    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@RequestBody Rule rule) {
        // 校验
        String error = validate(rule);
        if (error != null) {
            Map<String, Object> resp = new HashMap<>();
            resp.put("code", 400);
            resp.put("message", error);
            return ResponseEntity.badRequest().body(resp);
        }

        if (rule.getRuleId() == null || rule.getRuleId().isEmpty()) {
            rule.setRuleId("rule_" + System.currentTimeMillis());
        }
        ruleStore.put(rule);

        Map<String, Object> resp = new HashMap<>();
        resp.put("code", 0);
        resp.put("message", "success");
        Map<String, Object> data = new HashMap<>();
        data.put("rule_id", rule.getRuleId());
        data.put("created_at", java.time.Instant.now().toString());
        resp.put("data", data);
        return ResponseEntity.ok(resp);
    }

    /** 更新规则 */
    @PutMapping("/{ruleId}")
    public ResponseEntity<Map<String, Object>> update(@PathVariable String ruleId, @RequestBody Rule rule) {
        if (ruleStore.getById(ruleId) == null) {
            return ResponseEntity.notFound().build();
        }
        String error = validate(rule);
        if (error != null) {
            Map<String, Object> resp = new HashMap<>();
            resp.put("code", 400);
            resp.put("message", error);
            return ResponseEntity.badRequest().body(resp);
        }
        rule.setRuleId(ruleId);
        ruleStore.put(rule);

        Map<String, Object> resp = new HashMap<>();
        resp.put("code", 0);
        resp.put("message", "success");
        return ResponseEntity.ok(resp);
    }

    /** 删除规则 */
    @DeleteMapping("/{ruleId}")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable String ruleId) {
        Rule removed = ruleStore.remove(ruleId);
        if (removed == null) return ResponseEntity.notFound().build();

        Map<String, Object> resp = new HashMap<>();
        resp.put("code", 0);
        resp.put("message", "success");
        return ResponseEntity.ok(resp);
    }

    /** 启用/禁用规则 */
    @PatchMapping("/{ruleId}/status")
    public ResponseEntity<Map<String, Object>> toggle(@PathVariable String ruleId, @RequestBody Map<String, Boolean> body) {
        Rule rule = ruleStore.getById(ruleId);
        if (rule == null) return ResponseEntity.notFound().build();

        Boolean enabled = body.get("enabled");
        if (enabled == null) {
            Map<String, Object> resp = new HashMap<>();
            resp.put("code", 400);
            resp.put("message", "enabled 字段必填");
            return ResponseEntity.badRequest().body(resp);
        }
        ruleStore.toggle(ruleId, enabled);

        Map<String, Object> resp = new HashMap<>();
        resp.put("code", 0);
        resp.put("message", "success");
        return ResponseEntity.ok(resp);
    }

    /** 规则校验 */
    private String validate(Rule rule) {
        if (rule.getDisposition() == null) return "disposition 必填";
        if (!"block".equals(rule.getDisposition()) && !"mask_pass".equals(rule.getDisposition())
                && !"replace".equals(rule.getDisposition())) {
            return "disposition 仅支持 block / mask_pass / replace";
        }
        if ("mask_pass".equals(rule.getDisposition())) {
            if (rule.getMaskStrategy() == null) return "disposition=mask_pass 时 mask_strategy 必填";
        }
        if (rule.getMatchPattern() == null || rule.getMatchPattern().isEmpty()) {
            return "match_pattern 必填";
        }
        // 正则合法性校验
        try {
            java.util.regex.Pattern.compile(rule.getMatchPattern());
        } catch (Exception e) {
            return "match_pattern 正则不合法: " + e.getMessage();
        }
        return null;
    }
}
