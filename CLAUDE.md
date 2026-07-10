# CLAUDE.md — AI-FMS Project Instructions

## Role

你是 **AI-FMS（AI Finding Management System / AI指摘管理系统）** 的架构工程师和高级全栈开发。

你的工作原则：
- **先思考，再动手。** 任何代码修改前必须先理解上下文。
- **先规划，再执行。** 不要跳过设计直接写代码。
- **质量优先。** 代码要能通过 review，不是能跑就行。
- **留下痕迹。** 每次修改后更新 `docs/worklog/YYYY-MM-DD.md`。
- **保持骨架干净。** 不在初始化阶段生成业务代码。

---

## Development Workflow

所有任务必须遵循：

```
1. 理解需求 → 阅读相关代码，确认范围
2. 设计     → 给出实施计划（涉及的文件、步骤），等待确认
3. 实现     → 按计划逐步修改代码
4. 自测     → 运行测试，确认通过
5. 记录     → 更新 docs/worklog/YYYY-MM-DD.md
```

**禁止行为：**
- 不规划就直接大规模重构
- 猜测业务需求——有疑问先确认
- 在不确定的情况下删除已有代码
- 在初始化阶段生成业务代码

---

## Tech Stack

| 层级 | 技术 | 备注 |
|------|------|------|
| 语言 | Java 17+ | — |
| 框架 | Spring Boot 3 + WebFlux | 禁止 Spring MVC |
| 安全 | Spring Security WebFlux | 响应式安全链 |
| 数据库访问 | Spring Data R2DBC | 禁止 JPA、MyBatis、JDBC |
| 数据库 | PostgreSQL 15+ | — |
| 缓存 | Redis (Reactive) | Spring Data Redis Reactive |
| 迁移工具 | Flyway | 禁止 Hibernate DDL auto |
| 构建工具 | Maven | `mvnw` |
| 前端框架 | React 18 + TypeScript | — |
| UI 组件 | Ant Design 5 | — |
| 前端构建 | Vite 5 | — |
| 路由 | React Router 6 | — |
| 状态管理 | Zustand | 客户端状态 |
| HTTP 客户端 | Axios | 统一封装 |
| 容器化 | Docker + Docker Compose | — |

---

## Quick Commands

```bash
# 后端
./mvnw spring-boot:run      # 启动后端
./mvnw test                  # 运行测试
./mvnw flyway:migrate        # 执行数据库迁移
./mvnw clean package         # 打包

# 前端
cd frontend && npm run dev      # 启动前端开发服务器
cd frontend && npm run build    # 生产构建
cd frontend && npm run test     # 运行前端测试

# Docker
docker-compose up -d            # 启动所有服务
docker-compose logs -f backend  # 查看后端日志
```

---

## Project Structure

```
project-root/
├── CLAUDE.md                    # 本文件 — Claude 工作指导
├── AGENTS.md                    # 技术规范（架构约束、命名规则、禁止事项）
├── README.md                    # 项目说明和启动指南
│
├── .claude/
│   ├── settings.json           # Hook 配置 + 权限
│   ├── skills/                  # 可复用的任务模板
│   │   ├── backend-feature.md
│   │   ├── frontend-feature.md
│   │   ├── database-change.md
│   │   └── code-review.md
│   └── scripts/
│       ├── log-change.py       # 自动工作留痕
│       └── session-summary.py  # 会话结束摘要
│
├── backend/
│   └── src/main/java/com/aifms/
│       ├── config/             # Spring 配置（WebFlux, Security, R2DBC, Redis）
│       ├── common/             # 通用组件（Result<T>, 全局异常处理, 工具类）
│       ├── shared/             # 跨模块共享（基础 Entity, 通用 DTO）
│       └── modules/
│           ├── auth/           # 认证模块
│           │   ├── presentation/   # Controller
│           │   ├── application/    # Application Service
│           │   ├── domain/         # Domain Service + Entity
│           │   └── infrastructure/ # Repository
│           ├── user/           # 用户模块（同上结构）
│           ├── role/           # 角色模块
│           └── tenant/         # 租户模块
│
├── frontend/
│   └── src/
│       ├── api/                # Axios 封装 + API 接口定义
│       ├── services/           # 业务服务层
│       ├── pages/              # 页面组件
│       ├── components/         # 通用组件
│       ├── hooks/              # 自定义 Hooks
│       ├── layouts/            # 布局组件
│       ├── router/             # 路由配置
│       ├── store/              # Zustand 状态
│       ├── types/              # TypeScript 类型
│       ├── utils/              # 工具函数
│       └── styles/             # 全局样式
│
├── database/
│   ├── migration/              # Flyway SQL 脚本
│   ├── view/                   # 数据库视图
│   ├── function/               # 数据库函数
│   ├── trigger/                # 数据库触发器
│   └── seed/                   # 种子数据
│
├── docs/
│   ├── architecture.md         # 架构决策记录 (ADR)
│   ├── api-design.md           # API 设计规范
│   ├── database-design.md      # 数据库设计文档
│   └── worklog/                # 每日工作日志
│
├── nginx/                      # Nginx 配置
├── scripts/                    # 运维脚本
└── docker-compose.yml
```

---

## Architecture Rules (Hard Constraints)

这些是**不可协商**的硬性约束：

1. **Controller 禁止写业务逻辑** — 只做：参数校验 → 调用 Service → 返回 Result<T>
2. **Controller 禁止直接访问 Repository** — 必须通过 Application Service
3. **禁止 Spring MVC** — 不引入 `spring-boot-starter-web`
4. **禁止 JPA / MyBatis / JDBC** — 只使用 R2DBC
5. **禁止 `.block()`** — 全链路响应式，Mono/Flux 贯穿始终
6. **Entity 只表示数据库模型** — DTO 和 Entity 必须分离
7. **数据库变更只通过 Flyway** — 禁止手动改库，禁止 Hibernate DDL auto

---

## Coding Rules

**必须遵守 `AGENTS.md` 中的所有技术规范。**

特别强调：
- Controller → Application Service → Domain Service → Repository，单向依赖
- 模块间通过 `shared` 和 `common` 通信，禁止跨模块直接依赖
- 所有 public API 返回 `Result<T>` 统一包装
- 异常由全局 `GlobalExceptionHandler` 统一处理

### 注释规范

**所有注释必须使用中文，且符合 Javadoc 格式。**

| 元素 | 格式 | 示例 |
|------|------|------|
| 类/接口 | `/** 一句话描述。 */` | `/** 用户状态枚举，内置状态机。 */` |
| public 方法 | `/** 方法功能描述。 */` + `@param` / `@return` / `@throws` | 见下方 |
| 字段 | `/** 字段含义。 */` | `/** 用户名（唯一）。 */` |
| 行内注释 | `// 说明` | `// 检查邮箱唯一性` |

**Javadoc 完整示例：**

```java
/**
 * 创建新用户。
 * 校验用户名和邮箱唯一性，对密码进行哈希后持久化。
 *
 * @param request 创建用户请求体
 * @return 创建成功的用户响应
 * @throws BusinessException 40002 用户名已存在 / 40003 邮箱已存在
 */
public Mono<Result<UserResponse>> createUser(CreateUserRequest request) { ... }
```

**禁止：**
- 英文注释
- 无意义注释（如 `// getter`）
- 缺少 `@param` / `@return` / `@throws` 的 public 方法

---

## Worklog

每次完成任务后，更新 `docs/worklog/YYYY-MM-DD.md`：

```markdown
### HH:MM — 任务简述

- **类型：** 新功能 / Bug修复 / 重构 / 文档 / 初始化
- **范围：** 修改的文件列表
- **摘要：** 一句话说明
- **关联：** Issue / PR（如有）
```

Hook 会在每次文件修改后自动记录（见 `.claude/settings.json`）。

---

## Product UI/UX Rules

**AI-FMS 是面向客户交付的商业 SaaS 产品，不是内部管理系统。**

UI 必须达到的标准：
- **专业** — 企业级 SaaS 视觉质量
- **可演示** — 随时可以给客户看
- **一致性** — 所有页面遵循相同的设计语言
- **非开发感** — 不能看起来像"后端程序员随手写的 CRUD"

### 设计原则

| 禁止 | 必须 |
|------|------|
| Ant Design 默认样式裸用 | 通过 ConfigProvider 统一主题 |
| 裸 Table + Form 堆砌 | Card 包裹 + 页头 + 合理间距 |
| 密集信息无层次 | 清晰的视觉层级（标题→操作区→内容） |
| 全屏铺满、无边距 | 适当留白，`app-content` 统一边距 |
| 开发者视角的页面 | 考虑用户工作流和操作习惯 |

### 页面设计规则

每个业务页面在编码前必须输出设计计划，涵盖：

1. **页头（Page Header）** — 标题 + 描述 + 主操作按钮
2. **搜索/筛选区** — Card 包裹，相关筛选项分组
3. **主内容区** — 合理使用 Card / Table / List，禁止全屏裸组件
4. **四态覆盖** — Loading 态、Empty 态、Error 态（含重试）、正常态

### Table 规则

- 必须有：搜索/筛选区、分页、Loading 态、Empty 态、行操作
- 列宽合理，长文本省略 + Tooltip
- 避免列数过多（≤7 列）
- 操作列固定右侧

### Form 规则

- 字段逻辑分组（如：账户信息 / 个人信息），用 Divider 或分段标题分隔
- 校验消息友好（i18n 键）
- 长表单不分单列排到底 — 考虑两列或分组折叠
- 提交按钮右对齐，Cancel + Save 成对出现

### 通用组件规范

以下组件必须抽取到 `frontend/src/components/`，禁止页面内联重复：

| 组件 | 用途 | 状态 |
|------|------|------|
| `PageContainer` | 统一页头（标题 + 面包屑 + 操作区） | 待实现 |
| `SearchForm` | 可折叠的搜索表单 | 待实现 |
| `DataTable` | 内置 Loading/Empty/Error 的表格封装 | 待实现 |
| `StatusTag` | 统一的状态标签（颜色映射集中管理） | 已有（页面内联） |
| `EmptyState` | 空状态插画 + 引导操作 | 待实现 |
| `ConfirmDialog` | 统一的确认弹窗 | 已有（Popconfirm） |

### 主题

- 颜色、字号、间距统一通过 `ConfigProvider theme.token` 控制
- 禁止页面内联硬编码颜色值（状态色除外）
- 全局样式放在 `styles/global.css`

### AI-FMS 特有 UX 预览

后续页面设计时参考：

- **Dashboard** — AI 处理状态、待审核数、最近活动
- **指摘管理** — 优化 AI 提取结果的审核体验，原文与 AI 结果可对比
- **文档处理** — 上传状态、AI 处理进度、处理历史

### Review Checklist

每完成一个页面，检查：

- [ ] 可以拿去给客户演示吗？
- [ ] 看起来像商业 SaaS 还是内部工具？
- [ ] 信息层级清晰吗？
- [ ] 操作流程直观吗？
- [ ] 四态（Loading / Empty / Error / Normal）都覆盖了吗？

---

## Memory System

项目的关键事实和决策存储在 Memory 中（`~/.claude/projects/D--Job/memory/`）。
当你发现某个重要信息在 Memory 中缺失时，主动建议记录。
