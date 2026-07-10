# 分支管理规范

> AI-FMS 项目 Git 分支策略，所有人必须遵守。

---

## 一、长期分支

| 分支 | 用途 | 保护 | 说明 |
|------|------|------|------|
| `master` | **稳定基线** | 🔒 锁定 | 唯一真理源。所有临时分支从这里拉，每次发布后由 `prod` 同步 |
| `dev` | **集成测试** | 半保护 | 临时分支合入测试，**代码只进不出**，不稳定 |
| `release` | **发布候选** | 🔒 锁定 | 上线前冻结，只接受测试通过的 task 分支合入 |
| `prod` | **生产环境** | 🔒 锁定 | 线上代码，只接受 `release` 和 `hotfix/*` |

### 核心铁律

```
dev 的代码永远不会流向任何长期分支。

    dev ← feature/* （只进）
    dev →  ？？？   （不出）

所有流向长期分支的代码，必须来自从 master 拉出的 task 分支。
```

---

## 二、临时分支命名规范

| 类型 | 命名格式 | 示例 | 拉取源 | 合入目标 |
|------|----------|------|--------|----------|
| 新功能 | `feature/{模块}-{简述}` | `feature/role-crud` | `master` | `dev` → 测试通过后 → `release` |
| 前端页面 | `feature/frontend-{简述}` | `feature/frontend-login-page` | `master` | `dev` → 测试通过后 → `release` |
| Bug 修复 | `fix/{模块}-{问题}` | `fix/user-status-transition` | `master` | `dev` → 测试通过后 → `release` |
| 紧急修复 | `hotfix/{版本}-{问题}` | `hotfix/v0.1-sql-injection` | `prod` | `prod` + `dev` |
| 重构 | `refactor/{模块}-{范围}` | `refactor/user-dto-split` | `master` | `dev` → 测试通过后 → `release` |
| 文档 | `docs/{范围}` | `docs/api-guide` | `master` | `dev` → 测试通过后 → `release` |
| 发布准备 | `release/v{major}.{minor}.{patch}` | `release/v0.1.0` | `master` | `prod` → `master` |

### 命名规则

- 全小写英文
- 单词用 `-` 连接（kebab-case）
- 模块名：`user`, `role`, `auth`, `tenant`, `permission`
- 前端相关加 `frontend-` 前缀
- 简述 ≤5 个单词

---

## 三、分支流向图

```
                    master（唯一稳定基线）
                      │
          ┌───────────┼───────────┐
          ▼           ▼           ▼
    feature/A    feature/B    fix/C
          │           │           │
          └───────────┼───────────┘
                      ▼
                    dev（集成测试）
                      │
          ┌───────────┼───────────┐
          │ 测试通过    │ 测试失败    │
          ▼           ▼           │
    feature/A    feature/B    修 Bug → 重测
          │           │
          └───────────┼───────────┘
                      ▼
                  release（冻结）
                      │
                      ▼
                    prod（上线）
                      │
                      ▼
                  master（同步基线）


hotfix（例外）：

    prod ──→ hotfix/* ──→ prod
                │
                └──→ dev
```

### 为什么 dev 代码不能流出？

```
dev = feature/A + feature/B + fix/C + 未完成的实验代码

如果 release 从 dev 拉：
  release = dev 的完整快照
  问题：release 混入了所有未完成、未审核、甚至编译不过的代码

正确做法：
  release = 只合入测试通过的 task 分支
  每个 task 分支是干净的（从 master 拉），独立合入 release
  哪个没通过，哪个就不进 release
```

---

## 四、合并规则

| 源分支 | → `dev` | → `release` | → `prod` | → `master` |
|--------|---------|-------------|----------|------------|
| `feature/*` | ✅ 测试 | ✅ 通过后 | ❌ | ❌ |
| `fix/*` | ✅ 测试 | ✅ 通过后 | ❌ | ❌ |
| `refactor/*` | ✅ 测试 | ✅ 通过后 | ❌ | ❌ |
| `docs/*` | ✅ 测试 | ✅ 通过后 | ❌ | ❌ |
| `dev` | — | ❌ **禁止** | ❌ | ❌ |
| `release/*` | ❌ | — | ✅ | ❌ |
| `prod` | ❌ | ❌ | — | ✅ |
| `hotfix/*` | ✅ 合回 | ❌ | ✅ | ❌ |
| `master` | ❌ | ❌ | ❌ | — |

### 禁止事项

| 禁止 | 原因 |
|------|------|
| ❌ 从 `dev` 拉分支 | `dev` 不稳定，不是基线 |
| ❌ `dev` → 任何长期分支 | `dev` 代码只进不出 |
| ❌ `feature/*` → `prod` | 绕过 release 冻结 |
| ❌ 直接在长期分支 commit | 必须通过 PR |
| ❌ force push 长期分支 | 破坏历史 |
| ❌ 未完成代码合入 `dev` | 阻塞其他人测试 |

---

## 五、标准工作流

### 场景 1：开发新功能

```bash
# 1. 从 master 拉
git checkout master
git pull origin master
git checkout -b feature/role-crud

# 2. 开发
git commit -m "feat(role): 实现角色 CRUD"
git push origin feature/role-crud

# 3. PR → dev（集成测试）
# GitHub: feature/role-crud → dev
# CI 跑测试，Reviewer 审查

# 4. dev 上测试通过后 → PR → release
# GitHub: feature/role-crud → release
# 进入发布队列
```

### 场景 2：发布流程

```bash
# 1. 从 master 创建 release 分支
git checkout master
git pull origin master
git checkout -b release/v0.1.0
git push origin release/v0.1.0

# 2. 本次发布包含的 task 分支，逐个合入 release
# feature/role-crud → release  ✅
# feature/login-page → release ✅
# fix/user-status → release    ✅
# （未通过 dev 测试的分支，跳过，不进 release）

# 3. release 冻结测试
# 发现 Bug → 在原 task 分支修 → 重走 dev → release
git commit -m "fix: release 测试发现的问题"

# 4. 合入 prod
git checkout prod
git merge --squash release/v0.1.0
git commit -m "release: v0.1.0"
git push origin prod

# 5. 同步 master
git checkout master
git merge --squash release/v0.1.0
git commit -m "chore: v0.1.0 发布后同步 master"
git push origin master

# 6. 打 Tag + 清理
git tag -a v0.1.0 -m "Release v0.1.0"
git push origin v0.1.0
git branch -d release/v0.1.0
git push origin --delete release/v0.1.0
```

### 场景 3：修复 Bug

```bash
git checkout master
git pull origin master
git checkout -b fix/user-status-transition

git commit -m "fix(user): 修复状态转换"
git push origin fix/user-status-transition
# → PR: fix/user-status-transition → dev（测试）
# → 通过后: fix/user-status-transition → release
```

### 场景 4：紧急修复

```bash
# 从 prod 拉（修正在跑的代码）
git checkout prod
git pull origin prod
git checkout -b hotfix/v0.1-sql-injection

git commit -m "hotfix: 修复 SQL 注入"
git push origin hotfix/v0.1-sql-injection
# → PR 1: hotfix/v0.1-sql-injection → prod（立即部署）
# → PR 2: hotfix/v0.1-sql-injection → dev（同步）
```

---

## 六、各分支职责

```
master
  "我是唯一真理源。所有新工作从我开始，每次发布后由 prod 同步更新。"
  → 所有临时分支的拉取源
  → 被 prod 更新（发布后同步）

dev
  "我是集成试验场。所有 task 分支在我这里汇合测试。"
  → 接受 task 分支合入
  → 代码只进不出，绝不会流向 release/prod/master
  → 测试没通过？修 task 分支，重新合入 dev

release
  "我是发布快照。只包含本次上线且测试通过的 task 分支。"
  → 从 master 创建
  → 接受测试通过的 task 分支
  → 合并到 prod
  → 发布后删除

prod
  "我是线上代码。"
  → 只接受 release 和 hotfix
  → 发布后同步到 master
```

---

## 七、Commit Message 规范

```
{type}({scope}): {简短描述}

type:
  feat     — 新功能
  fix      — Bug 修复
  hotfix   — 紧急修复
  refactor — 重构
  docs     — 文档
  chore    — 杂项
  test     — 测试

示例：
  feat(user): 实现用户软删除
  fix(frontend): 修复编辑页状态切换 400 错误
  hotfix(auth): 修复登录绕过漏洞
  docs(readme): 补充 API 文档
```

---

## 八、分支清理

| 时机 | 操作 |
|------|------|
| task 分支合入 release 后 | 删除 task 分支 |
| release 合入 prod + master 后 | 删除 release 分支 |
| 废弃分支 | 30 天未活动可清理 |

```bash
git branch -d feature/xxx
git push origin --delete feature/xxx
git remote prune origin
```

---

## 九、快速参考

```bash
git checkout -b feature/user-xxx master   # 新功能
git checkout -b fix/user-xxx master       # 修 Bug
git checkout -b hotfix/v0.x-xxx prod      # 紧急修复
git checkout -b refactor/user-xxx master  # 重构
git checkout -b docs/xxx master           # 文档
git checkout -b release/v0.x.0 master     # 准备发布
```

| 操作 | 命令 |
|------|------|
| 同步 master | `git checkout master && git pull origin master` |
| 查看分支图 | `git log --oneline --graph --all` |
| 暂存工作 | `git stash && git stash pop` |
