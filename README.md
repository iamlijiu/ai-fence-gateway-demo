# AI 安全围栏网关 Demo

[![Java](https://img.shields.io/badge/Java-8-blue.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.0.8-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

## 项目简介

AI 安全围栏网关是一个用于保护 AI 接口调用的安全中间件，主要用于：
- 检测并脱敏请求中的敏感信息（身份证号、手机号、银行卡号等）
- 拦截包含密码、API Key 等高危信息的请求
- 提供完整的审计日志和监控能力

## 技术栈

- **Java 8**
- **Spring Boot 2.0.8**
- **MyBatis + Oracle 12c**
- **WebFlux（流式转发）**

## 功能特性

### 核心功能

| 功能 | 说明 |
|---|---|
| 规则匹配引擎 | 支持正则表达式、字段路径匹配、多规则优先级 |
| 敏感信息脱敏 | 掩码、替换、删除三种脱敏策略 |
| 请求拦截 | 检测到高危信息直接拦截 |
| 异常降级 | reject/passthrough 降级策略 + 熔断器 |
| 规则热加载 | 内存快照 + 版本号，变更即时生效 |
| 审计日志 | SHA-256 哈希记录，对齐数据库表结构 |

### 预置规则（9类）

| 规则ID | 名称 | 处置策略 | 优先级 |
|---|---|---|---|
| rule_005 | 密码验证码拦截 | block | 1 |
| rule_006 | API Key拦截 | block | 2 |
| rule_001 | 身份证号脱敏 | mask_pass | 5 |
| rule_002 | 手机号脱敏 | mask_pass | 10 |
| rule_004 | 银行卡号脱敏 | mask_pass | 15 |
| rule_003 | 邮箱脱敏 | mask_pass | 20 |
| rule_008 | 统一社会信用代码脱敏 | mask_pass | 25 |
| rule_007 | 内网IP替换 | replace | 30 |
| rule_009 | HTML标签删除 | delete | 35 |

## 项目结构

```
ai-fence-gateway-demo/
├── pom.xml                              # Maven 配置
├── README.md                            # 项目说明
├── LICENSE                              # Apache 2.0 许可证
├── sql/
│   ├── oracle_ddl.sql                   # Oracle DDL 脚本
│   └── README.md                        # DDL 执行说明
├── docs/
│   ├── oracle-setup.md                  # Oracle 配置指南
│   └── background/                      # 背景文档
│       ├── AI安全围栏-交付物导航.html
│       ├── AI安全围栏-一期范围表.html
│       ├── AI安全围栏-技术方案T1-T7.html
│       ├── AI安全围栏建设方案-定稿版.html
│       ├── AI安全围栏-敏感信息分类清单.html
│       ├── AI安全围栏-ZL-OPENID认证方案.html
│       ├── AI安全围栏-开发启动检查单.html
│       └── AI安全围栏-待决策清单.html
├── rules/
│   ├── preset-rules.json                # 预置规则集
│   └── README.md                        # 规则说明
└── src/
    ├── main/
    │   ├── java/com/zl/demo/
    │   │   ├── AiFenceGatewayDemoApplication.java
    │   │   ├── config/
    │   │   │   ├── DemoProperties.java
    │   │   │   └── HttpClientConfig.java
    │   │   ├── filter/
    │   │   │   └── ZlOpenIdFilter.java
    │   │   ├── controller/
    │   │   │   ├── ChatCompletionsController.java
    │   │   │   └── MockUpstreamController.java
    │   │   └── fence/
    │   │       ├── api/                 # API 接口
    │   │       ├── audit/               # 审计日志
    │   │       ├── context/             # 请求上下文
    │   │       ├── degrade/             # 熔断器
    │   │       ├── engine/              # 规则引擎
    │   │       ├── entity/              # 实体类
    │   │       ├── mapper/              # MyBatis Mapper
    │   │       ├── model/               # 模型类
    │   │       ├── monitor/             # 监控
    │   │       ├── store/               # 规则存储
    │   │       └── api/                 # API 接口
    │   └── resources/
    │       ├── application.yml          # 应用配置
    │       ├── mybatis-config.xml       # MyBatis 配置
    │       ├── mapper/                  # MyBatis XML
    │       └── rules/                   # 规则 JSON
    └── test/
        └── java/com/zl/demo/
            ├── engine/                  # 规则引擎测试
            ├── degrade/                 # 熔断器测试
            ├── monitor/                 # 监控测试
            ├── store/                   # 规则存储测试
            └── integration/             # 集成测试
```

## 快速开始

### 1. 环境要求

- Java 8
- Maven 3.6+
- Oracle 12c（可选，Demo 使用内存存储）

### 2. 构建项目

```bash
# 克隆项目
git clone https://github.com/yourusername/ai-fence-gateway-demo.git
cd ai-fence-gateway-demo

# 构建项目
mvn clean package -DskipTests

# 运行测试
mvn test
```

### 3. 启动应用

```bash
# 使用默认配置（内存存储）
java -jar target/ai-fence-gateway-demo-1.0.0.jar

# 使用 Oracle 数据源
export DB_PASSWORD=your_password
java -jar target/ai-fence-gateway-demo-1.0.0.jar
```

### 4. 验证接口

```bash
# 非流式请求
curl -X POST http://localhost:8080/v1/chat/completions \
  -H "Content-Type: application/json" \
  -H "ZL-OPENID: sbzj_device" \
  -d '{"model":"qwen-max","stream":false,"messages":[{"role":"user","content":"你好"}]}'

# 流式请求
curl -N -X POST http://localhost:8080/v1/chat/completions \
  -H "Content-Type: application/json" \
  -H "ZL-OPENID: sbzj_device" \
  -H "Accept: text/event-stream" \
  -d '{"model":"qwen-max","stream":true,"messages":[{"role":"user","content":"你好"}]}'

# 查询监控统计
curl http://localhost:8080/api/v1/metrics

# 查询规则列表
curl http://localhost:8080/api/v1/desensitization/rules
```

## API 接口

### 网关接口

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/v1/chat/completions` | OpenAI 兼容接口 |

### 规则管理接口

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/v1/desensitization/rules` | 查询规则列表 |
| GET | `/api/v1/desensitization/rules/{id}` | 查询单条规则 |
| POST | `/api/v1/desensitization/rules` | 创建规则 |
| PUT | `/api/v1/desensitization/rules/{id}` | 更新规则 |
| DELETE | `/api/v1/desensitization/rules/{id}` | 删除规则 |
| PATCH | `/api/v1/desensitization/rules/{id}/status` | 启用/禁用规则 |
| GET | `/api/v1/desensitization/rules/export` | 导出规则 |
| POST | `/api/v1/desensitization/rules/import` | 导入规则 |
| POST | `/api/v1/desensitization/rules/validate` | 验证规则 |

### 监控接口

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/v1/metrics` | 查询运行统计 |
| POST | `/api/v1/metrics/reset` | 重置计数器 |
| GET | `/api/v1/circuit-breaker` | 查询熔断器状态 |
| POST | `/api/v1/circuit-breaker/reset` | 重置熔断器 |
| GET | `/api/v1/fence/status` | 查询围栏配置 |

## 配置说明

### application.yml

```yaml
spring:
  datasource:
    url: jdbc:oracle:thin:@//localhost:1521/orcl
    username: ai_fence
    password: ${DB_PASSWORD}

demo:
  fence:
    enabled: true                    # 围栏总开关
    dry-run: false                   # 旁路观察模式
    degrade-strategy: passthrough    # 降级策略
    circuit-breaker-threshold: 5.0   # 熔断阈值
```

### 环境变量

| 变量 | 说明 | 默认值 |
|---|---|---|
| DB_PASSWORD | 数据库密码 | password |

## 测试

### 运行测试

```bash
# 运行所有测试
mvn test

# 运行特定测试类
mvn test -Dtest=RuleEngineTest

# 运行特定测试方法
mvn test -Dtest=RuleEngineTest#testPhone_normal
```

### 测试覆盖

| 测试类 | 测试数 | 覆盖范围 |
|---|---|---|
| RuleEngineTest | 35 | 9类敏感信息脱敏/拦截 |
| CircuitBreakerTest | 10 | 熔断器状态机 |
| MetricsCounterTest | 9 | 监控计数器 |
| InMemoryRuleStoreTest | 13 | 规则存储CRUD |
| ChatCompletionsIntegrationTest | 11 | 鉴权、拦截、监控API |
| EndToEndIntegrationTest | 16 | 完整请求流程 |
| MockUpstreamIntegrationTest | 5 | Mock上游响应 |

**总计：99个测试用例**

## 文档

### 背景文档

项目包含完整的背景文档，位于 `docs/background/` 目录：

| 文档 | 说明 |
|---|---|
| [AI安全围栏-交付物导航.html](docs/background/AI安全围栏-交付物导航.html) | 项目交付物清单 |
| [AI安全围栏-一期范围表.html](docs/background/AI安全围栏-一期范围表.html) | 一期功能范围定义 |
| [AI安全围栏-技术方案T1-T7.html](docs/background/AI安全围栏-技术方案T1-T7.html) | 技术方案详细设计 |
| [AI安全围栏建设方案-定稿版.html](docs/background/AI安全围栏建设方案-定稿版.html) | 建设方案定稿 |
| [AI安全围栏-敏感信息分类清单.html](docs/background/AI安全围栏-敏感信息分类清单.html) | 9类敏感信息定义 |
| [AI安全围栏-ZL-OPENID认证方案.html](docs/background/AI安全围栏-ZL-OPENID认证方案.html) | 认证方案设计 |
| [AI安全围栏-开发启动检查单.html](docs/background/AI安全围栏-开发启动检查单.html) | 开发前检查清单 |
| [AI安全围栏-待决策清单.html](docs/background/AI安全围栏-待决策清单.html) | 待决策事项 |

### 技术文档

- [Oracle DDL 执行说明](sql/README.md)
- [Oracle 配置指南](docs/oracle-setup.md)
- [规则集说明](rules/README.md)

## 许可证

本项目采用 [Apache License 2.0](LICENSE) 许可证。

## 联系方式

- 项目负责人：[待填写]
- 邮箱：[待填写]
