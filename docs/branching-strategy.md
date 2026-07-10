# 分支管理规范

> AI-FMS 项目 Git 分支策略，所有人必须遵守。

---

## 一、长期分支

| 分支 | 用途 | 保护 | 说明 |
|------|------|------|------|
| `prod` | **生产环境** | 🔒 锁定 | 线上运行代码，只接受 `release` 合并和 `hotfix/*` |
| `master` | **稳定主线** | 🔒 锁定 | 保持与 `prod` 同步，默认分支 |
| `release` | **发布候选** | 🔒 锁定 | 上线前冻结测试，只接受 `dev` 合并和 bugfix |
| `dev` | **开发集成分支** | 半保护 | 日常开发集成，接受所有 feature / fix 分支 |

---

## 二、临时分支命名规范

| 类型 | 命名格式 | 示例 | 说明 |
|------|----------|------|------|
| 新功能 | `feature/{模块}-{简述}` | `feature/role-crud` | 从 `dev` 拉，合回 `dev` |
| 前端页面 | `feature/frontend-{简述}` | `feature/frontend-login-page` | 从 `dev` 拉，合回 `dev` |
| Bug 修复 | `fix/{模块}-{问题}` | `fix/user-status-transition` | 从 `dev` 拉，合回 `dev` |
| 紧急修复 | `hotfix/{版本}-{问题}` | `hotfix/v0.1-login-bypass` | 从 `prod` 拉，合到 `prod` + `dev` |
| 重构 | `refactor/{模块}-{范围}` | `refactor/user-dto-split` | 从 `dev` 拉，合回 `dev` |
| 文档 | `docs/{范围}` | `docs/api-guide` | 从 `dev` 拉，合回 `dev` |
| 发布准备 | `release/v{major}.{minor}.{patch}` | `release/v0.1.0` | 从 `dev` 拉，合到 `prod` + `dev` |

### 命名规则

- 全小写英文
- 单词用 `-` 连接（kebab-case）
- 模块名使用项目已有模块名：`user`, `role`, `auth`, `tenant`, `permission`
- 前端相关加 `frontend-` 前缀
- 简述部分 ≤5 个单词，描述做了什么

---

## 三、分支流向图

```
feature/*  ─────────┐
fix/*      ─────────┤
refactor/* ─────────┤
                    ▼
docs/*     ────→  dev  ────→  release/vX.Y.Z  ────→  prod
                    ▲                      │            │
                    │                      │            │
                    └────── 合回 ──────────┘            │
                                                        │
hotfix/*  ──────────────────────────────────────────────┘
           (从 prod 拉，合回 prod + dev)
```

---

## 四、合并规则（Merge Map）

| 源分支 | → `dev` | → `release` | → `prod` | → `master` |
|--------|---------|-------------|----------|------------|
| `feature/*` | ✅ | ❌ | ❌ | ❌ |
| `fix/*` | ✅ | ❌ | ❌ | ❌ |
| `refactor/*` | ✅ | ❌ | ❌ | ❌ |
| `docs/*` | ✅ | ❌ | ❌ | ❌ |
| `dev` | — | ✅ 仅发布时 | ❌ | ❌ |
| `release/*` | ✅ 合回 | — | ✅ | ❌ |
| `prod` | ❌ | ❌ | — | ❌ |
| `hotfix/*` | ✅ 合回 | ❌ | ✅ | ❌ |
| `master` | ❌ | ❌ | ❌ | — |

### 禁止事项（强制）

| 禁止 | 原因 |
|------|------|
| ❌ `dev` → `prod` | 绕过 release 冻结流程 |
| ❌ `feature/*` → `prod` | 绕过 review |
| ❌ 直接在 `dev` / `prod` 上 commit | 所有改动必须通过分支 + PR |
| ❌ `prod` → 任何分支 | 生产代码不应回流 |
| ❌ force push 到任何长期分支 | 破坏历史 |
| ❌ 合并不完整的代码到 `dev` | 阻塞其他人 |
| ❌ merge commit 混用 squash | 统一使用 squash merge 保持历史干净 |

---

## 五、标准工作流

### 场景 1：开发新功能

```bash
# 1. 从 dev 拉分支
git checkout dev
git pull origin dev
git checkout -b feature/role-crud

# 2. 开发 + 提交
git add .
git commit -m "feat(role): 实现角色 CRUD"

# 3. 推送并创建 PR
git push origin feature/role-crud
# → 在 GitHub 创建 PR: feature/role-crud → dev
# → 至少 1 人 Review → Squash Merge
```

### 场景 2：修复 Bug（开发中发现）

```bash
git checkout dev
git pull origin dev
git checkout -b fix/user-status-transition

# 修复 + 提交
git commit -m "fix(user): 修复状态转换校验失败"
git push origin fix/user-status-transition
# → PR: fix/user-status-transition → dev
```

### 场景 3：紧急修复（线上）

```bash
# 从 prod 拉
git checkout prod
git pull origin prod
git checkout -b hotfix/v0.1-sql-injection

# 修复 + 提交
git commit -m "hotfix: 修复 SQL 注入漏洞"
git push origin hotfix/v0.1-sql-injection
# → PR 1: hotfix/v0.1-sql-injection → prod（立即部署）
# → PR 2: hotfix/v0.1-sql-injection → dev（同步修复到开发线）
```

### 场景 4：发布流程

```bash
# 1. 从 dev 创建 release 分支
git checkout dev
git pull origin dev
git checkout -b release/v0.1.0

# 2. 在 release 上冻结 → 测试 → 修 Bug
git commit -m "release: v0.1.0 版本冻结"

# 3. 合并到 prod
git checkout prod
git merge --squash release/v0.1.0
git commit -m "release: v0.1.0"
git push origin prod

# 4. 合回 dev（同步 release 上的修复）
git checkout dev
git merge --squash release/v0.1.0
git commit -m "chore: 同步 release/v0.1.0 修复到 dev"
git push origin dev

# 5. 打 Tag
git tag -a v0.1.0 -m "Release v0.1.0"
git push origin v0.1.0
```

---

## 六、Commit Message 规范

```
{type}({scope}): {简短描述}

type:
  feat     — 新功能
  fix      — Bug 修复
  hotfix   — 紧急修复
  refactor — 重构（不改功能）
  docs     — 文档
  chore    — 杂项（依赖、配置）
  test     — 测试
  style    — 格式（空格、分号等）

scope:
  模块名：user, role, auth, tenant, permission, db, frontend, ci

示例：
  feat(user): 实现用户软删除
  fix(frontend): 修复编辑页面状态切换 400 错误
  hotfix(auth): 修复登录绕过漏洞
  docs(readme): 补充 API 文档
  chore(deps): 升级 Spring Boot 3.3
```

---

## 七、分支清理

| 时机 | 操作 |
|------|------|
| PR 合并后 | **立即删除** 源分支（GitHub 提供按钮） |
| release 合入 prod 后 | 删除 `release/vX.Y.Z` 分支 |
| 废弃分支 | 超过 30 天未活动的 feature/fix 分支可以清理 |

```bash
# 本地清理
git branch -d feature/xxx

# 远程清理
git push origin --delete feature/xxx

# 清理本地已删除的远程分支引用
git remote prune origin
```

---

## 八、快速参考

```bash
# 我要...
git checkout -b feature/user-xxx dev       # 做新功能
git checkout -b fix/user-xxx dev           # 修 Bug
git checkout -b hotfix/v0.x-xxx prod       # 紧急修复
git checkout -b release/v0.x.0 dev         # 准备发布
git checkout -b docs/xxx dev               # 写文档
```

| 操作 | 命令 |
|------|------|
| 同步 dev | `git checkout dev && git pull origin dev` |
| 查看所有分支 | `git branch -a` |
| 查看分支合并情况 | `git log --oneline --graph --all` |
| 放弃本地修改 | `git checkout -- .` |
| 暂存当前工作 | `git stash && git stash pop` |
