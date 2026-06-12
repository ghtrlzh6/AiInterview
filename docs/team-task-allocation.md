# 四人分工与任务清单（按功能模块）

> **更新日期**：2026-06-02  
> **分工原则**：12 个功能模块由 A/B/C/D 四人认领（**A 3 个 · B 4 个 · C 3 个 · D 2 个**）；每人对自己模块做**全栈**（后端 + 前端），自行联调。  
> **状态标记**：✅ 已完成 · 🔶 骨架/模拟模式（可演示，待完善）· ⬜ 待开发

---

## 一、当前项目进度概览

| 阶段 | 状态 | 说明 |
|------|------|------|
| Sprint 0 基础设施 | ✅ | Docker Compose、19 张表、JWT 认证、前后端脚手架、本地启动脚本 |
| Sprint 1 MVP 面试 | 🔶 | 主链路可跑通；题库仅 Java 岗位 5 道示例题 |
| Sprint 2 SSE + 追问 | ✅ | 流式对话、FollowUpStrategy、前端打字机与断线重连 |
| Sprint 3 评估报告 | 🔶 | 异步评估与报告 API 已有；无 API Key 时为模拟评分 |
| Sprint 3.5 管理后台 | 🔶 | 前后端页面齐全；知识库向量为模拟 |
| Sprint 4 多岗位 + 语音 | 🔶 | 四岗位 UI 已开放；其余岗位题库与岗位 Prompt 未补全；Web Speech 已有 |
| Sprint 5 RAG + 成长 + 推荐 | 🔶 | 成长曲线与资源推荐前后端已有；RAG/Chroma 未实现 |

**当前可演示路径**：注册/登录 → 选岗位 → 文字/语音面试 → 结束 → 轮询报告 → 成长曲线 → 知识库阅读 → 管理后台 CRUD（模拟 AI 模式）。

---

## 二、功能模块总览与负责人

| 成员 | 负责模块 |
|------|----------|
| **A** | M1 基础设施与工程化 · M2 用户认证与个人中心 · M11 管理后台 |
| **B** | M3 岗位与题库 · M4 AI 对话引擎 · M9 层级知识库 + RAG · M12 多模态输入（语音 / ASR） |
| **C** | M5 模拟面试会话 · M6 简历解析与项目深挖 · M7 手撕代码（BEHAVIOR） |
| **D** | M8 多维度评估与报告 · M10 成长曲线与资源推荐 |

| 编号 | 功能模块 | 负责人 |
|------|----------|--------|
| M1 | 基础设施与工程化 | **A** |
| M2 | 用户认证与个人中心 | **A** |
| M3 | 岗位与题库 | **B** |
| M4 | AI 对话引擎（LLM / SSE / 追问） | **B** |
| M5 | 模拟面试会话 | **C** |
| M6 | 简历解析与项目深挖 | **C** |
| M7 | 手撕代码（BEHAVIOR） | **C** |
| M8 | 多维度评估与报告 | **D** |
| M9 | 层级知识库 + RAG | **B** |
| M10 | 成长曲线与资源推荐 | **D** |
| M11 | 管理后台 | **A** |
| M12 | 多模态输入（语音 / ASR） | **B** |

---

## 三、各模块任务明细

### M1 基础设施与工程化 — 负责人：**A**

| 状态 | 任务 |
|------|------|
| ✅ | Spring Boot 3.3 脚手架、`Result<T>`、全局异常、MyBatis-Plus |
| ✅ | `docker-compose.yml`（MySQL / Redis / Chroma） |
| ✅ | `sql/init.sql`（19 张表 DDL + 基础种子数据） |
| ✅ | `.env.example`、`scripts/load-env.ps1`、`start-backend.ps1`、`start-frontend.ps1` |
| ✅ | Swagger / OpenAPI 配置 |
| ✅ | 前端工程化：`router` / `layouts` 壳 / `utils/request.ts` / `App.vue` |
| ✅ | 远端 MySQL 部署与 `.env` 生产配置文档（`docs/deployment.md`） |
| ✅ | Nginx 反向代理 + 前端 `dist` 静态部署（`deploy/nginx/ai-interview.conf`） |

---

### M2 用户认证与个人中心 — 负责人：**A**（全栈）

| 状态 | 任务 | 负责人 |
|------|------|--------|
| ✅ | 注册 / 登录 / 刷新 Token / 登出（Redis 黑名单） | A |
| ✅ | `UserController` 档案查询与更新 | A |
| ✅ | 前端登录/注册页、路由守卫、Token 持久化 | A |
| ✅ | 首页仪表盘骨架 | A |
| ✅ | 个人档案编辑页（昵称、学校、专业、目标岗位） | A |
| ✅ | 首页「最近面试得分」「面试次数」对接真实 API | A + D（数据来自 D 模块 API） |

---

### M3 岗位与题库 — 负责人：**B**（全栈）

| 状态 | 任务 | 负责人 |
|------|------|--------|
| ✅ | `PositionController`、`QuestionController` | B |
| ✅ | 按难度比例抽题逻辑（`InterviewService.startInterview`） | B |
| 🔶 | 种子数据：Java 岗位仅 **5** 道示例题 | B |
| ⬜ | 四岗位题库 SQL（Java/Web/Python/游戏 各 ≥20~30 题，覆盖四类题型） | B |
| ✅ | `AdminQuestionController` CRUD + 批量导入 | B |
| ✅ | 管理后台题目管理页 | B |
| ⬜ | 题目与知识库节点关联维护（`primary_kb_module_id`、`t_question_kb_point`） | B |
| ⬜ | 手撕题池扩充（`t_coding_challenge` Hot100 等） | C（M7 联动） |

---

### M4 AI 对话引擎 — 负责人：**B**

| 状态 | 任务 |
|------|------|
| ✅ | `LlmService` 接口 + `DeepSeekLlmService`（同步 + 流式） |
| ✅ | 无 API Key 时模拟回复降级 |
| ✅ | `FollowUpStrategy`（每题最多 2 次追问） |
| ✅ | `prompts/interview.system.txt`、`evaluation.*.txt` |
| ⬜ | 四岗位差异化面试官 Prompt（`interview_system_web/python/game.txt`） |
| ⬜ | Prompt 从 `t_system_config` 热加载（依赖 A 管理后台配置） |
| ⬜ | 配置真实 `LLM_API_KEY` 后全流程联调验证 |
| ⬜ | AI 出题接口 `POST /admin/ai/questions/generate` 完善与联调 | B（前后端） |

---

### M5 模拟面试会话 — 负责人：**C**（全栈）

| 状态 | 任务 | 负责人 |
|------|------|--------|
| ✅ | 开始 / 发消息（SSE）/ 结束 / 会话详情 / 消息历史 | C |
| ✅ | 前端岗位选择、面试间、结束页、报告轮询 | C |
| ✅ | SSE 打字机、`ConnectionStatus` 断线重连 | C |
| ⬜ | 面试间展示当前题型标签（TECH / SCENARIO / PROJECT / BEHAVIOR） | C |
| ⬜ | 集成本模块 M6/M7 + B 模块 M12（简历 / 手撕 / 语音） | C + B |
| ⬜ | 四岗位端到端联调（各完成 1 次面试 + 报告） | C + D |

---

### M6 简历解析与项目深挖 — 负责人：**C**（全栈）

| 状态 | 任务 | 负责人 |
|------|------|--------|
| 🔶 | `ResumeController` PDF 上传 + PDFBox 文本提取 | C |
| 🔶 | 项目条目 mock 抽取（`mockExtractProjects`） | C |
| ⬜ | LLM 结构化解析简历项目（替换 mock，依赖 B 的 LLM） | C |
| ⬜ | 前端简历上传组件 + 解析状态轮询 | C |
| ⬜ | 选岗页关联简历快照（`resumeSnapshotId`）并在 PROJECT_DEEP 题注入上下文 | C |

---

### M7 手撕代码（BEHAVIOR） — 负责人：**C**（全栈）

| 状态 | 任务 | 负责人 |
|------|------|--------|
| ✅ | `POST /interviews/{id}/coding-submit` + `t_session_coding_submit` | C |
| 🔶 | init.sql 仅 2 道 Hot100 + 1 道 BEHAVIOR 关联题 | C |
| ⬜ | LLM 评审代码提交并生成追问（依赖 B 的 LLM） | C |
| ⬜ | 前端 BEHAVIOR 题型：题目描述 + 代码编辑器 + 提交 | C |
| ⬜ | 手撕题与 M5 面试流程状态机打通 | C |

---

### M8 多维度评估与报告 — 负责人：**D**（全栈）

| 状态 | 任务 | 负责人 |
|------|------|--------|
| ✅ | `AiEvaluationService` 异步逐题评分 + 综合报告 | D |
| 🔶 | 无 API Key 时使用随机/模板模拟分 | D |
| ✅ | `ReportController` 详情 / 列表 / 分享 | D |
| ✅ | 前端报告详情（雷达图、亮点/不足/建议） | D |
| ✅ | 前端报告列表 + 结束页轮询跳转 | D |
| ⬜ | 报告页展示**逐题点评**（`dimensionScores` 列表） | D |
| ⬜ | 配置 API Key 后真实 LLM 评分联调 | D |
| ⬜ | 报告分享页（`/reports/share/:token` 公开访问页） | D |
| ⬜ | RAG 增强评分（集成到 `AiEvaluationService`，依赖 B 的 M9） | D + B |

---

### M9 层级知识库 + RAG — 负责人：**B**（全栈）

| 状态 | 任务 | 负责人 |
|------|------|--------|
| ✅ | `KbController` 树形目录 + Markdown 正文 | B |
| ✅ | 前端知识库阅读页（懒加载树 + Markdown 渲染） | B |
| ✅ | `AdminKbController` 节点/文章 CRUD | B |
| ✅ | 管理后台知识库管理页 | B |
| 🔶 | Admin 向量化接口为**模拟**（仅更新 `is_vectorized`） | B |
| ⬜ | 四岗位知识库正文 SQL（每岗位 ≥10 篇） | B |
| ⬜ | `RagService`：Embedding → Chroma 写入 | B |
| ⬜ | `RagService.search()` + 评估/出题上下文注入 | B |
| ⬜ | `ChromaConfig` 与 Docker Chroma 真实联通 | B |
| ⬜ | Admin 向量化按钮调用真实 `RagService` | B |

---

### M10 成长曲线与资源推荐 — 负责人：**D**（全栈）

| 状态 | 任务 | 负责人 |
|------|------|--------|
| ✅ | `GrowthController` + `GrowthService` | D |
| ✅ | `ResourceController` 推荐 / 反馈 / 搜索 | D |
| ✅ | 前端成长曲线页（ECharts 折线图） | D |
| ✅ | 前端资源页（报告推荐 + 搜索 + 反馈） | D |
| 🔶 | 种子数据仅 3 条学习资源 | D |
| ⬜ | 四岗位学习资源 SQL（每岗位 ≥15 条） | D |
| ⬜ | 报告详情页内嵌推荐区块（M8 与 M10 同负责人，自行整合） | D |
| ⬜ | `demo_student` 历史数据 + 成长曲线 3 点演示 | D |

---

### M11 管理后台 — 负责人：**A**（全栈）

| 状态 | 任务 | 负责人 |
|------|------|--------|
| ✅ | Admin 统计 / 岗位 / 用户 / AI 配置 / Prompt API | A |
| ✅ | `AdminResourceController` 资源 CRUD | A |
| ✅ | 前端 AdminLayout + 路由守卫（ADMIN 角色） | A |
| ✅ | 前端仪表盘、岗位管理、用户管理、AI 配置页 | A |
| ✅ | 管理后台**学习资源管理页**（对接 `AdminResourceController`） | A |
| ✅ | AI 配置页：Key 保存后真实连通性测试 | A |
| ✅ | `SystemConfigService` 热更新与 LLM 参数即时生效 | A |

---

### M12 多模态输入（语音 / ASR） — 负责人：**B**（全栈）

| 状态 | 任务 | 负责人 |
|------|------|--------|
| ✅ | 前端 `VoiceInput` + Web Speech API | B |
| ✅ | 选岗页文字/语音模式切换 | B |
| 🔶 | `AsrController` 讯飞 ASR **模拟** | B |
| ⬜ | `XunfeiAsrService` 真实 WebSocket 转写 | B |
| ⬜ | 前端在不支持 Web Speech 时降级调用 `/asr/convert` | B |
| ⬜ | 语音组件嵌入 C 的面试间 / 选岗页联调 | B + C |

---

## 四、成员分工总表（全栈）

### 成员 A — M1 · M2 · M11

**负责模块**：基础设施与工程化、用户认证与个人中心、管理后台

**主要前端**：`router` / `layouts` / `request.ts` · `LoginPage` / `RegisterPage` / `HomePage` / 个人档案 · Admin：`DashboardPage` / `PositionsPage` / `UsersPage` / `AiConfigPage` / 学习资源管理页（待建）

**近期优先任务**：
1. ✅ 个人档案编辑页 + 首页数据对接（调用 D 的成长/报告 API）  
2. ✅ 管理后台学习资源管理页 + AI 配置真实联调  
3. ✅ 远端 MySQL + Nginx 生产部署文档与配置模板  

---

### 成员 B — M3 · M4 · M9 · M12

**负责模块**：岗位与题库、AI 对话引擎、层级知识库 + RAG、多模态输入（语音 / ASR）

**主要前端**：Admin：`QuestionsPage` / `KbPage` · `KnowledgePage` · `VoiceInput` · 选岗页语音模式 · ASR 降级

**近期优先任务**：
1. ⬜ 四岗位题库 SQL 批量录入（P0）  
2. ⬜ 四岗位差异化 Prompt + 真实 LLM 联调  
3. ⬜ 四岗位知识库 SQL + `RagService` + Chroma  
4. ⬜ 语音/ASR 组件与 C 的面试流程联调  

---

### 成员 C — M5 · M6 · M7

**负责模块**：模拟面试会话、简历解析与项目深挖、手撕代码（BEHAVIOR）

**主要前端**：`PositionSelectPage` / `InterviewRoomPage` / `InterviewEndPage` · `utils/sse.ts` · `ConnectionStatus` · 简历上传 · BEHAVIOR 代码编辑器

**近期优先任务**：
1. ⬜ 面试间题型标签 + 四岗位端到端自测（与 D 报告联调）  
2. ⬜ 简历上传 UI + LLM 解析 + `resumeSnapshotId`  
3. ⬜ BEHAVIOR 代码编辑器 + LLM 评审 + 手撕题池扩充  
4. ⬜ 集成 B 的语音组件 + 嵌入 M6/M7 到面试间  

---

### 成员 D — M8 · M10

**负责模块**：多维度评估与报告、成长曲线与资源推荐

**主要前端**：`ReportListPage` / `ReportDetailPage` / 报告分享页 · `GrowthPage` / `ResourcesPage`

**近期优先任务**：
1. ⬜ 报告页逐题点评 + 报告内嵌推荐资源  
2. ⬜ 报告分享公开页 + 真实 LLM 评估联调  
3. ⬜ 四岗位学习资源 SQL 扩充 + 成长曲线演示数据  
4. ⬜ 与 B 联调：RAG 增强评估报告质量  

---

## 五、协作与联调节点

| 时间点 | 对齐内容 | 参与人 |
|--------|----------|--------|
| 每周初 | 各模块 API Request/Response 字段 | A/B/C/D |
| 题库补全后 | 四岗位各跑通 1 次完整面试 | B + C + D |
| API Key 配置后 | A 后台保存 Key → B LLM / D 评估联调 | A + B + D |
| B 语音就绪后 | M12 嵌入 C 的面试间 / 选岗页 | B + C |
| B RAG 就绪后 | M9 检索增强 D 的 M8 评估 | B + D |

---

## 六、优先级建议（工期紧张时）

| 优先级 | 任务 | 负责人 |
|--------|------|--------|
| **P0** | 四岗位题库 SQL | B |
| **P0** | 真实 LLM 联调（面试 + 评估） | B + D（A 配 Key） |
| **P0** | 四岗位端到端（C 面试 → D 报告） | C + D |
| **P1** | 简历 + 手撕嵌入面试 | C |
| **P1** | 个人档案 + Admin 资源管理页 | A |
| **P1** | 语音嵌入面试 | B + C |
| **P2** | RAG + Chroma | B |
| **P2** | RAG 增强评估 | B + D |
| **P2** | 讯飞 ASR 备用 | B |
| **P3** | 报告分享页 | D |
| **P3** | Nginx 部署 | A |

---

## 七、模块依赖关系（简图）

```
A：M1 基础设施 ──► M2 认证用户 ──► M11 管理后台
                         │
B：M3 岗位题库 ──► M4 AI 引擎 ──► M9 知识库/RAG
         │              │              │
         │              │         M12 语音/ASR
         │              │              │
         └──────────────┴──────► C：M5 面试会话
                                   ├── M6 简历
                                   └── M7 手撕代码
                                         │
                                         ▼
                                   D：M8 评估报告 ──► M10 成长/推荐
```
