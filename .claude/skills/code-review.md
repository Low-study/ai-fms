# Code Review — 代码审查

## 触发条件

- 功能开发完成后
- Pull Request 提交前
- 用户要求 review
- 怀疑某段代码有潜在问题

---

## 审查维度

### 1. DDD 架构检查

| 检查项 | 标准 |
|--------|------|
| Controller 是否包含业务逻辑 | Controller = 校验 → 调 Service → 返回 Result<T> |
| Controller 是否直接调 Repository | 必须经过 Application Service |
| 依赖方向是否正确 | Presentation → Application → Domain ← Infrastructure |
| 跨模块依赖 | 模块间只能通过 shared/common 通信 |
| Entity 是否被当 DTO 用 | Entity ≠ DTO，必须分离 |

### 2. WebFlux / R2DBC 检查

| 检查项 | 严重程度 |
|--------|----------|
| 存在 `.block()` 调用 | 🔴 阻断 |
| 混入 Spring MVC 依赖 | 🔴 阻断 |
| 混入 JPA / MyBatis / JDBC | 🔴 阻断 |
| `Mono.zip` 中分支可能为空 | 🟡 警告 |
| `flatMap` 嵌套 > 3 层 | 🟡 建议重构 |
| 未处理 `Mono.empty()` 场景 | 🟡 警告 |

### 3. 安全性检查

| 检查项 | 严重程度 |
|--------|----------|
| SQL 注入（参数未用 `:param`） | 🔴 阻断 |
| 敏感信息硬编码（密码、密钥） | 🔴 阻断 |
| Controller 缺少 `@Valid` | 🔴 阻断 |
| 接口未做权限控制 | 🔴 阻断 |
| 日志打印敏感信息 | 🟡 警告 |

### 4. 性能检查

| 检查项 | 严重程度 |
|--------|----------|
| N+1 查询 | 🔴 阻断 |
| 列表查询无分页 | 🔴 阻断 |
| 循环中执行数据库操作 | 🟡 警告 |
| 未使用 Redis 缓存热点数据 | 🟢 建议 |

### 5. 代码质量

| 检查项 | 严重程度 |
|--------|----------|
| 命名不符合 AGENTS.md | 🟡 建议 |
| 方法 > 50 行 | 🟡 建议拆分 |
| 魔法数字/字符串 | 🟡 建议提取常量 |
| 重复代码 | 🟡 建议抽取 |
| 测试缺失 | 🟡 警告 |

---

## 输出格式

```
## Code Review 报告

### 🔴 阻断项（必须修复）
- [ ] xxx — 原因

### 🟡 警告项（建议修复）
- [ ] xxx — 原因

### 🟢 建议项（可选）
- [ ] xxx — 原因

### 总体评价
xxx
```
