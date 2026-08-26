-- ============================================================
-- AI安全围栏 - Oracle DDL 执行脚本
-- 版本：v1.0
-- 创建日期：2024-XX-XX
-- 说明：一期上线所需4张表 + 预置规则数据
-- ============================================================

-- ============================================================
-- 1. desensitization_rule - 脱敏规则表
-- ============================================================
CREATE TABLE desensitization_rule (
  rule_id        VARCHAR2(64)   NOT NULL,
  rule_name      VARCHAR2(128)  NOT NULL,
  match_type     VARCHAR2(32)   NOT NULL,  -- regex / field_path / custom
  match_pattern  VARCHAR2(512),
  field_path     VARCHAR2(256),
  mask_strategy  VARCHAR2(32),             -- mask_middle / replace / delete
  mask_config    CLOB,                     -- JSON: keep_prefix/keep_suffix/mask_char
  disposition    VARCHAR2(32)   NOT NULL,  -- mask_pass / block / replace / delete
  priority       NUMBER(5)      NOT NULL,
  enabled        NUMBER(1)      DEFAULT 1 NOT NULL,
  scope          CLOB,                     -- JSON: routes/consumers/models
  rule_version   NUMBER(10)     DEFAULT 1 NOT NULL,  -- 灰度/回滚版本号
  gray_scope     CLOB,                     -- JSON: 灰度范围（空=全量）
  description    VARCHAR2(512),
  created_at     TIMESTAMP      DEFAULT SYSTIMESTAMP NOT NULL,
  updated_at     TIMESTAMP      DEFAULT SYSTIMESTAMP NOT NULL,
  created_by     VARCHAR2(64)   NOT NULL,
  updated_by     VARCHAR2(64),
  deleted        NUMBER(1)      DEFAULT 0 NOT NULL,
  CONSTRAINT pk_des_rule PRIMARY KEY (rule_id)
);

-- 添加表注释
COMMENT ON TABLE desensitization_rule IS 'AI安全围栏-脱敏规则表';
COMMENT ON COLUMN desensitization_rule.rule_id IS '规则唯一标识';
COMMENT ON COLUMN desensitization_rule.rule_name IS '规则名称';
COMMENT ON COLUMN desensitization_rule.match_type IS '匹配类型：regex/field_path/custom';
COMMENT ON COLUMN desensitization_rule.match_pattern IS '匹配模式（正则表达式）';
COMMENT ON COLUMN desensitization_rule.field_path IS 'JSON字段路径，如$.messages[*].content';
COMMENT ON COLUMN desensitization_rule.mask_strategy IS '脱敏策略：mask_middle/replace/delete';
COMMENT ON COLUMN desensitization_rule.mask_config IS '脱敏配置JSON：keep_prefix/keep_suffix/mask_char';
COMMENT ON COLUMN desensitization_rule.disposition IS '处置策略：mask_pass/block/replace/delete';
COMMENT ON COLUMN desensitization_rule.priority IS '优先级，数值越小越优先';
COMMENT ON COLUMN desensitization_rule.enabled IS '是否启用：1=启用，0=禁用';
COMMENT ON COLUMN desensitization_rule.scope IS '生效范围JSON：routes/consumers/models';
COMMENT ON COLUMN desensitization_rule.rule_version IS '规则版本号，用于灰度/回滚';
COMMENT ON COLUMN desensitization_rule.gray_scope IS '灰度范围JSON（空=全量生效）';
COMMENT ON COLUMN desensitization_rule.description IS '规则说明';
COMMENT ON COLUMN desensitization_rule.created_at IS '创建时间';
COMMENT ON COLUMN desensitization_rule.updated_at IS '更新时间';
COMMENT ON COLUMN desensitization_rule.created_by IS '创建人';
COMMENT ON COLUMN desensitization_rule.updated_by IS '更新人';
COMMENT ON COLUMN desensitization_rule.deleted IS '逻辑删除：0=正常，1=已删除';

-- ============================================================
-- 2. desensitization_log - 脱敏操作日志表
-- ============================================================
CREATE TABLE desensitization_log (
  log_id             VARCHAR2(64)  NOT NULL,
  request_id         VARCHAR2(64)  NOT NULL,
  rule_id            VARCHAR2(64)  NOT NULL,
  rule_name          VARCHAR2(128),
  field_path         VARCHAR2(256),
  mask_strategy      VARCHAR2(32),
  disposition        VARCHAR2(32),             -- 规则配置的处置策略
  disposition_result VARCHAR2(32)  NOT NULL,   -- 实际结果: pass/blocked/degraded/passthrough
  risk_level         VARCHAR2(16),             -- 高/中/低
  original_hash      VARCHAR2(128),            -- SHA-256(系统盐+原文)，仅比对审计
  desensitized_len   NUMBER(10),               -- 脱敏后内容长度
  cost_ms            NUMBER(10),               -- 处理耗时（毫秒）
  route              VARCHAR2(128)  NOT NULL,
  consumer           VARCHAR2(128),            -- ZL-OPENID 调用方标识
  model              VARCHAR2(128),
  timestamp          TIMESTAMP      NOT NULL,
  CONSTRAINT pk_des_log PRIMARY KEY (log_id)
);

-- 添加表注释
COMMENT ON TABLE desensitization_log IS 'AI安全围栏-脱敏操作日志表';
COMMENT ON COLUMN desensitization_log.log_id IS '日志唯一标识';
COMMENT ON COLUMN desensitization_log.request_id IS '请求唯一标识';
COMMENT ON COLUMN desensitization_log.rule_id IS '命中规则ID';
COMMENT ON COLUMN desensitization_log.rule_name IS '命中规则名称';
COMMENT ON COLUMN desensitization_log.field_path IS '命中字段路径';
COMMENT ON COLUMN desensitization_log.mask_strategy IS '脱敏策略';
COMMENT ON COLUMN desensitization_log.disposition IS '规则配置的处置策略';
COMMENT ON COLUMN desensitization_log.disposition_result IS '实际处置结果：pass/blocked/degraded/passthrough';
COMMENT ON COLUMN desensitization_log.risk_level IS '风险等级：高/中/低';
COMMENT ON COLUMN desensitization_log.original_hash IS '原文SHA-256哈希（带盐）';
COMMENT ON COLUMN desensitization_log.desensitized_len IS '脱敏后内容长度';
COMMENT ON COLUMN desensitization_log.cost_ms IS '处理耗时（毫秒）';
COMMENT ON COLUMN desensitization_log.route IS '请求路由';
COMMENT ON COLUMN desensitization_log.consumer IS '调用方标识（ZL-OPENID）';
COMMENT ON COLUMN desensitization_log.model IS '模型名称';
COMMENT ON COLUMN desensitization_log.timestamp IS '日志时间';

-- 创建索引
CREATE INDEX idx_des_log_req  ON desensitization_log (request_id);
CREATE INDEX idx_des_log_rt   ON desensitization_log (route, timestamp);
CREATE INDEX idx_des_log_cons ON desensitization_log (consumer, timestamp);
CREATE INDEX idx_des_log_rule ON desensitization_log (rule_id, timestamp);
CREATE INDEX idx_des_log_ts   ON desensitization_log (timestamp);

-- ============================================================
-- 3. violation_log - 违规记录表（二期启用，表先建）
-- ============================================================
CREATE TABLE violation_log (
  violation_id    VARCHAR2(64)  NOT NULL,
  request_id      VARCHAR2(64)  NOT NULL,
  model           VARCHAR2(128),
  route           VARCHAR2(128),
  consumer        VARCHAR2(128),
  violation_type  VARCHAR2(64)  NOT NULL,  -- sensitive_word/script_injection/politics/porn/terror
  risk_level      VARCHAR2(16)  NOT NULL,  -- 高/中/低
  content_hash    VARCHAR2(128),           -- 违规内容哈希，不存明文
  hit_snippet     VARCHAR2(512),           -- 脱敏后的命中片段（限长）
  detect_mode     VARCHAR2(16),            -- rule / semantic
  feedback_status VARCHAR2(16)  DEFAULT 'pending',  -- pending / sent / failed
  created_at      TIMESTAMP     DEFAULT SYSTIMESTAMP NOT NULL,
  CONSTRAINT pk_violation PRIMARY KEY (violation_id)
);

-- 添加表注释
COMMENT ON TABLE violation_log IS 'AI安全围栏-违规记录表（二期启用）';
COMMENT ON COLUMN violation_log.violation_id IS '违规记录唯一标识';
COMMENT ON COLUMN violation_log.request_id IS '请求唯一标识';
COMMENT ON COLUMN violation_log.model IS '模型名称';
COMMENT ON COLUMN violation_log.route IS '请求路由';
COMMENT ON COLUMN violation_log.consumer IS '调用方标识';
COMMENT ON COLUMN violation_log.violation_type IS '违规类型：sensitive_word/script_injection/politics等';
COMMENT ON COLUMN violation_log.risk_level IS '风险等级：高/中/低';
COMMENT ON COLUMN violation_log.content_hash IS '违规内容哈希（不存明文）';
COMMENT ON COLUMN violation_log.hit_snippet IS '脱敏后的命中片段（限长512字符）';
COMMENT ON COLUMN violation_log.detect_mode IS '检测模式：rule（规则）/semantic（语义）';
COMMENT ON COLUMN violation_log.feedback_status IS '反馈状态：pending/sent/failed';
COMMENT ON COLUMN violation_log.created_at IS '创建时间';

-- 创建索引
CREATE INDEX idx_viol_req  ON violation_log (request_id);
CREATE INDEX idx_viol_type ON violation_log (violation_type, created_at);
CREATE INDEX idx_viol_ts   ON violation_log (created_at);

-- ============================================================
-- 4. rule_change_log - 规则变更日志表
-- ============================================================
CREATE TABLE rule_change_log (
  change_id     VARCHAR2(64) NOT NULL,
  rule_id       VARCHAR2(64) NOT NULL,
  action        VARCHAR2(16) NOT NULL,  -- create/update/delete/enable/disable/gray/rollback
  rule_version  NUMBER(10),
  operator      VARCHAR2(64) NOT NULL,
  change_detail CLOB,                   -- 变更前后 JSON 快照
  created_at    TIMESTAMP   DEFAULT SYSTIMESTAMP NOT NULL,
  CONSTRAINT pk_rule_change PRIMARY KEY (change_id)
);

-- 添加表注释
COMMENT ON TABLE rule_change_log IS 'AI安全围栏-规则变更日志表';
COMMENT ON COLUMN rule_change_log.change_id IS '变更记录唯一标识';
COMMENT ON COLUMN rule_change_log.rule_id IS '规则ID';
COMMENT ON COLUMN rule_change_log.action IS '操作类型：create/update/delete/enable/disable/gray/rollback';
COMMENT ON COLUMN rule_change_log.rule_version IS '规则版本号';
COMMENT ON COLUMN rule_change_log.operator IS '操作人';
COMMENT ON COLUMN rule_change_log.change_detail IS '变更详情JSON（变更前后快照）';
COMMENT ON COLUMN rule_change_log.created_at IS '创建时间';

-- 创建索引
CREATE INDEX idx_rule_chg ON rule_change_log (rule_id, created_at);
CREATE INDEX idx_rule_chg_ts ON rule_change_log (created_at);

-- ============================================================
-- 5. 初始化预置规则数据（9类敏感信息）
-- ============================================================

-- #1 身份证号脱敏
INSERT INTO desensitization_rule (
  rule_id, rule_name, match_type, match_pattern, field_path,
  disposition, mask_strategy, mask_config, priority, enabled, scope,
  rule_version, description, created_by
) VALUES (
  'rule_001', '身份证号脱敏', 'regex', '\d{17}[\dXx]', '$.messages[*].content',
  'mask_pass', 'mask_middle', '{"keep_prefix":6,"keep_suffix":4,"mask_char":"*"}',
  5, 1, '{}',
  1, '匹配18位身份证号（最后一位可以是X），保留前6位后4位，中间用*替换', 'system'
);

-- #2 手机号脱敏
INSERT INTO desensitization_rule (
  rule_id, rule_name, match_type, match_pattern, field_path,
  disposition, mask_strategy, mask_config, priority, enabled, scope,
  rule_version, description, created_by
) VALUES (
  'rule_002', '手机号脱敏', 'regex', '(?<!\d)1[3-9]\d{9}(?!\d)', '$.messages[*].content',
  'mask_pass', 'mask_middle', '{"keep_prefix":3,"keep_suffix":4,"mask_char":"*"}',
  10, 1, '{}',
  1, '匹配11位手机号（13x-19x开头），使用负向前瞻避免匹配身份证号子串', 'system'
);

-- #3 邮箱脱敏
INSERT INTO desensitization_rule (
  rule_id, rule_name, match_type, match_pattern, field_path,
  disposition, mask_strategy, mask_config, priority, enabled, scope,
  rule_version, description, created_by
) VALUES (
  'rule_003', '邮箱脱敏', 'regex', '[a-zA-Z0-9._%+\-]+@[a-zA-Z0-9.\-]+\.[a-zA-Z]{2,}', '$.messages[*].content',
  'mask_pass', 'mask_middle', '{"keep_prefix":1,"keep_suffix":0,"mask_char":"*"}',
  20, 1, '{}',
  1, '匹配标准邮箱格式，保留第1个字符，其余用*替换', 'system'
);

-- #4 银行卡号脱敏
INSERT INTO desensitization_rule (
  rule_id, rule_name, match_type, match_pattern, field_path,
  disposition, mask_strategy, mask_config, priority, enabled, scope,
  rule_version, description, created_by
) VALUES (
  'rule_004', '银行卡号脱敏', 'regex', '[1-9]\d{15,18}', '$.messages[*].content',
  'mask_pass', 'mask_middle', '{"keep_prefix":4,"keep_suffix":4,"mask_char":"*"}',
  15, 1, '{}',
  1, '匹配16-19位银行卡号，保留前4位后4位，中间用*替换', 'system'
);

-- #5 密码验证码拦截
INSERT INTO desensitization_rule (
  rule_id, rule_name, match_type, match_pattern, field_path,
  disposition, mask_strategy, mask_config, priority, enabled, scope,
  rule_version, description, created_by
) VALUES (
  'rule_005', '密码验证码拦截', 'regex', '(密码|验证码|password|token)[：: =]+\S+', '$.messages[*].content',
  'block', NULL, NULL,
  1, 1, '{}',
  1, '匹配包含密码/验证码/token关键词的内容，直接拦截请求（高危信息）', 'system'
);

-- #6 API Key拦截
INSERT INTO desensitization_rule (
  rule_id, rule_name, match_type, match_pattern, field_path,
  disposition, mask_strategy, mask_config, priority, enabled, scope,
  rule_version, description, created_by
) VALUES (
  'rule_006', 'API Key拦截', 'regex', '(sk\-[A-Za-z0-9]{20,}|Bearer\s+[A-Za-z0-9._~+/\\-]+=*)', '$.messages[*].content',
  'block', NULL, NULL,
  2, 1, '{}',
  1, '匹配OpenAI API Key（sk-开头20+字符）或Bearer Token，直接拦截', 'system'
);

-- #7 内网IP替换
INSERT INTO desensitization_rule (
  rule_id, rule_name, match_type, match_pattern, field_path,
  disposition, mask_strategy, mask_config, priority, enabled, scope,
  rule_version, description, created_by
) VALUES (
  'rule_007', '内网IP替换', 'regex', '(10\.\d{1,3}\.\d{1,3}\.\d{1,3}|172\.(1[6-9]|2\d|3[01])\.\d{1,3}\.\d{1,3}|192\.168\.\d{1,3}\.\d{1,3})', '$.messages[*].content',
  'replace', 'replace', NULL,
  30, 1, '{}',
  1, '匹配内网IP地址（10.x/172.16-31.x/192.168.x），替换为[REDACTED]', 'system'
);

-- #8 统一社会信用代码脱敏
INSERT INTO desensitization_rule (
  rule_id, rule_name, match_type, match_pattern, field_path,
  disposition, mask_strategy, mask_config, priority, enabled, scope,
  rule_version, description, created_by
) VALUES (
  'rule_008', '统一社会信用代码脱敏', 'regex', '[0-9A-HJ-NPQRTUWXY]{18}', '$.messages[*].content',
  'mask_pass', 'mask_middle', '{"keep_prefix":2,"keep_suffix":4,"mask_char":"*"}',
  25, 1, '{}',
  1, '匹配18位统一社会信用代码，保留前2位后4位，中间用*替换', 'system'
);

-- #9 HTML标签删除
INSERT INTO desensitization_rule (
  rule_id, rule_name, match_type, match_pattern, field_path,
  disposition, mask_strategy, mask_config, priority, enabled, scope,
  rule_version, description, created_by
) VALUES (
  'rule_009', 'HTML标签删除', 'regex', '<[^>]+>', '$.messages[*].content',
  'delete', 'delete', NULL,
  35, 1, '{}',
  1, '匹配HTML标签（<...>），直接删除标签内容（保留文本）', 'system'
);

COMMIT;

-- ============================================================
-- 6. 创建序列（用于生成日志ID）
-- ============================================================

CREATE SEQUENCE seq_des_log_id
  START WITH 1
  INCREMENT BY 1
  NOCACHE
  NOCYCLE;

CREATE SEQUENCE seq_violation_id
  START WITH 1
  INCREMENT BY 1
  NOCACHE
  NOCYCLE;

CREATE SEQUENCE seq_rule_change_id
  START WITH 1
  INCREMENT BY 1
  NOCACHE
  NOCYCLE;

-- ============================================================
-- 7. 数据保留周期配置说明
-- ============================================================
/*
数据保留周期（按审计要求）：

1. desensitization_log: 180天（可归档冷存储）
   - 建议创建定时任务，每月归档6个月前的数据到冷存储
   - 归档后删除原表数据

2. violation_log: 3年（对应"违规记录持久化保存"）
   - 建议创建定时任务，每年归档2年前的数据

3. rule_change_log: 3年（审计要求）
   - 建议创建定时任务，每年归档2年前的数据

4. desensitization_rule: 永久保留（逻辑删除）
   - deleted=1 的记录可定期清理（建议保留1年）

归档SQL示例（按月归档desensitization_log）：
-- 创建归档表（结构相同）
CREATE TABLE desensitization_log_archive AS SELECT * FROM desensitization_log WHERE 1=0;

-- 归档6个月前的数据
INSERT INTO desensitization_log_archive
SELECT * FROM desensitization_log WHERE timestamp < ADD_MONTHS(SYSTIMESTAMP, -6);

-- 删除已归档数据
DELETE FROM desensitization_log WHERE timestamp < ADD_MONTHS(SYSTIMESTAMP, -6);
COMMIT;
*/

-- ============================================================
-- 8. 统计查询SQL示例
-- ============================================================
/*
-- 按路由统计脱敏量（最近24小时）
SELECT route, COUNT(*) as count
FROM desensitization_log
WHERE timestamp > SYSTIMESTAMP - 1
GROUP BY route
ORDER BY count DESC;

-- 按规则统计命中量（最近24小时）
SELECT rule_id, rule_name, COUNT(*) as count
FROM desensitization_log
WHERE timestamp > SYSTIMESTAMP - 1
GROUP BY rule_id, rule_name
ORDER BY count DESC;

-- 按业务方统计拦截量（最近24小时）
SELECT consumer, COUNT(*) as blocked_count
FROM desensitization_log
WHERE disposition_result = 'blocked'
  AND timestamp > SYSTIMESTAMP - 1
GROUP BY consumer
ORDER BY blocked_count DESC;

-- 查询高风险日志（最近1小时）
SELECT log_id, request_id, rule_id, rule_name, risk_level, timestamp
FROM desensitization_log
WHERE risk_level = '高'
  AND timestamp > SYSTIMESTAMP - 1/24
ORDER BY timestamp DESC;

-- 统计平均处理耗时（按路由）
SELECT route,
       AVG(cost_ms) as avg_cost_ms,
       MAX(cost_ms) as max_cost_ms,
       COUNT(*) as request_count
FROM desensitization_log
WHERE timestamp > SYSTIMESTAMP - 1
GROUP BY route;
*/

-- ============================================================
-- 9. 验证SQL
-- ============================================================

-- 验证表创建
SELECT table_name FROM user_tables
WHERE table_name IN ('DESENSITIZATION_RULE', 'DESENSITIZATION_LOG', 'VIOLATION_LOG', 'RULE_CHANGE_LOG');

-- 验证预置规则数据
SELECT rule_id, rule_name, disposition, priority
FROM desensitization_rule
WHERE deleted = 0
ORDER BY priority;

-- 验证索引
SELECT index_name, table_name, column_name
FROM user_ind_columns
WHERE table_name IN ('DESENSITIZATION_LOG', 'VIOLATION_LOG', 'RULE_CHANGE_LOG')
ORDER BY table_name, index_name;

-- 验证序列
SELECT sequence_name, last_number
FROM user_sequences
WHERE sequence_name LIKE 'SEQ_%';
