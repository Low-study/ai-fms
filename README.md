# AI-FMS — AI 指摘管理系统

AI-FMS（AI Finding Management System）是面向企业客户的商业 SaaS 产品，用于 AI 辅助指摘（finding）管理。

业务全流程：**文件导入 → AI 解析理解 → 结构化整理 → 数据库保存 → 用户查看与处理 → RAG 相似案例检索**。

---

## 目录

- [环境要求](#环境要求)
- [快速开始](#快速开始)
- [技术栈](#技术栈)
- [当前进度](#当前进度)
- [系统架构](#系统架构)
- [Agent 管道详解](#agent-管道详解)
- [项目结构](#项目结构)
- [API 文档](#api-文档)
- [数据库设计](#数据库设计)
- [前端架构](#前端架构)
- [环境变量参考](#环境变量参考)
- [开发工作流](#开发工作流)
- [编码规范](#编码规范)
- [测试](#测试)
- [常见问题](#常见问题)
- [部署](#部署)
- [WSL 迁移指南](#wsl-迁移指南)
- [参考文档](#参考文档)

---

## 环境要求

| 工具 | 最低版本 | 验证命令 | 说明 |
|------|----------|----------|------|
| Node.js | 18+ | `node -v` | 前端构建与 Vite dev server |
| npm | 9+ | `npm -v` | 包管理 |
| Java JDK | 21 | `java -version` | 运行 Spring Boot 3.5.x |
| Maven | 3.9+ | `./mvnw --version` | 后端构建与 Flyway 迁移 |
| PostgreSQL | 15+（已实测17） | `psql -U postgres -c "SELECT version();"` | 主数据库 + pgvector 扩展 |
| Redis | 5+（已实测5.0.14） | `redis-cli ping` | 缓存 + Streams 任务队列 |
| MinIO | 最新稳定版 | `minio.exe server` | S3 兼容文件存储 |
| Ollama | 0.3+ | `ollama --version` | 本地 embedding 推理（BGE-M3） |
| DeepSeek API Key | — | 注册 [platform.deepseek.com](https://platform.deepseek.com) | LLM 文本生成（Agent 管道核心） |

### 推荐额外工具

| 工具 | 用途 |
|------|------|
| **WSL 2 + Ubuntu 24.04** | 替换裸 Windows 开发环境，所有基础设施跑在 Linux 侧 |
| **Docker 24+** | 可选——若无 Docker，PostgreSQL/Redis/MinIO 均支持原生 Windows 安装 |

---

## 快速开始

> 以下步骤基于 **Windows 11 原生环境**（不在 Docker 内，不在 WSL 内），覆盖了首次启动时所有已被验证的阻塞点和解决方案。

### 1. 克隆项目

```bash
git clone https://github.com/Low-study/ai-fms.git
cd ai-fms
```

### 2. 启动 PostgreSQL（原生 Windows）

本项目使用原生 PostgreSQL 17（非 Docker）。

```bash
# 如已安装为服务，确认运行状态
Get-Service -Name "postgresql*" | Select-Object Name, Status

# 如未安装，从 https://www.enterprisedb.com/downloads/postgresql-postgresql-downloads 下载安装
# 安装时记住超级用户密码，默认用户名 postgres

# 验证连接
psql -U postgres -c "SELECT 1;"
```

### 3. 启动 Redis（原生 Windows）

从 GitHub 下载 [Redis-x64-5.0.14.1.zip](https://github.com/tporadowski/redis/releases/download/v5.0.14.1/Redis-x64-5.0.14.1.zip)，解压后直接运行：

```bash
# 启动 Redis 服务
redis-server.exe

# 新终端验证
redis-cli ping
# 预期输出: PONG
```

### 4. 启动 MinIO（原生 Windows）

从 [MinIO 下载页](https://dl.min.io/server/minio/release/windows-amd64/minio.exe) 下载 `minio.exe`：

```bash
# 创建数据目录并启动
mkdir D:\ai-fms\data\minio
minio.exe server D:\ai-fms\data\minio --console-address :9001

# 控制台 http://localhost:9001，默认凭据 minioadmin / minioadmin
# API 端口 9000
```

### 5. 安装并启动 Ollama（本地 BGE-M3 embedding）

```bash
# 从 https://ollama.com/download/windows 下载安装，或
winget install Ollama.Ollama

# 启动 Ollama 后台服务
ollama serve

# 拉取 BGE-M3 模型（1.2GB，一次操作）
ollama pull bge-m3
```

### 6. 安装 pgvector 扩展（仅一次）

```bash
# 下载与 PostgreSQL 17 匹配的 pgvector 预编译包
# https://github.com/pgvector/pgvector/releases

# 将 vector.dll 复制到 PostgreSQL lib 目录
# 将 vector.control 和 sql 文件复制到 extension 目录
# 详见 pgvector 官方文档
```

### 7. 运行数据库迁移

```bash
cd backend

# 设置数据库凭据
$env:DB_USERNAME = "postgres"
$env:DB_PASSWORD = "你的密码"

# 执行全部迁移（V001-V011）
./mvnw flyway:migrate -Dflyway.user=postgres -Dflyway.password=你的密码

# 验证
./mvnw flyway:info -Dflyway.user=postgres -Dflyway.password=你的密码
# 预期: V001-V011 全部 Success
```

> **注意**: 如果 Flyway 认证失败（`user 'null'`），使用环境变量方式：
> ```bash
> $env:DB_USERNAME = "postgres"; $env:DB_PASSWORD = "密码"
> ./mvnw flyway:migrate
> ```

### 8. 设置 LLM API Key

```bash
# DeepSeek API key（在 https://platform.deepseek.com 生成）
$env:LLM_API_KEY = "sk-xxxxxxxxxxxxxxxx"
$env:LLM_BASE_URL = "https://api.deepseek.com"
```

### 9. 启动后端

```bash
cd backend
$env:LLM_API_KEY = "sk-xxxxxxxxxxxxxxxx"
$env:LLM_BASE_URL = "https://api.deepseek.com"
$env:JAVA_TOOL_OPTIONS = "-Dfile.encoding=UTF-8"   # 解决中文日志乱码
./mvnw spring-boot:run
```

**预期输出**:
```
Started Application in 3.5 seconds
Netty started on port 8080
MinIO 桶 'aifms' 已自动创建
Embedding 模型初始化完成: baseUrl=http://localhost:11434/v1, model=bge-m3
```

### 10. 启动前端

```bash
cd frontend
npm install   # 首次运行
npm run dev
```

前端页面：**http://localhost:3000**

### 11. 验证全栈

```bash
# 健康检查
curl http://localhost:8080/api/v1/health
# → {"code":0,"message":"success","data":"OK","success":true}

# 指摘列表
curl http://localhost:8080/api/v1/findings?page=1&size=5
# → 分页 JSON 返回（初次为空列表）

# 创建测试指摘
curl -X POST http://localhost:8080/api/v1/findings \
  -H "Content-Type: application/json" \
  -d '{"title":"test"}'

# 测试 AI 导入（上传 test-samples/issue-01-login-failure-ja.txt）
curl -X POST http://localhost:8080/api/v1/agents/import \
  -F "file=@D:\ai-fms\test-samples\issue-01-login-failure-ja.txt"

# 前端: 打开 http://localhost:3000 → 点 "指摘一覧" → 点 "导入指摘" → 拖文件
```

---

## 技术栈

| 层级 | 技术 | 版本 | 说明 |
|------|------|------|------|
| **语言** | Java | 21 | LTS |
| **框架** | Spring Boot 3 + WebFlux | 3.5.16 | 禁止 Spring MVC |
| **安全** | Spring Security WebFlux | — | 响应式安全链（当前全放行） |
| **数据库访问** | Spring Data R2DBC | — | 禁止 JPA / MyBatis / JDBC |
| **数据库** | PostgreSQL | 17 | 主数据库 |
| **向量扩展** | pgvector | 0.8.5 | 嵌入 PostgreSQL，HNSW 余弦索引 |
| **缓存 / 队列** | Redis | 5.0.14 / 7+ | 缓存 + Streams 任务队列（包在 modules/task 抽象后） |
| **迁移工具** | Flyway | — | V001–V011，禁止 Hibernate DDL auto |
| **文件存储** | MinIO | 最新稳定版 | S3 兼容自托管，启动自动建桶 |
| **构建工具** | Maven (mvnw) | 3.9+ | — |
| **前端框架** | React + TypeScript | 18 | strict mode |
| **UI 组件** | Ant Design | 5 | ConfigProvider 主题定制 |
| **构建工具** | Vite | 6 | dev 端口 3000，proxy → 8080 |
| **路由** | React Router | 6 | — |
| **状态管理** | Zustand | — | 客户端状态 |
| **HTTP 客户端** | Axios | — | 统一拦截 + Result<T> 解包（SSE 场景用原生 fetch） |
| **国际化** | react-i18next | — | 三语（zh-CN / ja-JP / en-US） |
| **AI Agent** | LangChain4j | 1.0.0 | Agent Runtime + Tool Calling + Supervisor + sub-agent |
| **LLM（对话/抽取）** | DeepSeek | deepseek-chat | 经由 ChatModelPort 接口 |
| **LLM（向量化）** | BGE-M3 via Ollama | 1024维 | 本地运行，Ollama 提供 OpenAI 兼容 /v1/embeddings |
| **文档解析** | Apache Tika | 3.1.0 | PDF/Office 格式提取 |
| **MCP** | Spring AI 2.0 | 延 MVP-3 | MCP Server（外部契约延迟冻结） |

---

## 当前进度

> 详细进度见 [`docs/progress.md`](docs/progress.md)

```
✅ 项目骨架 + 全局配置（WebFlux, Security, R2DBC, Redis）
✅ 通用组件（Result<T>, GlobalExceptionHandler, ErrorCodes, PasswordHasher）
✅ 用户管理模块 — CRUD + 状态机 + 软删除 + 密码加密 + 登录锁定
✅ 错误码体系 — YAML 权威定义 + Java 常量
✅ 数据库迁移 — Flyway V001–V011
✅ 前端 7 页面 — Dashboard / 用户列表 / 创建 / 编辑 / 指摘一覧 / 指摘导入 / 指摘详情
✅ 前端 7 通用组件 — PageContainer / SearchForm / DataTable / StatusTag / EmptyState / ConfirmDialog / SseProgress
✅ 前端 i18n 三语 — zh-CN / ja-JP / en-US
✅ 前端 UI — SaaS 产品级（设计系统 + 主题 + 四态覆盖）
✅ AI Agent 引擎 — LangChain4j Agent Runtime + DeepSeek LLM + BGE-M3 Embedding + Apache Tika
✅ AI 指摘管理（核心业务）— 文件导入 → AI 解析 → 自动分类 → RAG 检索 → 处理建议 → 一览查看
✅ pgvector 向量存储 — 嵌入 PostgreSQL，HNSW 余弦检索，与业务表隔离
✅ MinIO 文件存储 — S3 兼容自托管，启动自动建桶
✅ Redis Streams 任务队列 — 长任务异步处理，TaskService<T> 接口抽象
✅ Agent 管道 — Supervisor + 3 sub-agent（IngestSubAgent / RagSubAgent / ReportQaSubAgent）
✅ ADR-008~013 — 6 条架构决策记录

⏳ 角色管理模块
⏳ 认证模块（Spring Security WebFlux + JWT）
⏳ 租户管理模块
⏳ 权限管理模块
⏳ MVP-3 MCP Server（spring-ai-mcp-server @McpTool 暴露外部接口）
```

---

## 系统架构

```
┌──────────────────────────────────────────────────────────────────┐
│                    前端  React 18 + AntD 5 + i18n 三语             │
│     HomePage / UserListPage / IssueListPage(指摘一覧)               │
│     IssueImportPage(SSE 进度) / IssueDetailPage(AI 结果)            │
│     components: PageContainer/DataTable/StatusTag/SseProgress...    │
└──────────────┬───────────────────────────────────────────────────┘
               │ REST + SSE
┌──────────────▼───────────────────────────────────────────────────┐
│                    Spring Boot 3.5.16 WebFlux  :8080               │
│  ┌─────────────────────────────────────────────────────────────┐ │
│  │ Presentation: FindingController / AgentController /          │ │
│  │   FileController / UserController                           │ │
│  ├─────────────────────────────────────────────────────────────┤ │
│  │ Application: FindingAppService / AgentAppService /           │ │
│  │   FileAppService / TaskAppService（手写 Sequential 编排）      │ │
│  ├─────────────────────────────────────────────────────────────┤ │
│  │ Domain: Finding+状态机 / 7 端口抽象 / 5 Skill domain 接口     │ │
│  │   ChatModelPort / EmbeddingModelPort / OcrPort /             │ │
│  │   DocumentParserPort / PromptTemplatePort /                  │ │
│  │   AgentExecutionLogPort / McpClientPort                      │ │
│  ├─────────────────────────────────────────────────────────────┤ │
│  │ Infrastructure:                                              │ │
│  │  • LangChain4jChatModelAdapter → DeepSeek API                │ │
│  │  • LangChain4jEmbeddingAdapter → Ollama BGE-M3               │ │
│  │  • TikaDocumentParser (PDF/text)                              │ │
│  │  • CloudOcrAdapter (mock placeholder)                         │ │
│  │  • PromptTemplateRepository (R2DBC)                            │ │
│  │  • AgentExecutionLogRepository (R2DBC)                        │ │
│  │  • LangChain4jMcpClient (@Lazy, 延时连接)                     │ │
│  │  • MinioFileStorageAdapter (启动自动建桶)                      │ │
│  │  • RedisStreamsTaskService (ReactiveRedisTemplate)            │ │
│  └─────────────────────────────────────────────────────────────┘ │
└──────┬──────────┬──────────┬──────────┬─────────────────────────┘
       │          │          │          │
  ┌────▼────┐ ┌──▼───┐ ┌───▼───┐ ┌───▼────────┐
  │PostgreSQL│ │Redis │ │MinIO  │ │ Ollama     │
  │ 17 +    │ │5/7+  │ │:9000  │ │ :11434     │
  │ pgvector│ │Stream│ │(S3)   │ │ BGE-M3     │
  │ :5432   │ │:6379 │ │       │ │ embedding  │
  └─────────┘ └──────┘ └───────┘ └────────────┘
```

### Agent 管道架构（MVP-1/MVP-2）

```
用户上传文件（txt/pdf/png）
         │
         v
  AgentController POST /api/v1/agents/import
         │
         v
  FileApplicationService  →  MinIO 存储
         │
         v
  FindingApplicationService.create(OPEN)
         │
         v
  TaskService.submit → Redis Streams
         │
         v
  SupervisorAgent 路由分发
         │
    ┌────┼────┐
    v    v    v
Ingest  RAG  ReportQa       ← 3 个 SubAgent 并行/顺序
(DkSk) (BGE) (DkSk)
    │    │    │
    v    v    v
  parsed  vec  report
  issue  sim   + qa
    │    │    │
    └────┼────┘
         v
  FindingApplicationService.update(CLASSIFIED) → PostgreSQL
         │
         v
  SSE 推送进度 → 前端 IssueImportPage
         │
         v
  IssueDetailPage 展示 AI 分类结果 + RAG 相似案例
```

---

## Agent 管道详解

### 三阶段流水线

| 阶段 | SubAgent | AI 模型 | 功能 | 产物 |
|------|----------|---------|------|------|
| **Ingest** | IngestSubAgent | DeepSeek chat | 解析文档文本 + 分类（category/priority/severity/system/assignee/tags） | ParsedIssue + ClassifiedIssue |
| **RAG** | RagSubAgent | BGE-M3 (Ollama) → pgvector | 查询文本向量化 → HNSW 余弦检索历史相似指摘 | SimilarIssues（含相似度得分） |
| **ReportQa** | ReportQaSubAgent | DeepSeek chat | 基于分类+相似案例生成处理报告与用户回复 | reportDraft + qaReply |

### 数据流（以日语 txt 为例）

```
输入: "ログインできませんでした。昨日アップデート後から発生。正しいパスワードでも認証エラー。"

1. IngestSubAgent
   └─ DeepSeek API chat → 解析+结构化
   └─ 输出: {title:"ログイン認証エラー", category:"認証異常", priority:"高",
             severity:"重大", system:"関係者認証システム", tags:"ログイン,認証,アップデート"}

2. RagSubAgent
   └─ BGE-M3(Ollama) embed(query) → float[1024]
   └─ pgvector: SELECT * FROM issue_embeddings ORDER BY embedding <-> query LIMIT 5
   └─ 返回: 相似历史指摘列表（含相似度 score）

3. ReportQaSubAgent
   └─ DeepSeek API chat → 结合分类+相似案例生成处理报告
   └─ 输出: reportDraft (完整的日语处理手順書) + qaReply (用户回复文案)
```

### 关键组件

| 组件 | 文件路径 | 说明 |
|------|----------|------|
| AgentController | `modules/agent/presentation/AgentController.java` | SSE 端点，接收 multipart → 启动管道 |
| AgentApplicationService | `modules/agent/application/AgentApplicationService.java` | 编排管道，进度推送，执行日志记录 |
| SupervisorAgent | `modules/agent/application/SupervisorAgent.java` | LangChain4j agentic Supervisor，路由 3 个 SubAgent |
| IngestSubAgent | `modules/agent/application/IngestSubAgent.java` | 解析+分类 |
| RagSubAgent | `modules/agent/application/RagSubAgent.java` | 向量检索 |
| ReportQaSubAgent | `modules/agent/application/ReportQaSubAgent.java` | 报告+回复 |
| KnowledgeRagSkillAdapter | `modules/agent/infrastructure/KnowledgeRagSkillAdapter.java` | pgvector 查询实现，embedding 失败降级保护 |

---

## 项目结构

```
ai-fms/
│
├── README.md                      ← 本文件（入门手册）
├── CLAUDE.md                      ← Claude Code 项目指导
├── AGENTS.md                      ← 技术规范（禁止事项、命名、代码示例）
├── docker-compose.yml             ← PostgreSQL 16 + Redis 7 + MinIO（开发环境）
├── opencode.json                  ← OpenCode 配置
├── backend.log                    ← 后端运行日志（由启动脚本 Tee-Object 生成）
│
├── backend/                       ← Spring Boot 3.5 WebFlux 后端
│   ├── pom.xml                    ← Maven 依赖（LangChain4j, MinIO, Tika, pgvector）
│   └── src/
│       ├── main/java/com/aifms/
│       │   ├── Application.java   ← 启动入口
│       │   ├── config/            ← Spring 配置
│       │   │   ├── AgentConfig.java          ← Agent 管道配置（ChatModel + Supervisor 装配）
│       │   │   ├── SecurityConfig.java       ← WebFlux Security（全放行）
│       │   │   ├── WebFluxConfig.java        ← CORS + 路由
│       │   │   ├── R2dbcConfig.java          ← PostgreSQL R2DBC
│       │   │   ├── RedisConfig.java          ← Redis Reactive
│       │   │   ├── OpenApiConfig.java        ← Swagger UI
│       │   │   └── GlobalExceptionHandler.java
│       │   ├── common/            ← 通用组件
│       │   │   ├── Result.java
│       │   │   ├── ErrorCodes.java
│       │   │   ├── dto/PageResult.java
│       │   │   └── exception/     ← BusinessException 等
│       │   ├── shared/entity/BaseEntity.java
│       │   └── modules/
│       │       ├── auth/          ← 认证（仅 HealthController）
│       │       ├── user/          ← 用户模块（DDD 四层完整）
│       │       ├── finding/       ← 指摘模块 ★ 核心业务
│       │       │   ├── presentation/    ← FindingController + DTO
│       │       │   ├── application/     ← FindingApplicationService
│       │       │   ├── domain/          ← Finding + FindingStatus 状态机
│       │       │   └── infrastructure/  ← FindingEntity + FindingRepository
│       │       ├── agent/         ← Agent 引擎 ★
│       │       │   ├── presentation/    ← AgentController (SSE)
│       │       │   ├── application/     ← AgentApplicationService + Supervisor
│       │       │   ├── domain/          ← 7 端口 + 5 Skill 接口 + DTOs
│       │       │   └── infrastructure/  ← LangChain4j 适配器 / Tika / OCR / Embedding / Prompt / Log / MCP
│       │       ├── file/          ← 文件模块
│       │       │   ├── domain/          ← FileStoragePort 接口
│       │       │   └── infrastructure/  ← MinioFileStorageAdapter
│       │       └── task/          ← 任务队列
│       │           ├── domain/          ← TaskService<T> 接口
│       │           └── infrastructure/  ← RedisStreamsTaskService
│       ├── main/resources/
│       │   ├── application.yml    ← 应用配置（DB, Redis, MinIO, AI LLM/Embedding/OCR/MCP）
│       │   ├── application-dev.yml← dev profile（免 Redis 启动）
│       │   └── error-codes.yml    ← 错误码权威定义
│       └── test/
│           ├── java/com/aifms/ContextLoadSmokeTest.java
│           └── java/com/aifms/modules/
│               ├── user/          ← 25 个单测
│               ├── finding/       ← 4 个单测（domain + application）
│               ├── file/          ← FileApplicationServiceTest
│               ├── task/          ← RedisStreamsTaskServiceTest（需 Docker）
│               └── agent/         ← SkillAdapterTest + AgentControllerTest + KnowledgeRagSkillAdapterTest
│
├── frontend/                      ← React 18 + TypeScript 前端
│   ├── package.json
│   ├── vite.config.ts             ← Vite 6，proxy /api → :8080
│   └── src/
│       ├── api/                   ← client.ts / userApi / findingApi / issueApi / healthApi
│       ├── components/            ← PageContainer/SearchForm/DataTable/StatusTag/EmptyState/ConfirmDialog/SseProgress
│       ├── i18n/locales/          ← zh-CN / ja-JP / en-US
│       ├── layouts/MainLayout.tsx
│       ├── pages/
│       │   ├── HomePage.tsx / UserListPage.tsx / UserCreatePage.tsx / UserEditPage.tsx
│       │   ├── IssueListPage.tsx      ← 指摘一覧
│       │   ├── IssueImportPage.tsx    ← 导入（fetch SSE）
│       │   └── IssueDetailPage.tsx    ← 详情（结构化展示 + RAG 结果）
│       ├── router/index.tsx
│       ├── store/useAppStore.ts
│       ├── styles/global.css
│       └── types/                 ← user / finding / index
│
├── database/migration/            ← Flyway SQL（V001–V011）
│   ├── V001__init.sql
│   ├── V002__create_users.sql
│   ├── V003__partial_unique_indexes.sql
│   ├── V004__drop_indexes_and_constraints.sql
│   ├── V005__create_findings.sql
│   ├── V006__enable_pgvector.sql
│   ├── V007__create_issue_embeddings.sql
│   ├── V008__create_prompt_templates.sql
│   ├── V009__create_agent_executions.sql
│   ├── V010__create_file_uploads.sql
│   └── V011__seed_prompt_templates.sql
│
├── test-samples/                  ← AI 导入测试用文件
│   ├── issue-01-login-failure-ja.txt
│   ├── issue-02-data-sync-cn.txt
│   └── issue-03-permission-en.txt
│
├── scripts/                       ← 运维脚本
│   └── start-backend.ps1          ← 后端启动脚本（含 LLM key + UTF-8 编码）
│
├── docs/
│   ├── progress.md                ← 项目进度
│   ├── architecture.md            ← ADR-001~013 架构决策记录
│   ├── branching-strategy.md      ← Git 分支管理
│   └── worklog/                   ← 每日工作日志
│
└── nginx/                         ← 生产 Nginx 配置
```

---

## API 文档

### 基础信息

- 基础 URL：`http://localhost:8080/api/v1`
- 统一响应体：`Result<T>` — `{code:0, message:"success", data:T, timestamp:long}`
- 分页响应：`PageResult<T>` — `{items:T[], page:int, size:int, total:long, totalPages:int}`
- 错误码范围：`400xx` 客户端 / `500xx` 服务端

### 指摘接口（/api/v1/findings）

| 方法 | 路径 | 说明 | 参数 |
|------|------|------|------|
| `GET` | `/findings` | 分页搜索 | Query: `keyword`(可选), `page`(≥1), `size`(1-100) |
| `GET` | `/findings/{id}` | 查询详情 | Path: UUID |
| `POST` | `/findings` | 创建指摘 | Body: CreateFindingRequest |
| `PUT` | `/findings/{id}` | 更新指摘 | Path: UUID, Body: UpdateFindingRequest |
| `DELETE` | `/findings/{id}` | 软删除 | Path: UUID |

### Agent 接口（/api/v1/agents）

| 方法 | 路径 | 说明 | Content-Type |
|------|------|------|-------------|
| `POST` | `/agents/import` | 上传文件启动 AI 管道 | multipart/form-data → `text/event-stream` (SSE) |

SSE 事件流格式：
```
event:ingest
data:{"ticketId":"...","stepName":"ingest","percentage":40,"message":"IngestSubAgent 完成"}

event:rag
data:{"ticketId":"...","stepName":"rag","percentage":60,"message":"RagSubAgent 完成"}

event:reportQa
data:{"ticketId":"...","stepName":"done","percentage":100,"message":"完成"}
```

### 文件接口（/api/v1/files）

| 方法 | 路径 | 说明 |
|------|------|------|
| `POST` | `/files` | 上传文件（multipart） |

### 用户接口（/api/v1/users）

| 方法 | 路径 | 说明 |
|------|------|------|
| `GET` | `/users` | 分页列表 + 搜索 |
| `GET` | `/users/{id}` | 查询单个 |
| `POST` | `/users` | 创建用户 |
| `PUT` | `/users/{id}` | 更新用户（diff-based） |
| `DELETE` | `/users/{id}` | 软删除 |

### 健康检查

| 方法 | 路径 | 说明 |
|------|------|------|
| `GET` | `/health` | `{"code":0,"data":"OK"}` |

### curl 示例

```bash
# 导入指摘（SSE 流）
curl -N -X POST http://localhost:8080/api/v1/agents/import \
  -F "file=@D:\ai-fms\test-samples\issue-01-login-failure-ja.txt"

# 查询指摘列表
curl "http://localhost:8080/api/v1/findings?keyword=ログイン&page=1&size=10"

# 查询指摘详情
curl http://localhost:8080/api/v1/findings/{id}
```

---

## 数据库设计

### 迁移历史

| 版本 | 文件 | 内容 |
|------|------|------|
| V001 | `init.sql` | pgcrypto 扩展 + `update_updated_at_column()` 函数 |
| V002 | `create_users.sql` | users 表 |
| V003 | `partial_unique_indexes.sql` | 部分唯一索引（排除 DELETED） |
| V004 | `drop_indexes_and_constraints.sql` | 简化约束（只留 PK+NOT NULL+DEFAULT） |
| V005 | `create_findings.sql` | findings 业务表（18 列） |
| V006 | `enable_pgvector.sql` | `CREATE EXTENSION vector` |
| V007 | `create_issue_embeddings.sql` | 向量表（HNSW 余弦索引） |
| V008 | `create_prompt_templates.sql` | 版本化 Prompt 模板表 |
| V009 | `create_agent_executions.sql` | Agent 执行日志 + 事件表 |
| V010 | `create_file_uploads.sql` | 文件元数据表 |
| V011 | `seed_prompt_templates.sql` | 5 个 Skill Prompt 模板种子数据 |

### findings 表

| 列名 | 类型 | 说明 |
|------|------|------|
| `id` | UUID PK | 主键 |
| `title` | TEXT NOT NULL | 标题（AI 管道更新） |
| `description` | TEXT | 描述 |
| `category` | TEXT | 分类（如"認証異常"） |
| `priority` | TEXT | 优先级（高/中/低） |
| `severity` | TEXT | 严重度（重大/一般/轻微） |
| `system` | TEXT | 所属系统 |
| `assignee` | TEXT | 负责人 |
| `tags` | TEXT | 标签 |
| `status` | VARCHAR(20) | OPEN/ANALYZING/CLASSIFIED/RESOLVED/CLOSED |
| `source_type` | VARCHAR(20) | 来源类型（AUTO=AI 导入） |
| `source_file_id` | UUID | 关联文件 |
| `title_ja` | TEXT | 日语标题 |
| `report_draft` | TEXT | AI 生成的处理报告 |
| `qa_reply` | TEXT | AI 生成的用户回复 |
| `resolution` | TEXT | 解决方案 |
| `created_at` | TIMESTAMPTZ | 创建时间 |
| `updated_at` | TIMESTAMPTZ | 更新时间 |

### FindingStatus 状态机

```
OPEN → ANALYZING → CLASSIFIED → RESOLVED → CLOSED
```

### issue_embeddings 表（与 findings 隔离）

| 列名 | 类型 | 说明 |
|------|------|------|
| `id` | UUID PK | 主键 |
| `finding_id` | UUID FK | 关联 findings.id |
| `content` | TEXT | 被嵌入的原始文本片段 |
| `embedding` | vector(2000) | BGE-M3 1024 维向量（HNSW 索引） |
| `model_name` | VARCHAR(50) | 嵌入模型名（如 bge-m3） |
| `created_at` | TIMESTAMPTZ | 创建时间 |

**设计决策：**
- 数据库只保留 NOT NULL + DEFAULT + PRIMARY KEY，复杂约束由后端应用层校验（V004 约定）
- 向量表（V007）与业务表（V005）分 migration、分 repository、分 domain 隔离
- pgvector HNSW 索引是唯一例外（向量检索必需，不属业务索引禁止范畴）

---

## 前端架构

### 路由表

| 路径 | 页面 | 说明 |
|------|------|------|
| `/` | HomePage | Dashboard |
| `/users` | UserListPage | 用户列表 |
| `/users/create` | UserCreatePage | 创建用户 |
| `/users/:id/edit` | UserEditPage | 编辑用户 |
| `/issues` | IssueListPage | **指摘一覧** — 分页搜索、AI 分类列、状态标签 |
| `/issues/import` | IssueImportPage | **导入指摘** — 拖拽上传、SSE 三步骤进度 |
| `/issues/:id` | IssueDetailPage | **指摘详情** — 结构化展示、RAG 相似案例、AI 建议 |
| `*` | → `/` | 404 重定向 |

### 通用组件

| 组件 | 文件 | 用途 |
|------|------|------|
| PageContainer | `components/PageContainer.tsx` | 标题+副标题+操作区 |
| SearchForm | `components/SearchForm.tsx` | 关键词搜索+筛选 |
| DataTable | `components/DataTable.tsx` | 数据表（四态覆盖） |
| StatusTag | `components/StatusTag.tsx` | 色标状态标签 |
| EmptyState | `components/EmptyState.tsx` | 空数据引导 |
| ConfirmDialog | `components/ConfirmDialog.tsx` | 危险操作确认 |
| SseProgress | `components/SseProgress.tsx` | SSE 流式步骤进度 |

### 国际化

支持三语切换：中文（zh-CN，默认回退）/ 日本語（ja-JP）/ English（en-US）。所有用户可见字符串均通过 `t()` 函数读取。语言检测：localStorage → navigator.language → zh-CN。Ant Design 组件文案同步切换。

---

## 环境变量参考

| 变量 | 默认值 | 用途 | 必需？ |
|------|--------|------|--------|
| `LLM_API_KEY` | — | DeepSeek API Key | ✅ 是（AI 功能） |
| `LLM_BASE_URL` | `https://api.deepseek.com` | LLM 端点 | 否 |
| `LLM_MODEL` | `deepseek-chat` | LLM 模型 | 否 |
| `EMBEDDING_BASE_URL` | `http://localhost:11434/v1` | BGE-M3 Ollama 端点 | 否 |
| `EMBEDDING_MODEL` | `bge-m3` | Embedding 模型 | 否 |
| `MINIO_ENDPOINT` | `http://localhost:9000` | MinIO API 端点 | 否 |
| `MINIO_ACCESS_KEY` | `minioadmin` | MinIO 访问密钥 | 否 |
| `MINIO_SECRET_KEY` | `minioadmin` | MinIO 秘密密钥 | 否 |
| `MINIO_BUCKET` | `aifms` | MinIO 桶名（启动自动建） | 否 |
| `DB_USERNAME` | `postgres` | PostgreSQL 用户名 | 否 |
| `DB_PASSWORD` | `postgres` | PostgreSQL 密码 | ✅ 是（迁移/启动） |
| `REDIS_HOST` | `localhost` | Redis 主机 | 否 |
| `REDIS_PORT` | `6379` | Redis 端口 | 否 |
| `JAVA_TOOL_OPTIONS` | `-Dfile.encoding=UTF-8` | Java 编码（防止中文日志乱码） | 推荐 |

---

## 开发工作流

```
1. 理解需求 → 阅读相关代码，确认范围
2. 设计     → 输出实施计划（涉及文件 + 步骤），等待确认
3. 实现     → 按计划逐步修改代码
4. 自测     → ./mvnw test && npm run build
5. 记录     → 更新 docs/worklog/YYYY-MM-DD.md
6. 提交     → git commit -m "类型: 简述" && git push
```

### 常用命令

```bash
# ── 后端 ──
./mvnw spring-boot:run                 # 启动（端口 8080）
./mvnw test                             # 运行测试
./mvnw flyway:migrate                   # 执行迁移
./mvnw flyway:info                      # 迁移状态
./mvnw clean package -DskipTests        # 打包

# ── 前端 ──
cd frontend
npm run dev                             # 启动（端口 3000，proxy → 8080）
npm run build                           # 生产构建
npm run lint                            # ESLint 检查

# ── AI 测试 ──
curl -N -X POST http://localhost:8080/api/v1/agents/import \
  -F "file=@test-samples/issue-01-login-failure-ja.txt"

# ── 基础设施（Windows 原生） ──
redis-server.exe                        # 启动 Redis
minio.exe server D:\ai-fms\data\minio --console-address :9001   # 启动 MinIO
ollama serve                            # 启动 Ollama
```

---

## 编码规范

详细规范见 `CLAUDE.md` 和 `AGENTS.md`。核心约束：

### 后端（硬性禁止）

| # | 禁止事项 | 说明 |
|---|---------|------|
| 1 | Spring MVC | 不引入 `spring-boot-starter-web` |
| 2 | JPA / MyBatis / JDBC | 只使用 R2DBC |
| 3 | `.block()` | 全链路 Mono/Flux |
| 4 | Controller 写业务 | 只做校验→调 Service→返回 Result |
| 5 | Controller 调 Repository | 必须经过 ApplicationService |
| 6 | Hibernate DDL auto | 表结构通过 Flyway 管理 |
| 7 | 手动改数据库 | 所有 DDL/DML 通过 Flyway |
| 8 | 改已提交 Migration | 变更 = 新建 migration |
| 9 | Entity 当 DTO | 必须分离 |

### 前端（硬性）

| # | 规则 |
|---|------|
| 1 | 所有文本 `t()` 国际化 |
| 2 | TypeScript strict，禁止 `any` |
| 3 | 四态覆盖：Loading / Empty / Error / Normal |
| 4 | 使用 ConfigProvider theme token，不硬编码颜色 |

---

## 测试

```bash
cd backend
./mvnw test
# 预期: Tests run: 60+, Failures: 0

cd frontend
npm run build
# tsc -b && vite build，TypeScript 错误立即暴露
```

测试结构：
```
backend/src/test/java/com/aifms/
├── ContextLoadSmokeTest.java              ← Spring Boot 3.5 启动验证
├── modules/user/                          ← 25 个单测（domain + application）
├── modules/finding/                       ← 4 个单测
├── modules/file/                          ← FileApplicationServiceTest
├── modules/task/                          ← RedisStreamsTaskServiceTest（需 Docker）
└── modules/agent/                         ← SkillAdapterTest / AgentControllerTest
```

---

## 常见问题

### 问题 1：后端启动时 `No value found for user`（R2DBC 认证失败）

**原因**：`application.yml` 中 R2DBC URL 不含凭据，`${DB_USERNAME:postgres}` 默认解析失败。

**解决**：把 R2DBC URL 改为含凭据格式：
```yaml
spring.r2dbc.url: r2dbc:postgresql://postgres:密码@localhost:5432/aifms
spring.r2dbc.username: postgres
spring.r2dbc.password: 密码
```

### 问题 2：Flyway 迁移认证失败（`user 'null'`）

**原因**：PowerShell 启动子进程时环境变量未继承。

**解决**：
```bash
$env:DB_USERNAME = "postgres"
$env:DB_PASSWORD = "密码"
./mvnw flyway:migrate
```

### 问题 3：后端日志 / 终端中文乱码

**原因**：Windows 默认编码非 UTF-8。

**解决**：启动时设置 Java 编码：
```bash
$env:JAVA_TOOL_OPTIONS = "-Dfile.encoding=UTF-8"
./mvnw spring-boot:run
```
或使用 `scripts/start-backend.ps1`（已预置编码修复）。

### 问题 4：导入指摘返回 500 错误

**排查链**：
1. 检查 Redis 是否运行：`redis-cli ping`
2. 检查 MinIO 桶是否存在：后端启动日志应有 `MinIO 桶 'aifms' 已自动创建`
3. 检查 DeepSeek API key 是否设置：`$env:LLM_API_KEY`
4. 查看 `backend.log` 以获取具体异常堆栈

**已知已修复的根因**：
- Redis `AgentTask` 序列化失败 → 已改为 JSON 字符串存储
- OpenAI embedding API 不可达（国内墙） → 已改为本地 Ollama BGE-M3
- DeepSeek 无 embedding API → RAG 步骤已加 `onErrorReturn(empty)` 降级保护

### 问题 5：AUTO-IMPORT-xxx 占位记录残留

**原因**：Agent 管道在启动时创建占位记录，如果中途失败（Redis/LLM 等异常），占位记录不会被清理。

**解决**：通过 API 删除：
```bash
curl -X DELETE http://localhost:8080/api/v1/findings/{id}
```

### 问题 6：前端"处理失败"但后端已成功

**原因**：前端 `issueApi.import()` 用 Axios 发 POST，但后端返回 SSE（`text/event-stream`）。Axios 解 JSON 失败。

**解决**：已改用原生 `fetch()` + `ReadableStream` + 手动 SSE 解析（`issueApi.importStream` + `parseSseStream`）。强制刷新浏览器（Ctrl+F5）获取最新前端代码。

### 问题 7：前端 RAG 步骤"转圈"卡死

**原因**：React `useState` 闭包陈旧值判断。

**解决**：已移除闭包检查，SSE 流结束直接设为 `complete`。强制刷新浏览器。

### 问题 8：pgvector HNSW 维度限制

**原因**：pgvector 0.8.x HNSW 限制最大 2000 维。

**解决**：V007 migration 中 `vector(2000)` 替代 `vector(3072)`。BGE-M3 输出 1024 维，在限制内。

### 问题 9：ollama pull 下载中断

**原因**：1.2GB 模型文件下载时间长，bash 会话超时。

**解决**：重新执行 `ollama pull bge-m3`——Ollama 支持断点续传。

### 问题 10：后端启动命令失败（`-Dfile.encoding=UTF-8` 被当作生命周期阶段）

**原因**：PowerShell 参数解析问题。

**解决**：使用 `$env:JAVA_TOOL_OPTIONS` 环境变量替代 Maven 命令行 `-D` 参数。

---

## 部署

### Docker Compose 部署（含全部基础设施）

```bash
docker compose up -d     # PostgreSQL + Redis + MinIO
```

### 生产构建

```bash
# 后端打包
cd backend && ./mvnw clean package -DskipTests

# 前端打包
cd frontend && npm run build
# 产出: frontend/dist/
```

---

## WSL 迁移指南

### 动机

当前开发环境是 Windows 11 原生（PostgreSQL/Redis/MinIO/Ollama 均为 Windows 版本）。迁移至 WSL 2 + Ubuntu 可获得更好的生态兼容性（pgvector 安装、Redis 源安装、Docker 支持）和更稳定的文件系统性能。

### 迁移步骤

#### 1. 安装 WSL 2

```powershell
# 管理员 PowerShell
wsl --install -d Ubuntu-24.04
wsl --set-default-version 2
```

#### 2. 在 WSL 内安装基础设施

```bash
# 进入 WSL
wsl

# PostgreSQL 16 + pgvector
sudo apt update
sudo apt install postgresql-16 postgresql-16-pgvector
sudo systemctl enable postgresql
sudo systemctl start postgresql

# Redis
sudo apt install redis-server
sudo systemctl enable redis-server
sudo systemctl start redis-server

# MinIO
wget https://dl.min.io/server/minio/release/linux-amd64/minio
chmod +x minio
mkdir -p ~/minio-data
./minio server ~/minio-data --console-address :9001 &

# Ollama
curl -fsSL https://ollama.com/install.sh | sh
ollama serve &
ollama pull bge-m3
```

#### 3. 迁移项目代码

```bash
# 将项目从 Windows 文件系统复制到 WSL 内部（推荐 /home/user/ai-fms）
# 不要在 /mnt/c 下运行——跨文件系统 I/O 极慢，Java/Maven 尤其受影响
cp -r /mnt/d/ai-fms ~/ai-fms
```

#### 4. 更新配置

```yaml
# backend/src/main/resources/application.yml
spring:
  r2dbc:
    url: r2dbc:postgresql://localhost:5432/aifms   # WSL 内 localhost 指向 Linux 侧
  flyway:
    url: jdbc:postgresql://localhost:5432/aifms

minio:
  endpoint: http://localhost:9000

ai:
  embedding:
    base-url: http://localhost:11434/v1
```

#### 5. 数据库迁移

```bash
cd ~/ai-fms/backend
./mvnw flyway:migrate -Dflyway.user=postgres -Dflyway.password=密码
```

#### 6. 验证

```bash
# 启动后端
./mvnw spring-boot:run

# 启动前端（npm 仍在 Windows 侧或 WSL 内均可）
cd ~/ai-fms/frontend && npm run dev
```

### WSL 注意事项

- **不要在 `/mnt/c` 或 `/mnt/d` 下运行项目**——WSL 跨文件系统 I/O 比原生 Linux 文件系统慢 5-10 倍，Maven/Java 编译尤受影响
- WSL 内的 `localhost` 自动映射到 Windows `localhost`，无需额外配置
- 如从 Windows 浏览器访问 WSL 内前端，使用 `http://localhost:3000` 即可（WSL 自动端口转发）
- Ollama 模型文件存放在 `~/.ollama/models/`，迁移时一并复制

---

## 参考文档

| 文档 | 内容 |
|------|------|
| [`AGENTS.md`](AGENTS.md) | 技术规范（禁止事项、命名、代码示例） |
| [`docs/progress.md`](docs/progress.md) | 项目进度 |
| [`docs/architecture.md`](docs/architecture.md) | ADR-001~013 架构决策记录 |
| [`docs/branching-strategy.md`](docs/branching-strategy.md) | Git 分支管理 |
| [`docs/worklog/`](docs/worklog/) | 每日工作日志 |
