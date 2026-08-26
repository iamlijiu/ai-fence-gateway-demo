# ai-fence-gateway-demo

**AI 安全围栏网关 Demo** —— 用于与现有网关工程对齐对照，验证一期核心功能。

技术栈与现有网关一致：**Spring Boot 2.0.8 + WebFlux 5.0.5 + Tomcat**，Java 8。
提供 **OpenAI 兼容**的单一入口 `POST /v1/chat/completions`，按请求体 `stream: true/false` 分流：

| stream | 转发方式 | 返回形式 |
|---|---|---|
| `true` | **WebClient**（响应式） | **SSE**（text/event-stream）逐块返回 |
| `false` | **RestTemplate**（阻塞） | **JSON** 一次性返回 |

内置 **Mock 上游**（`/mock/v1/chat/completions`，OpenAI 兼容格式），无外部 API Key 即可端到端验证；接入真实供应商只需改配置。

---

## 一、功能清单（P0 已补齐）

| 功能 | 状态 | 说明 |
|---|---|---|
| 规则匹配引擎 | ✅ | 字段路径 + 正则、9 类敏感信息、scope（route/consumer/model）、多规则优先级 |
| 掩码脱敏 | ✅ | `mask_pass` + `mask_middle`，如 `138****1234` |
| 替换脱敏 | ✅ | `replace` → `[REDACTED]` |
| 删除脱敏 | ✅ | `delete` → 直接移除匹配内容 |
| 直接拦截 | ✅ | `block` → 403 拒绝 |
| 异常降级 | ✅ | `reject`/`passthrough` 策略 + 降级原因记录 |
| 围栏总开关 | ✅ | `demo.fence.enabled` 一键启停 |
| dry-run 旁路观察 | ✅ | `demo.fence.dry-run` 只检测不执行 |
| 熔断器 | ✅ | 基于异常率的滑动窗口熔断（CLOSED→OPEN→HALF_OPEN） |
| 规则热加载 | ✅ | 内存快照 + 版本号，变更即时生效 |
| 规则管理 API | ✅ | CRUD + 启停（`/api/v1/desensitization/rules`） |
| 监控统计 API | ✅ | `/api/v1/metrics` + `/api/v1/circuit-breaker` |
| 审计日志 | ✅ | SHA-256 哈希记录原始值，不含明文 |
| 统一请求标识 | ✅ | UUID request_id 全链路传递 |
| ZL-OPENID 鉴权 | ✅ | 白名单校验 + 绕过路径 |

---

## 二、目录结构

```
ai-fence-gateway-demo/
├── pom.xml                              # Spring Boot 2.0.8.RELEASE，Java 8
└── src/main/
    ├── java/com/zl/demo/
    │   ├── AiFenceGatewayDemoApplication.java   # 启动类
    │   ├── config/
    │   │   ├── DemoProperties.java              # demo.* 配置（上游/超时/白名单/围栏开关）
    │   │   └── HttpClientConfig.java            # RestTemplate + WebClient
    │   ├── filter/
    │   │   └── ZlOpenIdFilter.java              # ZL-OPENID 简单认证（Servlet Filter）
    │   ├── controller/
    │   │   ├── ChatCompletionsController.java   # 网关入口（围栏管线 + stream 分流）
    │   │   └── MockUpstreamController.java      # 内置 Mock 上游
    │   └── fence/
    │       ├── engine/
    │       │   └── RuleEngine.java              # 规则匹配引擎
    │       ├── model/
    │       │   ├── Rule.java                    # 规则模型（disposition/scope）
    │       │   └── MatchResult.java             # 匹配结果
    │       ├── store/
    │       │   └── InMemoryRuleStore.java       # 规则存储 + 9 条预置规则
    │       ├── context/
    │       │   └── FenceContext.java            # 请求上下文
    │       ├── audit/
    │       │   └── AuditLogger.java             # 审计日志
    │       ├── degrade/
    │       │   └── CircuitBreaker.java          # 熔断器
    │       ├── monitor/
    │       │   └── MetricsCounter.java          # 监控计数器
    │       └── api/
    │           ├── RuleController.java          # 规则管理 API
    │           └── MetricsController.java       # 监控统计 API
    └── resources/
        └── application.yml                      # 配置
```

## 三、构建与运行

```bash
export JAVA_HOME=<Linux JDK8 路径>   # 例：/home/cbj/workspace/.tools/jdk8u502-b07
mvn -q -DskipTests clean package -Dmaven.repo.local=/home/cbj/workspace/.m2/repository
java -jar target/ai-fence-gateway-demo-1.0.0.jar
```

默认端口 `8080`，默认上游为内置 Mock（`http://localhost:8080/mock/v1`）。

## 四、验证用例（curl）

### 4.1 基础转发

**非流式（stream=false）→ 一次性 JSON**

```bash
curl -s -X POST http://localhost:8080/v1/chat/completions \
  -H "Content-Type: application/json" \
  -H "ZL-OPENID: sbzj_device" \
  -d '{"model":"qwen-max","stream":false,"messages":[{"role":"user","content":"你好"}]}'
```

**流式（stream=true）→ SSE 逐块返回**

```bash
curl -N -s -X POST http://localhost:8080/v1/chat/completions \
  -H "Content-Type: application/json" \
  -H "Accept: text/event-stream" \
  -H "ZL-OPENID: sbzj_device" \
  -d '{"model":"qwen-max","stream":true,"messages":[{"role":"user","content":"你好"}]}'
```

### 4.2 鉴权拦截

```bash
curl -s -X POST http://localhost:8080/v1/chat/completions \
  -H "Content-Type: application/json" \
  -d '{"model":"qwen-max","stream":false,"messages":[]}'
# 预期：403 {"code":403,"message":"ZL-OPENID invalid or missing"}
```

### 4.3 敏感信息脱敏

```bash
# 手机号脱敏（mask_pass）
curl -s -X POST http://localhost:8080/v1/chat/completions \
  -H "Content-Type: application/json" \
  -H "ZL-OPENID: sbzj_device" \
  -d '{"model":"qwen-max","stream":false,"messages":[{"role":"user","content":"我的手机号是13812341234"}]}'
# 预期：响应中手机号变为 138****1234

# 密码拦截（block）
curl -s -X POST http://localhost:8080/v1/chat/completions \
  -H "Content-Type: application/json" \
  -H "ZL-OPENID: sbzj_device" \
  -d '{"model":"qwen-max","stream":false,"messages":[{"role":"user","content":"密码: abc123"}]}'
# 预期：403 {"code":403,"message":"请求包含敏感信息，已被安全围栏拦截"}
```

### 4.4 监控与规则管理

```bash
# 查询围栏运行统计
curl -s http://localhost:8080/api/v1/metrics

# 查询熔断器状态
curl -s http://localhost:8080/api/v1/circuit-breaker

# 查询围栏配置
curl -s http://localhost:8080/api/v1/fence/status

# 查询规则列表
curl -s http://localhost:8080/api/v1/desensitization/rules

# 创建新规则
curl -s -X POST http://localhost:8080/api/v1/desensitization/rules \
  -H "Content-Type: application/json" \
  -d '{"rule_name":"测试规则","match_pattern":"test","disposition":"mask_pass","mask_strategy":"mask_middle"}'
```

## 五、配置说明

```yaml
demo:
  upstream-base-url: http://localhost:8080/mock/v1  # 上游地址
  connect-timeout-ms: 5000
  read-timeout-ms: 60000
  openid:
    enabled: true
    whitelist: [sbzj_device, app_yl_yongying, hx_core]
    bypass-paths: [/mock, /api]
  fence:
    enabled: true                    # 围栏总开关
    dry-run: false                   # 旁路观察模式
    degrade-strategy: passthrough    # 降级策略：reject/passthrough
    circuit-breaker-threshold: 5.0   # 熔断阈值（异常率 %）
    circuit-breaker-window-seconds: 60
    circuit-breaker-recovery-seconds: 300
```

## 六、预置规则（9 类）

| 规则 ID | 名称 | 处置 | 匹配内容 |
|---|---|---|---|
| rule_001 | 身份证号脱敏 | mask_pass | `\d{17}[\dXx]` |
| rule_002 | 手机号脱敏 | mask_pass | `1[3-9]\d{9}` |
| rule_003 | 邮箱脱敏 | mask_pass | 邮箱格式 |
| rule_004 | 银行卡号脱敏 | mask_pass | 16-19 位数字 |
| rule_005 | 密码验证码拦截 | block | `密码\|验证码\|password\|token` |
| rule_006 | API Key 拦截 | block | `sk-xxx` 或 `Bearer xxx` |
| rule_007 | 内网IP替换 | replace | 10.x / 172.16-31.x / 192.168.x |
| rule_008 | 统一社会信用代码脱敏 | mask_pass | 18 位信用代码 |
| rule_009 | HTML标签删除 | delete | `<...>` 标签 |

## 七、与一期范围表对照

| # | 功能项 | 状态 | 说明 |
|---|---|---|---|
| 1 | HTTP Header/Body 解析 | ✅ | `@RequestBody` + Jackson |
| 2 | 嵌套字段解析 | ✅ | `$.messages[*].content` |
| 3 | Prompt 文本提取 | ✅ | 全文扫描 `collectTextNodes` |
| 5 | 规则匹配引擎 | ✅ | 正则 + scope + 优先级 |
| 6 | 掩码/替换脱敏 | ✅ | mask_middle / replace |
| 7 | 删除脱敏 | ✅ | delete |
| 8 | 直接拦截 | ✅ | block → 403 |
| 9 | 报文重组 | ✅ | 脱敏后 JSON 回写 |
| 13 | 异常降级 | ✅ | reject/passthrough + 熔断器 |
| 15 | 统一请求标识 | ✅ | UUID request_id |
| 16 | 规则配置 CRUD | ✅ | `/api/v1/desensitization/rules` |
| 17 | 脱敏操作日志 | ✅ | AuditLogger |
| 18 | 规则热加载 | ✅ | 内存快照（Demo 简化版） |
| 19 | 基础监控 | ✅ | `/api/v1/metrics` |
| 20 | 规则 scope | ✅ | route/consumer/model |
