# Oracle DDL 执行说明

## 文件清单

| 文件 | 说明 |
|---|---|
| `oracle_ddl.sql` | 主DDL脚本（建表+索引+序列+预置数据） |

## 执行前准备

### 1. 确认数据库环境

```sql
-- 检查Oracle版本（建议11g+）
SELECT * FROM v$version;

-- 检查表空间使用情况
SELECT tablespace_name, used_space, tablespace_size, used_percent
FROM dba_tablespace_usage_metrics;
```

### 2. 确认执行用户权限

执行用户需要以下权限：
- `CREATE TABLE`
- `CREATE INDEX`
- `CREATE SEQUENCE`
- `INSERT` / `UPDATE` / `DELETE`
- 对目标表空间的配额

```sql
-- 检查当前用户权限
SELECT * FROM user_sys_privs WHERE privilege LIKE '%CREATE%';
SELECT * FROM user_role_privs;
```

### 3. 确认表空间

建议创建专用表空间：

```sql
-- 创建表空间（示例）
CREATE TABLESPACE ts_ai_fence
DATAFILE '/u01/oracle/oradata/TS_AI_FENCE01.dbf' SIZE 100M
AUTOEXTEND ON NEXT 100M MAXSIZE 10G;

-- 创建用户并授权
CREATE USER ai_fence IDENTIFIED BY <password>
DEFAULT TABLESPACE ts_ai_fence
TEMPORARY TABLESPACE temp;

GRANT CONNECT, RESOURCE TO ai_fence;
GRANT CREATE TABLE, CREATE INDEX, CREATE SEQUENCE TO ai_fence;
ALTER USER ai_fence QUOTA UNLIMITED ON ts_ai_fence;
```

## 执行步骤

### 方式一：SQL*Plus 执行

```bash
# 连接数据库
sqlplus ai_fence/<password>@<host>:<port>/<service_name>

# 执行DDL脚本
@/path/to/oracle_ddl.sql

# 检查执行结果
SELECT table_name FROM user_tables;
SELECT COUNT(*) FROM desensitization_rule;
```

### 方式二：PL/SQL Developer 执行

1. 打开 PL/SQL Developer
2. 连接到目标数据库
3. 打开 `oracle_ddl.sql` 文件
4. 按 F8 执行脚本
5. 检查输出窗口是否有错误

### 方式三：DBeaver 执行

1. 打开 DBeaver
2. 连接到 Oracle 数据库
3. 打开 `oracle_ddl.sql` 文件
4. 点击"执行SQL脚本"按钮
5. 检查执行日志

## 执行后验证

### 1. 验证表创建

```sql
-- 检查4张表是否创建成功
SELECT table_name, num_rows, last_analyzed
FROM user_tables
WHERE table_name IN ('DESENSITIZATION_RULE', 'DESENSITIZATION_LOG', 'VIOLATION_LOG', 'RULE_CHANGE_LOG');
```

### 2. 验证预置规则数据

```sql
-- 检查9条预置规则
SELECT rule_id, rule_name, disposition, priority, enabled
FROM desensitization_rule
WHERE deleted = 0
ORDER BY priority;

-- 预期结果：
-- rule_005  密码验证码拦截  block      1   1
-- rule_006  API Key拦截    block      2   1
-- rule_001  身份证号脱敏    mask_pass  5   1
-- rule_002  手机号脱敏      mask_pass  10  1
-- rule_004  银行卡号脱敏    mask_pass  15  1
-- rule_003  邮箱脱敏        mask_pass  20  1
-- rule_008  统一社会信用代码 mask_pass  25  1
-- rule_007  内网IP替换      replace    30  1
-- rule_009  HTML标签删除    delete     35  1
```

### 3. 验证索引

```sql
-- 检查索引是否创建成功
SELECT index_name, table_name, column_name, column_position
FROM user_ind_columns
WHERE table_name IN ('DESENSITIZATION_LOG', 'VIOLATION_LOG', 'RULE_CHANGE_LOG')
ORDER BY table_name, index_name, column_position;
```

### 4. 验证序列

```sql
-- 检查序列是否创建成功
SELECT sequence_name, min_value, max_value, increment_by, last_number
FROM user_sequences
WHERE sequence_name LIKE 'SEQ_%';
```

### 5. 测试插入

```sql
-- 测试插入一条日志
INSERT INTO desensitization_log (
  log_id, request_id, rule_id, rule_name, field_path,
  mask_strategy, disposition, disposition_result, risk_level,
  original_hash, desensitized_len, cost_ms, route, consumer, model, timestamp
) VALUES (
  'test-001', 'req-001', 'rule_001', '身份证号脱敏', '$.messages[*].content',
  'mask_middle', 'mask_pass', 'pass', '低',
  'abc123hash', 10, 5, '/v1/chat/completions', 'test_user', 'qwen-max', SYSTIMESTAMP
);

-- 验证插入成功
SELECT * FROM desensitization_log WHERE log_id = 'test-001';

-- 清理测试数据
DELETE FROM desensitization_log WHERE log_id = 'test-001';
COMMIT;
```

## 数据保留周期配置

### 1. desensitization_log（180天）

建议创建定时任务，每月归档6个月前的数据：

```sql
-- 创建归档表（只需执行一次）
CREATE TABLE desensitization_log_archive AS
SELECT * FROM desensitization_log WHERE 1=0;

-- 每月执行的归档SQL
BEGIN
  -- 归档6个月前的数据
  INSERT INTO desensitization_log_archive
  SELECT * FROM desensitization_log
  WHERE timestamp < ADD_MONTHS(SYSTIMESTAMP, -6);

  -- 删除已归档数据
  DELETE FROM desensitization_log
  WHERE timestamp < ADD_MONTHS(SYSTIMESTAMP, -6);

  COMMIT;
END;
/
```

### 2. violation_log 和 rule_change_log（3年）

```sql
-- 每年执行的归档SQL
BEGIN
  -- 归档2年前的数据
  INSERT INTO violation_log_archive
  SELECT * FROM violation_log
  WHERE created_at < ADD_MONTHS(SYSTIMESTAMP, -24);

  DELETE FROM violation_log
  WHERE created_at < ADD_MONTHS(SYSTIMESTAMP, -24);

  COMMIT;
END;
/
```

## 常见问题

### Q1: ORA-00955: name is already used by an existing object

**原因**：表或索引已存在

**解决**：
```sql
-- 检查是否存在同名对象
SELECT object_name, object_type FROM user_objects
WHERE object_name IN ('DESENSITIZATION_RULE', 'DESENSITIZATION_LOG');

-- 如果存在，先删除再重建（谨慎操作！）
DROP TABLE desensitization_log CASCADE CONSTRAINTS;
DROP TABLE desensitization_rule CASCADE CONSTRAINTS;
-- ... 然后重新执行DDL
```

### Q2: ORA-01950: no privileges on tablespace

**原因**：用户没有表空间配额

**解决**：
```sql
-- 授予表空间配额
ALTER USER ai_fence QUOTA UNLIMITED ON ts_ai_fence;
-- 或指定配额
ALTER USER ai_fence QUOTA 100M ON ts_ai_fence;
```

### Q3: ORA-02270: no matching unique or primary key

**原因**：主键约束创建失败

**解决**：
```sql
-- 检查是否有重复数据
SELECT rule_id, COUNT(*)
FROM desensitization_rule
GROUP BY rule_id
HAVING COUNT(*) > 1;

-- 删除重复数据后重建主键
```

### Q4: 预置规则插入失败

**原因**：可能是字符转义问题

**解决**：
```sql
-- 检查正则表达式是否正确
SELECT rule_id, match_pattern
FROM desensitization_rule
WHERE rule_id = 'rule_007';

-- 如果正则有问题，手动更新
UPDATE desensitization_rule
SET match_pattern = '(10\.\d{1,3}\.\d{1,3}\.\d{1,3}|172\.(1[6-9]|2\d|3[01])\.\d{1,3}\.\d{1,3}|192\.168\.\d{1,3}\.\d{1,3})'
WHERE rule_id = 'rule_007';
COMMIT;
```

## 回滚方案

如果需要回滚，执行以下SQL：

```sql
-- 删除序列
DROP SEQUENCE seq_des_log_id;
DROP SEQUENCE seq_violation_id;
DROP SEQUENCE seq_rule_change_id;

-- 删除表（注意：会删除所有数据！）
DROP TABLE rule_change_log CASCADE CONSTRAINTS;
DROP TABLE violation_log CASCADE CONSTRAINTS;
DROP TABLE desensitization_log CASCADE CONSTRAINTS;
DROP TABLE desensitization_rule CASCADE CONSTRAINTS;

-- 删除归档表（如果存在）
DROP TABLE desensitization_log_archive CASCADE CONSTRAINTS;
DROP TABLE violation_log_archive CASCADE CONSTRAINTS;
DROP TABLE rule_change_log_archive CASCADE CONSTRAINTS;
```

## 性能优化建议

### 1. 分区表（推荐）

对于 `desensitization_log` 表，建议按月分区：

```sql
CREATE TABLE desensitization_log (
  -- ... 列定义 ...
)
PARTITION BY RANGE (timestamp)
INTERVAL (NUMTOYMINTERVAL(1, 'MONTH'))
(
  PARTITION p_init VALUES LESS THAN (TO_DATE('2024-01-01', 'YYYY-MM-DD'))
);
```

### 2. 索引优化

```sql
-- 复合索引（覆盖常用查询）
CREATE INDEX idx_des_log_route_ts ON desensitization_log (route, timestamp DESC);
CREATE INDEX idx_des_log_consumer_ts ON desensitization_log (consumer, timestamp DESC);
```

### 3. 统计信息收集

```sql
-- 定期收集统计信息
EXEC DBMS_STATS.GATHER_TABLE_STATS('AI_FENCE', 'DESENSITIZATION_LOG');
EXEC DBMS_STATS.GATHER_TABLE_STATS('AI_FENCE', 'DESENSITIZATION_RULE');
```

## 联系方式

如有问题，请联系：
- DBA：[待填写]
- 开发负责人：[待填写]
