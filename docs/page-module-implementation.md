# 页面模块实现说明

本文档说明前端各页面如何对应到前端模块、API 模块和后端业务模块，便于按页面追踪实现逻辑。

## 一、整体组织方式

项目采用前后端分离结构：

- 前端入口：`frontend/src/main.ts` 挂载 Vue 应用，`frontend/src/App.vue` 承载路由出口。
- 路由模块：`frontend/src/router/index.ts` 定义普通用户页面、登录注册页面和管理后台页面。
- 布局模块：`frontend/src/layouts/MainLayout.vue`、`AuthLayout.vue`、`AdminLayout.vue` 分别承载用户端、认证页和管理端页面框架。
- 页面模块：`frontend/src/pages/**` 是具体页面实现。
- API 模块：`frontend/src/api/**` 封装页面调用的后端接口。
- 状态模块：`frontend/src/stores/auth.ts` 管理登录用户状态，`frontend/src/stores/interview.ts` 管理面试会话状态。
- 请求模块：`frontend/src/utils/request.ts` 统一处理 Axios、Token、响应解包和 401 跳转，`frontend/src/utils/sse.ts` 处理流式面试回复。
- 后端模块：`backend/src/main/java/com/aiinterview/controller/**` 提供接口，`service/**` 编排业务，`mapper/**` 访问数据库实体。

前端页面与后端的主要调用链为：

```text
页面组件 pages/**.vue
  -> 前端 API 封装 api/**.ts
  -> request.ts 或 sse.ts
  -> 后端 Controller
  -> 后端 Service
  -> Mapper / Entity / AI Service
```

## 二、路由与页面对应关系

`frontend/src/router/index.ts` 是页面入口总表。

| 路由 | 页面组件 | 布局 | 主要功能 |
| --- | --- | --- | --- |
| `/` | `pages/home/HomePage.vue` | `MainLayout.vue` | 首页、快捷入口、最近面试 |
| `/interview/select` | `pages/interview/PositionSelectPage.vue` | `MainLayout.vue` | 选择岗位、配置题量、上传简历、创建面试 |
| `/interview/:sessionId` | `pages/interview/InterviewRoomPage.vue` | `MainLayout.vue` | 面试对话、SSE 回复、语音输入、手撕代码提交 |
| `/interview/:sessionId/end` | `pages/interview/InterviewEndPage.vue` | `MainLayout.vue` | 面试结束、轮询报告生成状态 |
| `/reports` | `pages/report/ReportListPage.vue` | `MainLayout.vue` | 历史报告列表 |
| `/reports/:reportId` | `pages/report/ReportDetailPage.vue` | `MainLayout.vue` | 报告详情、雷达图、建议、资源入口 |
| `/growth` | `pages/growth/GrowthPage.vue` | `MainLayout.vue` | 能力成长曲线 |
| `/knowledge` | `pages/knowledge/KnowledgePage.vue` | `MainLayout.vue` | 知识库树和 Markdown 文章阅读 |
| `/resources` | `pages/resources/ResourcesPage.vue` | `MainLayout.vue` | 推荐学习资源和资源搜索 |
| `/auth/login` | `pages/auth/LoginPage.vue` | `AuthLayout.vue` | 登录 |
| `/auth/register` | `pages/auth/RegisterPage.vue` | `AuthLayout.vue` | 注册并自动登录 |
| `/admin` | `pages/admin/DashboardPage.vue` | `AdminLayout.vue` | 管理后台统计 |
| `/admin/positions` | `pages/admin/PositionsPage.vue` | `AdminLayout.vue` | 岗位管理 |
| `/admin/questions` | `pages/admin/QuestionsPage.vue` | `AdminLayout.vue` | 题库管理和 AI 出题 |
| `/admin/kb` | `pages/admin/KbPage.vue` | `AdminLayout.vue` | 知识库目录和文章管理 |
| `/admin/ai-config` | `pages/admin/AiConfigPage.vue` | `AdminLayout.vue` | AI Key、模型、Prompt 配置 |
| `/admin/users` | `pages/admin/UsersPage.vue` | `AdminLayout.vue` | 用户和角色管理 |

路由守卫通过 `requiresAuth`、`guestOnly`、`requiresAdmin` 控制访问权限。普通页面需要 Token；管理后台还会通过 `auth.fetchProfile()` 获取用户角色，并使用 `auth.isAdmin` 判断是否允许进入。

## 三、通用基础模块

### 1. 登录态与权限

相关文件：

- `frontend/src/stores/auth.ts`
- `frontend/src/api/auth.ts`
- `frontend/src/api/user.ts`
- `frontend/src/utils/request.ts`
- `backend/src/main/java/com/aiinterview/controller/AuthController.java`
- `backend/src/main/java/com/aiinterview/controller/UserController.java`
- `backend/src/main/java/com/aiinterview/security/**`

实现方式：

- 登录页调用 `authStore.login()`，内部调用 `authApi.login()`。
- 登录成功后，`request.ts` 的 `setTokens()` 将 `accessToken` 和 `refreshToken` 存入 `localStorage`。
- `authStore.fetchProfile()` 调用用户信息接口，将 `userInfo` 缓存在 Pinia 和 `localStorage`。
- 所有 Axios 请求通过请求拦截器自动添加 `Authorization: Bearer <token>`。
- 响应拦截器统一处理后端 `Result` 格式，`code === 200` 时只返回 `data`，401 时清空 Token 并跳转登录页。
- 后端由 Spring Security、`JwtAuthFilter`、`JwtUtil`、`SecurityUtils.currentUserId()` 共同完成认证和当前用户识别。

### 2. 布局与导航

相关文件：

- `frontend/src/layouts/MainLayout.vue`
- `frontend/src/layouts/AuthLayout.vue`
- `frontend/src/layouts/AdminLayout.vue`

实现方式：

- 用户端页面共用 `MainLayout.vue`，用于统一导航、页面容器和用户操作。
- 登录注册共用 `AuthLayout.vue`，避免展示主导航。
- 管理后台共用 `AdminLayout.vue`，路由层面要求 `requiresAdmin: true`。

## 四、普通用户页面模块

### 1. 首页

页面：`frontend/src/pages/home/HomePage.vue`

对应功能：

- 展示用户昵称。
- 提供“开始模拟面试”“评估报告”“成长曲线”“知识库”等快捷入口。
- 展示最近 5 条面试历史。

模块对应：

| 页面功能 | 前端模块 | 后端模块 |
| --- | --- | --- |
| 获取当前用户昵称 | `stores/auth.ts` | `UserController`、`UserService` |
| 最近面试历史 | `api/user.ts` 的 `getMyInterviews()` | `UserController`、`UserServiceImpl` |
| 页面跳转 | `vue-router` | 无 |

实现说明：

- 页面挂载时调用 `userApi.getMyInterviews({ page: 1, size: 5 })`。
- 历史记录有 `reportId` 时展示“查看报告”，点击跳转到 `/reports/:reportId`。
- 快捷卡片仅负责路由跳转，不直接处理业务数据。

### 2. 选择岗位与创建面试页

页面：`frontend/src/pages/interview/PositionSelectPage.vue`

对应功能：

- 获取岗位列表。
- 设置输入方式：文字或语音。
- 设置题目数量。
- 上传 PDF 简历并轮询解析状态。
- 根据岗位、题量、输入方式、简历快照创建面试会话。

模块对应：

| 页面功能 | 前端模块 | 后端模块 |
| --- | --- | --- |
| 岗位列表 | `api/position.ts` | `PositionController`、`PositionServiceImpl`、`PositionMapper` |
| 输入方式状态 | `stores/interview.ts` 的 `inputMode` | `InterviewSession.inputMode` |
| 简历上传 | `api/resume.ts` | `ResumeController`、`ResumeServiceImpl`、`UserResumeMapper`、`ResumeProjectMapper` |
| 创建面试 | `stores/interview.ts` 的 `start()`、`api/interview.ts` 的 `startInterview()` | `InterviewController.start()`、`InterviewServiceImpl.start()` |

实现说明：

- `onMounted()` 调用 `positionApi.listPositions()` 加载岗位卡片。
- 简历上传使用 Element Plus `el-upload`，手动组装 `FormData` 后调用 `resumeApi.uploadResume()`。
- 上传成功后，`pollResume()` 每 1.2 秒调用一次 `resumeApi.getResumeStatus()`，最多 20 次；解析成功后加载项目列表并默认选中该简历。
- 点击“进入面试间”时，页面调用 `interview.reset()` 清空旧会话，再调用 `interview.start(positionCode, { questionCount, resumeSnapshotId })`。
- 后端 `InterviewServiceImpl.start()` 会校验岗位和简历归属，创建 `InterviewSession`，挑选题目，保存 `InterviewQuestion`，返回第一条面试官消息和当前题目信息。

### 3. 面试间页面

页面：`frontend/src/pages/interview/InterviewRoomPage.vue`

对应功能：

- 展示当前岗位、题号、当前题目类型和标题。
- 展示对话消息，并将 Markdown 转为 HTML。
- 发送用户回答，接收 SSE 流式面试官回复。
- 支持语音输入转文字。
- 对手撕代码题展示代码输入区并提交代码。
- 主动结束面试并跳转报告生成页。

模块对应：

| 页面功能 | 前端模块 | 后端模块 |
| --- | --- | --- |
| 面试状态 | `stores/interview.ts` | `InterviewServiceImpl` 的会话上下文 |
| 流式回复 | `utils/sse.ts`、`stores/interview.ts` 的 `sendMessage()` | `InterviewController.message()`、`InterviewServiceImpl.sendMessage()` |
| 追问/下一题/结束事件 | `stores/interview.ts` 的 `onEvent` 分支 | `FollowUpStrategy`、`InterviewServiceImpl.streamResponse()` |
| 语音输入 | `components/VoiceInput.vue`、`api/interview.ts` 的 `convertAsr()` | `AsrController`、`XunfeiAsrService` |
| 代码提交 | `api/interview.ts` 的 `submitCoding()`、`getLatestCodingSubmit()` | `InterviewController.codingSubmit()`、`InterviewServiceImpl.codingSubmit()` |
| 结束面试 | `stores/interview.ts` 的 `end()` | `InterviewController.end()`、`InterviewServiceImpl.endSession()`、`AiEvaluationService.evaluateAsync()` |

实现说明：

- 页面主要状态不直接存在页面组件中，而是集中在 `useInterviewStore()`，包括 `sessionId`、`messages`、`currentQuestion`、`streaming`、`reportId`、`connectionOk`。
- 用户发送消息时，`stores/interview.ts` 先把用户消息追加到本地消息列表，再创建一条空的面试官消息。
- `streamSse()` 使用 `fetch` 请求 `/interviews/{sessionId}/message`，按 `data:` 行解析 JSON 事件。
- 后端通过 `SseEmitter` 返回事件，前端按事件类型更新页面：
  - `token`：持续拼接流式内容。
  - `done`：标记流结束并同步消息 ID 和当前题目信息。
  - `next_question`：切换到下一题。
  - `interview_end`：保存 `reportId`，随后页面跳转到结束页。
  - `error`：标记连接异常并展示错误文本。
- 当前题目为 `BEHAVIOR` 时，页面展示“手撕代码提交”区域。该区域提交代码后调用后端保存 `SessionCodingSubmit`，并返回简短代码评审。
- 点击结束面试时，页面调用 `interview.end()`，后端会将会话状态改为 `COMPLETED`，创建 `EvaluationReport`，并异步触发报告生成。

### 4. 面试结束页

页面：`frontend/src/pages/interview/InterviewEndPage.vue`

对应功能：

- 根据 URL query 中的 `reportId` 轮询报告状态。
- 报告完成前显示进度条。
- 报告完成后允许跳转报告详情页。

模块对应：

| 页面功能 | 前端模块 | 后端模块 |
| --- | --- | --- |
| 报告状态轮询 | `api/report.ts` 的 `getReport()` | `ReportController.getReport()`、`ReportServiceImpl.getReport()` |
| 报告异步生成 | 无直接前端模块 | `AiEvaluationService.evaluateAsync()` |

实现说明：

- 页面挂载后立即调用 `pollReport()`，之后每 3 秒执行一次。
- 当 `report.reportStatus === 'COMPLETED'` 时，将进度置为 100 并清除定时器。
- 后端报告生成由面试结束时触发，不在该页面触发。该页面只负责查询生成结果。

### 5. 报告列表与报告详情页

页面：

- `frontend/src/pages/report/ReportListPage.vue`
- `frontend/src/pages/report/ReportDetailPage.vue`

对应功能：

- 报告列表展示历史已完成报告。
- 报告详情展示综合分、维度分、雷达图、亮点、待改进、总结和建议。
- 报告详情可跳转到基于报告的学习资源推荐。

模块对应：

| 页面功能 | 前端模块 | 后端模块 |
| --- | --- | --- |
| 报告列表 | `api/report.ts` 的 `listReports()` | `ReportController.listReports()`、`ReportServiceImpl.listReports()` |
| 报告详情 | `api/report.ts` 的 `getReport()` | `ReportController.getReport()`、`ReportServiceImpl.toDetailMap()` |
| 雷达图 | `components/charts/RadarChart.vue` | 使用报告 `scores` 数据 |
| Markdown 总结 | `marked` | 使用报告 `summary` 数据 |
| 资源入口 | 路由跳转 `/resources?reportId=...` | `ResourceController` |

实现说明：

- `ReportServiceImpl.toDetailMap()` 会组装岗位名称、综合分、维度分、总结、亮点、弱项、建议和逐题评分。
- 维度分字段包括 `tech`、`expression`、`logic`、`depth`、`confidence`，前端传给 `RadarChart` 展示。
- `AiEvaluationService.evaluateAsync()` 负责在报告生成时写入 `EvaluationReport`、`DimensionScore`、`GrowthRecord` 和推荐资源记录。

### 6. 成长曲线页

页面：`frontend/src/pages/growth/GrowthPage.vue`

对应功能：

- 按岗位和时间范围筛选成长记录。
- 展示综合分变化、最强维度、最弱维度。
- 使用折线图展示多维能力变化。

模块对应：

| 页面功能 | 前端模块 | 后端模块 |
| --- | --- | --- |
| 岗位筛选 | `api/position.ts` | `PositionController` |
| 成长数据 | `api/growth.ts` | `GrowthController`、`GrowthServiceImpl`、`GrowthRecordMapper` |
| 折线图 | `components/charts/GrowthLineChart.vue` | 使用成长记录数据 |

实现说明：

- 页面挂载时先加载岗位列表，再调用 `growthApi.getGrowth({ positionCode, days })`。
- `watch([positionCode, days], load)` 保证筛选变化时自动刷新。
- 成长数据来自报告生成阶段写入的 `GrowthRecord`，所以用户完成至少一次报告后才会有曲线数据。

### 7. 知识库页面

页面：`frontend/src/pages/knowledge/KnowledgePage.vue`

对应功能：

- 左侧懒加载知识库目录树。
- 点击节点后加载节点详情和相关文章列表。
- 点击文章后加载 Markdown 正文并渲染。

模块对应：

| 页面功能 | 前端模块 | 后端模块 |
| --- | --- | --- |
| 目录树 | `api/kb.ts` 的 `getKbTree()` | `KbController`、`KbServiceImpl`、`KbNodeMapper` |
| 节点详情 | `api/kb.ts` 的 `getKbNode()` | `KbController`、`KbServiceImpl` |
| 文章正文 | `api/kb.ts` 的 `getKbArticle()` | `KbController`、`KbServiceImpl`、`KbArticleMapper` |
| Markdown 渲染 | `marked` | 无 |

实现说明：

- `el-tree` 使用 lazy 模式，展开节点时调用 `loadTreeNode()`。
- 节点点击后，如果节点有关联文章，则默认加载第一篇文章；如果没有文章，则展示节点预览内容。
- 文章正文由后端返回 `bodyMarkdown`，前端通过 `marked.parse()` 渲染。

### 8. 学习资源页面

页面：`frontend/src/pages/resources/ResourcesPage.vue`

对应功能：

- 如果 URL 带 `reportId`，展示该报告对应的推荐资源。
- 支持对推荐资源反馈“有帮助 / 没帮助”。
- 支持按主题关键字搜索学习资源。

模块对应：

| 页面功能 | 前端模块 | 后端模块 |
| --- | --- | --- |
| 报告推荐资源 | `api/resource.ts` 的 `getRecommendations()` | `ResourceController`、`ResourceServiceImpl`、`UserRecommendationMapper` |
| 推荐反馈 | `api/resource.ts` 的 `feedbackRecommendation()` | `ResourceController`、`ResourceServiceImpl` |
| 资源搜索 | `api/resource.ts` 的 `searchResources()` | `ResourceController`、`ResourceServiceImpl`、`LearningResourceMapper` |

实现说明：

- 报告详情页跳转资源页时会附带 `reportId`，资源页根据该参数加载推荐。
- 推荐资源由 `AiEvaluationService.generateRecommendations()` 在报告生成时写入。
- 搜索表格和推荐卡片共用资源实体 `LearningResource`，但推荐列表还包含 `reason`、`recommendationId` 等推荐记录字段。

## 五、管理后台页面模块

管理后台页面全部挂在 `/admin` 下，路由层面要求 `requiresAuth` 和 `requiresAdmin`。前端 API 主要集中在 `frontend/src/api/admin.ts`，后端接口集中在 `backend/src/main/java/com/aiinterview/controller/admin/**`。

### 1. 管理首页

页面：`frontend/src/pages/admin/DashboardPage.vue`

对应模块：

- 前端：`adminApi.getAdminStats()`
- 后端：`AdminStatsController`、`AdminStatsServiceImpl`

实现说明：

- 页面展示后台统计数据，例如用户、面试、题目、报告等总量或趋势。
- 数据由后台统计服务聚合多个 Mapper 后返回。

### 2. 岗位管理

页面：`frontend/src/pages/admin/PositionsPage.vue`

对应模块：

- 前端：`adminListPositions()`、`adminCreatePosition()`、`adminUpdatePosition()`、`adminUpdatePositionStatus()`、`adminDeletePosition()`
- 后端：`AdminPositionController`、`PositionServiceImpl`、`PositionMapper`

实现说明：

- 管理端维护岗位编码、名称、描述、技术栈、启用状态。
- 用户端选择岗位页读取的是启用后的岗位数据，所以此页面会直接影响用户可选岗位。
- 面试创建时也会使用岗位编码匹配题库和 Prompt。

### 3. 题库管理

页面：`frontend/src/pages/admin/QuestionsPage.vue`

对应模块：

- 前端：`adminListQuestions()`、`adminCreateQuestion()`、`adminUpdateQuestion()`、`adminDeleteQuestion()`、`adminBatchImportQuestions()`、`adminGenerateQuestions()`
- 后端：`AdminQuestionController`、`AdminAiController`、`QuestionServiceImpl`、`QuestionMapper`

实现说明：

- 题目字段包括岗位、题型、难度、主题、题干、参考答案等。
- 面试开始时，`InterviewServiceImpl.pickQuestions()` 会按岗位和题型从题库选题。
- 若配置了 AI 出题，管理端可通过 `AdminAiController` 调用 LLM 生成题目，再导入题库。
- 手撕代码题会通过题目的 `codingChallengeId` 关联 `CodingChallenge`，面试间据此展示题目 Markdown 和代码提交区域。

### 4. 知识库管理

页面：`frontend/src/pages/admin/KbPage.vue`

对应模块：

- 前端：`adminKbNodes()`、`adminCreateKbNode()`、`adminUpdateKbNode()`、`adminDeleteKbNode()`、`adminCreateKbArticle()`、`adminUpdateKbArticle()`、`adminDeleteKbArticle()`、`adminVectorizeArticle()`、`adminVectorizePendingBatch()`
- 后端：`AdminKbController`、`KbServiceImpl`、`KbNodeMapper`、`KbArticleMapper`

实现说明：

- 管理端维护知识库目录节点和文章正文。
- 用户端知识库页读取同一套 `KbNode` 和 `KbArticle` 数据。
- 向量化接口用于将文章内容写入向量检索系统，便于后续扩展 AI 检索增强能力。

### 5. AI 配置页

页面：`frontend/src/pages/admin/AiConfigPage.vue`

对应模块：

- 前端：`adminGetAiConfig()`、`adminUpdateAiConfig()`、`adminTestAiConfig()`
- 后端：`AdminAiController`、`AdminPromptController`、`PromptService`、`DeepSeekLlmService`、`SystemConfigMapper`

实现说明：

- 页面用于维护 LLM API Key、模型配置和 Prompt。
- 后端 `LlmService` 会根据配置判断真实 AI 是否可用。
- 如果 AI 不可用，面试回复、代码评审和报告评分会走内置模拟逻辑，保证流程仍能跑通。

### 6. 用户管理

页面：`frontend/src/pages/admin/UsersPage.vue`

对应模块：

- 前端：`adminListUsers()`、`adminGetUser()`、`adminUpdateUserRole()`
- 后端：`AdminUserController`、`UserServiceImpl`、`UserMapper`

实现说明：

- 页面用于查看用户列表和调整用户角色。
- 路由守卫依赖用户角色判断管理员权限，因此角色变更会影响用户能否访问 `/admin`。

## 六、核心业务链路说明

### 1. 从选择岗位到进入面试间

```text
PositionSelectPage.vue
  -> useInterviewStore.start()
  -> api/interview.ts startInterview()
  -> POST /api/v1/interviews/start
  -> InterviewController.start()
  -> InterviewServiceImpl.start()
  -> 创建 InterviewSession
  -> 选择 Question 并创建 InterviewQuestion
  -> 保存第一条 ChatMessage
  -> 返回 sessionId、firstMessage、currentQuestion
  -> 跳转 InterviewRoomPage.vue
```

这里的关键点是：页面只负责收集岗位、题量、输入方式、简历快照；真正的题目选择和会话初始化在后端完成。

### 2. 从用户回答到面试官流式回复

```text
InterviewRoomPage.vue 点击发送
  -> useInterviewStore.sendMessage()
  -> utils/sse.ts streamSse()
  -> POST /api/v1/interviews/{sessionId}/message
  -> InterviewController.message()
  -> InterviewServiceImpl.sendMessage()
  -> 保存用户 ChatMessage
  -> FollowUpStrategy.decide()
  -> LlmService.chatStream()
  -> SseEmitter 返回 token / next_question / interview_end / done
  -> 前端根据事件更新消息列表和当前题目
```

这里的关键点是：普通 Axios 不适合流式输出，所以面试回复使用 `fetch` 和 `ReadableStream` 解析 SSE。

### 3. 从结束面试到报告完成

```text
InterviewRoomPage.vue 主动结束或后端判定结束
  -> InterviewServiceImpl.endSession()
  -> InterviewSession 标记 COMPLETED
  -> 创建 EvaluationReport，状态 GENERATING
  -> AiEvaluationService.evaluateAsync(reportId)
  -> 写入 DimensionScore
  -> 更新 EvaluationReport 为 COMPLETED
  -> 写入 GrowthRecord
  -> 写入 UserRecommendation
  -> InterviewEndPage.vue 轮询 getReport()
  -> ReportDetailPage.vue 展示报告
```

这里的关键点是：报告生成是异步任务，结束页只是轮询报告状态；成长曲线和推荐资源都是报告生成的后续产物。

### 4. 从报告到学习资源推荐

```text
ReportDetailPage.vue
  -> 跳转 /resources?reportId=xxx
  -> ResourcesPage.vue
  -> api/resource.ts getRecommendations(reportId)
  -> ResourceController
  -> ResourceServiceImpl
  -> 查询 UserRecommendation 和 LearningResource
  -> 页面展示推荐原因、资源链接和反馈按钮
```

这里的关键点是：推荐不是实时生成，而是在报告生成阶段由 `AiEvaluationService.generateRecommendations()` 预先写入推荐记录。

## 七、页面功能与数据表的对应关系

| 页面/功能 | 主要实体 |
| --- | --- |
| 登录注册 | `User` |
| 首页最近面试 | `InterviewSession`、`EvaluationReport`、`Position` |
| 岗位选择 | `Position` |
| 简历上传和项目深挖 | `UserResume`、`ResumeProject`、`Question` |
| 面试会话 | `InterviewSession`、`InterviewQuestion`、`Question`、`ChatMessage` |
| 手撕代码提交 | `CodingChallenge`、`SessionCodingSubmit` |
| 报告详情 | `EvaluationReport`、`DimensionScore`、`Question`、`Position` |
| 成长曲线 | `GrowthRecord` |
| 知识库 | `KbNode`、`KbArticle` |
| 学习资源 | `LearningResource`、`UserRecommendation` |
| 管理后台 | 上述实体的管理接口 |

## 八、修改页面时的定位建议

- 如果要改页面展示结构，优先看 `frontend/src/pages/**`。
- 如果要改接口字段或请求路径，优先看 `frontend/src/api/**` 和对应 Controller。
- 如果要改登录、Token、权限跳转，优先看 `stores/auth.ts`、`utils/request.ts`、`router/index.ts` 和后端 `security/**`。
- 如果要改面试流程，优先看 `stores/interview.ts`、`InterviewRoomPage.vue`、`InterviewController`、`InterviewServiceImpl`、`FollowUpStrategy`。
- 如果要改报告评分和推荐逻辑，优先看 `AiEvaluationService`、`ReportServiceImpl`、`ResourceServiceImpl`。
- 如果要改管理端 CRUD，优先看 `frontend/src/api/admin.ts`、`pages/admin/**` 和 `controller/admin/**`。
