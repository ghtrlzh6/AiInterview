# 开发计划 — 前端开发（人员 C）

## 角色定位

**职责范围**：全部用户侧 Vue 页面 + 管理后台前端页面 + 所有 composable 和 API 封装

你是团队的**前端全栈负责人**。你负责的代码覆盖用户能"看到"的一切，包括用户面试区和管理员后台。你的工作从 Sprint 0 就开始，并且在整个项目周期内持续。

**优势**：前端与后端可以高度并行——Sprint 0 你可以用 Mock 数据完成登录页，Sprint 1 你可以先做 UI 框架等后端 API 就绪再联调。

---

## 与其他成员的依赖关系

| 依赖方向 | 内容 | 时间节点 |
|----------|------|----------|
| **A → C**（你等待） | Task 0.6 完成：Auth API 可用，才能联调登录注册 | Sprint 0 末 |
| **A → C**（你等待） | Task 1.5/1.6 完成：面试 API 可用，Task 1.7/1.8 联调 | Sprint 1 中期 |
| **A → C**（你等待） | Task 2.3 完成：SSE 接口改造完成，Task 2.4 才能接入真实流 | Sprint 2 |
| **A → C**（你等待） | Task 3.4 完成：报告 API 可用，Task 3.5/3.6 联调 | Sprint 3 |
| **A → C**（你等待） | Task 3.5.x 完成：Admin API 可用，管理后台联调 | Sprint 3.5 |
| **A → C**（你等待） | Task 5.4/5.7 完成：成长/资源 API 可用，Task 5.5/5.7 联调 | Sprint 5 |
| **C → A**（你提供） | 发现 API 字段不匹配时，及时反馈给 A | 全程 |
| **策略**：等 API 可用前 | 用 Mock 数据完成页面开发和交互逻辑，API 就绪后一次性联调 | 全程 |

---

## 时间线概览

| 周次 | Sprint | 你的主要任务 |
|------|--------|-------------|
| 第 1 周 | Sprint 0 | Task 0.8：项目初始化 + 认证流程（Mock→联调）|
| 第 2-3 周 | Sprint 1 | Task 1.7：面试间 UI；Task 1.8：结束页 + 联调 |
| 第 4 周 | Sprint 2 | Task 2.4：SSE 打字机效果；Task 2.5：断线重连 |
| 第 5 周 | Sprint 3 | Task 3.5：报告详情页；Task 3.6：历史列表 + 轮询 |
| 第 6 周 | Sprint 3.5 | Task 3.5.6-3.5.9：全部管理后台页面 |
| 第 6-7 周 | Sprint 4 | Task 4.3：开放三岗位；Task 4.4：语音输入 |
| 第 7-8 周 | Sprint 5 | Task 5.5：成长曲线；Task 5.7：推荐资源前端 |

---

## Sprint 0：项目初始化 + 认证流程（第 1 周）

> 本 Sprint 与后端完全并行。Auth API 未就绪时，先 Mock 登录逻辑（写死 Token），API 就绪后联调。

### Task 0.8 — 前端项目初始化 + 认证流程 🔴

**涉及文件**
- `frontend/` 项目初始化文件（`package.json` / `vite.config.ts` / `tailwind.config.js`）
- `frontend/src/router/index.ts`（路由 + 路由守卫：未登录跳 `/login`，已登录访问 `/login` 跳 `/`）
- `frontend/src/api/http.ts`（Axios 实例：请求拦截器注入 `Authorization: Bearer <token>`；响应拦截器处理 401 自动跳登录页）
- `frontend/src/stores/auth.ts`（Pinia：存储 `token` + `userInfo`；使用 `pinia-plugin-persistedstate` 持久化到 localStorage）
- `frontend/src/views/auth/LoginView.vue`
- `frontend/src/views/auth/RegisterView.vue`
- `frontend/src/views/home/HomeView.vue`（仪表盘骨架：展示昵称 + 面试次数占位 `--`）

**实现内容**
- `vite.config.ts` 配置代理：`/api` → `http://localhost:8080`（避免跨域）
- 路由守卫：从 `auth.ts` 读取 token，无 token 跳 `/login`
- `http.ts` Axios 实例：超时 15000ms；401 时清空 auth store 并跳转 `/login`
- 登录页：表单校验 + 提交 → 存储 token/userInfo → 跳转首页
- 注册页：密码确认校验 + 提交 → 成功后跳登录
- 首页：显示"Hi，{昵称}，欢迎回来" + 导航卡片（开始面试 / 历史报告 / 成长曲线）

**分步完成建议**：
1. 先完成脚手架初始化和路由配置（不依赖后端）
2. Mock 登录：`auth.ts` 中直接写入假 token，跳过 API 验证
3. 等**人员 A 完成 Task 0.6**（Auth API）后，切换为真实 API 调用并联调

**完成标志**：注册→登录→看到首页→刷新登录态保持；登出后跳转登录页。

### Sprint 0 验收标准
- [ ] 路由守卫工作正常（未登录访问 `/` 跳 `/login`）
- [ ] 登录成功后跳转首页并显示用户昵称
- [ ] 刷新页面登录态不丢失
- [ ] 401 响应自动跳转登录页

---

## Sprint 1：面试 UI（第 2-3 周）

> 等人员 A 完成 Task 1.5/1.6 后进行联调；等待期间先做 UI 和交互逻辑，用 Mock 数据驱动。

### Task 1.7 — 前端岗位选择页 + InterviewRoom.vue 🔴

**涉及文件**
- `frontend/src/views/position/PositionSelectView.vue`
- `frontend/src/views/interview/InterviewRoom.vue`
- `frontend/src/api/interview.ts`（封装面试相关接口调用）
- `frontend/src/composables/useInterview.ts`（状态管理：消息列表、题目进度、`appendMessage()`）

**实现内容**

`PositionSelectView.vue`：
- 3 张岗位卡片：Java 后端（可点击）/ Web 前端（"即将开放"灰色）/ Python 算法（"即将开放"灰色）
- 点击可用岗位调用 `POST /api/v1/interviews/start`，收到 `{ sessionId, firstMessage }` 后跳转面试间
- 跳转路由：`/interview/:sessionId`

`InterviewRoom.vue`：
- 对话气泡列表：AI 消息（左侧灰色背景）/ 用户消息（右侧蓝色背景）
- 底部输入区：多行 Textarea + 发送按钮（Enter 发送，Shift+Enter 换行）
- 右上角题目进度：`第 X / 共 10 题`（从 `useInterview.ts` 中的 state 读取）
- 结束面试按钮 + `ElMessageBox.confirm` 二次确认
- Loading 状态：发送后按钮禁用 + AI 气泡显示"正在思考…"骨架屏

`useInterview.ts`：
- `messages: Ref<ChatMessage[]>`：消息列表
- `currentQuestionIndex: Ref<number>`：当前题号
- `appendMessage(role, content)`：追加消息到列表
- `sendMessage(content)`：调 API → 收到响应 → `appendMessage` AI 回复
- Sprint 2 中此方法将被改造为 SSE 接收

**完成标志**（Mock 版本可验收）：选择 Java 后端 → 进入面试间 → 看到模拟开场消息 → 输入内容 → 收到模拟下一题。

---

### Task 1.8 — 前端面试结束页 + 端到端联调 🔴

**涉及文件**
- `frontend/src/views/interview/InterviewEndView.vue`（"报告生成中"占位页）
- 联调验收：解决跨域、Token、接口字段不匹配等问题

**实现内容**
- 面试结束后跳转此页，展示动画占位（如旋转图标 + "正在生成评估报告…"）
- `useInterview.ts` 中监听面试结束事件，调用 `POST /api/v1/interviews/{id}/end`，然后跳转
- 预留轮询逻辑（Sprint 3 中此页会激活轮询报告状态）：
  ```typescript
  // TODO Sprint 3: 激活此轮询
  // const { startPolling } = useReportPolling(sessionId)
  ```

**完成标志**（真实 API 联调版本）：完整走通"选岗位→开始面试→回答 5+ 题→结束→看到结束占位页"，数据库有正确记录。

### Sprint 1 验收标准
- [ ] 岗位选择页显示 3 张卡片（1 张可点击，2 张禁用）
- [ ] 面试间对话气泡样式正确（AI/用户区分）
- [ ] 题目进度实时更新
- [ ] 联调完成：前后端完整走通一轮面试

---

## Sprint 2：SSE 打字机效果（第 4 周）

> 等人员 A 完成 Task 2.3（SSE 接口改造）后进行联调。

### Task 2.4 — 前端 SSE 接收改造（打字机效果）🔴

**涉及文件**
- `frontend/src/views/interview/InterviewRoom.vue`（SSE 接收逻辑替换原 Axios 调用）
- `frontend/src/composables/useInterview.ts`（新增 `connectSse()` / `disconnectSse()`）

**实现内容**

使用 `fetch` + `ReadableStream` 接收 SSE（原因：`EventSource` 不支持自定义 Header，而我们需要携带 `Authorization`）：

```typescript
const response = await fetch(`/api/v1/interviews/${sessionId}/message`, {
  method: 'POST',
  headers: { 'Authorization': `Bearer ${token}`, 'Content-Type': 'application/json' },
  body: JSON.stringify({ content: userInput })
})
const reader = response.body!.getReader()
const decoder = new TextDecoder()
```

SSE 事件处理：
- `event: token` + `data: "xxx"` → 逐 token 追加到当前 AI 气泡文本（打字机效果）
- `event: next_question` + `data: "{questionIndex: 3}"` → 更新题目进度
- `event: interview_end` → 调 `end` 接口，跳转结束页

**完成标志**：AI 回复有打字机逐字效果；题目进度实时更新；面试自动结束跳转。

---

### Task 2.5 — 前端断线重连 + 联调 🔴

**涉及文件**
- `frontend/src/composables/useInterview.ts`（断线检测 + 自动重连逻辑）
- `frontend/src/components/interview/ConnectionStatus.vue`（网络状态指示组件）

**实现内容**
- SSE `fetch` 抛出异常时，显示友好提示横幅（`ConnectionStatus.vue`："连接已断开，正在重连…"）
- 自动尝试重连最多 3 次（指数退避：1s / 2s / 4s）
- 超出重连次数：显示"手动重连"按钮
- 重连时重新发送**最后一条用户消息**（因为服务器没有处理完成）

**完成标志**：模拟网络断开（Chrome DevTools Network 调为 Offline）后能自动重连并继续面试；可演示追问场景（回答"不太清楚"触发追问）。

### Sprint 2 验收标准
- [ ] AI 回复逐字打字机效果
- [ ] 断线后自动重连（最多 3 次）
- [ ] 追问场景可演示（与人员 B 的 Task 2.2 联调）

---

## Sprint 3：评估报告页面（第 5 周）

> 等人员 A 完成 Task 3.4（ReportController）后进行联调。报告 API 未就绪时，用 Mock 数据完成页面 UI。

### Task 3.5 — 前端报告详情页 ReportDetail.vue 🔴

**涉及文件**
- `frontend/src/views/report/ReportDetailView.vue`
- `frontend/src/components/report/RadarChart.vue`（ECharts RadarChart 封装）
- `frontend/src/components/report/DimensionScoreList.vue`（逐题点评列表）
- `frontend/src/api/report.ts`

**实现内容**

页面布局（从上到下）：
1. **头部**：综合得分（大字居中）+ 等级标签（优秀 ≥85 / 良好 ≥70 / 待提升）+ 岗位/时长信息
2. **雷达图区**：5 维度雷达图（技术准确性 / 逻辑清晰度 / 回答深度 / 表达能力 / 自信程度）
3. **综合总结**：Markdown 渲染（使用 `marked` 库），支持加粗/列表格式
4. **亮点/不足/建议**：三栏卡片（列表形式）
5. **逐题点评**：可折叠列表，默认展开前 3 题；每题显示题目 + 三维分数 + 点评文字

`RadarChart.vue` 封装：
```typescript
defineProps<{ scores: { label: string, value: number }[] }>()
```

**完成标志**：页面正确展示雷达图和全部报告内容；Markdown 总结正确渲染加粗/列表格式。

---

### Task 3.6 — 前端历史报告列表 + 轮询逻辑 🔴

**涉及文件**
- `frontend/src/views/report/ReportListView.vue`
- `frontend/src/views/interview/InterviewEndView.vue`（激活 Task 1.8 预留的轮询逻辑）

**实现内容**

`ReportListView.vue`：
- 历史报告列表，按时间倒序排列
- 每行显示：岗位名称 / 综合得分（评分后显示，生成中显示 Loading）/ 面试时长 / 状态徽章（生成中/已完成）/ 查看详情按钮

`InterviewEndView.vue` 轮询激活：
- 每 3 秒调用 `GET /api/v1/reports/{reportId}` 轮询
- 若 status=GENERATING，继续轮询
- 若 status=COMPLETED，停止轮询并跳转 `/report/:reportId`
- 轮询超时（2 分钟）后显示"请手动刷新"提示

**完成标志**：面试结束后轮询自动跳转报告详情；历史列表可查看多次面试记录。

### Sprint 3 验收标准
- [ ] 雷达图正确渲染 5 个维度
- [ ] Markdown 总结正确解析
- [ ] 面试结束后自动轮询并跳转报告（联调通过）
- [ ] 历史报告列表展示正确

---

## Sprint 3.5：管理后台（第 6 周）

> 等人员 A 完成 Task 3.5.1-3.5.5（Admin API）后联调。等待期间先做 UI 骨架和 Mock 数据。

### Task 3.5.6 — AdminLayout + 路由守卫 + 仪表盘页 🟡

**涉及文件**
- `frontend/src/layouts/AdminLayout.vue`（侧边栏导航）
- `frontend/src/router/index.ts`（新增 `/admin/**` 路由组）
- `frontend/src/views/admin/AdminDashboardView.vue`
- `frontend/src/api/admin.ts`（封装全部 admin 接口）

**实现内容**

`AdminLayout.vue` 侧边栏菜单项：
- 仪表盘 / 岗位管理 / 题目管理 / 知识库 / AI 配置 / 用户管理

路由守卫（加在 `/admin/**` 路由的 `beforeEnter`）：
```typescript
(to, from, next) => {
  const auth = useAuthStore()
  if (auth.userInfo?.role !== 'ADMIN') {
    ElMessage.error('无权限访问管理后台')
    next('/')
  } else {
    next()
  }
}
```

`AdminDashboardView.vue`：
- 4 个统计卡片：总用户数 / 今日面试数 / 题库总量 / 待向量化文档数
- 各岗位面试数量柱状图（ECharts BarChart）

**完成标志**：普通用户访问 `/admin` 被拦截并提示；admin 用户正常进入仪表盘并看到统计数据。

---

### Task 3.5.7 — 前端岗位管理页 + 题目管理页 🟡

**涉及文件**
- `frontend/src/views/admin/PositionManageView.vue`
- `frontend/src/views/admin/QuestionManageView.vue`
- `frontend/src/components/admin/QuestionFormDialog.vue`（新增/编辑弹窗）

**实现内容**

`PositionManageView.vue`：
- Element Plus 表格：显示岗位名称 / 代码 / 状态 / 操作
- 启用/停用开关（调 `PATCH /api/v1/admin/positions/{id}/status`）
- 编辑弹窗（名称 + 描述）

`QuestionManageView.vue`：
- 筛选栏：岗位选择 / 题型选择 / 难度选择
- 分页表格：题目内容（截断 50 字）/ 岗位 / 题型 / 难度 / 操作
- 新增/编辑弹窗 `QuestionFormDialog.vue`（包含全部字段，含参考答案 Textarea）
- 删除：`ElMessageBox.confirm` 确认后调删除接口
- 批量导入：下载模板 JSON 按钮 + 文件选择上传

**完成标志**：通过界面新增题目后，在表格中能立即看到；批量导入 JSON 后数量正确增加。

---

### Task 3.5.8 — 前端 AI 配置页 🟡

**涉及文件**
- `frontend/src/views/admin/AiConfigView.vue`
- `frontend/src/components/admin/PromptEditor.vue`（带变量说明的 Textarea 编辑器）

**实现内容**

`AiConfigView.vue`：
- LLM 参数分组展示（api_key 显示 `***masked***`，点击"修改"切换为可编辑 Input）
- 参数：API Key / Base URL / Model / Temperature / Max Tokens
- "保存配置"按钮：调 `PUT /api/v1/admin/ai-config`
- "测试连通性"按钮：点击后显示 Loading → 展示延迟（`xxx ms`）或错误信息

`PromptEditor.vue`：
- 顶部：Prompt Key 选择下拉（interview_system / follow_up / evaluation_question / evaluation_final）
- 变量说明区：动态展示当前 Prompt 支持的变量（如 `{question}` / `{user_answer}`）
- 大 Textarea 编辑区：显示当前 Prompt 内容，可编辑
- 底部：预览区（填充示例变量 → 点击"预览"→ 调 `POST /api/v1/admin/prompts/{key}/preview` → 展示渲染结果）
- "保存"按钮：调 `PUT /api/v1/admin/prompts/{key}`

**完成标志**：修改 temperature 并保存后，测试连通性显示正常延迟；Prompt 预览功能返回填充后的文本。

---

### Task 3.5.9 — 前端知识库管理页 + 用户管理页 🟡

**涉及文件**
- `frontend/src/views/admin/KnowledgeManageView.vue`
- `frontend/src/views/admin/UserManageView.vue`

**实现内容**

`KnowledgeManageView.vue`：
- 分页表格：标题 / 岗位 / 文档类型 / 向量化状态徽章（已向量化=绿色/未向量化=橙色）
- 新增/编辑：抽屉式表单（含长文本内容编辑 Textarea）
- "触发向量化"按钮：调 `POST /api/v1/admin/knowledge/{id}/vectorize`，成功后状态变绿色

`UserManageView.vue`：
- 分页表格 + 用户名搜索框
- 角色修改：表格内下拉选择 USER/ADMIN + `ElMessageBox.confirm`（"确认修改该用户角色？"）

**完成标志**：知识库文档触发向量化后状态变"已向量化"；角色修改弹出确认框，确认后更新。

### Sprint 3.5 验收标准
- [ ] 非 ADMIN 用户访问 `/admin` 被拦截
- [ ] 仪表盘统计卡片数据正确
- [ ] 题目管理 CRUD 全部可用
- [ ] AI 配置保存 + 连通性测试可用
- [ ] Prompt 编辑 + 预览可用

---

## Sprint 4：三岗位开放 + 语音输入（第 6-7 周）

### Task 4.3 — 前端岗位选择页开放三个岗位 + 联调 🔴

**涉及文件**
- `frontend/src/views/position/PositionSelectView.vue`（移除"即将开放"限制）
- `frontend/src/api/position.ts`（从接口动态获取岗位列表）

**实现内容**
- 改造岗位卡片为**动态渲染**：`GET /api/v1/positions`（返回 `is_active=true` 的岗位）
- 移除硬编码的"即将开放"逻辑
- 每张岗位卡片：显示岗位名称 + 描述 + 适合人群 + 开始面试按钮

**完成标志**：三个岗位均可点击发起面试；各自完成完整面试流程（题目内容明显不同）。

---

### Task 4.4 — 前端 Web Speech API 语音输入 🟡

**涉及文件**
- `frontend/src/components/interview/VoiceInput.vue`（麦克风按钮组件）
- `frontend/src/composables/useSpeechRecognition.ts`（封装 Web Speech API）

**实现内容**

`useSpeechRecognition.ts`：
```typescript
const recognition = new (window.SpeechRecognition || window.webkitSpeechRecognition)()
recognition.lang = 'zh-CN'
recognition.interimResults = true  // 实时中间结果
recognition.onresult = (e) => { transcript.value = e.results[e.results.length-1][0].transcript }
```

`VoiceInput.vue`：
- 检测浏览器兼容性：`!('SpeechRecognition' in window || 'webkitSpeechRecognition' in window)` → 显示"当前浏览器不支持语音输入"提示，按钮禁用
- 按钮样式：麦克风图标；录音中 → 红色脉冲动画
- 点击开始录音 → 识别结果实时填入面试间 Textarea → 点击停止 / 识别结束后自动停止

**完成标志**：Chrome/Edge 下中文语音识别后内容出现在输入框；不支持的浏览器显示降级提示。

### Sprint 4 验收标准
- [ ] 三岗位均可动态展示并发起面试
- [ ] 语音识别在 Chrome/Edge 下工作正常
- [ ] 不支持语音的浏览器有友好降级提示

---

## Sprint 5：成长曲线 + 推荐资源前端（第 7-8 周）

### Task 5.5 — 前端成长曲线页 GrowthChart.vue 🟡

**涉及文件**
- `frontend/src/views/growth/GrowthView.vue`
- `frontend/src/components/growth/GrowthLineChart.vue`（ECharts LineChart 封装）

**实现内容**

`GrowthView.vue`：
- 顶部：岗位筛选 Tab（Java 后端 / Web 前端 / Python 算法 / 全部）
- 趋势标签：`↑ 进步中` / `↓ 有待提升` / `→ 保持稳定`（从 API `trend` 字段读取）

`GrowthLineChart.vue`：
- ECharts 折线图：横轴为面试日期，纵轴 0-100
- 5 条折线各一种颜色（技术准确性 / 逻辑清晰度 / 回答深度 / 表达能力 / 自信程度）
- 图例可点击显示/隐藏各维度

**首页激活**：将 `HomeView.vue` 中的 `--` 占位替换为调 API 获取的最近面试综合得分。

**完成标志**：有 3+ 次历史面试时折线图正确渲染；切换岗位筛选数据正确更新。

---

### Task 5.7 — 推荐资源嵌入报告页（前端部分）🟡

**涉及文件**
- `frontend/src/views/report/ReportDetailView.vue`（底部追加推荐资源区块）
- `frontend/src/components/report/ResourceRecommendations.vue`

**实现内容**

`ResourceRecommendations.vue`：
- 在 `ReportDetailView.vue` 底部追加"推荐学习资源"区块
- 每条资源：类型图标（📄 文章 / 🔗 题目 / 🎥 视频）+ 标题 + 点击跳转链接
- 每条资源右侧：有帮助 👍 / 没帮助 👎 反馈按钮（点击调 `POST /feedback` 接口，反馈后按钮禁用）

**完成标志**：报告底部出现推荐资源列表；点击反馈不报错，按钮禁用状态正确。

### Sprint 5 验收标准
- [ ] 成长折线图正确渲染（3+ 次历史数据）
- [ ] 岗位筛选功能正常
- [ ] 报告底部推荐资源显示，反馈功能可用

---

## 你的任务汇总

| 任务 | Sprint | 优先级 | 前置依赖 |
|------|--------|--------|---------|
| Task 0.8 项目初始化 + 认证 | S0 | 🔴 | A 的 Task 0.6（联调时）|
| Task 1.7 岗位选择 + 面试间 | S1 | 🔴 | A 的 Task 1.5/1.6（联调时）|
| Task 1.8 结束页 + 联调 | S1 | 🔴 | A 的 Task 1.6 |
| Task 2.4 SSE 打字机效果 | S2 | 🔴 | A 的 Task 2.3 |
| Task 2.5 断线重连 + 联调 | S2 | 🔴 | Task 2.4 |
| Task 3.5 报告详情页 | S3 | 🔴 | A 的 Task 3.4（联调时）|
| Task 3.6 历史列表 + 轮询 | S3 | 🔴 | A 的 Task 3.4 |
| Task 3.5.6 AdminLayout + 仪表盘 | S3.5 | 🟡 | A 的 Task 3.5.2 |
| Task 3.5.7 岗位 + 题目管理页 | S3.5 | 🟡 | A 的 Task 3.5.3 |
| Task 3.5.8 AI 配置页 + Prompt 编辑 | S3.5 | 🟡 | A 的 Task 3.5.5 |
| Task 3.5.9 知识库 + 用户管理页 | S3.5 | 🟡 | A 的 Task 3.5.4 |
| Task 4.3 三岗位开放 | S4 | 🔴 | D 的 Task 4.1（数据）|
| Task 4.4 语音输入 | S4 | 🟡 | 无（独立实现）|
| Task 5.5 成长曲线页 | S5 | 🟡 | A 的 Task 5.4 |
| Task 5.7 推荐资源前端 | S5 | 🟡 | A 的 Task 5.7 |

---

## 开发效率建议

1. **先做 UI 骨架，再联调数据**：每个页面先用 Mock 数据完成交互逻辑，API 就绪后替换真实数据，一次性解决跨域/字段问题。

2. **统一 Mock 数据管理**：在 `src/mocks/` 目录存放 Mock 响应 JSON，便于前后端字段对齐时快速发现差异。

3. **接口字段不匹配时**：第一时间在团队沟通，记录在接口约定文档中，避免双方各自修改造成混乱。

4. **ECharts 封装原则**：每个图表写成独立组件（`RadarChart.vue` / `GrowthLineChart.vue`），接收 `data` prop，不在组件内部调 API，便于复用和测试。
