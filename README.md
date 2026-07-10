# AI-FMS — AI 指摘管理系统

AI-FMS（AI Finding Management System）是面向企业客户的商业 SaaS 产品，用于 AI 辅助指摘（finding）管理。

支持文件导入 → AI 解析理解 → 结构化整理 → 数据库保存 → 用户查看与处理的全流程。

---

## 技术栈

| 层级 | 技术 | 版本 |
|------|------|------|
| 语言 | Java | 17+ |
| 框架 | Spring Boot 3 + WebFlux | 3.x |
| 安全 | Spring Security WebFlux | — |
| 数据库访问 | Spring Data R2DBC | — |
| 数据库 | PostgreSQL | 15+ |
| 缓存 | Redis（Reactive） | 7+ |
| 迁移 | Flyway | — |
| 构建 | Maven | 3.9+ |
| 前端 | React + TypeScript | 18 |
| UI 组件 | Ant Design | 5 |
| 构建 | Vite | 6 |
| 路由 | React Router | 6 |
| 状态管理 | Zustand | — |
| 国际化 | react-i18next | — |

---

## 当前进度

> 详细进度见 [`docs/progress.md`](docs/progress.md)

| 模块 | 状态 |
|------|------|
| 项目骨架 + 配置 | ✅ 完成 |
| 用户管理（CRUD + 状态机 + 软删除） | ✅ 完成 |
| 数据库迁移（4 个版本） | ✅ 完成 |
| 前端（4 页面 + i18n 三语 + SaaS 级 UI） | ✅ 完成 |
| 角色 / 认证 / 租户 / 权限 | ⏳ 待实现 |
| AI 指摘管理（核心业务） | ⏳ 待实现 |

---

## 快速开始

### 环境要求

- Node.js 18+ & npm 9+
- Java JDK 17+
- Maven 3.9+（或使用 `./mvnw`）
- PostgreSQL 15+（端口 5432）
- Redis 7+（端口 6379）

### 1. 启动基础设施

```bash
docker compose up -d postgres redis
```

### 2. 数据库迁移

```bash
cd backend
./mvnw flyway:migrate
```

### 3. 启动后端

```bash
cd backend
./mvnw spring-boot:run
```

后端接口：http://localhost:8080

- Swagger UI: http://localhost:8080/swagger-ui.html
- 健康检查: http://localhost:8080/api/v1/health

### 4. 启动前端

```bash
cd frontend
npm install
npm run dev
```

前端页面：http://localhost:3000

---

## 项目结构

```
ai-fms/
├── CLAUDE.md                 ← 项目指导（架构规则 + 编码规范 + UI/UX 规则）
├── AGENTS.md                 ← 技术规范
├── .claude/
│   ├── settings.json         ← Hook 配置 + 权限
│   └── skills/
│       └── ui-review/        ← UI Review Skill
├── .mcp.json                 ← Playwright MCP 配置
│
├── backend/                  ← Spring Boot 3 WebFlux
│   └── src/main/java/com/aifms/
│       ├── config/           ← WebFlux, Security, R2DBC, Redis
│       ├── common/           ← Result<T>, 异常处理, ErrorCodes, PasswordHasher
│       ├── shared/           ← 跨模块共享
│       └── modules/
│           └── user/         ← 用户模块（DDD 四层）
│
├── frontend/                 ← React + TypeScript + Ant Design 5
│   └── src/
│       ├── api/              ← Axios 封装 + API 接口
│       ├── components/       ← 通用组件
│       ├── i18n/             ← 国际化（zh-CN, ja-JP, en-US）
│       ├── layouts/          ← MainLayout
│       ├── pages/            ← 页面组件（Dashboard, UserList, UserCreate, UserEdit）
│       ├── router/           ← 路由配置
│       ├── store/            ← Zustand 状态
│       ├── styles/           ← 全局样式
│       └── types/            ← TypeScript 类型
│
├── database/
│   └── migration/            ← Flyway SQL 脚本（V001–V004）
│
├── docs/
│   ├── progress.md           ← 项目进度报告
│   ├── worklog/              ← 每日工作日志
│   └── architecture.md       ← 架构决策记录
│
├── docker-compose.yml
└── docker-compose.override.example.yml
```

---

## 开发指南

请先阅读项目根目录下的指导文件：

| 文件 | 内容 |
|------|------|
| `CLAUDE.md` | 架构约束、编码规范、UI/UX 产品标准 |
| `AGENTS.md` | 详细技术规范（命名、禁止事项等） |
| `docs/progress.md` | 当前进度和下一步计划 |
| `docs/architecture.md` | 架构决策记录（ADR） |

### 开发工作流

```
1. 理解需求 → 阅读相关代码
2. 设计     → 输出实施计划，等待确认
3. 实现     → 按计划逐步修改
4. 自测     → 运行测试，确认通过
5. 记录     → 更新 docs/worklog/YYYY-MM-DD.md
```

### 国际化

所有面向用户的文字必须通过 `useTranslation().t()` 渲染。

支持语言：中文（zh-CN）、日本語（ja-JP）、English（en-US）。

---

## License

Proprietary. All rights reserved.
