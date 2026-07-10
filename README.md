# AI-FMS — AI 指摘管理系统

AI-FMS（AI Finding Management System）是面向企业客户的商业 SaaS 产品，用于 AI 辅助指摘（finding）管理。

业务全流程：**文件导入 → AI 解析理解 → 结构化整理 → 数据库保存 → 用户查看与处理**。

---

## 目录

- [技术栈](#技术栈)
- [当前进度](#当前进度)
- [系统架构](#系统架构)
- [快速开始](#快速开始)
- [项目结构](#项目结构)
- [后端架构](#后端架构)
- [API 文档](#api-文档)
- [数据库设计](#数据库设计)
- [前端架构](#前端架构)
- [国际化（i18n）](#国际化i18n)
- [开发工作流](#开发工作流)
- [编码规范](#编码规范)
- [测试](#测试)
- [常见问题](#常见问题)
- [部署](#部署)
- [参考文档](#参考文档)

---

## 技术栈

| 层级 | 技术 | 版本 | 说明 |
|------|------|------|------|
| **语言** | Java | 17+ | LTS 长期支持 |
| **框架** | Spring Boot 3 + WebFlux | 3.x | 禁止 Spring MVC |
| **安全** | Spring Security WebFlux | — | 响应式安全链 |
| **数据库访问** | Spring Data R2DBC | — | 禁止 JPA / MyBatis / JDBC |
| **数据库** | PostgreSQL | 15+ | — |
| **缓存** | Redis（Reactive） | 7+ | Spring Data Redis Reactive |
| **迁移工具** | Flyway | — | 禁止 Hibernate DDL auto |
| **构建工具** | Maven (mvnw) | 3.9+ | — |
| **前端框架** | React + TypeScript | 18 | strict mode |
| **UI 组件** | Ant Design | 5 | ConfigProvider 主题定制 |
| **构建工具** | Vite | 6 | — |
| **路由** | React Router | 6 | — |
| **状态管理** | Zustand | — | 客户端状态 |
| **HTTP 客户端** | Axios | — | 统一拦截 + Result<T> 解包 |
| **国际化** | react-i18next | — | 三语（zh-CN / ja-JP / en-US） |
| **容器化** | Docker + Docker Compose | — | — |

---

## 当前进度

> 详细进度见 [`docs/progress.md`](docs/progress.md)

```
✅ 项目骨架 + 全局配置（WebFlux, Security, R2DBC, Redis）
✅ 通用组件（Result<T>, GlobalExceptionHandler, ErrorCodes, PasswordHasher）
✅ 用户管理模块 — CRUD + 状态机 + 软删除 + 密码加密 + 登录锁定
✅ 错误码体系 — YAML 权威定义 + Java 常量
✅ 数据库迁移 — Flyway V001–V004
✅ 前端 4 页面 — Dashboard / 用户列表 / 创建 / 编辑
✅ 前端 i18n 三语 — zh-CN / ja-JP / en-US
✅ 前端 UI — SaaS 产品级（设计系统 + 主题 + 四态覆盖）
✅ 后端测试 — 25 个单元测试全绿

⏳ 角色管理模块
⏳ 认证模块（Spring Security WebFlux + JWT）
⏳ 租户管理模块
⏳ 权限管理模块
⏳ AI 指摘管理（核心业务）
```

---

## 系统架构

```
┌──────────────────────────────────────────────────────────┐
│                       Nginx (端口 80)                      │
│                  静态资源 + API 反向代理                      │
└──────────┬───────────────────────────────┬───────────────┘
           │                               │
     ┌─────▼──────┐                  ┌─────▼──────┐
     │  Frontend   │                  │  Backend    │
     │  React 18   │  ──HTTP/REST──▶  │  Spring     │
     │  Vite 6     │                  │  Boot 3     │
     │  端口 3000   │                  │  WebFlux    │
     └────────────┘                  │  端口 8080   │
                                      └──┬──────┬──┘
                                         │      │
                                  ┌──────▼┐ ┌───▼───┐
                                  │ Post- │ │ Redis │
                                  │ greSQL│ │  7+   │
                                  │  15+  │ │ 缓存   │
                                  └───────┘ └───────┘
```

### 后端四层 DDD 架构

```
┌─────────────────────────────────────────────┐
│              Presentation 层                  │
│  Controller — 参数校验 → 调 Service → Result │
│  禁止写业务逻辑，禁止直接访问 Repository         │
└──────────────────┬──────────────────────────┘
                   │
┌──────────────────▼──────────────────────────┐
│              Application 层                   │
│  ApplicationService — 编排业务流程             │
│  事务管理、DTO 转换、权限校验                    │
└──────────────────┬──────────────────────────┘
                   │
┌──────────────────▼──────────────────────────┐
│                Domain 层                      │
│  DomainService + Entity — 纯领域逻辑           │
│  状态机、业务规则、不依赖框架                     │
└──────────────────┬──────────────────────────┘
                   │
┌──────────────────▼──────────────────────────┐
│            Infrastructure 层                  │
│  Repository + Entity — 数据持久化              │
│  R2DBC、Flyway                               │
└─────────────────────────────────────────────┘
```

---

## 快速开始

### 环境要求

| 工具 | 最低版本 | 检查命令 |
|------|----------|----------|
| Node.js | 18+ | `node -v` |
| npm | 9+ | `npm -v` |
| Java JDK | 17+ | `java -version` |
| Maven | 3.9+ | `./mvnw --version` |
| PostgreSQL | 15+ | `psql --version` |
| Redis | 7+ | `redis-cli --version` |
| Docker | 24+ | `docker --version` |

### 1. 克隆项目

```bash
git clone <repo-url>
cd ai-fms
```

### 2. 启动基础设施

```bash
docker compose up -d postgres redis
```

检查服务状态：
```bash
docker compose ps
```

### 3. 数据库迁移

```bash
cd backend
./mvnw flyway:migrate
```

验证迁移：
```bash
./mvnw flyway:info
# 应显示 Version 001–004 均为 Success
```

### 4. 启动后端

```bash
./mvnw spring-boot:run
```

**可用端点：**

| 地址 | 用途 |
|------|------|
| http://localhost:8080 | 后端 API |
| http://localhost:8080/swagger-ui.html | Swagger UI |
| http://localhost:8080/api/v1/users | 用户列表接口 |
| http://localhost:8080/api/v1/health | 健康检查 |

### 5. 启动前端

```bash
cd frontend
npm install
npm run dev
```

前端页面：http://localhost:3000

### 6. 验证

```bash
# 创建用户
curl -X POST http://localhost:8080/api/v1/users \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","email":"admin@example.com","password":"admin123"}'

# 列表查询
curl http://localhost:8080/api/v1/users?page=1&size=10

# 前端页面
open http://localhost:3000
```

---

## 项目结构

```
ai-fms/
│
├── CLAUDE.md                      ← 项目指导（角色 + 工作流 + 架构规则 + UI/UX 标准）
├── AGENTS.md                      ← 技术规范（命名、禁止事项、代码示例）
├── README.md                      ← 本文件
├── .gitignore                     ← Git 忽略规则
├── .mcp.json                      ← MCP 服务器配置（Playwright）
├── docker-compose.yml             ← 开发环境（PostgreSQL + Redis）
│
├── .claude/                       ← Claude Code 项目配置
│   ├── settings.json              ← Hook（log-change / session-summary）+ 权限
│   ├── skills/                    ← 可复用任务模板
│   │   ├── backend-feature.md     ← 新后端模块
│   │   ├── frontend-feature.md    ← 新前端页面
│   │   ├── database-change.md     ← 数据库变更
│   │   ├── code-review.md         ← 代码审查
│   │   ├── bug-fix.md             ← Bug 修复
│   │   ├── refactor.md            ← 重构
│   │   ├── release.md             ← 发布
│   │   └── ui-review/             ← UI 产品标准审查
│   └── scripts/
│       ├── log-change.py          ← 文件修改自动留痕
│       └── session-summary.py     ← 会话结束摘要
│
├── backend/                       ← Spring Boot 3 WebFlux 后端
│   ├── pom.xml                    ← Maven 依赖
│   ├── mvnw / mvnw.cmd            ← Maven Wrapper
│   └── src/
│       ├── main/java/com/aifms/
│       │   ├── Application.java           ← 启动入口
│       │   ├── config/                    ← Spring 配置
│       │   │   ├── SecurityConfig.java    ← WebFlux Security
│       │   │   ├── WebFluxConfig.java     ← CORS + 路由
│       │   │   ├── R2dbcConfig.java       ← PostgreSQL R2DBC
│       │   │   ├── RedisConfig.java       ← Redis Reactive
│       │   │   ├── OpenApiConfig.java     ← Swagger
│       │   │   └── GlobalExceptionHandler.java ← 全局异常处理
│       │   ├── common/                    ← 通用组件（跨模块复用）
│       │   │   ├── Result.java            ← 统一响应体 {code, message, data}
│       │   │   ├── ErrorCodes.java        ← 错误码常量
│       │   │   ├── dto/PageResult.java    ← 分页结果
│       │   │   ├── exception/             ← BusinessException, ResourceNotFoundException
│       │   │   └── security/              ← PasswordHasher 接口 + BCrypt 实现
│       │   ├── shared/                    ← 跨模块共享
│       │   │   └── entity/BaseEntity.java ← 基础 Entity（id, createdAt, updatedAt）
│       │   └── modules/                   ← 业务模块（DDD 四层）
│       │       ├── auth/presentation/     ← HealthController
│       │       └── user/                  ← 用户模块
│       │           ├── presentation/      ← UserController + DTO
│       │           ├── application/       ← UserApplicationService
│       │           ├── domain/            ← User, UserStatus, UserDomainService
│       │           └── infrastructure/    ← UserEntity, UserRepository, UserMapper
│       ├── main/resources/
│       │   ├── application.yml            ← 应用配置
│       │   └── error-codes.yml            ← 错误码权威定义
│       └── test/java/com/aifms/modules/user/
│           ├── application/UserApplicationServiceTest.java
│           ├── domain/UserDomainServiceTest.java
│           ├── domain/UserStatusTest.java
│           └── domain/UserTest.java
│
├── frontend/                      ← React 18 + TypeScript 前端
│   ├── package.json               ← npm 依赖
│   ├── vite.config.ts             ← Vite 配置（dev 端口 3000 + proxy → 8080）
│   ├── tsconfig.json              ← TypeScript 配置（strict）
│   ├── index.html                 ← HTML 入口
│   └── src/
│       ├── main.tsx               ← React 入口（ConfigProvider 主题 + locale）
│       ├── App.tsx                ← 根组件
│       ├── api/                   ← Axios 封装 + API 接口定义
│       │   ├── client.ts          ← Axios 实例（拦截器 + Result 解包）
│       │   ├── userApi.ts         ← 用户模块 6 个 API
│       │   └── healthApi.ts       ← 健康检查
│       ├── components/            ← 通用组件（待扩充）
│       ├── hooks/                 ← 自定义 Hooks
│       ├── i18n/                  ← 国际化
│       │   ├── index.ts           ← i18next 配置（语言检测 + 初始化）
│       │   └── locales/
│       │       ├── zh-CN.json     ← 中文（默认回退）
│       │       ├── ja-JP.json     ← 日本語
│       │       └── en-US.json     ← English
│       ├── layouts/
│       │   └── MainLayout.tsx     ← 主布局（Sider + Header + Breadcrumb + Footer）
│       ├── pages/
│       │   ├── HomePage.tsx       ← Dashboard
│       │   ├── UserListPage.tsx   ← 用户列表 + 搜索 + 分页
│       │   ├── UserCreatePage.tsx ← 创建用户表单
│       │   └── UserEditPage.tsx   ← 编辑用户表单
│       ├── router/
│       │   └── index.tsx          ← 路由配置（5 条路由）
│       ├── store/
│       │   └── useAppStore.ts     ← Zustand 全局状态
│       ├── styles/
│       │   └── global.css         ← 全局设计系统（动画 + 布局 + 工具类）
│       ├── types/
│       │   ├── index.ts           ← 通用类型
│       │   └── user.ts            ← User, CreateUserRequest, UpdateUserRequest
│       └── utils/                 ← 工具函数
│
├── database/                      ← 数据库
│   └── migration/                 ← Flyway SQL 迁移
│       ├── V001__init.sql         ← 扩展 + 函数
│       ├── V002__create_users.sql ← users 表
│       ├── V003__partial_unique_indexes.sql ← Partial Unique Index
│       └── V004__drop_indexes_and_constraints.sql ← 简化约束
│
├── docs/                          ← 文档
│   ├── progress.md                ← 项目进度报告
│   ├── architecture.md            ← 架构决策记录（ADR）
│   ├── worklog/                   ← 每日工作日志
│   │   ├── 2026-07-10.md
│   │   └── 2026-07-11.md
│   └── adr/                       ← ADR 详细记录
│
├── nginx/                         ← Nginx 配置
├── scripts/                       ← 运维脚本
└── docker-compose.yml             ← Docker Compose
```

---

## 后端架构

### 用户模块数据流

```
HTTP Request
    │
    ▼
┌──────────────────────────────────────────────┐
│ UserController                               │
│   → 参数校验（@Valid）                          │
│   → 调用 ApplicationService                    │
│   → 返回 Result<T>                            │
└──────────────────┬───────────────────────────┘
                   │
┌──────────────────▼───────────────────────────┐
│ UserApplicationService                       │
│   → 业务编排                                    │
│   → 唯一性校验（username / email）               │
│   → 密码加密（PasswordHasher）                   │
│   → 状态机校验（UserDomainService）              │
│   → 事务包裹（TransactionalOperator）           │
└──────────────────┬───────────────────────────┘
                   │
┌──────────────────▼───────────────────────────┐
│ UserDomainService                            │
│   → validateStatusTransition()                │
│   → validatePasswordStrength()                │
│                                               │
│ User (Domain Object)                          │
│   → changeStatus(), softDelete()              │
│   → recordLoginFailure(), recordLoginSuccess()│
│   → isLocked(), changePassword()              │
│                                               │
│ UserStatus (Enum + State Machine)             │
│   ACTIVE ↔ LOCKED ↔ DISABLED → DELETED        │
│   canTransitionTo(target): boolean            │
└──────────────────┬───────────────────────────┘
                   │
┌──────────────────▼───────────────────────────┐
│ UserRepository (R2DBC)                       │
│   → findByUsername(), findByEmail()          │
│   → findAllNonDeleted(), findByIdNonDeleted()│
│   → 手写 @Query（过滤 DELETED）               │
│                                               │
│ UserEntity (@Table "users")                   │
│   → 持久化专用，零领域逻辑                       │
│                                               │
│ UserMapper (static)                           │
│   → Entity ↔ Domain 双向转换                   │
└──────────────────────────────────────────────┘
```

### UserStatus 状态机

```
         ┌─────────┐
         │  ACTIVE  │
         └────┬─────┘
        ┌─────┼─────────┐
        ▼     ▼          ▼
   ┌────────┐ ┌──────────┐ ┌────────┐
   │ LOCKED │ │ DISABLED │ │ DELETED│ (终态)
   └───┬────┘ └────┬─────┘ └────────┘
       │            │
       └──→ ACTIVE ←┘
```

转移规则：`canTransitionTo()` 校验，非法转移抛出 `BusinessException(40011)`。

---

## API 文档

### 基础 URL

```
http://localhost:8080/api/v1
```

### 统一响应格式

```json
{
  "code": 200,
  "message": "操作成功",
  "data": { ... },
  "timestamp": "2026-07-11T12:00:00Z"
}
```

### 用户接口

| 方法 | 路径 | 说明 | 请求体 |
|------|------|------|--------|
| `GET` | `/users` | 分页列表 + 搜索 | Query: `keyword`, `page`(≥1), `size`(1-100) |
| `GET` | `/users/{id}` | 查询单个 | — |
| `POST` | `/users` | 创建用户 | `{username, email, password, displayName?, phone?}` |
| `PUT` | `/users/{id}` | 更新用户（diff） | `{username?, email?, password?, displayName?, phone?, status?}` |
| `DELETE` | `/users/{id}` | 软删除 | — |
| `GET` | `/health` | 健康检查 | — |

### 请求示例

```bash
# 创建用户
curl -X POST http://localhost:8080/api/v1/users \
  -H "Content-Type: application/json" \
  -d '{
    "username": "zhangsan",
    "email": "zhangsan@example.com",
    "password": "password123",
    "displayName": "张三",
    "phone": "+86 13800000000"
  }'

# 分页搜索
curl "http://localhost:8080/api/v1/users?keyword=zhang&page=1&size=20"

# 更新状态
curl -X PUT http://localhost:8080/api/v1/users/{id} \
  -H "Content-Type: application/json" \
  -d '{"status": "LOCKED"}'

# 软删除
curl -X DELETE http://localhost:8080/api/v1/users/{id}
```

### 错误码

| 错误码 | HTTP 状态 | 含义 | 触发场景 |
|--------|-----------|------|----------|
| `200` | 200 | 成功 | — |
| `40001` | 400 | 参数校验失败 | `@Valid` 校验不通过 |
| `40002` | 400 | 用户名已存在 | 创建/更新时 username 重复 |
| `40003` | 400 | 邮箱已存在 | 创建/更新时 email 重复 |
| `40010` | 400 | 密码强度不足 | 密码 < 8 字符 |
| `40011` | 400 | 状态转换非法 | 违反状态机规则 |
| `40401` | 404 | 用户不存在 | ID 无效或已删除 |
| `50001` | 500 | 服务器内部错误 | 未预期异常 |

**编码规则：** `4xxxx` 客户端错误，`5xxxx` 服务端错误。第二段按业务域划分：`x001–x009` Common，`x010–x029` User，`x030–x049` Role...

权威定义文件：`backend/src/main/resources/error-codes.yml`

---

## 数据库设计

### users 表

| 列名 | 类型 | 约束 | 说明 |
|------|------|------|------|
| `id` | UUID | PK, DEFAULT gen_random_uuid() | 主键 |
| `username` | VARCHAR(50) | NOT NULL | 用户名 |
| `email` | VARCHAR(255) | NOT NULL | 邮箱 |
| `password_hash` | VARCHAR(255) | NOT NULL | BCrypt 哈希 |
| `display_name` | VARCHAR(100) | — | 显示名称 |
| `phone` | VARCHAR(30) | — | 手机号 |
| `status` | VARCHAR(20) | NOT NULL, DEFAULT 'ACTIVE' | ACTIVE/LOCKED/DISABLED/DELETED |
| `tenant_id` | UUID | — | 租户 ID（多租户预留） |
| `last_login_at` | TIMESTAMPTZ | — | 最后登录时间 |
| `password_changed_at` | TIMESTAMPTZ | NOT NULL, DEFAULT NOW() | 密码修改时间 |
| `failed_login_count` | INT | NOT NULL, DEFAULT 0 | 登录失败次数 |
| `locked_until` | TIMESTAMPTZ | — | 锁定到期时间 |
| `created_at` | TIMESTAMPTZ | NOT NULL, DEFAULT NOW() | 创建时间 |
| `updated_at` | TIMESTAMPTZ | NOT NULL, DEFAULT NOW() | 更新时间 |

**设计决策：**
- 数据库只保留 NOT NULL + DEFAULT + PRIMARY KEY，复杂约束由后端应用层校验
- 无索引、无 CHECK 约束、无触发器（性能优化延后）
- 已删除用户标识符可复用（Partial Unique Index 排除 DELETED 状态）

---

## 前端架构

### 组件树

```
<App>
  <BrowserRouter>
    <ConfigProvider theme={...} locale={...}>
      <MainLayout>                          ← 全局布局
        ├── Sider                           ← 固定侧边栏
        │   ├── Logo (SafetyOutlined + "AI-FMS")
        │   ├── Menu (Dashboard / Users / Roles / Tenants / Teams)
        │   └── Language Switcher [ ZH | JA | EN ]
        │
        ├── Header                          ← 顶部栏（sticky）
        │   ├── Collapse Button
        │   ├── Breadcrumb (自动生成)
        │   └── Avatar Dropdown (Profile / Logout)
        │
        └── Content                         ← 内容区
            └── <Outlet />                  ← 路由页面
                ├── / → HomePage            ← Dashboard
                ├── /users → UserListPage   ← 用户列表
                ├── /users/create → UserCreatePage ← 创建用户
                └── /users/:id/edit → UserEditPage ← 编辑用户
      </MainLayout>
    </ConfigProvider>
  </BrowserRouter>
</App>
```

### 路由表

| 路径 | 页面 | 说明 |
|------|------|------|
| `/` | HomePage | Dashboard |
| `/users` | UserListPage | 用户列表 + 搜索 + 分页 |
| `/users/create` | UserCreatePage | 创建用户表单 |
| `/users/:id/edit` | UserEditPage | 编辑用户表单 |
| `*` | → `/` | 404 重定向 |

### 状态管理

- **服务端状态：** Axios → 组件内 `useState`（数据获取、加载、错误）
- **客户端状态：** Zustand（`useAppStore`）→ 全局 UI 状态（侧边栏折叠、当前语言）
- **表单状态：** Ant Design `Form.useForm()`
- **URL 状态：** React Router（页码、搜索关键词通过 search params）

### 数据流

```
User Action
    │
    ▼
Page Component (useState: loading, error, data)
    │
    ▼
userApi.getById(id) / userApi.list(params) / userApi.create(body)
    │
    ▼
Axios Instance (client.ts)
    ├── Request Interceptor: 添加 Content-Type
    └── Response Interceptor:
        ├── 2xx → 解包 Result<T>.data
        └── non-2xx → 构造 ApiError { code, message }
    │
    ▼
Backend /api/v1/users
    │
    ▼
Page Component
    ├── setData()    → 渲染 Table / Statistic / Form
    ├── setError()   → Alert 组件
    └── setLoading() → Skeleton / Spin
```

---

## 国际化（i18n）

### 架构

```
i18next (react-i18next)
    │
    ├── 语言检测: localStorage → navigator.language → zh-CN（回退）
    │
    ├── 资源文件:
    │   ├── zh-CN.json  ← 中文（默认）
    │   ├── ja-JP.json  ← 日本語
    │   └── en-US.json  ← English
    │
    ├── Ant Design 同步: ConfigProvider locale
    │   └── 切换语言后 window.location.reload() 同步组件文案
    │
    └── 错误码映射: ApiError.code → t(`error.${code}`)
```

### 使用方法

```tsx
import { useTranslation } from 'react-i18next';

function MyComponent() {
  const { t } = useTranslation();

  return (
    <div>
      <h1>{t('user.title')}</h1>                    {/* 简单键 */}
      <p>{t('user.userCount', { count: 5 })}</p>    {/* 插值 */}
      <span>{t('userStatus.ACTIVE')}</span>          {/* 状态映射 */}
    </div>
  );
}
```

### 添加新语言

1. 创建 `frontend/src/i18n/locales/{lang}.json`
2. 复制 `zh-CN.json` 的结构，翻译所有值
3. 在 `i18n/index.ts` 的 `SUPPORTED_LANGS` 中添加条目
4. 在 `main.tsx` 的 `antdLocales` 中添加对应 locale

---

## 开发工作流

```
1. 理解需求 → 阅读相关代码，确认范围
       │
2. 设计     → 输出实施计划（涉及文件 + 步骤），等待确认
       │
3. 实现     → 按计划逐步修改代码
       │
4. 自测     → ./mvnw test  &&  npm run build
       │
5. 记录     → 更新 docs/worklog/YYYY-MM-DD.md
       │
6. 提交     → git commit -m "类型: 简述" && git push
```

### 常用命令

```bash
# ── 后端 ──
./mvnw spring-boot:run        # 启动（端口 8080）
./mvnw test                    # 运行测试
./mvnw flyway:migrate          # 执行迁移
./mvnw flyway:info             # 迁移状态
./mvnw clean package           # 打包

# ── 前端 ──
cd frontend
npm run dev                    # 启动（端口 3000）
npm run build                  # 生产构建
npm run lint                   # ESLint 检查

# ── 基础设施 ──
docker compose up -d           # 启动所有服务
docker compose logs -f backend # 后端日志
docker compose down            # 停止所有服务
```

### 新增业务模块流程

以新增 `Role` 模块为例：

```
1. 数据库：database/migration/V005__create_roles.sql
2. 后端：
   backend/.../modules/role/
   ├── presentation/    → RoleController + DTO
   ├── application/     → RoleApplicationService
   ├── domain/          → Role + RoleDomainService
   └── infrastructure/  → RoleEntity + RoleRepository + RoleMapper
3. 前端：
   frontend/src/
   ├── types/role.ts
   ├── api/roleApi.ts
   └── pages/RoleListPage.tsx / RoleCreatePage.tsx / RoleEditPage.tsx
4. 路由：frontend/src/router/index.tsx 添加路由
5. i18n：三个 locale JSON 添加 role.* 键
```

---

## 编码规范

详细规范见 `CLAUDE.md` 和 `AGENTS.md`。核心约束：

### 后端（硬性）

| 规则 | 说明 |
|------|------|
| 禁止 Spring MVC | 不引入 `spring-boot-starter-web` |
| 禁止 JPA / MyBatis / JDBC | 只使用 R2DBC |
| 禁止 `.block()` | 全链路 Mono/Flux |
| Controller 禁止写业务逻辑 | 只做 参数校验 → 调 Service → 返回 Result |
| Entity ↔ DTO 分离 | Entity 只表示数据库模型，不暴露到 Controller |
| 数据库变更只通过 Flyway | 禁止手动改库、禁止 Hibernate DDL auto |
| 所有注释必须中文 | Javadoc 格式，含 @param / @return / @throws |

### 前端（硬性）

| 规则 | 说明 |
|------|------|
| 所有用户可见字符串必须 `t()` | 禁止硬编码任何语言文本 |
| TypeScript strict mode | 禁止 `any`（除非有充分理由） |
| 四态必须覆盖 | Loading / Empty / Error / Normal |
| 使用 ConfigProvider theme | 禁止页面内联颜色（状态色除外） |

### UI 产品标准

这是面向客户交付的商业 SaaS，不是内部系统：
- 不能看起来像"后端程序员写的 CRUD"
- 页头 + Card 包裹 + 合理间距
- 表单逻辑分组 + 按钮右对齐
- Empty 状态有图标 + 引导文案
- Error 状态可重试

---

## 测试

### 后端测试

```bash
cd backend
./mvnw test
```

测试结构：
```
src/test/java/com/aifms/modules/user/
├── domain/
│   ├── UserStatusTest.java           ← 状态机转移规则
│   ├── UserTest.java                 ← 领域对象方法
│   └── UserDomainServiceTest.java    ← 密码校验
└── application/
    └── UserApplicationServiceTest.java ← 6 个业务场景（Mock Repository）
```

### 前端构建检查

```bash
cd frontend
npm run build    # tsc + vite build，有 TypeScript 错误会立即暴露
```

---

## 常见问题

### PostgreSQL 连接失败

```bash
# 检查服务状态
docker compose ps

# 检查端口
lsof -i :5432

# 重建容器
docker compose down -v
docker compose up -d postgres
```

### Flyway 迁移失败

```bash
# 查看迁移状态
./mvnw flyway:info

# 修复失败后重新执行
./mvnw flyway:repair
./mvnw flyway:migrate
```

### 前端热更新不生效

```bash
# 清除 Vite 缓存
cd frontend
rm -rf node_modules/.vite
npm run dev
```

### 中文 curl 乱码（Windows）

```bash
# 不要用命令行直接传中文，先用 Write 工具创建 JSON 文件
# 然后用 --data-binary @file.json 发送
```

---

## 部署

### Docker Compose 部署

```bash
# 构建镜像
docker compose build

# 启动
docker compose up -d

# 查看日志
docker compose logs -f
```

### 生产构建

```bash
# 后端打包
cd backend && ./mvnw clean package -DskipTests

# 前端打包
cd frontend && npm run build
# 产出在 frontend/dist/，由 Nginx 提供静态服务
```

### 环境变量

| 变量 | 默认值 | 说明 |
|------|--------|------|
| `POSTGRES_DB` | `aifms` | 数据库名 |
| `POSTGRES_USER` | `aifms` | 数据库用户 |
| `POSTGRES_PASSWORD` | `aifms` | 数据库密码 |
| `REDIS_PASSWORD` | — | Redis 密码 |
| `SPRING_PROFILES_ACTIVE` | `dev` | Spring Profile |

---

## 参考文档

| 文档 | 内容 |
|------|------|
| [`CLAUDE.md`](CLAUDE.md) | 项目指导（角色 + 工作流 + 架构规则 + UI/UX 标准） |
| [`AGENTS.md`](AGENTS.md) | 技术规范（命名、禁止事项） |
| [`docs/progress.md`](docs/progress.md) | 项目进度 + 下一步计划 |
| [`docs/architecture.md`](docs/architecture.md) | 架构决策记录（ADR 001–007） |
| [`docs/branching-strategy.md`](docs/branching-strategy.md) | Git 分支管理规范 |
| [`docs/worklog/`](docs/worklog/) | 每日工作日志 |
