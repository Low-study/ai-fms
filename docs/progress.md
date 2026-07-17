# AI-FMS 项目进度报告

> 更新于 2026-07-17

---

## 项目概述

**AI-FMS（AI Finding Management System）** 是面向客户交付的商业 SaaS 产品，用于 AI 辅助指摘（finding）管理。支持文件导入、AI 解析理解、结构化整理、数据库保存、用户查看与处理的全流程。

当前阶段：**MVP-1 端到端完成**，AI 指摘管理核心流程已打通（文件导入 → AI 解析 → 分类 → RAG → 报告 → QA），后续依次实现角色、认证、租户、权限模块及 MVP-2 增强。

---

## 一、后端（Spring Boot 3 WebFlux）

### 架构

```
DDD 四层架构：
Controller → ApplicationService → DomainService → Repository
      ↑                                              │
      └── 单向依赖，模块间通过 shared/common 通信 ──┘
```

### 已完成模块

| 模块 | 路径 | 状态 |
|------|------|------|
| 项目骨架 | `backend/src/main/java/com/aifms/` | ✅ |
| 全局配置 | `config/` — WebFlux, Security, R2DBC, Redis | ✅ |
| 通用组件 | `common/` — `Result<T>`, `GlobalExceptionHandler`, `ErrorCodes`, `PasswordHasher` | ✅ |
| 跨模块共享 | `shared/` | ✅ |
| **用户模块** | `modules/user/` | ✅ 完整 |
| **Finding 模块** | `modules/finding/` | ✅ MVP-1 完整 |
| **File 模块** | `modules/file/` | ✅ MVP-1 完整 |
| **Task 模块** | `modules/task/` | ✅ MVP-1 完整 |
| **Agent 模块** | `modules/agent/` | ✅ MVP-1 完整 |
| 角色模块 | `modules/role/` | ⏳ 待实现 |
| 认证模块 | `modules/auth/` | ⏳ 待实现 |
| 租户模块 | `modules/tenant/` | ⏳ 待实现 |

### 用户模块功能清单

| 功能 | 接口 | 状态 |
|------|------|------|
| 分页列表 + 关键词搜索 | `GET /api/v1/users` | ✅ |
| 查询单个用户 | `GET /api/v1/users/{id}` | ✅ |
| 创建用户 | `POST /api/v1/users` | ✅ |
| 更新用户（diff-based） | `PUT /api/v1/users/{id}` | ✅ |
| 软删除 | `DELETE /api/v1/users/{id}` | ✅ |
| 状态机校验 | `UserStatus.canTransitionTo()` | ✅ |
| 密码加密 | `BCryptPasswordHasher` | ✅ |
| 用户名/邮箱唯一性 | Repository + ApplicationService | ✅ |
| 登录失败锁定 | `recordLoginFailure()` → 5 次后 LOCKED | ✅ |
| 已删除用户标识符复用 | Partial Unique Index（方案 A） | ✅ |

### 错误码体系

| 范围 | 类别 | 已定义 |
|------|------|--------|
| 40001 | 参数校验失败 | ✅ |
| 40002–40003 | 用户重复（用户名/邮箱） | ✅ |
| 40010–40011 | 密码强度 / 状态转换非法 | ✅ |
| 40401 | 用户不存在 | ✅ |
| 50001 | 服务器内部错误 | ✅ |

### 测试

- 单元测试：25 个，全部通过
- 定义文件：`error-codes.yml`（权威）+ `ErrorCodes.java`（常量）

---

## 二、数据库（PostgreSQL 15）

| 迁移 | 内容 | 状态 |
|------|------|------|
| V001 | 初始化（扩展 + 函数） | ✅ 已应用 |
| V002 | `users` 表创建 | ✅ 已应用 |
| V003 | Partial Unique Indexes（排除 DELETED） | ✅ 已应用 |
| V004 | 移除索引/CHECK/触发器（约束交后端） | ✅ 已应用 |

策略：数据库只保留 NOT NULL + DEFAULT + PRIMARY KEY，复杂约束由后端应用层校验。

---

## 三、前端（React 18 + TypeScript + Ant Design 5）

### 页面

| 路由 | 页面 | 状态 | 四态覆盖 |
|------|------|------|----------|
| `/` | Dashboard | ✅ | Loading ✅ / Error ⏳ / Normal ✅ |
| `/users` | 用户列表 | ✅ | Loading ✅ / Empty ✅ / Error ✅ / Normal ✅ |
| `/users/create` | 创建用户 | ✅ | Error ✅ / Normal ✅ |
| `/users/:id/edit` | 编辑用户 | ✅ | Loading ✅ / Error ✅ / Normal ✅ |

### i18n 国际化

| 语言 | 覆盖率 | 文件 |
|------|--------|------|
| 中文（zh-CN） | 100% | `locales/zh-CN.json` |
| 日本語（ja-JP） | 100% | `locales/ja-JP.json` |
| English（en-US） | 100% | `locales/en-US.json` |

- 运行时语言检测（localStorage → navigator.language → zh-CN 回退）
- 后端错误码 → 前端 i18n 消息映射（`error.{code}`）
- Ant Design 组件文案同步（ConfigProvider locale）
- 语言切换器：侧边栏底部 `[ ZH | JA | EN ]`

### UI 质量

已按 **商业 SaaS 产品标准** 完成自审和重构：

- ✅ 全局设计系统（`global.css` + ConfigProvider theme token）
- ✅ 页面入场动画（fadeIn）
- ✅ 统计卡片渐变图标容器
- ✅ 面包屑自动生成
- ✅ 表单分节设计（账户 / 个人 / 状态）
- ✅ Loading → Skeleton（非 Spin）
- ✅ Empty 状态引导文案
- ✅ Error 状态可重试
- ✅ 所有 placeholder 已 i18n
- ✅ `CLAUDE.md` 新增 Product UI/UX Rules 章节

### 前端文件结构

```
frontend/src/
├── api/              ← Axios 封装 + API 接口
├── components/       ← 通用组件（待扩充）
├── hooks/            ← 自定义 Hooks
├── i18n/
│   ├── index.ts      ← i18next 配置
│   └── locales/      ← zh-CN, ja-JP, en-US
├── layouts/
│   └── MainLayout.tsx
├── pages/
│   ├── HomePage.tsx
│   ├── UserListPage.tsx
│   ├── UserCreatePage.tsx
│   └── UserEditPage.tsx
├── router/
│   └── index.tsx
├── store/            ← Zustand
├── styles/
│   └── global.css
├── types/
│   └── user.ts
└── utils/
```

---

## 四、工具链与规范

| 工具 | 用途 | 状态 |
|------|------|------|
| `.claude/settings.json` | Hook（PostToolUse → log-change / Stop → session-summary） | ✅ |
| `.claude/skills/ui-review/` | UI Review Skill（SaaS 产品标准审查） | ✅ |
| `.mcp.json` | Playwright MCP（页面截图自检） | ✅ |
| `CLAUDE.md` | 项目指导（架构规则 + 编码规范 + UI/UX 规则） | ✅ |
| `AGENTS.md` | 技术规范 | ✅ |
| `docs/worklog/` | 每日工作日志 | ✅ |

---

## 五、MVP-1 完成

> AI 指摘管理（Phase-0 + Wave 1-3，27 任务中 16 完成）

### Phase-0 闸门

| 任务 | 状态 |
|------|------|
| Spring Boot 3.3.5 → 3.5.16 升级 | ✅ |
| ContextLoadSmokeTest 通过 | ✅ |

### Wave 1 — 基础设施

| 任务 | 状态 |
|------|------|
| ADR-008~013 架构决策记录 | ✅ |
| V005 Flyway — `findings` 表 | ✅ |
| V006 Flyway — `pgvector` 扩展 | ✅ |
| V007 Flyway — `issue_embeddings` 表 | ✅ |
| V008 Flyway — `prompt_templates` 表 | ✅ |
| V009 Flyway — `agent_executions` 表 | ✅ |
| V010 Flyway — `file_uploads` 表 | ✅ |
| modules/file — `FileStoragePort` + MinIO 适配器 | ✅ |
| modules/task — `TaskService<T>` + Redis Streams | ✅ |
| modules/agent — 7 端口抽象 + 5 Skill domain 纯接口 | ✅ |
| docker-compose — MinIO + pgvector | ✅ |
| 前端 7 通用组件（PageContainer/SearchForm/DataTable/StatusTag/EmptyState/ConfirmDialog/SseProgress） | ✅ |

### Wave 2 — 业务与 Agent

| 任务 | 状态 |
|------|------|
| modules/finding — 四层 DDD CRUD + 状态机 | ✅ |
| 5 SkillAdapter（parse/classify/rag/report/qa） | ✅ |
| LangChain4jChatModelAdapter（DeepSeek） | ✅ |
| TikaDocumentParser + CloudOcrAdapter | ✅ |
| LangChain4jMcpClient | ✅ |
| PromptTemplatePortAdapter + V011 seed | ✅ |
| AgentExecutionLogPortAdapter（fail-safe write） | ✅ |
| 前端 IssueImportPage（Upload.Dragger + SSE） | ✅ |
| 前端 IssueDetailPage（结构化 Issue + StatusTag） | ✅ |

### Wave 3 — 端到端

| 任务 | 状态 |
|------|------|
| AgentController `POST /api/v1/agents/import` → SSE | ✅ |
| AgentApplicationService `runSequential`（5 Skill, boundedElastic） | ✅ |

---

## 六、下一步计划

### P0 — 下一步

- [ ] 角色管理模块（CRUD + 权限关联预留）
- [ ] 认证模块（Spring Security WebFlux + JWT）
- [ ] 前端登录页 + 认证流程

### P1 — 后续

- [ ] 租户管理模块
- [ ] 权限管理模块
- [ ] E2E 测试（Playwright）
- [ ] CI/CD（GitHub Actions）

### P2 — 远期

- [ ] 文档处理流水线（多格式增强）
- [ ] 审批工作流
- [ ] 报表与统计
