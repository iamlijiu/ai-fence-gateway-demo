package com.zl.demo.fence.api;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.zl.demo.fence.model.Rule;
import com.zl.demo.fence.store.InMemoryRuleStore;
import com.zl.demo.fence.store.JsonRuleLoader;

/**
 * 规则导入/导出 API
 * <p>
 * 支持从JSON文件导入规则、导出当前规则为JSON格式。
 * 用于：
 * 1. 初始规则集加载
 * 2. 规则备份与恢复
 * 3. 规则版本管理
 * </p>
 */
@RestController
@RequestMapping("/api/v1/desensitization/rules")
public class RuleImportExportController {

    private static final Logger log = LoggerFactory.getLogger(RuleImportExportController.class);

    private final InMemoryRuleStore ruleStore;
    private final JsonRuleLoader jsonRuleLoader;

    public RuleImportExportController(InMemoryRuleStore ruleStore, JsonRuleLoader jsonRuleLoader) {
        this.ruleStore = ruleStore;
        this.jsonRuleLoader = jsonRuleLoader;
    }

    /**
     * 导出当前所有规则为JSON格式
     */
    @GetMapping("/export")
    public ResponseEntity<Map<String, Object>> exportRules() {
        List<Rule> rules = ruleStore.getAll();

        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("message", "success");

        Map<String, Object> data = new HashMap<>();
        data.put("version", "1.0.0");
        data.put("description", "AI安全围栏规则导出");
        data.put("total", rules.size());
        data.put("rules", rules.stream().map(this::ruleToMap).collect(java.util.stream.Collectors.toList()));
        result.put("data", data);

        return ResponseEntity.ok(result);
    }

    /**
     * 从ClassPath导入预置规则
     *
     * @param classpath 资源路径（默认：rules/preset-rules.json）
     * @param覆盖 true=覆盖现有规则，false=仅导入新规则
     */
    @PostMapping("/import")
    public ResponseEntity<Map<String, Object>> importRules(
            @RequestParam(defaultValue = "rules/preset-rules.json") String classpath,
            @RequestParam(defaultValue = "false") boolean overwrite) {

        Map<String, Object> result = new HashMap<>();

        // 验证JSON格式
        if (!jsonRuleLoader.validate(classpath)) {
            result.put("code", 400);
            result.put("message", "JSON格式验证失败");
            return ResponseEntity.badRequest().body(result);
        }

        // 加载规则
        List<Rule> rules = jsonRuleLoader.loadFromClasspath(classpath);
        if (rules.isEmpty()) {
            result.put("code", 400);
            result.put("message", "未加载到任何规则");
            return ResponseEntity.badRequest().body(result);
        }

        // 导入规则
        int imported = 0;
        int skipped = 0;
        for (Rule rule : rules) {
            if (overwrite || ruleStore.getById(rule.getRuleId()) == null) {
                ruleStore.put(rule);
                imported++;
            } else {
                skipped++;
            }
        }

        log.info("[规则导入] 从 {} 导入 {} 条规则，跳过 {} 条", classpath, imported, skipped);

        result.put("code", 0);
        result.put("message", "导入成功");
        Map<String, Object> data = new HashMap<>();
        data.put("imported", imported);
        data.put("skipped", skipped);
        data.put("total", rules.size());
        result.put("data", data);

        return ResponseEntity.ok(result);
    }

    /**
     * 验证JSON文件格式
     */
    @PostMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateRules(
            @RequestParam(defaultValue = "rules/preset-rules.json") String classpath) {

        Map<String, Object> result = new HashMap<>();
        boolean valid = jsonRuleLoader.validate(classpath);

        result.put("code", valid ? 0 : 400);
        result.put("message", valid ? "格式验证通过" : "格式验证失败");
        result.put("classpath", classpath);

        return valid ? ResponseEntity.ok(result) : ResponseEntity.badRequest().body(result);
    }

    /**
     * 将Rule对象转换为Map
     */
    private Map<String, Object> ruleToMap(Rule rule) {
        Map<String, Object> map = new HashMap<>();
        map.put("rule_id", rule.getRuleId());
        map.put("rule_name", rule.getRuleName());
        map.put("match_type", rule.getMatchType());
        map.put("match_pattern", rule.getMatchPattern());
        map.put("field_path", rule.getFieldPath());
        map.put("disposition", rule.getDisposition());
        map.put("mask_strategy", rule.getMaskStrategy());
        map.put("mask_config", rule.getMaskConfig());
        map.put("priority", rule.getPriority());
        map.put("enabled", rule.isEnabled());
        map.put("scope", rule.getScope());
        return map;
    }
}
