# Release — 发布准备

## 触发条件

- "准备发布"
- "打一个版本"
- "发布 v1.x"

---

## 执行流程

### Phase 1: 代码检查

```
1.  检查是否有未提交的代码 → git status
2.  git diff 确认变更内容
3.  运行全量测试
```

```bash
cd backend && ./mvnw test          # 后端测试
cd frontend && npm run test        # 前端测试
cd frontend && npm run build       # 前端构建
```

### Phase 2: 数据库检查

```
4. 对比本地 migration 和数据库已执行的 migration
5. 确认所有 migration 已提交到代码仓库
6. 确认无未执行的 migration
```

```bash
./mvnw flyway:info                 # 查看 migration 状态
```

### Phase 3: 文档检查

```
7.  review docs/worklog/ — 补全遗漏的工作日志
8.  review docs/architecture.md — 更新过时的架构决策
9.  review docs/api-design.md — 确认 API 文档反映最新接口
10. review README.md — 确认启动方式仍然正确
```

### Phase 4: 版本号

```
11. 更新 backend/pom.xml 中的 <version>
12. 更新 frontend/package.json 中的 version
13. 更新 docs/architecture.md 中的版本引用（如有）
```

### Phase 5: 打标签

```
14. git tag v{版本号}
15. git push origin v{版本号}
16. 记录发布日志
```

---

## 发布检查清单

```
□ ./mvnw test 全绿
□ npm run test 全绿
□ npm run build 成功
□ git status 无未提交文件
□ Flyway migration 无未提交文件
□ ./mvnw flyway:info — 迁移状态正常
□ docs/worklog/ 已更新
□ docs/architecture.md 无过期内容
□ docs/api-design.md 无过期内容
□ README.md 启动说明正确
□ 版本号已更新 (pom.xml + package.json)
□ git tag 已创建并推送
```

---

## 版本号规则

```
v{major}.{minor}.{patch}

major: 破坏性变更（API 不兼容）
minor: 新功能（向后兼容）
patch: Bug 修复 / 小改进

例如:
v0.0.1 → 初始开发版本
v0.1.0 → 第一个可演示的里程碑
v1.0.0 → 第一个正式发布
```

## 禁止行为

- ❌ 测试不绿就发布
- ❌ migration 未提交就发布
- ❌ 忘记打 tag
