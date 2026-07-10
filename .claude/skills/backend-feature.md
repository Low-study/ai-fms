# Backend Feature — 后端功能开发

## 触发条件

当用户要求新增后端功能时使用。例如：
- "实现用户管理模块"
- "添加订单查询接口"
- "新增角色权限校验"

## 不适用场景

- 纯数据库变更 → 使用 `database-change`
- Bug 修复 → 待创建 `bug-fix`
- 前端功能 → 使用 `frontend-feature`

---

## 执行流程

### Phase 1: 需求分析

1. 确认功能属于哪个模块（auth / user / role / tenant）
2. 检查是否需要新表 → 有则先调用 `database-change`
3. 确认 API 契约（路径、请求/响应格式、权限）
4. 确认是否需要跨模块依赖

### Phase 2: 设计

5. 确定涉及的文件清单：

```
modules/{module}/
├── presentation/{Module}Controller.java
├── application/{Module}ApplicationService.java
├── domain/{Entity}.java               (Entity)
├── domain/{Domain}Service.java        (Domain Service，如需要)
└── infrastructure/{Entity}Repository.java

common/
├── Result.java                        (已有)
└── exception/{Xxx}Exception.java      (如需要)

shared/dto/
├── {Action}{Entity}Request.java
└── {Entity}Response.java
```

6. 输出文件清单和设计思路，等待用户确认

### Phase 3: 实现（严格按顺序）

```
7.  Migration     → 如果涉及新表/改表，先写 Flyway 脚本
8.  Entity        → 数据库模型（@Table，R2DBC 映射）
9.  Repository    → 数据访问接口
10. DTO           → Request + Response（放在 shared/dto/）
11. Exception     → 自定义异常（如需要）
12. Domain Service→ 纯业务逻辑（如需要）
13. Application Service → 用例编排（禁止 .block()）
14. Controller    → REST 接口（只做校验+调用）
15. GlobalExceptionHandler → 注册新异常（如需要）
```

### Phase 4: 测试

16. Domain Service 单元测试（覆盖率 ≥ 90%）
17. Application Service 单元测试（覆盖率 ≥ 80%）
18. Controller 集成测试（WebTestClient）

### Phase 5: 记录

19. 更新 `docs/worklog/YYYY-MM-DD.md`
20. 更新 `docs/api-design.md`（如有新 API）
21. 更新 `docs/database-design.md`（如有新表）

---

## Checklist

- [ ] Migration 文件符合 `V{序号}__{描述}.sql` 命名
- [ ] Entity 使用 `@Table` 注解，字段映射正确
- [ ] Repository 继承 `ReactiveCrudRepository`，返回 Mono/Flux
- [ ] DTO 与 Entity 分离，放在 shared/dto/
- [ ] Application Service 不包含 `.block()` 调用
- [ ] Controller 只做：校验 → 调 Service → 返回 Result<T>
- [ ] Controller 不直接访问 Repository
- [ ] 异常由 GlobalExceptionHandler 处理
- [ ] 测试覆盖率达标
- [ ] `./mvnw test` 全部通过
- [ ] Worklog 已更新
- [ ] 相关设计文档已更新
