# Oracle 数据库配置说明

## 1. 环境要求

- Oracle 12c R1 (12.1.0.2) 或更高版本
- Java 8
- MyBatis Spring Boot Starter 1.3.2

## 2. 安装 Oracle JDBC 驱动

由于 Oracle JDBC 驱动不在 Maven 中央仓库，需要手动安装到本地仓库。

### 方式一：手动安装到本地仓库

```bash
# 1. 下载 Oracle JDBC 驱动
# 从 Oracle 官网下载 ojdbc8.jar (适用于 Java 8)
# 下载地址：https://www.oracle.com/database/technologies/appdev/jdbc-downloads.html

# 2. 安装到本地 Maven 仓库
mvn install:install-file \
  -Dfile=ojdbc8-12.2.0.1.jar \
  -DgroupId=com.oracle \
  -DartifactId=ojdbc8 \
  -Dversion=12.2.0.1 \
  -Dpackaging=jar

# 3. 取消 pom.xml 中 Oracle 驱动的注释
```

### 方式二：使用公司私有仓库

```xml
<!-- 在 pom.xml 中添加私有仓库 -->
<repositories>
    <repository>
        <id>company-repo</id>
        <url>https://maven.company.com/repository/releases/</url>
    </repository>
</repositories>
```

### 方式三：使用 Oracle 官方 Maven 仓库

```xml
<!-- 在 pom.xml 中添加 Oracle 仓库 -->
<repositories>
    <repository>
        <id>oracle-maven</id>
        <url>https://maven.oracle.com</url>
    </repository>
</repositories>

<!-- 需要配置 Oracle Maven 仓库认证 -->
<settings>
    <servers>
        <server>
            <id>oracle-maven</id>
            <username>oracle-sso-username</username>
            <password>oracle-sso-password</password>
        </server>
    </servers>
</settings>
```

## 3. 配置数据源

### 3.1 application.yml 配置

```yaml
spring:
  datasource:
    # Oracle JDBC 连接字符串格式
    # 格式1: SID方式
    url: jdbc:oracle:thin:@//hostname:port:SID
    # 格式2: Service Name方式
    url: jdbc:oracle:thin:@//hostname:port/service_name
    # 格式3: TNS方式
    url: jdbc:oracle:thin:@(DESCRIPTION=(ADDRESS=(PROTOCOL=TCP)(HOST=hostname)(PORT=port))(CONNECT_DATA=(SERVICE_NAME=service_name)))

    username: ai_fence
    password: ${DB_PASSWORD:password}
    driver-class-name: oracle.jdbc.OracleDriver

    # HikariCP 连接池配置
    hikari:
      minimum-idle: 5
      maximum-pool-size: 20
      idle-timeout: 300000
      max-lifetime: 1800000
      connection-timeout: 30000
      pool-name: AI-Fence-HikariPool
```

### 3.2 环境变量配置

```bash
# 设置数据库密码环境变量
export DB_PASSWORD=your_password

# 或在启动时指定
java -jar ai-fence-gateway-demo-1.0.0.jar --DB_PASSWORD=your_password
```

### 3.3 多环境配置

创建 `application-dev.yml`、`application-test.yml`、`application-prod.yml`：

```yaml
# application-dev.yml
spring:
  datasource:
    url: jdbc:oracle:thin:@//localhost:1521/orcl
    username: ai_fence_dev
    password: dev_password
```

## 4. 连接池配置说明

### HikariCP 参数说明

| 参数 | 默认值 | 说明 |
|---|---|---|
| minimum-idle | 5 | 最小空闲连接数 |
| maximum-pool-size | 20 | 最大连接数 |
| idle-timeout | 300000 | 空闲连接超时时间（毫秒） |
| max-lifetime | 1800000 | 连接最大存活时间（毫秒） |
| connection-timeout | 30000 | 连接超时时间（毫秒） |

### 性能调优建议

```yaml
spring:
  datasource:
    hikari:
      # 生产环境建议配置
      minimum-idle: 10
      maximum-pool-size: 50
      idle-timeout: 600000
      max-lifetime: 3600000
      connection-timeout: 10000

      # Oracle 特定配置
      data-source-properties:
        oracle.jdbc.implicitStatementCacheSize: 20
        oracle.jdbc.defaultRowPrefetch: 20
```

## 5. MyBatis Oracle 兼容性配置

### 5.1 类型处理器

MyBatis 已配置以下类型处理器：

```xml
<!-- mybatis-config.xml -->
<typeHandlers>
    <!-- Java 8 LocalDateTime <-> Oracle TIMESTAMP -->
    <typeHandler handler="org.apache.ibatis.type.LocalDateTimeTypeHandler"
                 javaType="java.time.LocalDateTime"
                 jdbcType="TIMESTAMP"/>

    <!-- CLOB类型处理 -->
    <typeHandler handler="org.apache.ibatis.type.ClobTypeHandler"
                 javaType="java.lang.String"
                 jdbcType="CLOB"/>
</typeHandlers>
```

### 5.2 XML 中的 JDBC 类型

在 Mapper XML 中已添加 jdbcType 映射：

```xml
<!-- 示例 -->
#{ruleId, jdbcType=VARCHAR}
#{maskConfig, jdbcType=CLOB}
#{priority, jdbcType=INTEGER}
#{createdAt, jdbcType=TIMESTAMP}
```

## 6. Oracle 12c 特性使用

### 6.1 FETCH FIRST 语法

Oracle 12c 支持标准的 FETCH FIRST 语法：

```sql
-- 查询前10条记录
SELECT * FROM desensitization_rule
WHERE enabled = 1
ORDER BY priority
FETCH FIRST 10 ROWS ONLY
```

### 6.2 批量插入

Oracle 12c 支持 INSERT ALL 语法：

```sql
INSERT ALL
  INTO table (col1, col2) VALUES (val1, val2)
  INTO table (col1, col2) VALUES (val3, val4)
SELECT 1 FROM DUAL
```

### 6.3 序列使用

Oracle 使用序列生成自增ID：

```sql
-- 创建序列
CREATE SEQUENCE seq_des_log_id
  START WITH 1
  INCREMENT BY 1
  NOCACHE
  NOCYCLE;

-- 使用序列
INSERT INTO desensitization_log (log_id, ...)
VALUES (seq_des_log_id.NEXTVAL, ...);
```

## 7. 故障排查

### 7.1 连接失败

```
ORA-12514: TNS:listener does not currently know of service requested in connect descriptor
```

**解决方案**：
1. 检查服务名是否正确
2. 检查监听器状态：`lsnrctl status`
3. 检查防火墙设置

### 7.2 字符集问题

```
ORA-12899: value too large for column
```

**解决方案**：
1. 检查数据库字符集：`SELECT * FROM nls_database_parameters WHERE parameter LIKE '%CHARACTERSET%';`
2. 检查表字段长度
3. 使用 VARCHAR2 而不是 VARCHAR

### 7.3 权限问题

```
ORA-01031: insufficient privileges
```

**解决方案**：
```sql
-- 授予必要权限
GRANT CONNECT, RESOURCE TO ai_fence;
GRANT CREATE TABLE, CREATE INDEX, CREATE SEQUENCE TO ai_fence;
ALTER USER ai_fence QUOTA UNLIMITED ON ts_ai_fence;
```

### 7.4 连接池耗尽

```
HikariPool-1 - Connection is not available, request timed out
```

**解决方案**：
1. 增加 maximum-pool-size
2. 检查是否有连接泄漏
3. 减少 idle-timeout 和 max-lifetime

## 8. 监控配置

### 8.1 HikariCP 监控

```yaml
# 启用 HikariCP 监控
spring:
  datasource:
    hikari:
      register-mbeans: true
```

### 8.2 MyBatis SQL 日志

```yaml
logging:
  level:
    com.zl.demo.fence.mapper: DEBUG
    org.mybatis: DEBUG
```

### 8.3 Oracle 会话监控

```sql
-- 查看当前会话
SELECT sid, serial#, username, status, machine
FROM v$session
WHERE username = 'AI_FENCE';

-- 查看活跃连接数
SELECT COUNT(*) FROM v$session WHERE username = 'AI_FENCE' AND status = 'ACTIVE';
```

## 9. 备份与恢复

### 9.1 数据导出

```bash
# 导出表结构和数据
expdp ai_fence/password tables=DESENSITIZATION_RULE,DESENSITIZATION_LOG \
  dumpfile=ai_fence_backup.dmp directory=DATA_PUMP_DIR
```

### 9.2 数据导入

```bash
# 导入数据
impdp ai_fence/password dumpfile=ai_fence_backup.dmp directory=DATA_PUMP_DIR
```

## 10. 性能优化

### 10.1 索引优化

```sql
-- 复合索引（覆盖常用查询）
CREATE INDEX idx_des_log_route_ts ON desensitization_log (route, timestamp DESC);
CREATE INDEX idx_des_log_consumer_ts ON desensitization_log (consumer, timestamp DESC);
```

### 10.2 分区表（推荐）

```sql
-- 按月分区
CREATE TABLE desensitization_log (
  -- ... 列定义 ...
)
PARTITION BY RANGE (timestamp)
INTERVAL (NUMTOYMINTERVAL(1, 'MONTH'))
(
  PARTITION p_init VALUES LESS THAN (TO_DATE('2024-01-01', 'YYYY-MM-DD'))
);
```

### 10.3 统计信息收集

```sql
-- 定期收集统计信息
EXEC DBMS_STATS.GATHER_TABLE_STATS('AI_FENCE', 'DESENSITIZATION_LOG');
EXEC DBMS_STATS.GATHER_TABLE_STATS('AI_FENCE', 'DESENSITIZATION_RULE');
```

## 11. 安全建议

### 11.1 密码安全

- 使用环境变量存储密码
- 生产环境使用密码加密
- 定期轮换密码

### 11.2 网络安全

- 使用 SSL/TLS 加密连接
- 限制数据库访问IP
- 使用 VPN 或专用网络

### 11.3 权限最小化

```sql
-- 创建只读用户（用于报表）
CREATE USER ai_fence_readonly IDENTIFIED BY password;
GRANT CONNECT TO ai_fence_readonly;
GRANT SELECT ON desensitization_rule TO ai_fence_readonly;
GRANT SELECT ON desensitization_log TO ai_fence_readonly;
```
