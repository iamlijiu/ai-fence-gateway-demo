# 规则集说明

## 文件结构

```
rules/
├── README.md              # 本说明文档
└── preset-rules.json      # 预置规则集（9类敏感信息）
```

## 预置规则清单

| 规则ID | 规则名称 | 处置策略 | 脱敏策略 | 优先级 | 说明 |
|---|---|---|---|---|---|
| rule_001 | 身份证号脱敏 | mask_pass | mask_middle | 5 | 保留前6后4，中间用*替换 |
| rule_002 | 手机号脱敏 | mask_pass | mask_middle | 10 | 保留前3后4，中间用*替换 |
| rule_003 | 邮箱脱敏 | mask_pass | mask_middle | 20 | 保留前1，其余用*替换 |
| rule_004 | 银行卡号脱敏 | mask_pass | mask_middle | 15 | 保留前4后4，中间用*替换 |
| rule_005 | 密码验证码拦截 | block | - | 1 | 直接拦截（高危信息） |
| rule_006 | API Key拦截 | block | - | 2 | 直接拦截（sk-xxx/Bearer） |
| rule_007 | 内网IP替换 | replace | replace | 30 | 替换为[REDACTED] |
| rule_008 | 统一社会信用代码脱敏 | mask_pass | mask_middle | 25 | 保留前2后4，中间用*替换 |
| rule_009 | HTML标签删除 | delete | delete | 35 | 直接删除HTML标签 |

## 处置策略说明

| disposition | 说明 | 示例 |
|---|---|---|
| `mask_pass` | 脱敏后放行 | 138****1234 |
| `block` | 直接拦截请求 | 返回403错误 |
| `replace` | 替换为[REDACTED]后放行 | [REDACTED] |
| `delete` | 直接删除匹配内容 | 标签被删除，保留文本 |

## 使用方式

### 1. 通过API导入规则

```bash
# 从ClassPath导入预置规则（覆盖模式）
curl -X POST "http://localhost:8080/api/v1/desensitization/rules/import?classpath=rules/preset-rules.json&overwrite=true"

# 验证JSON格式
curl -X POST "http://localhost:8080/api/v1/desensitization/rules/validate?classpath=rules/preset-rules.json"
```

### 2. 通过API导出规则

```bash
# 导出当前所有规则
curl -s http://localhost:8080/api/v1/desensitization/rules/export | jq .
```

### 3. 手动加载规则（代码中）

```java
@Autowired
private JsonRuleLoader jsonRuleLoader;

// 从ClassPath加载
List<Rule> rules = jsonRuleLoader.loadFromClasspath("rules/preset-rules.json");

// 添加到规则存储
for (Rule rule : rules) {
    ruleStore.put(rule);
}
```

## JSON格式规范

```json
{
  "version": "1.0.0",
  "description": "规则集描述",
  "rules": [
    {
      "rule_id": "rule_001",
      "rule_name": "规则名称",
      "match_type": "regex",
      "match_pattern": "正则表达式",
      "field_path": "$.messages[*].content",
      "disposition": "mask_pass",
      "mask_strategy": "mask_middle",
      "mask_config": {
        "keep_prefix": 3,
        "keep_suffix": 4,
        "mask_char": "*"
      },
      "priority": 10,
      "enabled": true,
      "scope": {},
      "description": "规则说明"
    }
  ]
}
```

## 字段说明

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| rule_id | String | 是 | 规则唯一标识 |
| rule_name | String | 是 | 规则名称 |
| match_type | String | 是 | 匹配类型（regex/field_path） |
| match_pattern | String | 是 | 匹配模式（正则表达式） |
| field_path | String | 是 | JSON字段路径 |
| disposition | String | 是 | 处置策略（mask_pass/block/replace/delete） |
| mask_strategy | String | 否 | 脱敏策略（mask_middle/replace/delete） |
| mask_config | Object | 否 | 脱敏配置 |
| priority | int | 是 | 优先级（数值越小越优先） |
| enabled | boolean | 是 | 是否启用 |
| scope | Object | 否 | 生效范围 |
| description | String | 否 | 规则说明 |

## scope配置示例

```json
{
  "scope": {
    "consumers": ["sbzj_device", "app_yl_yongying"],
    "models": ["qwen-max", "gpt-4"],
    "routes": ["/v1/chat/completions"]
  }
}
```

- `consumers`: 适用的业务方（为空=全量生效）
- `models`: 适用的模型（为空=全量生效）
- `routes`: 适用的路由（为空=全量生效）

## 扩展规则

如需添加新规则，可在 `rules/preset-rules.json` 中添加，格式如下：

```json
{
  "rule_id": "rule_010",
  "rule_name": "自定义规则",
  "match_type": "regex",
  "match_pattern": "要匹配的正则表达式",
  "field_path": "$.messages[*].content",
  "disposition": "mask_pass",
  "mask_strategy": "mask_middle",
  "mask_config": {
    "keep_prefix": 2,
    "keep_suffix": 2,
    "mask_char": "*"
  },
  "priority": 50,
  "enabled": true,
  "scope": {}
}
```

## 注意事项

1. **正则性能**：复杂正则可能影响匹配效率，建议优先级高的规则使用简单正则
2. **优先级冲突**：多条规则可能匹配同一内容，优先级高的先执行
3. **scope为空**：表示全量生效，适用于所有业务方/模型/路由
4. **mask_config**：仅在 `disposition=mask_pass` 时生效
5. **热加载**：规则变更后通过Apollo信号通知网关，≤5分钟生效
