# AI-FMS Architecture Decision Records

---

## ADR-001: Spring Boot 3 WebFlux（响应式架构）

**日期：** 2026-07-10 | **状态：** 已采纳

### 决策
使用 Spring Boot 3 WebFlux 作为后端框架，禁止 Spring MVC。

### 理由
- 非阻塞 I/O，高并发吞吐
- 与 R2DBC + Redis Reactive 全链路响应式
- Spring Boot 3 基于 Java 17+，长期支持

### 后果
- 禁止 `.block()`，全链路 Mono/Flux
- 需要适应响应式编程范式
- 部分传统库不兼容

---

## ADR-002: R2DBC（禁止 JPA / MyBatis / JDBC）

**日期：** 2026-07-10 | **状态：** 已采纳

### 决策
使用 Spring Data R2DBC + 手写 SQL，禁止 JPA、MyBatis、JDBC。

### 理由
- JPA/Hibernate 不支持真正的非阻塞 I/O
- 手写 SQL 更可控，消除 N+1 黑盒问题

### 后果
- 无自动建表，必须通过 Flyway
- 无关联懒加载，需手动 JOIN
- Entity 关系映射需手动处理

---

## ADR-003: Flyway 数据库迁移

**日期：** 2026-07-10 | **状态：** 已采纳

### 决策
使用 Flyway 管理数据库版本，禁止 Hibernate DDL auto。

### 理由
- 用户偏好 Flyway
- 纯 SQL 脚本，直观
- 禁止修改已提交 migration

### 后果
- 所有 DDL 变更通过 migration
- 数据库目录包含：migration / view / function / trigger / seed

---

## ADR-004: React + TypeScript + Ant Design + Vite

**日期：** 2026-07-10 | **状态：** 已采纳

### 决策
- React 18 + TypeScript（strict）
- Ant Design 5（UI 组件）
- Vite 5（构建）
- React Router 6（路由）
- Zustand（客户端状态）
- Axios（HTTP 客户端）

### 理由
- Ant Design 提供企业级 UI 组件开箱即用
- Vite 构建速度远优于 Webpack
- Zustand 轻量，比 Redux 简洁
- TypeScript 提供类型安全

---

## ADR-005: 统一响应格式 Result<T>

**日期：** 2026-07-10 | **状态：** 已采纳

### 决策
所有 API 返回 `Result<T>`：`{code, message, data, timestamp}`。

### 理由
- 前端统一处理（Axios 拦截器）
- 错误码体系便于排查
- 类型安全（泛型）

---

## ADR-006: DDD 模块化架构

**日期：** 2026-07-10 | **状态：** 已采纳

### 决策
后端采用 DDD 四层模块化架构：

```
modules/{module}/
├── presentation/    → Controller
├── application/     → Application Service
├── domain/          → Entity + Domain Service
└── infrastructure/  → Repository
```

### 理由
- 业务边界清晰
- 每个模块独立演进
- 模块间通过 shared/common 通信

---

## ADR-007: Maven 构建

**日期：** 2026-07-10 | **状态：** 已采纳

### 决策
使用 Maven（而非 Gradle）作为构建工具。

### 理由
- 用户明确指定 Maven
- XML 配置在企业项目中更常见

---

## ADR-008: LangChain4j 1.17 Agent Runtime

**日期：** 2026-07-17 | **状态：** 已采纳

### 决策
使用 LangChain4j 1.17 作为 Agent 核心运行时。

### 理由
- 原生支持 AiServices + @Tool 注解，减少样板代码
- 内置 Sequential / Parallel / Loop / Supervisor 工作流模式
- langchain4j-spring-boot-starter 开箱即用
- langchain4j-mcp 客户端原生集成
- ApacheTikaDocumentParser 支持多格式文档解析

### 后果
- 不引入 Spring AI 做 Agent，Spring AI 仅保留作 MCP Server（延至 MVP-3）
- Agent 核心运行时统一为 LangChain4j 1.17
- 需要熟悉 LangChain4j 工作流编排模型

---

## ADR-009: Spring AI 2.0 MCP Server（延迟冻结外部契约，MVP-3 才引入）

**日期：** 2026-07-17 | **状态：** 已采纳

### 决策
Spring AI 2.0 仅用于把 AI-FMS 内部能力通过 @McpTool 注解暴露成 MCP Server 供外部 agent 调用，MVP-1/2 不写 @McpTool、不冻结外部契约。

### 理由
- AI-FMS 未来被 OpenCode / Claude Code / 企业 Copilot 调用需要标准 MCP 接口
- 在内部能力（文件解析、指摘存储、RAG 检索）稳定前不过早冻结接口形状
- 先跑通核心 Agent 流程，再考虑对外暴露能力

### 后果
- MVP-1/2 不安装 spring-ai-mcp-server 依赖
- 架构图上 MCP Server 为占位标识
- MVP-3 阶段正式定义并冻结 MCP Tool 接口契约

---

## ADR-010: pgvector + R2DBC — 向量数据与业务表隔离

**日期：** 2026-07-17 | **状态：** 已采纳

### 决策
使用 pgvector 0.8.5 嵌入 PostgreSQL 16 存储向量，issue_embeddings 表与 findings 业务表分 migration、分 repository、分 domain 隔离。

### 理由
- r2dbc-postgresql 1.0.3+ 原生支持 vector 类型，零新基础设施
- HNSW 索引提供高性能向量检索
- 响应式查询 "ORDER BY embedding <-> $1" 自然集成到 WebFlux 链路
- 分离向量表和业务表避免交叉污染，便于独立演进

### 后果
- V006 迁移启用 pgvector 扩展
- V007 迁移创建独立 issue_embeddings 表
- findings 与 RAG 通过 finding_id 关联，不共字段
- Repository 层 issue_embeddings 和 findings 独立管理

---

## ADR-011: Spring Boot 3.5 升级

**日期：** 2026-07-17 | **状态：** 已采纳

### 决策
Phase-0 阶段将 Spring Boot 从 3.3.5 升级至 3.5.x。

### 理由
- spring-ai-mcp-server-spring-boot-starter 2.0 GA 锚定 Boot 3.5
- LangChain4j 1.17 在 Boot 3.5 环境验证通过
- 一次集中升级避免后续双版本管理

### 后果
- 新增 ContextLoadSmokeTest（@SpringBootTest + @ActiveProfiles("test")）验证上下文启动
- 新增 application-test.yml 禁用 Flyway 自动迁移
- 团队需关注 Boot 3.5 Release Notes 中的 Breaking Changes

---

## ADR-012: MinIO 文件存储

**日期：** 2026-07-17 | **状态：** 已采纳

### 决策
使用 MinIO（S3 兼容自托管）作为文件存储后端，通过 FileStoragePort 接口抽象。

### 理由
- S3 协议兼容，无云厂商锁定风险
- 自托管满足企业合规要求
- docker-compose 一键引入，开发环境零配置
- 接口抽象允许未来切换其他存储实现

### 后果
- docker-compose 新增 minio 服务
- modules/file 模块按 DDD 四层架构实现
- Infrastructure 层提供 MinioFileStorageAdapter 实现 FileStoragePort
- 文件上传下载通过 MinIO SDK（与 S3 SDK 兼容）

---

## ADR-013: Redis Streams 包在 Task Service 抽象后

**日期：** 2026-07-17 | **状态：** 已采纳

### 决策
使用 Redis Streams（复用现有 Redis 7）作为长任务队列，包在独立 modules/task 模块的 TaskService&lt;T&gt; 接口抽象后。

### 理由
- 复用已有 Redis 基础设施，不引入 RabbitMQ / Kafka 的运维面
- Agent Runtime（LangChain4j）仅依赖 TaskService 接口，不耦合 Redis
- 接口抽象允许未来根据需要替换为其他 MQ 实现（Kafka / RabbitMQ）

### 后果
- modules/task 模块按 DDD 四层架构实现
- Infrastructure 层提供 RedisStreamsTaskService 实现
- Consumer Group 模式保证消息不丢失 + 有序处理
- 生产者（LangChain4j Agent）通过 TaskService.submit(task) 投递任务

---

## 技术债务

| ID | 描述 | 优先级 | 状态 |
|----|------|--------|------|
| — | （暂无） | — | — |
