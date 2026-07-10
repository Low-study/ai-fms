# Frontend Feature — 前端功能开发

## 触发条件

- "新增用户列表页面"
- "实现 XX 管理页面"
- "添加搜索组件"

---

## 执行流程

### Phase 1: 需求分析

1. 确认路由和权限
2. 确认 API 接口（请求/响应格式已由后端定义）
3. 确认 UI 交互细节
4. 确认是否需要新的通用组件

### Phase 2: 设计

5. 确定文件清单：

```
src/
├── types/{domain}.ts           → TypeScript 类型
├── api/{resource}Api.ts        → API 调用
├── services/{resource}Service.ts → 业务服务
├── hooks/use{Name}.ts          → 自定义 Hook
├── components/{Name}.tsx       → 通用组件
├── pages/{Name}Page.tsx        → 页面组件
└── router/index.tsx            → 添加路由
```

6. 输出文件清单，等待确认

### Phase 3: 实现（严格按顺序）

```
7.  types      → TypeScript 类型定义（禁止 any）
8.  api        → API 调用函数（使用统一 apiClient）
9.  services   → 业务服务层（如有复杂逻辑）
10. hooks      → TanStack Query Hook
11. components → 通用展示组件（如有）
12. pages      → 页面组件（组装布局+组件）
13. router     → 注册路由
```

### Phase 4: 状态覆盖

每个页面/组件必须处理 4 种状态：

```
- Loading → Ant Design <Spin> 或 <Skeleton>
- Empty   → Ant Design <Empty>
- Error   → Ant Design <Alert> + <Button> 重试
- Success → 正常渲染
```

### Phase 5: 测试

14. 组件测试（React Testing Library）
15. Hook 测试（如有复杂逻辑）

### Phase 6: 记录

16. 更新 `docs/worklog/YYYY-MM-DD.md`
17. 更新 `docs/api-design.md`（如 API 有变更）

---

## Checklist

- [ ] TypeScript 类型完整，零 `any`
- [ ] API 调用使用统一 `apiClient`
- [ ] 服务端状态用 TanStack Query，客户端状态用 Zustand
- [ ] 页面包含 Loading / Empty / Error / Success 四种状态
- [ ] 表单用 React Hook Form + Zod
- [ ] 组件遵循展示型/容器型分离
- [ ] 路由已注册
- [ ] 无 `console.log` 残留
- [ ] 字符串使用 i18n（不使用硬编码中文）
- [ ] `npm run build` 成功
- [ ] `npm run test` 通过
- [ ] Worklog 已更新
