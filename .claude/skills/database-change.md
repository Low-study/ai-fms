# Database Change — 数据库变更

## 触发条件

- 新增表 / 修改表结构
- 创建索引 / 视图 / 函数 / 触发器
- 数据迁移
- 种子数据变更

## 核心原则

> **永远通过 Flyway 管理数据库变更。禁止手动修改数据库。**

---

## 执行流程

### Phase 1: 分析变更

1. 检查 `database/migration/` 下已有的 migration 文件
2. 确定变更类型：
   - 新建表 → `migration/`
   - 视图 → `view/`
   - 函数 → `function/`
   - 触发器 → `trigger/`
   - 种子数据 → `seed/`

### Phase 2: 编写 Migration

3. 确定版本号（查看已有文件，递增）
4. 编写 SQL：

```sql
-- database/migration/V002__create_users.sql

CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(50) NOT NULL,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_status ON users(status);
```

规则：
- 文件名：`V{序号}__{描述}.sql`（双下划线）
- SQL 包含幂等性保护（`IF NOT EXISTS` / `IF EXISTS`）
- 禁止修改已提交的 migration
- DDL 和 DML 分开

### Phase 3: 更新代码

5. 更新对应 Entity（`@Table` 映射）
6. 更新 Repository（新查询方法）
7. 更新 DTO（字段变化）

### Phase 4: 验证

8. 本地执行：`./mvnw flyway:migrate`
9. 验证表结构：`\d table_name`
10. 运行测试：`./mvnw test`

### Phase 5: 记录

11. 更新 `docs/worklog/YYYY-MM-DD.md`
12. 更新 `docs/database-design.md`

---

## 安全检查清单

- [ ] Migration 文件名符合 `V{序号}__{描述}.sql` 格式
- [ ] SQL 包含幂等性保护
- [ ] 未修改已有 migration 文件
- [ ] Entity 字段与数据库列一一对应
- [ ] Repository 查询与新结构兼容
- [ ] `./mvnw flyway:migrate` 执行成功
- [ ] `./mvnw test` 全部通过
- [ ] 破坏性变更已在 worklog 注明回滚方案

## 禁止操作

- ❌ 手动连接数据库执行 DDL/DML
- ❌ 修改已提交的 migration 文件
- ❌ 使用 Hibernate `ddl-auto: update/create`
- ❌ 混合 DDL 和 DML 在同一 migration
