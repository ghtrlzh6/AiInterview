# 开发计划（敏捷迭代模型）

> 总工期参考：**8 周 / 6 个 Sprint**（适合 3-4 人团队竞赛场景）  
> **敏捷原则**：每个 Sprint 结束时交付可运行、可演示的软件增量；先跑通最小可用路径，再逐步扩展功能；架构设计对扩展开放、对修改封闭。  
> 优先级标记：🔴 必做核心功能 / 🟡 重要功能 / 🟢 加分项  
> **任务粒度**：每个编号任务（如 `Task 1.2`）为一个独立工作单元，设计为可在**一个 AI Agent Session** 中单独完成。

---

## 四人分工方案

> **分工原则**：按模块垂直切分，减少相互等待；前后端各有主责但需联调配合；A/B/C 负责后端，D 负责全部前端。  
> 每周 Sprint 启动时对齐接口契约（Request/Response DTO），避免前后端并行时的 blocking。

### 人员角色总览

| 成员 | 角色定位 | 核心负责模块 |
|------|----------|-------------|
| **成员 A** | 后端基础架构 & 安全认证 | 项目脚手架、数据库、Security/JWT、用户模块、管理后台后端 |
| **成员 B** | 后端面试核心 & AI 对话 | 题库模块、面试模块、LLM/SSE 流式、追问策略、多岗位扩展 |
| **成员 C** | 后端评估报告 & AI 增强 | 评估模块、报告模块、RAG 知识检索、成长曲线、资源推荐 |
| **成员 D** | 前端全栈 | 全部前端页面与组件（Vue3 + ECharts + 语音输入） |

---

### 成员 A — 后端基础架构 & 安全认证

**模块**：基础设施 / 认证模块 / 用户模块 / 管理后台（后端）

| Sprint | Task | 内容 | 优先级 |
|--------|------|------|--------|
| Sprint 0 | Task 0.1 | Spring Boot 脚手架 + Docker Compose（MySQL/Redis/Chroma） | 🔴 |
| Sprint 0 | Task 0.2 | 数据库建表脚本 DDL（14 张表 + 索引） | 🔴 |
| Sprint 0 | Task 0.3 | 初始化数据 DML（岗位/配置/admin 账号） | 🔴 |
| Sprint 0 | Task 0.4 | MyBatis-Plus 配置 + `Result<T>` + 全局异常处理 | 🔴 |
| Sprint 0 | Task 0.5 | Spring Security + JWT 过滤器链 | 🔴 |
| Sprint 0 | Task 0.6 | AuthController（注册/登录/刷新/登出） | 🔴 |
| Sprint 0 | Task 0.7 | UserController + `LlmService` 接口定义（占位） | 🔴 |
| Sprint 3.5 | Task 3.5.1 | `SystemConfigService` 热更新配置服务 | 🟡 |
| Sprint 3.5 | Task 3.5.2 | `AdminStatsController` + `AdminPositionController` | 🟡 |
| Sprint 3.5 | Task 3.5.4 | `AdminKnowledgeController` + `AdminUserController` | 🟡 |
| Sprint 3.5 | Task 3.5.5 | `AdminAiConfigController`（AI配置 + Prompt 管理） | 🟡 |
| Sprint 4 | Task 4.5 | `AsrController`（讯飞 ASR 备用方案） | 🟢 |

**关键产出**：可运行的开发环境、完整认证流程、管理后台后端 API  
**与他人接口约定**：`Result<T>` 格式、JWT 验证规则、`LlmService` 接口签名（Sprint 0 末与 B 对齐）

---

### 成员 B — 后端面试核心 & AI 对话

**模块**：题库模块 / 面试模块 / AI 对话引擎 / 多岗位扩展

| Sprint | Task | 内容 | 优先级 |
|--------|------|------|--------|
| Sprint 1 | Task 1.1 | Java 后端题库 SQL 数据（30 题） | 🔴 |
| Sprint 1 | Task 1.2 | `QuestionController` + `QuestionService` + `QuestionMapper` | 🔴 |
| Sprint 1 | Task 1.3 | `DeepSeekLlmService` 实现 + `interview_system.txt` Prompt | 🔴 |
| Sprint 1 | Task 1.4 | 题目按难度比例抽取 + 面试会话创建（`startInterview()`） | 🔴 |
| Sprint 1 | Task 1.5 | `InterviewController.start`（`POST /interviews/start`） | 🔴 |
| Sprint 1 | Task 1.6 | `InterviewController`（message / end / getMessages） | 🔴 |
| Sprint 2 | Task 2.1 | `LlmService.chatStream()` 流式实现（OkHttp SSE） | 🔴 |
| Sprint 2 | Task 2.2 | `FollowUpStrategy` + 追问 Prompt（JSON 结构化输出） | 🔴 |
| Sprint 2 | Task 2.3 | `InterviewController.message` 改造为 SSE + 状态机防护 | 🔴 |
| Sprint 3.5 | Task 3.5.3 | `AdminQuestionController`（题目 CRUD + 批量导入） | 🟡 |
| Sprint 4 | Task 4.1 | Web 前端题库 SQL 数据（30 题） | 🔴 |
| Sprint 4 | Task 4.2 | Python 算法 + 游戏客户端题库 SQL + 各岗位定制 Prompt | 🔴 |

**关键产出**：完整面试对话后端、SSE 流式推送、追问逻辑、四岗位题库  
**与他人接口约定**：`POST /interviews/start` Response（与 D 对齐）、SSE 事件格式（`token`/`done`/`next_question`/`interview_end`）

---

### 成员 C — 后端评估报告 & AI 增强

**模块**：评估模块 / 报告模块 / RAG 知识检索 / 成长曲线 / 资源推荐

| Sprint | Task | 内容 | 优先级 |
|--------|------|------|--------|
| Sprint 3 | Task 3.1 | 评估 Prompt 模板设计（逐题评分 + 综合报告） | 🔴 |
| Sprint 3 | Task 3.2 | `AiEvaluationService` 异步逐题评分 + `t_dimension_score` 持久化 | 🔴 |
| Sprint 3 | Task 3.3 | 综合报告生成 + `t_evaluation_report` 状态更新 + `t_growth_record` 写入 | 🔴 |
| Sprint 3 | Task 3.4 | `ReportController`（报告详情 / 历史列表） | 🔴 |
| Sprint 5 | Task 5.1 | 知识库文档内容录入（每岗位 10+ 篇 SQL） | 🟡 |
| Sprint 5 | Task 5.2 | `RagService` 向量化（Embedding → Chroma） | 🟡 |
| Sprint 5 | Task 5.3 | `RagService.search()` + 评估服务集成 RAG | 🟡 |
| Sprint 5 | Task 5.4 | `GrowthController`（成长数据 + 趋势计算） | 🟡 |
| Sprint 5 | Task 5.6 | 学习资源 SQL + `ResourceService` 推荐逻辑 | 🟡 |
| Sprint 5 | Task 5.7 | `ResourceController`（推荐接口 + 用户反馈接口） | 🟡 |
| Sprint 5 | Task 5.8 | 演示数据预置 + 演示脚本（`demo_student` 账号与历史记录） | 🔴 |

**关键产出**：多维度自动评分、结构化报告、RAG 检索增强、成长曲线数据、资源推荐  
**与他人接口约定**：`GET /reports/{id}` Response 结构（与 D 对齐）、`GET /growth` 返回时间序列格式

---

### 成员 D — 前端全栈

**模块**：全部 Vue3 前端（用户端 + 管理后台）

| Sprint | Task | 内容 | 优先级 |
|--------|------|------|--------|
| Sprint 0 | Task 0.8 | 前端项目初始化（Vite+Vue3+TS+Pinia） + 登录/注册页 + 路由守卫 | 🔴 |
| Sprint 1 | Task 1.7 | 岗位选择页 + `InterviewRoom.vue`（对话气泡 + 进度 + 发送） | 🔴 |
| Sprint 1 | Task 1.8 | 面试结束占位页 + Sprint 1 端到端联调 | 🔴 |
| Sprint 2 | Task 2.4 | SSE 接收改造（打字机效果 + 题目进度实时更新） | 🔴 |
| Sprint 2 | Task 2.5 | 断线重连 + 网络状态指示组件 | 🔴 |
| Sprint 3 | Task 3.5 | 报告详情页（雷达图 + 逐题点评 + Markdown 渲染） | 🔴 |
| Sprint 3 | Task 3.6 | 历史报告列表 + 报告轮询自动跳转 | 🔴 |
| Sprint 3.5 | Task 3.5.6 | `AdminLayout` + 路由守卫 + 管理仪表盘（统计卡片 + 柱状图） | 🟡 |
| Sprint 3.5 | Task 3.5.7 | 岗位管理页 + 题目管理页（CRUD + 批量导入） | 🟡 |
| Sprint 3.5 | Task 3.5.8 | AI 配置页（掩码 + 连通性测试 + Prompt 编辑器） | 🟡 |
| Sprint 3.5 | Task 3.5.9 | 知识库管理页 + 用户管理页 | 🟡 |
| Sprint 4 | Task 4.3 | 四岗位前端开放 + 岗位列表动态渲染 | 🔴 |
| Sprint 4 | Task 4.4 | Web Speech API 语音输入组件（降级兼容） | 🟡 |
| Sprint 5 | Task 5.5 | 成长曲线页（ECharts 折线图 + 趋势标签） | 🟡 |
| Sprint 5 | Task 5.7 | 报告页推荐资源区块 + 反馈按钮 | 🟡 |

**关键产出**：完整用户侧界面、管理后台所有页面、成长曲线可视化  
**与他人接口约定**：与 A/B/C 在每 Sprint 启动时对齐 Request/Response 字段；SSE 事件格式（与 B 对齐）

---

### 各 Sprint 并行任务分配速查

| Sprint | 成员 A | 成员 B | 成员 C | 成员 D |
|--------|--------|--------|--------|--------|
| **Sprint 0**（第1周） | Task 0.1~0.7 全部基础设施 | — | — | Task 0.8 前端初始化 |
| **Sprint 1**（第2-3周） | — | Task 1.1~1.6 面试后端 | — | Task 1.7~1.8 面试前端 |
| **Sprint 2**（第4周） | — | Task 2.1~2.3 SSE/追问 | — | Task 2.4~2.5 前端SSE改造 |
| **Sprint 3**（第5周） | — | — | Task 3.1~3.4 评估报告后端 | Task 3.5~3.6 报告前端 |
| **Sprint 3.5**（第6周） | Task 3.5.1/2/4/5 管理后台后端 | Task 3.5.3 题目管理 | — | Task 3.5.6~3.5.9 管理后台前端 |
| **Sprint 4**（第6-7周） | Task 4.5 ASR | Task 4.1~4.2 多岗位题库+Prompt | — | Task 4.3~4.4 四岗位前端+语音 |
| **Sprint 5**（第7-8周） | — | — | Task 5.1~5.8 RAG+成长+推荐 | Task 5.5/5.7 成长/推荐前端 |

---

## Sprint 总览

| Sprint | 名称 | 周期 | 任务数 | 交付物（可演示） |
|--------|------|------|--------|------------------|
| Sprint 0 | 基础设施 | 第 1 周 | 8 | 环境一键启动，认证流程可用 |
| Sprint 1 | MVP 面试流程 | 第 2-3 周 | 8 | 单岗位文字面试端到端可跑通 |
| Sprint 2 | AI 对话增强 | 第 4 周 | 5 | 真实 LLM 驱动追问，流式输出 |
| Sprint 3 | 评估报告 | 第 5 周 | 6 | 多维度自动评分，报告可查看 |
| Sprint 3.5 | 管理后台 | 第 6 周 | 9 | 题库/岗位/知识库/AI配置/Prompt 可视化管理 |
| Sprint 4 | 多岗位 + 语音 | 第 6-7 周 | 5 | 四岗位差异化，语音输入可用 |
| Sprint 5 | RAG + 成长 + 推荐 | 第 7-8 周 | 8 | 知识库检索、成长曲线、资源推荐 |

---

## Sprint 0：基础设施（第 1 周）

### 目标
建立可重复的开发环境，实现用户认证流程。所有后续 Sprint 的编码工作都建立在本 Sprint 的地基上。

> **架构决策（一次性，后续不改）**：统一响应格式 `Result<T>`、全局异常处理器、数据库公共字段约定（id/created_at/updated_at/is_deleted）、AI 服务层抽象接口 `LlmService`、支持多 LLM 切换的 `AiServiceFactory`。

---

#### Task 0.1 — Spring Boot 项目脚手架 + Docker 环境 🔴

**涉及文件**
- `backend/pom.xml`（全部依赖，参见 tech-stack.md）
- `backend/src/main/resources/application.yml`（DB/Redis/JWT/AI 配置项用 `${ENV:默认值}` 占位）
- `docker-compose.yml`（MySQL 8.0 + Redis 7 + Chroma 0.5，含健康检查和 volume）
- `.env.example`（环境变量说明）

**实现内容**：Spring Initializr 生成骨架；pom.xml 引入 tech-stack.md 中全部依赖；docker-compose 配置三服务持久化存储。

**完成标志**：`docker-compose up` 启动无报错；`mvn compile` 编译通过。

---

#### Task 0.2 — 数据库建表脚本（DDL）🔴

**涉及文件**
- `sql/init.sql`（仅 DDL 部分，14 张表，含全部索引）

**实现内容**：按 database-design.md 建立全部 14 张表，每表含公共字段，建立 api-design.md 查询涉及的全部索引。

**完成标志**：`mysql < sql/init.sql` 执行成功，`SHOW TABLES;` 显示 14 张表。

---

#### Task 0.3 — 数据库初始化数据（DML）🔴

**涉及文件**
- `sql/init.sql`（在 DDL 之后追加 INSERT 语句）

**实现内容**
- 4 条 `t_position` 记录（JAVA_BACKEND / WEB_FRONTEND / PYTHON_ALGO / GAME_CLIENT）
- 10 条 `t_system_config` 默认记录（LLM 参数 + 3 个 Prompt 占位模板，参见 database-design.md）
- 1 条 admin 用户（`admin` / BCrypt(`admin123456`) / role=`ADMIN`）

**完成标志**：`SELECT * FROM t_position;` 4 行；`SELECT config_key FROM t_system_config;` 10 行；admin 可登录。

---

#### Task 0.4 — MyBatis-Plus 配置 + 全局基础组件 🔴

**涉及文件**
- `com/aiinterview/config/MybatisPlusConfig.java`
- `com/aiinterview/common/result/Result.java`
- `com/aiinterview/common/exception/GlobalExceptionHandler.java`
- `com/aiinterview/common/exception/BusinessException.java`

**实现内容**：MP 开启逻辑删除、驼峰转换、分页插件；`Result<T>` 包含 code/message/data，提供 `ok()`/`fail()` 工厂；`GlobalExceptionHandler` 捕获 BusinessException、参数校验异常、兜底 Exception。

**完成标志**：故意抛 BusinessException，接口返回预期 JSON 格式；参数校验失败返回 400 + 字段错误信息。

---

#### Task 0.5 — Spring Security + JWT 过滤器链 🔴

**涉及文件**
- `com/aiinterview/config/SecurityConfig.java`
- `com/aiinterview/security/JwtUtil.java`
- `com/aiinterview/security/JwtAuthFilter.java`
- `com/aiinterview/security/UserDetailsServiceImpl.java`

**实现内容**：放行 `/api/v1/auth/**`；`/api/v1/admin/**` 仅 ADMIN 角色可访问；JwtUtil 用 jjwt 生成/解析 Token，密钥从配置读取；JwtAuthFilter 从 Bearer Header 提取并注入 SecurityContext。

**完成标志**：无 Token 访问受保护接口返回 401；USER Token 访问 `/api/v1/admin/**` 返回 403；ADMIN Token 通过。

---

#### Task 0.6 — AuthController + AuthService（注册/登录/刷新/登出）🔴

**涉及文件**
- `com/aiinterview/controller/AuthController.java`
- `com/aiinterview/service/AuthService.java` + `impl/AuthServiceImpl.java`
- `com/aiinterview/mapper/UserMapper.java`
- `com/aiinterview/entity/User.java`
- `com/aiinterview/dto/auth/RegisterRequest.java` / `LoginRequest.java` / `AuthResponse.java`

**实现内容**：注册校验用户名唯一，BCrypt 加密写库（role 默认 USER）；登录返回 accessToken（2h）+ refreshToken（7d）+ 用户基本信息；刷新用 refreshToken 换新 accessToken；登出将 accessToken 写入 Redis 黑名单（TTL = 剩余有效期）。

**完成标志**：Postman 注册→登录→刷新→登出全流程通过；重复注册返回 400 错误信息。

---

#### Task 0.7 — UserController + LlmService 接口定义 🔴

**涉及文件**
- `com/aiinterview/controller/UserController.java`
- `com/aiinterview/service/UserService.java` + `impl/`
- `com/aiinterview/dto/user/UserProfileResponse.java` / `UpdateProfileRequest.java`
- `com/aiinterview/service/LlmService.java`（接口，定义 `chat()` 和 `chatStream()` 签名）
- `com/aiinterview/service/AiServiceFactory.java`（接口 + 空 DeepSeek 实现占位）

**实现内容**：`GET /api/v1/users/me` 返回当前用户档案；`PUT /api/v1/users/me` 更新昵称/学校/专业/目标岗位；LlmService 接口有空实现不影响编译。

**完成标志**：登录后 `GET /api/v1/users/me` 返回正确用户信息；`PUT` 更新后再次 GET 数据变更。

---

#### Task 0.8 — 前端项目初始化 + 认证流程 🔴

**涉及文件**
- `frontend/` 项目初始化文件（`package.json` / `vite.config.ts` / `tailwind.config.js`）
- `frontend/src/router/index.ts`（路由 + 路由守卫：未登录跳 `/login`，已登录访问 `/login` 跳 `/`）
- `frontend/src/api/http.ts`（Axios 实例：拦截器 + Token 注入 + 401 自动跳转登录页）
- `frontend/src/stores/auth.ts`（Pinia：token + 用户信息 + localStorage 持久化）
- `frontend/src/views/auth/LoginView.vue` / `RegisterView.vue`
- `frontend/src/views/home/HomeView.vue`（仪表盘骨架：展示昵称 + 面试次数占位 `--`）

**实现内容**：Vite + Vue3 + TS + Element Plus + Tailwind + Pinia 脚手架；登录/注册页调用后端接口；首页显示用户基本信息。

**完成标志**：注册→登录→看到首页→刷新登录态保持；登出后跳转登录页。

### Sprint 0 验收标准
- [ ] `docker-compose up` 一键启动开发环境
- [ ] 注册 → 登录 → 刷新 Token → 登出 全流程可用
- [ ] 前端登录后能看到用户信息，刷新不丢失登录态
- [ ] `LlmService` 接口已定义，项目可正常编译
- [ ] admin 账号调用 `/api/v1/admin/stats` 返回 200；普通用户返回 403

---

## Sprint 1：MVP 面试流程（第 2-3 周）

### 目标
**交付第一个可演示的端到端面试流程**：针对 Java 后端单一岗位，用户可以开始一场文字面试、回答问题、结束面试。AI 本 Sprint 采用最简 Prompt（顺序出题，不追问），重点是打通全链路。

> **扩展性设计**：`InterviewService.processUserMessage()` 设计为可插拔策略（`SimpleStrategy` → `FollowUpStrategy`），面试间组件消息添加抽出为 `appendMessage()` 工具函数，为 Sprint 2 SSE 改造留接入点。

---

#### Task 1.1 — Java 后端题库 SQL 数据 🔴

**涉及文件**
- `sql/data_java_backend.sql`（30 条 INSERT INTO t_question）

**实现内容**：覆盖全部 4 种题型（TECH_KNOWLEDGE / PROJECT_DEEP / SCENARIO / BEHAVIOR），难度分布简单 30% / 中等 50% / 困难 20%，每题含参考答案（`answer_reference`）。

**完成标志**：执行后 `SELECT COUNT(*) FROM t_question WHERE position_code='JAVA_BACKEND';` = 30。

---

#### Task 1.2 — QuestionController + QuestionService + QuestionMapper 🔴

**涉及文件**
- `com/aiinterview/controller/QuestionController.java`
- `com/aiinterview/service/QuestionService.java` + `impl/`
- `com/aiinterview/mapper/QuestionMapper.java`
- `com/aiinterview/entity/Question.java`
- `com/aiinterview/dto/question/QuestionResponse.java`

**实现内容**：`GET /api/v1/questions?positionCode=JAVA_BACKEND&type=&difficulty=` 分页返回题目列表（不返回 answer_reference 字段，防止前端泄露答案）。

**完成标志**：按岗位查询返回正确题目列表；按 type/difficulty 过滤结果正确。

---

#### Task 1.3 — DeepSeekLlmService 实现 + interview_system Prompt 🔴

**涉及文件**
- `com/aiinterview/service/impl/DeepSeekLlmService.java`
- `com/aiinterview/service/AiServiceFactory.java`（注册 DeepSeek 实现）
- `src/main/resources/prompts/interview_system.txt`（最简版 System Prompt）

**实现内容**：用 OkHttp 调用 DeepSeek Chat API（`/v1/chat/completions`）；API Key / baseUrl 从 `SystemConfigService` 读取（为后续热更新铺路）；同步 `chat()` 方法拼接 messages 数组，返回 content 字符串；Prompt 模板：扮演面试官、按顺序提问、语气专业简洁。

**完成标志**：单测传入一条用户消息，能收到 LLM 非空回复；API Key 错误时抛出 BusinessException 提示友好信息。

---

#### Task 1.4 — 题目抽取逻辑 + 面试会话创建 🔴

**涉及文件**
- `com/aiinterview/service/impl/InterviewServiceImpl.java`（`startInterview()` 方法）
- `com/aiinterview/entity/InterviewSession.java`
- `com/aiinterview/entity/InterviewQuestion.java`
- `com/aiinterview/mapper/InterviewSessionMapper.java`
- `com/aiinterview/mapper/InterviewQuestionMapper.java`

**实现内容**：按岗位从题库中按难度比例随机抽取题目（简单3题/中等5题/困难2题，共10题）；创建 `t_interview_session`（status=IN_PROGRESS）；创建 `t_interview_question` 记录（按序号排列）。

**完成标志**：调用 `startInterview("JAVA_BACKEND", userId)` 后数据库有 1 条 session 记录和 10 条 question 记录。

---

#### Task 1.5 — InterviewController.start 🔴

**涉及文件**
- `com/aiinterview/controller/InterviewController.java`（`POST /api/v1/interviews/start`）
- `com/aiinterview/dto/interview/StartInterviewRequest.java`
- `com/aiinterview/dto/interview/StartInterviewResponse.java`

**实现内容**：调用 Task 1.4 的 `startInterview()`；调用 `LlmService.chat()` 生成口语化开场白 + 第一题提问（System Prompt + 题目作为上下文）；将 AI 开场消息保存到 `t_chat_message`；返回 `{ sessionId, firstMessage }`。

**完成标志**：Postman 调用后返回 sessionId 和 AI 开场消息，数据库有对应记录。

---

#### Task 1.6 — InterviewController.message / end / getMessages 🔴

**涉及文件**
- `com/aiinterview/controller/InterviewController.java`（另外 3 个接口）
- `com/aiinterview/service/strategy/SimpleStrategy.java`
- `com/aiinterview/service/strategy/InterviewStrategy.java`（接口）
- `com/aiinterview/entity/ChatMessage.java`
- `com/aiinterview/mapper/ChatMessageMapper.java`

**实现内容**
- `POST /api/v1/interviews/{id}/message`：保存用户消息到 `t_chat_message`；`SimpleStrategy` 判断是否所有题目已完成（若未完成则构造"下一题提问"Prompt 调 LLM）；保存 AI 回复；返回 JSON 响应
- `POST /api/v1/interviews/{id}/end`：更新 session status=COMPLETED，记录 `duration_seconds`
- `GET /api/v1/interviews/{id}/messages`：返回完整对话历史列表

**完成标志**：能进行 5 轮对话且数据库 t_chat_message 有正确记录；结束后 session status 变为 COMPLETED。

---

#### Task 1.7 — 前端岗位选择页 + InterviewRoom.vue 🔴

**涉及文件**
- `frontend/src/views/position/PositionSelectView.vue`
- `frontend/src/views/interview/InterviewRoom.vue`
- `frontend/src/api/interview.ts`（封装面试相关接口调用）
- `frontend/src/composables/useInterview.ts`（状态管理：消息列表、题目进度、`appendMessage()`）

**实现内容**
- 岗位选择页：4 张岗位卡片（Java后端可进入，其余显示"即将开放"）；点击调用 `start` 接口，跳转面试间
- 面试间：对话气泡列表（AI/用户区分样式）；文字输入框 + 发送（Enter）；题目进度（第 X / 共 10 题）；结束面试按钮（二次确认）

**完成标志**：选 Java 后端 → 看到 AI 开场消息 → 输入回答 → 收到 AI 下一题 → 点击结束。

---

#### Task 1.8 — 前端面试结束页 + 端到端联调 🔴

**涉及文件**
- `frontend/src/views/interview/InterviewEndView.vue`（"报告生成中"占位页）
- 联调：解决跨域、Token 过期、接口字段不匹配等问题

**实现内容**：面试结束后跳转此页，展示"正在生成评估报告…"动画占位；预留轮询逻辑（Sprint 3 激活）。

**完成标志**：完整走通"选岗位→开始面试→回答 5+ 题→结束→看到结束占位页"，对话记录在数据库正确保存。

### Sprint 1 验收标准
- [ ] 选 Java 后端 → 开始面试 → 回答 5+ 题 → 结束面试 全流程无报错
- [ ] 对话记录正确保存到数据库
- [ ] 面试结束后能看到"报告生成中"占位页

---

## Sprint 2：AI 对话增强（第 4 周）

### 目标
在 Sprint 1 能跑通的基础上，实现**动态追问**和 **SSE 流式输出**（打字机效果），让面试体验更贴近真实。

---

#### Task 2.1 — LlmService.chatStream() 流式实现 🔴

**涉及文件**
- `com/aiinterview/service/impl/DeepSeekLlmService.java`（新增 `chatStream()` 方法）
- `com/aiinterview/service/LlmService.java`（接口补充 `chatStream()` 签名）

**实现内容**：用 OkHttp 发起 SSE 请求到 DeepSeek API（`stream: true`）；逐行解析 `data:` 字段提取 token；通过回调接口 `StreamHandler.onToken(String)` / `onDone()` / `onError(Throwable)` 通知调用方。

**完成标志**：单测调用 `chatStream()`，控制台可见逐字打印的流式输出。

---

#### Task 2.2 — FollowUpStrategy + 追问 Prompt 🔴

**涉及文件**
- `com/aiinterview/service/strategy/FollowUpStrategy.java`
- `src/main/resources/prompts/follow_up_question.txt`

**实现内容**：Prompt 要求 LLM 输出结构化 JSON `{ "action": "follow_up|next_question|end", "content": "..." }`；追问规则：回答不完整/不准确时追问，每题最多追问 2 次后强制推进；`FollowUpStrategy` 解析 JSON 决定下一步动作。

**完成标志**：传入"不知道"作为用户回答，`action` 为 `follow_up`；传入详细回答，`action` 为 `next_question`。

---

#### Task 2.3 — InterviewController.message 改造为 SSE + 状态机防护 🔴

**涉及文件**
- `com/aiinterview/controller/InterviewController.java`（`message` 接口返回类型改为 `SseEmitter`）
- `com/aiinterview/service/impl/InterviewServiceImpl.java`（替换 SimpleStrategy → FollowUpStrategy，集成 chatStream）

**实现内容**：接口返回 `SseEmitter`；调用 `chatStream()` 逐 token 通过 `SseEmitter.send()` 推送事件类型 `token`；流结束时发送 `done` / `next_question` / `interview_end` 事件；已结束的会话拒绝继续接收消息（返回 400）。

**完成标志**：curl 调用接口，可见逐行输出的 `data:` 流；发送完最后一题后出现 `interview_end` 事件。

---

#### Task 2.4 — 前端 SSE 接收改造（打字机效果）🔴

**涉及文件**
- `frontend/src/views/interview/InterviewRoom.vue`（SSE 接收逻辑替换原 Axios 调用）
- `frontend/src/composables/useInterview.ts`（新增 `connectSse()` / `disconnectSse()`）

**实现内容**：使用 `fetch` + `ReadableStream` 接收 SSE（兼容需要携带 Authorization Header 的场景）；逐 token 追加到当前 AI 气泡（打字机效果）；收到 `next_question` 更新题目进度；收到 `interview_end` 自动跳转结束页。

**完成标志**：AI 回复有打字机逐字效果；题目进度实时更新；面试自动结束跳转。

---

#### Task 2.5 — 前端断线重连 + 联调 🔴

**涉及文件**
- `frontend/src/composables/useInterview.ts`（断线检测 + 自动重连逻辑）
- `frontend/src/components/interview/ConnectionStatus.vue`（网络状态指示组件）

**实现内容**：SSE 出错时显示友好提示横幅（"连接已断开，正在重连…"）；自动尝试重连最多 3 次（指数退避）；超出重连次数显示"手动重连"按钮。

**完成标志**：模拟网络断开后能自动重连并继续面试；可演示追问场景（回答"不太清楚"触发追问）。

### Sprint 2 验收标准
- [ ] AI 回复有打字机流式效果
- [ ] AI 能对不完整的回答发出追问（可演示 1-2 次追问场景）
- [ ] 全部题目答完后系统自动结束面试

---

## Sprint 3：评估报告（第 5-6 周）

### 目标
**本 Sprint 是赛题评分的核心交付**：实现多维度自动评分 + 结构化报告生成，前端展示雷达图和完整报告详情。

---

#### Task 3.1 — 评估 Prompt 模板设计 🔴

**涉及文件**
- `src/main/resources/prompts/evaluation_question.txt`（逐题评分 Prompt）
- `src/main/resources/prompts/evaluation_final.txt`（综合报告 Prompt）

**实现内容**
- `evaluation_question.txt`：输入变量 `{question}` / `{reference_answer}` / `{user_answer}`；要求 LLM 输出 JSON `{ "tech_score": 0-100, "logic_score": 0-100, "depth_score": 0-100, "comment": "..." }`
- `evaluation_final.txt`：输入所有题目分数汇总；要求输出 JSON `{ "overall_score", "expression_score", "confidence_score", "summary", "highlights": [], "weaknesses": [], "suggestions": [] }`

**完成标志**：用 curl 手动调 DeepSeek API 带入模板内容，能得到符合 JSON Schema 的回复。

---

#### Task 3.2 — AiEvaluationService 逐题评分 🔴

**涉及文件**
- `com/aiinterview/service/AiEvaluationService.java` + `impl/`
- `com/aiinterview/entity/DimensionScore.java`
- `com/aiinterview/mapper/DimensionScoreMapper.java`
- `com/aiinterview/entity/EvaluationReport.java`
- `com/aiinterview/mapper/EvaluationReportMapper.java`

**实现内容**：`@Async` 方法 `evaluate(sessionId)`；先创建 `t_evaluation_report`（status=GENERATING）；遍历该 session 所有题目的用户回答，逐题调 LLM 评分；解析 JSON 写入 `t_dimension_score`；捕获异常将 report status 置为 FAILED。

**完成标志**：面试结束后触发评估，10 秒内 `t_dimension_score` 有 10 条记录，`t_evaluation_report` status 为 GENERATING。

---

#### Task 3.3 — AiEvaluationService 综合报告生成 🔴

**涉及文件**
- `com/aiinterview/service/impl/AiEvaluationServiceImpl.java`（续 Task 3.2，补充综合报告逻辑）
- `com/aiinterview/entity/GrowthRecord.java`
- `com/aiinterview/mapper/GrowthRecordMapper.java`

**实现内容**：逐题评分完成后，汇总各题分数调 LLM 生成综合报告；解析 JSON 更新 `t_evaluation_report`（overall_score/expression_score/confidence_score/summary/highlights/weaknesses/suggestions，status=COMPLETED）；同步写入 `t_growth_record`（记录本次面试各维度得分 + 时间戳）。

**完成标志**：60 秒内 `t_evaluation_report` status=COMPLETED，`t_growth_record` 有 1 条新记录。

---

#### Task 3.4 — ReportController 🔴

**涉及文件**
- `com/aiinterview/controller/ReportController.java`
- `com/aiinterview/dto/report/ReportDetailResponse.java`（含 dimensionScores 列表）
- `com/aiinterview/dto/report/ReportListItemResponse.java`

**实现内容**
- `GET /api/v1/reports/{reportId}`：返回完整报告（含 5 维度分数 + 逐题点评）；若 status=GENERATING 返回 `{ "status": "GENERATING" }` 供前端轮询
- `GET /api/v1/reports?page=&size=`：当前用户历史报告列表（按时间倒序，含得分/岗位/时长）

**完成标志**：Postman 查询报告详情返回正确 JSON；status=GENERATING 时返回占位响应。

---

#### Task 3.5 — 前端报告详情页 ReportDetail.vue 🔴

**涉及文件**
- `frontend/src/views/report/ReportDetailView.vue`
- `frontend/src/components/report/RadarChart.vue`（ECharts RadarChart 封装）
- `frontend/src/components/report/DimensionScoreList.vue`（逐题点评列表）
- `frontend/src/api/report.ts`

**实现内容**：综合得分大字 + 等级标签（优秀≥85/良好≥70/待提升）；5 维度雷达图；综合总结（Markdown 渲染，`marked` 库）；亮点/不足/建议三栏；逐题点评可折叠（默认展开前 3 题）。

**完成标志**：页面正确展示雷达图和全部报告内容；Markdown 总结正确渲染加粗/列表格式。

---

#### Task 3.6 — 前端历史报告列表 + 轮询逻辑 🔴

**涉及文件**
- `frontend/src/views/report/ReportListView.vue`
- `frontend/src/views/interview/InterviewEndView.vue`（激活 Task 1.8 预留的轮询逻辑）

**实现内容**：历史报告列表按时间倒序，展示综合得分/岗位/时长/状态徽章；面试结束页每 3 秒轮询报告状态，COMPLETED 后自动跳转 ReportDetail 页。

**完成标志**：面试结束后轮询自动跳转报告详情；历史列表可查看多次面试记录。

### Sprint 3 验收标准
- [ ] 面试结束后 60 秒内报告自动生成并展示
- [ ] 报告包含：综合得分、5 维度雷达图、总结、亮点/不足/建议
- [ ] 逐题点评内容与用户实际回答相关
- [ ] 历史报告列表可访问

---

## Sprint 3.5：管理后台（第 6 周）

### 目标
交付可用的管理后台，让管理员通过界面管理题库、知识库和 AI 配置，不阻塞面试主流程开发。

> Sprint 0 已预留好 Security 规则和 `t_system_config` 数据基础，本 Sprint 只添加 Controller + 前端页面，**不涉及任何破坏性改动**。

---

#### Task 3.5.1 — SystemConfigService（热更新配置服务）🟡

**涉及文件**
- `com/aiinterview/service/SystemConfigService.java` + `impl/`
- `com/aiinterview/mapper/SystemConfigMapper.java`
- `com/aiinterview/entity/SystemConfig.java`

**实现内容**：应用启动时 `@PostConstruct` 从 `t_system_config` 预热全部配置到 ConcurrentHashMap；提供 `get(key)` / `set(key, value)` 方法；`set()` 同时写库 + 更新内存缓存（热更新，无需重启）；`DeepSeekLlmService` 改为从 `SystemConfigService.get("ai.llm.api_key")` 读取参数。

**完成标志**：通过 SystemConfigService 修改 temperature 后，立即下一次 LLM 调用使用新值（无需重启服务）。

---

#### Task 3.5.2 — AdminStatsController + AdminPositionController 🟡

**涉及文件**
- `com/aiinterview/controller/admin/AdminStatsController.java`
- `com/aiinterview/controller/admin/AdminPositionController.java`
- `com/aiinterview/dto/admin/StatsResponse.java`

**实现内容**
- `GET /api/v1/admin/stats`：返回总用户数、今日面试数、各岗位面试数量、题库总量
- 岗位 CRUD：`GET /POST /PUT /{id} /DELETE /{id} /PATCH /{id}/status`（启用/停用）

**完成标志**：Postman 用 admin Token 调用统计接口返回正确数据；停用岗位后前台岗位选择页不显示该岗位。

---

#### Task 3.5.3 — AdminQuestionController（题目 CRUD + 批量导入）🟡

**涉及文件**
- `com/aiinterview/controller/admin/AdminQuestionController.java`
- `com/aiinterview/service/admin/AdminQuestionService.java` + `impl/`
- `com/aiinterview/dto/admin/QuestionImportItem.java`

**实现内容**：题目 CRUD（含完整字段包括 answer_reference）；`POST /api/v1/admin/questions/batch-import`：接收 JSON 数组批量插入，逐条校验 positionCode/questionType，返回成功数和失败行列表。

**完成标志**：通过接口新增题目后，新面试立即可能抽到该题；批量导入 5 条有 1 条格式错误时，正确返回 4 成功 1 失败。

---

#### Task 3.5.4 — AdminKnowledgeController + AdminUserController 🟡

**涉及文件**
- `com/aiinterview/controller/admin/AdminKnowledgeController.java`
- `com/aiinterview/controller/admin/AdminUserController.java`
- `com/aiinterview/entity/KnowledgeDoc.java`
- `com/aiinterview/mapper/KnowledgeDocMapper.java`

**实现内容**
- 知识库：CRUD + `POST /api/v1/admin/knowledge/{id}/vectorize`（触发向量化，更新 is_vectorized 状态）；向量化实际调用留 TODO（Sprint 5 实现）
- 用户：分页列表（支持按用户名搜索）+ `PATCH /api/v1/admin/users/{id}/role`（修改用户角色）

**完成标志**：知识库文档列表正常分页；修改用户角色后该用户实际权限立即生效。

---

#### Task 3.5.5 — AdminAiConfigController（AI 配置 + Prompt 管理）🟡

**涉及文件**
- `com/aiinterview/controller/admin/AdminAiConfigController.java`
- `com/aiinterview/service/admin/AdminAiConfigService.java` + `impl/`

**实现内容**
- `GET /api/v1/admin/ai-config`：返回所有配置，`is_sensitive=true` 的值替换为 `"***masked***"`
- `PUT /api/v1/admin/ai-config`：批量更新配置项（写库 + 刷新 SystemConfigService 缓存）
- `GET /api/v1/admin/ai-config/test`：用当前配置调一次 LLM，返回 `{ "success": true, "latency_ms": 320 }`
- `GET/PUT /api/v1/admin/prompts/{key}`：读取/覆盖 Prompt 模板
- `POST /api/v1/admin/prompts/{key}/preview`：用请求体中的变量值填充模板，返回渲染结果（不调用 LLM）

**完成标志**：API Key 在 GET 中显示掩码；PUT 更新 temperature 后连通性测试用新值；Prompt 预览返回填充后的文本。

---

#### Task 3.5.6 — 前端 AdminLayout + 路由守卫 + 仪表盘页 🟡

**涉及文件**
- `frontend/src/layouts/AdminLayout.vue`（侧边栏：仪表盘/岗位/题库/知识库/AI配置/用户）
- `frontend/src/router/index.ts`（新增 `/admin/**` 路由组，守卫检测 role==ADMIN）
- `frontend/src/views/admin/AdminDashboardView.vue`
- `frontend/src/api/admin.ts`（封装全部 admin 接口）

**实现内容**：路由守卫：非 ADMIN 访问 `/admin/**` 跳转首页并 ElMessage 提示"无权限"；仪表盘页展示 4 个统计卡片 + 各岗位面试数量柱状图（ECharts）。

**完成标志**：普通用户访问 `/admin` 被拦截；admin 用户正常进入仪表盘并看到统计数据。

---

#### Task 3.5.7 — 前端岗位管理页 + 题目管理页 🟡

**涉及文件**
- `frontend/src/views/admin/PositionManageView.vue`
- `frontend/src/views/admin/QuestionManageView.vue`
- `frontend/src/components/admin/QuestionFormDialog.vue`（新增/编辑弹窗）

**实现内容**
- 岗位管理：表格列表 + 启用/停用开关 + 编辑弹窗
- 题目管理：分页表格（含岗位/难度/题型筛选）+ 新增/编辑弹窗（含全部字段）+ 删除确认 + 批量导入（点击下载模板 JSON → 选择文件上传）

**完成标志**：通过界面新增题目后，马上在表格看到；批量导入 JSON 文件后，数量正确增加。

---

#### Task 3.5.8 — 前端 AI 配置页（掩码 + 连通性测试 + Prompt 编辑器）🟡

**涉及文件**
- `frontend/src/views/admin/AiConfigView.vue`
- `frontend/src/components/admin/PromptEditor.vue`（带变量说明的 Textarea 编辑器）

**实现内容**：LLM 参数分组展示（api_key 显示 `***`，点击"修改"切换为可编辑 Input）；"测试连通性"按钮点击后显示 Loading → 展示延迟或错误信息；Prompt 编辑器：切换 Prompt key 显示对应模板文本 + 变量说明 + 预览按钮（填充示例变量后展示渲染结果）。

**完成标志**：修改 temperature 并保存后，测试连通性看到响应；Prompt 预览功能返回正确渲染结果。

---

#### Task 3.5.9 — 前端知识库管理页 + 用户管理页 🟡

**涉及文件**
- `frontend/src/views/admin/KnowledgeManageView.vue`
- `frontend/src/views/admin/UserManageView.vue`

**实现内容**：知识库管理：文章列表 + 新增/编辑（含长文本内容编辑）+ 向量化状态徽章（已向量化/未向量化）+ "触发向量化"按钮；用户管理：分页列表 + 用户名搜索 + 角色修改（下拉选择 USER/ADMIN + 确认弹窗）。

**完成标志**：知识库文档手动触发向量化后状态变为"已向量化"；角色修改后数据库字段正确更新。

### Sprint 3.5 验收标准
- [ ] 管理员登录后可访问 `/admin`，普通用户被拒绝
- [ ] 通过界面新增/修改题目后，新面试中立即使用新题目
- [ ] AI 配置页修改 temperature 后，下一次 LLM 调用使用新值（不重启）
- [ ] Prompt 预览填入变量后能看到渲染结果
- [ ] 知识库文档手动向量化后，状态变为"已向量化"

---

## Sprint 4：多岗位 + 语音输入（第 6-7 周）

### 目标
在 Sprint 1-3 的 Java 后端功能基础上，水平扩展到 Web 前端、Python 算法和游戏客户端开发三个岗位，并增加语音输入支持。**架构无需改动，只需补充数据和配置**。

> 新岗位接入 checklist：① 录入 `t_question` 数据 ② 定制 Prompt 模板 ③ 前端岗位卡片开放。无需改后端代码逻辑。

---

#### Task 4.1 — Web 前端题库 SQL 数据 🔴

**涉及文件**
- `sql/data_web_frontend.sql`（30 条 INSERT INTO t_question）

**实现内容**：覆盖 Vue/React/工程化/性能优化/CSS/浏览器渲染等考点，覆盖 4 种题型，难度分布同 Java 后端。

**完成标志**：`SELECT COUNT(*) FROM t_question WHERE position_code='WEB_FRONTEND';` = 30。

---

#### Task 4.2 — Python 算法 + 游戏客户端题库 SQL + 各岗位定制 Prompt 🔴

**涉及文件**
- `sql/data_python_algo.sql`（20 条 INSERT INTO t_question）
- `sql/data_game_client.sql`（30 条 INSERT INTO t_question）
- `src/main/resources/prompts/interview_system_web.txt`（前端岗位面试官 Prompt）
- `src/main/resources/prompts/interview_system_python.txt`（算法岗位面试官 Prompt）
- `src/main/resources/prompts/interview_system_game.txt`（游戏客户端岗位面试官 Prompt）

**实现内容**：Python 算法题覆盖数据结构/算法/机器学习基础；游戏客户端题覆盖 Unity/Unreal、渲染管线、内存管理、网络同步、帧同步、ECS 架构等；各岗位 Prompt 侧重不同考察方向；`InterviewService.startInterview()` 按 positionCode 选择对应 Prompt 文件。

**完成标志**：`SELECT COUNT(*) FROM t_question WHERE position_code='GAME_CLIENT';` = 30；四个岗位各发起一次面试，Prompt 和题目内容均差异明显。

---

#### Task 4.3 — 前端岗位选择页开放四个岗位 + 联调 🔴

**涉及文件**
- `frontend/src/views/position/PositionSelectView.vue`（移除"即将开放"限制）
- `frontend/src/api/position.ts`（从接口动态获取岗位列表）

**实现内容**：岗位卡片从后端 `GET /api/v1/positions` 动态渲染（is_active=true 的才显示）；四个岗位均可发起面试；各自完成完整面试流程并生成报告。

**完成标志**：Web 前端、Python 算法、游戏客户端岗位各完成 1 次完整面试并生成报告；题目内容明显不同。

---

#### Task 4.4 — 前端 Web Speech API 语音输入 🟡

**涉及文件**
- `frontend/src/components/interview/VoiceInput.vue`（麦克风按钮组件）
- `frontend/src/composables/useSpeechRecognition.ts`（封装 Web Speech API）

**实现内容**：检测浏览器兼容性（不支持时显示文字提示，降级为纯文字输入）；按住录音/点击切换模式；`SpeechRecognition` 识别结果实时回填输入框（支持中文）；录音状态动画（脉冲波形）。

**完成标志**：Chrome/Edge 下中文语音能正确识别并填入输入框；不支持的浏览器显示友好降级提示。

---

#### Task 4.5 — 后端 AsrController（讯飞 ASR 备用方案）🟢

**涉及文件**
- `com/aiinterview/controller/AsrController.java`
- `com/aiinterview/service/AsrService.java` + `impl/XunfeiAsrService.java`

**实现内容**：`POST /api/v1/asr/convert`，接收 `multipart/form-data` 音频文件（WebM/WAV）；通过讯飞 WebSocket API 转写为文字并返回；API 配置从 `t_system_config` 读取。

**完成标志**：上传一段音频文件，接口返回正确识别的文字（备用路径，Web Speech API 可用时前端不调此接口）。

### Sprint 4 验收标准
- [ ] 四个岗位均可独立发起面试，题目内容差异明显
- [ ] Web 前端、Python 算法、游戏客户端岗位各完成 1 次完整面试并生成报告
- [ ] 语音输入可识别中文并填入对话框（Chrome/Edge 浏览器下）

---

## Sprint 5：RAG + 成长曲线 + 资源推荐（第 7-8 周）

### 目标
集成知识库检索增强评估质量（RAG），实现能力成长可视化，并完成学习资源推荐闭环，收尾演示准备。

---

#### Task 5.1 — 知识库文档内容录入 🟡

**涉及文件**
- `sql/data_knowledge_docs.sql`（每岗位 10+ 篇，INSERT INTO t_knowledge_doc）

**实现内容**：内容涵盖核心技术考点精讲 + 优秀回答范例；`doc_type` 区分 KNOWLEDGE（知识点）/ ANSWER_EXAMPLE（范例）；`topic` 标签与题目 topic 对应，便于后续 RAG 检索关联。

**完成标志**：四个岗位各有 10+ 条知识库文档，is_vectorized=false。

---

#### Task 5.2 — RagService 向量化（Embedding → Chroma）🟡

**涉及文件**
- `com/aiinterview/service/RagService.java` + `impl/RagServiceImpl.java`
- `com/aiinterview/config/ChromaConfig.java`（Chroma Client 配置）

**实现内容**：调用 DeepSeek Embedding API 将文档 content 转为向量；写入 Chroma（每个岗位一个 Collection，如 `java_backend_knowledge`）；存储时包含 metadata（docId/topic/positionCode）；写入成功后更新 `t_knowledge_doc.is_vectorized=true` 并记录 `chroma_ids`；`AdminKnowledgeController` 的向量化触发接口调用此方法。

**完成标志**：触发向量化后 is_vectorized=true；Chroma Dashboard 可见对应 Collection 有数据。

---

#### Task 5.3 — RagService 检索 + AiEvaluationService 集成 RAG 🟡

**涉及文件**
- `com/aiinterview/service/impl/RagServiceImpl.java`（新增 `search()` 方法）
- `com/aiinterview/service/impl/AiEvaluationServiceImpl.java`（评分时注入 RAG 结果）

**实现内容**：`search(userAnswer, positionCode, topK=5)`：将用户回答 Embedding 后在对应 Collection 相似度检索；返回 Top-5 知识片段；`AiEvaluationService` 逐题评分时，先调用 `RagService.search()`，将检索到的知识片段追加到 `evaluation_question.txt` Prompt 的参考内容部分。

**完成标志**：评分 Prompt 中可见"相关知识参考"片段；与未集成 RAG 时的点评对比，内容明显更具体。

---

#### Task 5.4 — GrowthController 🟡

**涉及文件**
- `com/aiinterview/controller/GrowthController.java`
- `com/aiinterview/dto/growth/GrowthDataResponse.java`（含按岗位分组的时间序列数据）

**实现内容**：`GET /api/v1/growth?positionCode=`（可选过滤）：查询当前用户 `t_growth_record`，按时间排序返回历次面试各维度得分列表；同时计算趋势（最近 3 次均值对比前 3 次：上升/下降/平稳）。

**完成标志**：完成 2+ 次面试后，接口返回含时间序列的成长数据，趋势字段正确。

---

#### Task 5.5 — 前端成长曲线页 GrowthChart.vue 🟡

**涉及文件**
- `frontend/src/views/growth/GrowthView.vue`
- `frontend/src/components/growth/GrowthLineChart.vue`（ECharts LineChart 封装）

**实现内容**：折线图展示历次面试 5 维度得分（每条线一种颜色）；岗位筛选 Tab；横轴为面试日期，纵轴 0-100；图表顶部显示趋势标签（↑上升 / ↓下降 / → 平稳）；首页仪表盘卡片展示"最近面试综合得分"激活（替换 `--` 占位）。

**完成标志**：有 3+ 次历史面试时折线图正确渲染；切换岗位筛选数据正确更新。

---

#### Task 5.6 — 学习资源 SQL + ResourceService 推荐逻辑 🟡

**涉及文件**
- `sql/data_resources.sql`（每岗位 15 条，INSERT INTO t_learning_resource）
- `com/aiinterview/service/ResourceService.java` + `impl/`
- `com/aiinterview/mapper/LearningResourceMapper.java`
- `com/aiinterview/entity/LearningResource.java`

**实现内容**：资源类型覆盖 ARTICLE/QUESTION/VIDEO；`generateRecommendations(reportId)`：读取报告弱项维度（分数 < 70 的维度）→ 按 topic 匹配资源 → 取 Top-5；写入 `t_user_recommendation`。

**完成标志**：存在弱项维度的报告调用后，`t_user_recommendation` 有 5 条推荐记录。

---

#### Task 5.7 — 推荐资源嵌入报告页 + 用户反馈接口 🟡

**涉及文件**
- `frontend/src/views/report/ReportDetailView.vue`（底部追加推荐资源区块）
- `frontend/src/components/report/ResourceRecommendations.vue`
- `com/aiinterview/controller/ResourceController.java`（`GET /api/v1/resources/recommendations/{reportId}` + `POST /feedback`）

**实现内容**：报告详情页底部展示 5 条推荐资源（标题/类型/点击打开链接）；每条资源有"有帮助 👍 / 没帮助 👎"反馈按钮（调接口写 `t_user_recommendation.is_helpful`）。

**完成标志**：报告底部出现推荐资源列表；点击反馈按钮不报错，数据库字段正确更新。

---

#### Task 5.8 — 演示数据预置 + 演示脚本验证 🔴

**涉及文件**
- `sql/data_demo.sql`（demo 账号 + 3 次历史面试数据 + 成长记录）
- `docs/demo-script.md`（5 分钟演示脚本详细步骤）

**实现内容**
- 预置 `demo_student` 账号（Java 后端，已有 3 次面试，综合得分 62/75/83，成长曲线有上升趋势）
- 确保演示第一题为经典 JVM 题（调整题目 order 或 SQL 种子）
- 演示脚本涵盖：注册/登录 → Java后端面试（展示追问+打字机）→ 报告（雷达图+点评）→ 成长曲线 → 推荐资源 → 游戏客户端岗位面试差异
- 全流程实际演练至少 2 遍，确保无卡顿

**完成标志**：5 分钟演示脚本演练通过；demo_student 成长曲线有 3 个数据点；演示视频录制完成。

### Sprint 5 验收标准
- [ ] 成长曲线折线图展示至少 3 次历史面试数据
- [ ] 报告底部出现 3+ 条推荐资源
- [ ] RAG 集成后评分点评内容更具体（可对比未集成时的效果）
- [ ] 5 分钟演示脚本演练通过，无卡顿

---

## 优先级矩阵（工期压缩时的取舍顺序）

| 优先级 | 功能 | 对应 Sprint |
|--------|------|-------------|
| P0 必须 | 认证 + 单岗位文字面试 + 评估报告 | Sprint 0-3 |
| P0 必须 | 四岗位差异化（至少各 20 题）| Sprint 4 |
| P1 重要 | SSE 流式输出 + AI 追问 | Sprint 2 |
| P1 重要 | 管理后台（题库/岗位/AI配置/Prompt）| Sprint 3.5 |
| P1 重要 | 语音输入（Web Speech API）| Sprint 4 |
| P1 重要 | 成长曲线可视化 | Sprint 5 |
| P2 加分 | RAG 知识检索 | Sprint 5 |
| P2 加分 | 学习资源推荐 | Sprint 5 |
| P3 锦上添花 | 报告分享链接 | Sprint 5+ |
| P3 锦上添花 | 移动端响应式 | Sprint 5+ |

---

## 关键里程碑

| 里程碑 | Sprint 末 | 交付物 |
|--------|-----------|--------|
| M1 - 环境就绪 | Sprint 0 末 | 认证流程可用，数据库就绪，管理员账号可登录 |
| M2 - MVP 可演示 | Sprint 1 末 | Java 后端单岗位文字面试端到端跑通 |
| M3 - AI 流式对话 | Sprint 2 末 | 追问 + 打字机效果可演示 |
| M4 - 报告核心交付 | Sprint 3 末 | 多维度评估报告完整可用 |
| M5 - 管理后台可用 | Sprint 3.5 末 | 题库/AI配置/Prompt 可通过界面管理 |
| M6 - 四岗位完整 | Sprint 4 末 | 四岗位均可面试，语音可用 |
| M7 - 演示就绪 | Sprint 5 末 | 全功能可用，演示视频录制完成 |
