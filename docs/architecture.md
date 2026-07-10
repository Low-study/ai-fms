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

## 技术债务

| ID | 描述 | 优先级 | 状态 |
|----|------|--------|------|
| — | （暂无） | — | — |
