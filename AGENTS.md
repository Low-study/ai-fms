# AGENTS.md — Technical Specifications

本文档定义项目的技术规范和架构约束。所有代码生成和修改必须遵守本文档。

---

## 1. Architecture Principles

### 1.1 DDD 分层架构

```
┌──────────────────────────────────────────────┐
│  Presentation 层 (presentation/)              │
│  - Controller（@RestController）              │
│  - 参数校验（@Valid）                          │
│  - 调用 Application Service                   │
│  - 返回 Result<T>                             │
│  - 不包含任何业务逻辑                           │
├──────────────────────────────────────────────┤
│  Application 层 (application/)                │
│  - Application Service（@Service）            │
│  - 用例编排、事务管理                           │
│  - 调用 Domain Service / Repository           │
│  - 返回 Mono<Result<T>> / Flux<Result<T>>     │
├──────────────────────────────────────────────┤
│  Domain 层 (domain/)                          │
│  - Entity（@Table 注解，R2DBC 映射）           │
│  - Domain Service（纯业务逻辑，@Service）       │
│  - 不依赖框架基础设施                           │
│  - 不依赖 Repository（通过接口反转）            │
├──────────────────────────────────────────────┤
│  Infrastructure 层 (infrastructure/)          │
│  - Repository（ReactiveCrudRepository）       │
│  - 外部服务适配器                               │
│  - 返回 Mono<T> / Flux<T>                     │
└──────────────────────────────────────────────┘
```

### 1.2 模块划分

```
modules/
├── auth/      ← 认证与授权
├── user/      ← 用户管理
├── role/      ← 角色管理
└── tenant/    ← 租户管理

每个模块独立拥有 presentation/application/domain/infrastructure 四层。
模块间通过 shared/ 和 common/ 通信，禁止循环依赖。
```

### 1.3 依赖方向

```
Presentation → Application → Domain ← Infrastructure
                                    ↑
                              (依赖反转)
```

- Application 依赖 Domain 接口
- Infrastructure 实现 Domain 接口
- Presentation 只知道 Application

---

## 2. 硬性禁止事项

以下行为**绝对禁止**，违反即为代码不合规：

| # | 禁止事项 | 说明 |
|---|---------|------|
| 1 | **禁止 Spring MVC** | 不引入 `spring-boot-starter-web`，只用 WebFlux |
| 2 | **禁止 JPA** | 不引入 `spring-boot-starter-data-jpa` |
| 3 | **禁止 MyBatis** | 不引入 MyBatis 及 MyBatis-Plus |
| 4 | **禁止 JDBC** | 不引入 `spring-boot-starter-jdbc` 或 `JdbcTemplate` |
| 5 | **禁止 `.block()`** | 全链路响应式，Mono/Flux 贯穿始终 |
| 6 | **禁止 Controller 写业务** | Controller 只能：校验 → 调 Service → 返回 |
| 7 | **禁止 Controller 调 Repository** | 必须经过 Application Service |
| 8 | **禁止 Hibernate DDL auto** | 表结构必须通过 Flyway 管理 |
| 9 | **禁止手动修改数据库** | 所有 DDL/DML 通过 Flyway 脚本 |
| 10 | **禁止修改已提交的 Migration** | 有变更 = 新建 migration 文件 |
| 11 | **禁止 Entity 当 DTO 用** | Entity 和 DTO 必须分离 |
| 12 | **禁止跨模块直接依赖** | 模块间通过 shared/common 通信 |

---

## 3. Naming Conventions

### 3.1 Java 命名

| 类型 | 命名规则 | 示例 |
|------|---------|------|
| Controller | `{Module}Controller` | `UserController` |
| Application Service | `{Module}ApplicationService` | `UserApplicationService` |
| Domain Service | `{Domain}Service` | `PasswordHashService` |
| Repository | `{Entity}Repository` | `UserRepository` |
| Entity | `{Entity}` | `User` |
| Request DTO | `{Action}{Entity}Request` | `CreateUserRequest` |
| Response DTO | `{Entity}Response` | `UserResponse` |
| Exception | `{描述}Exception` | `UserNotFoundException` |
| Config | `{Module}Config` | `SecurityConfig` |

### 3.2 数据库命名

| 对象 | 规则 | 示例 |
|------|------|------|
| 表名 | 小写 + 下划线 + 单数 | `users`, `roles`, `user_roles` |
| 主键 | `id` | `id UUID PRIMARY KEY` |
| 外键 | `{关联表}_id` | `user_id` |
| 时间戳 | `created_at`, `updated_at` | — |
| 索引 | `idx_{表}_{列}` | `idx_users_email` |
| 唯一约束 | `uq_{表}_{列}` | `uq_users_email` |
| Flyway 脚本 | `V{序号}__{描述}.sql` | `V001__create_users.sql` |

### 3.3 前端命名

| 类型 | 规则 | 示例 |
|------|------|------|
| 页面组件 | `{Name}Page.tsx` | `UserListPage.tsx` |
| 通用组件 | `{Name}.tsx` | `DataTable.tsx` |
| Hook | `use{Name}.ts` | `useUsers.ts` |
| API 函数 | `{resource}Api.ts` | `userApi.ts` |
| Service | `{resource}Service.ts` | `userService.ts` |
| 类型文件 | `{domain}.ts` | `user.ts` |
| Store | `use{Name}Store.ts` | `useUserStore.ts` |

---

## 4. API Design Rules

### 4.1 RESTful 规范

```
GET    /api/v1/{resources}          → 分页列表
GET    /api/v1/{resources}/{id}     → 详情
POST   /api/v1/{resources}          → 创建
PUT    /api/v1/{resources}/{id}     → 全量更新
PATCH  /api/v1/{resources}/{id}     → 部分更新
DELETE /api/v1/{resources}/{id}     → 删除
```

### 4.2 统一响应格式

```java
// Result<T> — 所有 API 统一返回此类型
public class Result<T> {
    private int code;        // 0=成功, 非0=错误
    private String message;  // 提示信息
    private T data;          // 业务数据
    private long timestamp;  // 时间戳

    public static <T> Result<T> success(T data) { ... }
    public static <T> Result<T> error(int code, String message) { ... }
}
```

成功响应：
```json
{"code": 0, "message": "success", "data": {...}, "timestamp": 1750618800000}
```

分页响应：
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "items": [...],
    "page": 1,
    "size": 20,
    "total": 100,
    "totalPages": 5
  },
  "timestamp": 1750618800000
}
```

错误码：
- `400xx` — 参数校验 / 业务规则错误
- `401xx` — 未认证
- `403xx` — 未授权
- `404xx` — 资源不存在
- `500xx` — 服务端错误

---

## 5. Error Handling

```java
// Controller — 不做 try-catch，交给全局异常处理
@PostMapping
public Mono<Result<UserResponse>> create(@Valid @RequestBody CreateUserRequest request) {
    return userApplicationService.create(request);
}

// Application Service — 使用 Mono.error() 传播
public Mono<Result<UserResponse>> create(CreateUserRequest request) {
    return userRepository.findByEmail(request.getEmail())
        .flatMap(existing -> Mono.<Result<UserResponse>>error(
            new BusinessException(40001, "邮箱已存在")
        ))
        .switchIfEmpty(Mono.defer(() -> {
            User user = User.from(request);
            return userRepository.save(user).map(UserResponse::from).map(Result::success);
        }));
}

// 全局异常处理
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(BusinessException.class)
    public Mono<Result<Void>> handleBusiness(BusinessException ex) {
        return Mono.just(Result.error(ex.getCode(), ex.getMessage()));
    }
}
```

---

## 6. Database Rules

### 6.1 Flyway 迁移

```
database/migration/
├── V001__init.sql
├── V002__create_users.sql
└── V003__add_user_status.sql
```

- 命名格式：`V{序号}__{描述}.sql`（双下划线）
- **严禁修改已执行的 migration**
- 每个 migration 独立可执行
- 目录：database/migration, view, function, trigger, seed

### 6.2 R2DBC Repository

```java
public interface UserRepository extends ReactiveCrudRepository<User, UUID> {
    Mono<User> findByEmail(String email);

    @Query("SELECT * FROM users WHERE status = :status ORDER BY created_at DESC LIMIT :limit OFFSET :offset")
    Flux<User> findByStatus(String status, long limit, long offset);

    @Query("SELECT COUNT(*) FROM users WHERE status = :status")
    Mono<Long> countByStatus(String status);
}
```

---

## 7. Testing Standards

| 层级 | 覆盖率要求 | 框架 |
|------|-----------|------|
| Domain Service | ≥ 90% | JUnit 5 + Mockito |
| Application Service | ≥ 80% | JUnit 5 + Mockito |
| Controller | ≥ 70% | WebTestClient |
| Repository | ≥ 60% | DataR2dbcTest |

### 7.1 测试命名

```java
// 格式：should_{预期行为}_when_{条件}
@Test
void shouldReturnUser_whenEmailExists() { ... }

@Test
void shouldThrowException_whenEmailDuplicate() { ... }
```

---

## 8. Frontend Rules

### 8.1 组件规范

```tsx
// 页面组件 — 组装容器
export default function UserListPage() {
  return (
    <PageLayout title="用户管理">
      <UserToolbar />
      <UserTable />
    </PageLayout>
  );
}

// 通用组件 — 展示型，props 驱动
interface DataTableProps<T> {
  data: T[];
  columns: ColumnType<T>[];
  loading?: boolean;
}
```

### 8.2 状态处理

每个页面/组件必须处理 4 种状态：
- **Loading** — Skeleton / Spin
- **Empty** — Empty 组件
- **Error** — 错误提示 + 重试按钮
- **Success** — 正常展示

### 8.3 API 封装

```typescript
// api/client.ts — Axios 统一封装
const apiClient = axios.create({
  baseURL: '/api/v1',
  timeout: 10000,
});

// 请求拦截器 — 添加 Token
apiClient.interceptors.request.use(...);

// 响应拦截器 — 统一解包 Result<T>
apiClient.interceptors.response.use(...);

// api/user.ts — 业务 API
export const userApi = {
  list: (params: PaginationParams) => apiClient.get<PaginatedData<User>>('/users', { params }),
  create: (data: CreateUserRequest) => apiClient.post<User>('/users', data),
  update: (id: string, data: UpdateUserRequest) => apiClient.put<User>(`/users/${id}`, data),
  delete: (id: string) => apiClient.delete(`/users/${id}`),
};
```
