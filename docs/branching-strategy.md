# 分支管理规范

> AI-FMS 项目 Git 分支策略，所有人必须遵守。

---

## 一、长期分支

| 分支 | 用途 | 保护 | 说明 |
|------|------|------|------|
| `prod` | **生产环境** | 🔒 锁定 | 线上运行代码，只接受 `release` 合并和 `hotfix/*` |
| `master` | **稳定基线** | 🔒 锁定 | **所有临时分支的拉取源。** 每次发布后与 `prod` 同步，始终可部署 |
| `release` | **发布候选** | 🔒 锁定 | 上线前冻结测试，从 `dev` 拉出，只修 Bug |
| `dev` | **开发集成** | 半保护 | 临时分支合入目标，**不稳定，禁止作为基线** |

### 为什么不能从 `dev` 拉分支？

```
dev = feature/A + feature/B + fix/C + 某人的实验代码 + ...
```

`dev` 包含所有开发者未经验证的代码，从它拉分支意味着：

- 新功能基线混入了别人未完成的代码
- 可能继承 `dev` 上的 Bug 或编译错误
- 多人并行开发时，彼此的代码互相污染

**正确做法：所有临时分支从 `master`（稳定基线）拉出，合入 `dev`（集成测试），通过 `release` 冻结后推向 `prod`。**

---

## 二、临时分支命名规范

| 类型 | 命名格式 | 示例 | 拉取源 | 合入目标 |
|------|----------|------|--------|----------|
| 新功能 | `feature/{模块}-{简述}` | `feature/role-crud` | `master` | `dev` |
| 前端页面 | `feature/frontend-{简述}` | `feature/frontend-login-page` | `master` | `dev` |
| Bug 修复 | `fix/{模块}-{问题}` | `fix/user-status-transition` | `master` | `dev` |
| 紧急修复 | `hotfix/{版本}-{问题}` | `hotfix/v0.1-sql-injection` | `prod` | `prod` + `dev` |
| 重构 | `refactor/{模块}-{范围}` | `refactor/user-dto-split` | `master` | `dev` |
| 文档 | `docs/{范围}` | `docs/api-guide` | `master` | `dev` |
| 发布准备 | `release/v{major}.{minor}.{patch}` | `release/v0.1.0` | `dev` | `prod` + `master` |

### 命名规则

- 全小写英文
- 单词用 `-` 连接（kebab-case）
- 模块名使用项目已有模块名：`user`, `role`, `auth`, `tenant`, `permission`
- 前端相关加 `frontend-` 前缀
- 简述部分 ≤5 个单词，描述做了什么

---

## 三、分支流向图

```
master (稳定基线 — 所有临时分支从这里拉)
  │
  ├── feature/role-crud ──────────┐
  ├── feature/frontend-login ─────┤
  ├── fix/user-status ────────────┤
  ├── refactor/user-dto ──────────┤
  └── docs/api-guide ─────────────┤
                                  ▼
            ┌───────────────→   dev  (集成测试 — 不稳定)
            │                     │
            │    release/v0.1.0 ←─┘  (冻结 — 只修 Bug)
            │         │
            │         ├──→ prod  (生产部署)
            │         │      │
            │         └──→ master  (同步基线)
            │
hotfix/* ───┘
(从 prod 拉，合回 prod + dev)
```

### 关键原则

```
拉取方向：  master → 临时分支     （从干净基线开始）
合入方向：  临时分支 → dev        （集成测试）
推进方向：  dev → release → prod → master 同步
例外：     hotfix 从 prod 拉      （修正在跑的代码）
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
| `release/*` | ❌ | — | ✅ | ✅ |
| `prod` | ❌ | ❌ | — | ❌ |
| `hotfix/*` | ✅ 合回 | ❌ | ✅ | ❌ |
| `master` | ❌ | ❌ | ❌ | — |

### 禁止事项（强制）

| 禁止 | 原因 |
|------|------|
| ❌ 从 `dev` 拉分支 | `dev` 是不稳定的集成代码，不是基线 |
| ❌ `dev` → `prod` | 绕过 release 冻结流程 |
| ❌ `feature/*` → `prod` | 绕过 review + 集成测试 |
| ❌ 直接在长期分支上 commit | 所有改动必须通过 PR |
| ❌ `prod` → 任何分支（hotfix 除外） | 生产代码不应回流到开发线 |
| ❌ force push 到任何长期分支 | 破坏历史 |
| ❌ 合并不完整的代码到 `dev` | 阻塞其他人 |
| ❌ merge commit 混用 squash | 统一使用 squash merge 保持历史干净 |

---

## 五、标准工作流

### 场景 1：开发新功能

```bash
# 1. 从 master（稳定基线）拉分支
git checkout master
git pull origin master
git checkout -b feature/role-crud

# 2. 开发 + 提交
git add .
git commit -m "feat(role): 实现角色 CRUD"

# 3. 推送并创建 PR
git push origin feature/role-crud
# → 在 GitHub 创建 PR: feature/role-crud → dev
# → 至少 1 人 Review → Squash Merge
# → dev 上跑集成测试
```

### 场景 2：修复 Bug（开发中发现）

```bash
git checkout master
git pull origin master
git checkout -b fix/user-status-transition

# 修复 + 提交
git commit -m "fix(user): 修复状态转换校验失败"
git push origin fix/user-status-transition
# → PR: fix/user-status-transition → dev
```

### 场景 3：紧急修复（线上）

```bash
# 从 prod 拉（因为线上跑的是 prod）
git checkout prod
git pull origin prod
git checkout -b hotfix/v0.1-sql-injection

# 修复 + 提交
git commit -m "hotfix: 修复 SQL 注入漏洞"
git push origin hotfix/v0.1-sql-injection
# → PR 1: hotfix/v0.1-sql-injection → prod（立即部署）
# → PR 2: hotfix/v0.1-sql-injection → dev（同步到开发线）
```

### 场景 4：发布流程

```bash
# 1. dev 经过集成测试后，创建 release 分支
git checkout dev
git pull origin dev
git checkout -b release/v0.1.0

# 2. release 上冻结 — 只修 Bug，不加新功能
git commit -m "release: v0.1.0 版本冻结"
git push origin release/v0.1.0
# → 测试团队在 release 上测试
# → 发现的 Bug 在 release 上直接修

# 3. 测试通过 → 合并到 prod
git checkout prod
git merge --squash release/v0.1.0
git commit -m "release: v0.1.0"
git push origin prod

# 4. 同步 master（让基线追上生产）
git checkout master
git merge --squash release/v0.1.0
git commit -m "chore: 同步 v0.1.0 到 master"
git push origin master

# 5. 打 Tag
git tag -a v0.1.0 -m "Release v0.1.0"
git push origin v0.1.0

# 6. 发布完成，删除 release 分支
git branch -d release/v0.1.0
git push origin --delete release/v0.1.0
```

---

## 六、各分支职责总结

```
master    "我永远是干净的。所有新工作从我这里开始。"
          → 只被 release 更新
          → 所有临时分支的拉取源

dev       "我是试验场。所有临时分支都在我这里汇合。"
          → 接受所有 feature/fix/refactor/docs 的 PR
          → 可能不稳定，禁止作为基线

release   "我在上线前冻结。只修 Bug，不接新功能。"
          → 从 dev 拉出
          → 合并到 prod + master
          → 发布后删除

prod      "我是线上代码。除了 release 和 hotfix，谁也不能碰我。"
          → 只接受 release 和 hotfix 合并
```

---

## 七、Commit Message 规范

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

## 八、分支清理

| 时机 | 操作 |
|------|------|
| PR 合并后 | **立即删除** 源分支 |
| release 合入 prod + master 后 | 删除 `release/vX.Y.Z` |
| 废弃分支 | 超过 30 天未活动可清理 |

```bash
git branch -d feature/xxx                  # 删除本地
git push origin --delete feature/xxx       # 删除远程
git remote prune origin                     # 清理本地过期引用
```

---

## 九、快速参考

```bash
# 我要...
git checkout -b feature/user-xxx master    # 做新功能
git checkout -b fix/user-xxx master        # 修 Bug
git checkout -b hotfix/v0.x-xxx prod       # 紧急修复
git checkout -b refactor/user-xxx master   # 重构
git checkout -b docs/xxx master            # 写文档
git checkout -b release/v0.x.0 dev         # 准备发布
```

| 操作 | 命令 |
|------|------|
| 同步 master | `git checkout master && git pull origin master` |
| 同步 dev | `git checkout dev && git pull origin dev` |
| 查看所有分支 | `git branch -a` |
| 查看分支合并图 | `git log --oneline --graph --all` |
| 放弃本地修改 | `git checkout -- .` |
| 暂存当前工作 | `git stash && git stash pop` |
