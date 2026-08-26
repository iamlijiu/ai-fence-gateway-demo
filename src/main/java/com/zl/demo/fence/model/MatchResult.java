package com.zl.demo.fence.model;

import java.util.ArrayList;
import java.util.List;

/**
 * 规则匹配结果：一条规则在一次请求中的命中情况
 */
public class MatchResult {

    private Rule rule;
    private List<FieldHit> hits = new ArrayList<>();

    public MatchResult(Rule rule) {
        this.rule = rule;
    }

    public Rule getRule() { return rule; }

    public List<FieldHit> getHits() { return hits; }

    public void addHit(String fieldPath, String originalValue, String desensitizedValue) {
        hits.add(new FieldHit(fieldPath, originalValue, desensitizedValue));
    }

    public boolean isHit() { return !hits.isEmpty(); }

    /** 单个字段的命中详情 */
    public static class FieldHit {
        private final String fieldPath;
        private final String originalValue;
        private final String desensitizedValue;

        public FieldHit(String fieldPath, String originalValue, String desensitizedValue) {
            this.fieldPath = fieldPath;
            this.originalValue = originalValue;
            this.desensitizedValue = desensitizedValue;
        }

        public String getFieldPath() { return fieldPath; }
        public String getOriginalValue() { return originalValue; }
        public String getDesensitizedValue() { return desensitizedValue; }
    }
}
