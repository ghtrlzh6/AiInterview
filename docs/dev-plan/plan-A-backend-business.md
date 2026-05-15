# 开发计划 — 后端业务开发（人员 A）

## 角色定位

**职责范围**：基础设施搭建 + 认证与安全 + 面试核心业务逻辑 + 报告查询接口 + 管理后台 Controller 层 + 成长/资源接口

你是团队的**后端基础层负责人**。Sprint 0 的工作量最集中，后续 Sprint 你的工作是"骨架"——业务接口、数据库操作、Controller 层，而不涉及 AI 具体实现（由人员 B 负责）。

---

## 与其他成员的依赖关系

| 依赖方向 | 内容 | 时间节点 |
|----------|------|----------|
| **A → B**（你提供） | Task 0.7 完成后，`LlmService` 接口定义确定 | Sprint 0 末 |
| **B → A**（你等待） | Task 1.3 完成后，`LlmService.chat()` 有真实实现，Task 1.5 才能用真 LLM | Sprint 1 中期 |
| **B → A**（你等待） | Task 2.1/2.2 完成后，`chatStream()` + `FollowUpStrategy` 可用，你才能做 Task 2.3 | Sprint 2 |
| **B → A**（你等待） | Task 3.2/3.3 完成后，评估报告有真实数据，Task 3.4 才有内容返回 | Sprint 3 |
| **A → C**（你提供） | Sprint 0 末 Auth API 可用，前端才能完成 Task 0.8 登录联调 | Sprint 0 末 |
| **A → C**（你提供） | Task 1.5/1.6 完成后，面试接口可用，前端才能联调 Task 1.7/1.8 | Sprint 1 |
| **A → C**（你提供） | Task 3.4 完成后，报告接口可用，前端才能联调 Task 3.5/3.6 | Sprint 3 |
| **A → C**（你提供） | Task 3.5.x 完成后，Admin API 可用，前端才能联调管理后台 | Sprint 3.5 |
| **D → A**（你等待） | Task 1.1 完成后，Java 题库数据就绪，可用真实数据测试面试流程 | Sprint 1 初 |

---

## 时间线概览

| 周次 | Sprint | 你的主要任务 |
|------|--------|-------------|
| 第 1 周 | Sprint 0 | 完成全部后端基础设施（Task 0.1-0.7）|
| 第 2-3 周 | Sprint 1 | 面试业务接口（Task 1.2, 1.4, 1.5, 1.6）|
| 第 4 周 | Sprint 2 | SSE 改造（Task 2.3，等 B 完成 2.1/2.2 后开始）|
| 第 5 周 | Sprint 3 | 报告 Controller（Task 3.4，等 B 完成 3.2/3.3 后完善）|
| 第 6 周 | Sprint 3.5 | 全部 Admin Controller（Task 3.5.1-3.5.5）|
| 第 6-7 周 | Sprint 4 | AsrController（Task 4.5）|
| 第 7-8 周 | Sprint 5 | 成长 + 资源接口（Task 5.4, 5.6, 5.7）|

---

## Sprint 0：基础设施（第 1 周）⭐ 全队阻塞，优先完成

> 本 Sprint 你的工作量最重。其他三人在等你完成 Auth API 和 LlmService 接口定义。目标：**Sprint 0 结束时全队都能开始并行开发**。

### Task 0.1 — Spring Boot 项目脚手架 + Docker 环境 🔴

**涉及文件**
- `backend/pom.xml`（全部依赖，参见 tech-stack.md）
- `backend/src/main/resources/application.yml`（DB/Redis/JWT/AI 配置项用 `${ENV:默认值}` 占位）
- `docker-compose.yml`（MySQL 8.0 + Redis 7 + Chroma 0.5，含健康检查和 volume）
- `.env.example`（环境变量说明）

**实现内容**：Spring Initializr 生成骨架；pom.xml 引入 tech-stack.md 中全部依赖；docker-compose 配置三服务持久化存储。

**完成标志**：`docker-compose up` 启动无报错；`mvn compile` 编译通过。

---

### Task 0.2 — 数据库建表脚本（DDL）🔴

**涉及文件**
- `sql/init.sql`（仅 DDL 部分，14 张表，含全部索引）

**实现内容**：按 database-design.md 建立全部 14 张表，每表含公共字段，建立 api-design.md 查询涉及的全部索引。

**完成标志**：`mysql < sql/init.sql` 执行成功，`SHOW TABLES;` 显示 14 张表。

---

### Task 0.3 — 数据库初始化数据（DML）🔴

**涉及文件**
- `sql/init.sql`（在 DDL 之后追加 INSERT 语句）

**实现内容**
- 3 条 `t_position` 记录（JAVA_BACKEND / WEB_FRONTEND / PYTHON_ALGO）
- 10 条 `t_system_config` 默认记录（LLM 参数 + 3 个 Prompt 占位模板，参见 database-design.md）
- 1 条 admin 用户（`admin` / BCrypt(`admin123456`) / role=`ADMIN`）

**完成标志**：`SELECT * FROM t_position;` 3 行；`SELECT config_key FROM t_system_config;` 10 行；admin 可登录。

---

### Task 0.4 — MyBatis-Plus 配置 + 全局基础组件 🔴

**涉及文件**
- `com/aiinterview/config/MybatisPlusConfig.java`
- `com/aiinterview/common/result/Result.java`
- `com/aiinterview/common/exception/GlobalExceptionHandler.java`
- `com/aiinterview/common/exception/BusinessException.java`

**实现内容**：MP 开启逻辑删除、驼峰转换、分页插件；`Result<T>` 包含 code/message/data，提供 `ok()`/`fail()` 工厂；`GlobalExceptionHandler` 捕获 BusinessException、参数校验异常、兜底 Exception。

**完成标志**：故意抛 BusinessException，接口返回预期 JSON 格式；参数校验失败返回 400 + 字段错误信息。

---

### Task 0.5 — Spring Security + JWT 过滤器链 🔴

**涉及文件**
- `com/aiinterview/config/SecurityConfig.java`
- `com/aiinterview/security/JwtUtil.java`
- `com/aiinterview/security/JwtAuthFilter.java`
- `com/aiinterview/security/UserDetailsServiceImpl.java`

**实现内容**：放行 `/api/v1/auth/**`；`/api/v1/admin/**` 仅 ADMIN 角色可访问；JwtUtil 用 jjwt 生成/解析 Token，密钥从配置读取；JwtAuthFilter 从 Bearer Header 提取并注入 SecurityContext。

**完成标志**：无 Token 访问受保护接口返回 401；USER Token 访问 `/api/v1/admin/**` 返回 403；ADMIN Token 通过。

---

### Task 0.6 — AuthController + AuthService（注册/登录/刷新/登出）🔴

**涉及文件**
- `com/aiinterview/controller/AuthController.java`
- `com/aiinterview/service/AuthService.java` + `impl/AuthServiceImpl.java`
- `com/aiinterview/mapper/UserMapper.java`
- `com/aiinterview/entity/User.java`
- `com/aiinterview/dto/auth/RegisterRequest.java` / `LoginRequest.java` / `AuthResponse.java`

**实现内容**：注册校验用户名唯一，BCrypt 加密写库（role 默认 USER）；登录返回 accessToken（2h）+ refreshToken（7d）+ 用户基本信息；刷新用 refreshToken 换新 accessToken；登出将 accessToken 写入 Redis 黑名单（TTL = 剩余有效期）。

**完成标志**：Postman 注册→登录→刷新→登出全流程通过；重复注册返回 400 错误信息。

**⚠️ 协作节点**：完成后立即通知**人员 C**，他/她需要用真实 Auth API 完成 Task 0.8 联调。

---

### Task 0.7 — UserController + LlmService 接口定义 🔴

**涉及文件**
- `com/aiinterview/controller/UserController.java`
- `com/aiinterview/service/UserService.java` + `impl/`
- `com/aiinterview/dto/user/UserProfileResponse.java` / `UpdateProfileRequest.java`
- `com/aiinterview/service/LlmService.java`（接口，定义 `chat()` 和 `chatStream()` 签名）
- `com/aiinterview/service/AiServiceFactory.java`（接口 + 空 DeepSeek 实现占位）

**实现内容**：`GET /api/v1/users/me` 返回当前用户档案；`PUT /api/v1/users/me` 更新昵称/学校/专业/目标岗位；LlmService 接口有空实现不影响编译。

**完成标志**：登录后 `GET /api/v1/users/me` 返回正确用户信息；`PUT` 更新后再次 GET 数据变更。

**⚠️ 协作节点**：完成后立即通知**人员 B**，他/她可以开始实现 Task 1.3 (LlmService 实现)。

### Sprint 0 验收标准
- [ ] `docker-compose up` 一键启动开发环境
- [ ] 注册 → 登录 → 刷新 Token → 登出 全流程可用
- [ ] admin 账号调用 `/api/v1/admin/stats` 返回 200；普通用户返回 403
- [ ] `LlmService` 接口已定义，项目可正常编译

---

## Sprint 1：MVP 面试流程（第 2-3 周）

> 等人员 B 完成 Task 1.3 后，Task 1.5 才能使用真实 LLM（期间可用 Mock 实现调试）。

### Task 1.2 — QuestionController + QuestionService + QuestionMapper 🔴

**涉及文件**
- `com/aiinterview/controller/QuestionController.java`
- `com/aiinterview/service/QuestionService.java` + `impl/`
- `com/aiinterview/mapper/QuestionMapper.java`
- `com/aiinterview/entity/Question.java`
- `com/aiinterview/dto/question/QuestionResponse.java`

**实现内容**：`GET /api/v1/questions?positionCode=JAVA_BACKEND&type=&difficulty=` 分页返回题目列表（**不返回 answer_reference 字段**，防止泄露答案）。

**完成标志**：按岗位查询返回正确题目列表；按 type/difficulty 过滤结果正确。

---

### Task 1.4 — 题目抽取逻辑 + 面试会话创建 🔴

**涉及文件**
- `com/aiinterview/service/impl/InterviewServiceImpl.java`（`startInterview()` 方法）
- `com/aiinterview/entity/InterviewSession.java`
- `com/aiinterview/entity/InterviewQuestion.java`
- `com/aiinterview/mapper/InterviewSessionMapper.java`
- `com/aiinterview/mapper/InterviewQuestionMapper.java`

**实现内容**：按岗位从题库中按难度比例随机抽取题目（简单 3 题/中等 5 题/困难 2 题，共 10 题）；创建 `t_interview_session`（status=IN_PROGRESS）；创建 `t_interview_question` 记录（按序号排列）。

**完成标志**：调用 `startInterview("JAVA_BACKEND", userId)` 后数据库有 1 条 session 记录和 10 条 question 记录。

---

### Task 1.5 — InterviewController.start 🔴

**涉及文件**
- `com/aiinterview/controller/InterviewController.java`（`POST /api/v1/interviews/start`）
- `com/aiinterview/dto/interview/StartInterviewRequest.java`
- `com/aiinterview/dto/interview/StartInterviewResponse.java`

**实现内容**：调用 Task 1.4 的 `startInterview()`；调用 `LlmService.chat()` 生成口语化开场白 + 第一题提问（System Prompt + 题目作为上下文）；将 AI 开场消息保存到 `t_chat_message`；返回 `{ sessionId, firstMessage }`。

**完成标志**：Postman 调用后返回 sessionId 和 AI 开场消息，数据库有对应记录。

> 注意：需等**人员 B 完成 Task 1.3**（DeepSeekLlmService），否则先用 Mock 实现：`LlmService.chat()` 直接返回硬编码字符串"好的，我们开始面试……"。

---

### Task 1.6 — InterviewController.message / end / getMessages 🔴

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

**完成标志**：能进行 5 轮对话且数据库 `t_chat_message` 有正确记录；结束后 session status 变为 COMPLETED。

**⚠️ 协作节点**：完成后通知**人员 C**，他/她可以开始联调 Task 1.7/1.8。

### Sprint 1 验收标准
- [ ] 选 Java 后端 → 开始面试 → 回答 5+ 题 → 结束面试 全流程无报错
- [ ] 对话记录正确保存到数据库
- [ ] 已结束的会话拒绝继续接收消息（返回 400）

---

## Sprint 2：SSE 改造（第 4 周）

> **前置条件**：等**人员 B 完成 Task 2.1（chatStream）和 Task 2.2（FollowUpStrategy）**后才能开始。

### Task 2.3 — InterviewController.message 改造为 SSE + 状态机防护 🔴

**涉及文件**
- `com/aiinterview/controller/InterviewController.java`（`message` 接口返回类型改为 `SseEmitter`）
- `com/aiinterview/service/impl/InterviewServiceImpl.java`（替换 SimpleStrategy → FollowUpStrategy，集成 chatStream）

**实现内容**：接口返回 `SseEmitter`；调用 `chatStream()` 逐 token 通过 `SseEmitter.send()` 推送事件类型 `token`；流结束时发送 `done` / `next_question` / `interview_end` 事件；已结束的会话拒绝继续接收消息（返回 400）。

**完成标志**：curl 调用接口，可见逐行输出的 `data:` 流；发送完最后一题后出现 `interview_end` 事件。

**⚠️ 协作节点**：完成后通知**人员 C**，他/她可以开始 Task 2.4 SSE 前端接收改造。

### Sprint 2 验收标准
- [ ] curl 调用 message 接口，出现逐行 `data:` SSE 流
- [ ] 所有题目答完后出现 `interview_end` 事件
- [ ] FollowUpStrategy 接入（依赖 B 的 Task 2.2 完成）

---

## Sprint 3：评估报告（第 5 周）

> **前置条件**：等**人员 B 完成 Task 3.2/3.3**（AiEvaluationService）后，ReportController 才能返回真实数据。你可以先实现接口骨架和数据库读取逻辑。

### Task 3.4 — ReportController 🔴

**涉及文件**
- `com/aiinterview/controller/ReportController.java`
- `com/aiinterview/dto/report/ReportDetailResponse.java`（含 dimensionScores 列表）
- `com/aiinterview/dto/report/ReportListItemResponse.java`

**实现内容**
- `GET /api/v1/reports/{reportId}`：返回完整报告（含 5 维度分数 + 逐题点评）；若 status=GENERATING 返回 `{ "status": "GENERATING" }` 供前端轮询
- `GET /api/v1/reports?page=&size=`：当前用户历史报告列表（按时间倒序，含得分/岗位/时长）

**实现顺序建议**：
1. 先完成 Controller 骨架 + Mapper 查询（数据库读取）
2. 在 `InterviewServiceImpl.end()` 中触发 `AiEvaluationService.evaluate(sessionId)` 的调用（异步 `@Async`）
3. 等 B 完成 Task 3.2/3.3 后，评估数据自然落库，接口立即有真实数据

**完成标志**：Postman 查询报告详情返回正确 JSON；status=GENERATING 时返回占位响应。

**⚠️ 协作节点**：完成后通知**人员 C**，他/她可以开始联调 Task 3.5/3.6。

### Sprint 3 验收标准
- [ ] 面试结束触发异步评估（即使 B 还没完成，接口需返回 GENERATING 状态）
- [ ] 报告列表分页正确
- [ ] 详情接口在 B 完成 3.2/3.3 后返回完整数据

---

## Sprint 3.5：管理后台（第 6 周）

### Task 3.5.1 — SystemConfigService（热更新配置服务）🟡

**涉及文件**
- `com/aiinterview/service/SystemConfigService.java` + `impl/`
- `com/aiinterview/mapper/SystemConfigMapper.java`
- `com/aiinterview/entity/SystemConfig.java`

**实现内容**：应用启动时 `@PostConstruct` 从 `t_system_config` 预热全部配置到 ConcurrentHashMap；提供 `get(key)` / `set(key, value)` 方法；`set()` 同时写库 + 更新内存缓存（热更新，无需重启）。

> 注意：`DeepSeekLlmService`（由人员 B 维护）需要改为从 `SystemConfigService.get("ai.llm.api_key")` 读取参数。请通知**人员 B** 同步修改。

**完成标志**：通过 SystemConfigService 修改 temperature 后，立即下一次 LLM 调用使用新值（无需重启服务）。

---

### Task 3.5.2 — AdminStatsController + AdminPositionController 🟡

**涉及文件**
- `com/aiinterview/controller/admin/AdminStatsController.java`
- `com/aiinterview/controller/admin/AdminPositionController.java`
- `com/aiinterview/dto/admin/StatsResponse.java`

**实现内容**
- `GET /api/v1/admin/stats`：返回总用户数、今日面试数、各岗位面试数量、题库总量
- 岗位 CRUD：`GET /POST /PUT /{id} /DELETE /{id} /PATCH /{id}/status`（启用/停用）

**完成标志**：Postman 用 admin Token 调用统计接口返回正确数据；停用岗位后前台岗位选择页不显示该岗位。

---

### Task 3.5.3 — AdminQuestionController（题目 CRUD + 批量导入）🟡

**涉及文件**
- `com/aiinterview/controller/admin/AdminQuestionController.java`
- `com/aiinterview/service/admin/AdminQuestionService.java` + `impl/`
- `com/aiinterview/dto/admin/QuestionImportItem.java`

**实现内容**：题目 CRUD（含完整字段包括 answer_reference）；`POST /api/v1/admin/questions/batch-import`：接收 JSON 数组批量插入，逐条校验 positionCode/questionType，返回成功数和失败行列表。

**完成标志**：通过接口新增题目后，新面试立即可能抽到该题；批量导入 5 条有 1 条格式错误时，正确返回 4 成功 1 失败。

---

### Task 3.5.4 — AdminKnowledgeController + AdminUserController 🟡

**涉及文件**
- `com/aiinterview/controller/admin/AdminKnowledgeController.java`
- `com/aiinterview/controller/admin/AdminUserController.java`
- `com/aiinterview/entity/KnowledgeDoc.java`
- `com/aiinterview/mapper/KnowledgeDocMapper.java`

**实现内容**
- 知识库：CRUD + `POST /api/v1/admin/knowledge/{id}/vectorize`（触发向量化，更新 is_vectorized 状态）；向量化实际调用留 TODO（Sprint 5 由人员 B 实现）
- 用户：分页列表（支持按用户名搜索）+ `PATCH /api/v1/admin/users/{id}/role`（修改用户角色）

**完成标志**：知识库文档列表正常分页；修改用户角色后该用户实际权限立即生效。

---

### Task 3.5.5 — AdminAiConfigController（AI 配置 + Prompt 管理）🟡

**涉及文件**
- `com/aiinterview/controller/admin/AdminAiConfigController.java`
- `com/aiinterview/service/admin/AdminAiConfigService.java` + `impl/`

**实现内容**
- `GET /api/v1/admin/ai-config`：返回所有配置，`is_sensitive=true` 的值替换为 `"***masked***"`
- `PUT /api/v1/admin/ai-config`：批量更新配置项（写库 + 刷新 SystemConfigService 缓存）
- `GET /api/v1/admin/ai-config/test`：用当前配置调一次 LLM，返回 `{ "success": true, "latency_ms": 320 }`
- `GET/PUT /api/v1/admin/prompts/{key}`：读取/覆盖 Prompt 模板（存储于 `t_system_config`）
- `POST /api/v1/admin/prompts/{key}/preview`：用请求体中的变量值填充模板，返回渲染结果（不调用 LLM）

**完成标志**：API Key 在 GET 中显示掩码；PUT 更新 temperature 后连通性测试用新值；Prompt 预览返回填充后的文本。

**⚠️ 协作节点**：完成后通知**人员 C**，他/她可以联调 Task 3.5.6-3.5.9 管理后台页面。

### Sprint 3.5 验收标准
- [ ] admin Token 调用 `/api/v1/admin/stats` 返回正确统计数据
- [ ] 通过接口新增/修改题目后，新面试立即使用新题目
- [ ] AI 配置 PUT 后，LLM 调用使用新配置（热更新，无需重启）
- [ ] Prompt 预览接口填入变量后返回渲染结果

---

## Sprint 4：多岗位扩展（第 6-7 周）

### Task 4.5 — 后端 AsrController（讯飞 ASR 备用方案）🟢

**涉及文件**
- `com/aiinterview/controller/AsrController.java`
- `com/aiinterview/service/AsrService.java` + `impl/XunfeiAsrService.java`

**实现内容**：`POST /api/v1/asr/convert`，接收 `multipart/form-data` 音频文件（WebM/WAV）；通过讯飞 WebSocket API 转写为文字并返回；API 配置从 `t_system_config` 读取。

**完成标志**：上传一段音频文件，接口返回正确识别的文字（备用路径，Web Speech API 可用时前端不调此接口）。

> 注意：这是加分项（🟢），若工期紧张可延后或简化实现。

### Sprint 4 验收标准
- [ ] 三岗位后端路由均支持（positionCode 已在岗位配置中）
- [ ] ASR 接口可接收音频并返回文字（或有说明的 TODO）

---

## Sprint 5：成长与资源接口（第 7-8 周）

### Task 5.4 — GrowthController 🟡

**涉及文件**
- `com/aiinterview/controller/GrowthController.java`
- `com/aiinterview/dto/growth/GrowthDataResponse.java`（含按岗位分组的时间序列数据）

**实现内容**：`GET /api/v1/growth?positionCode=`（可选过滤）：查询当前用户 `t_growth_record`，按时间排序返回历次面试各维度得分列表；同时计算趋势（最近 3 次均值对比前 3 次：上升/下降/平稳）。

**完成标志**：完成 2+ 次面试后，接口返回含时间序列的成长数据，趋势字段正确。

---

### Task 5.6 — ResourceService 推荐逻辑 🟡

**涉及文件**
- `com/aiinterview/service/ResourceService.java` + `impl/`
- `com/aiinterview/mapper/LearningResourceMapper.java`
- `com/aiinterview/entity/LearningResource.java`

**实现内容**：`generateRecommendations(reportId)`：读取报告弱项维度（分数 < 70 的维度）→ 按 topic 匹配资源（由人员 D 提供的 `sql/data_resources.sql` 数据）→ 取 Top-5；写入 `t_user_recommendation`。

> 依赖**人员 D 完成 `sql/data_resources.sql`** 数据文件。

**完成标志**：存在弱项维度的报告调用后，`t_user_recommendation` 有 5 条推荐记录。

---

### Task 5.7 — ResourceController（后端部分）🟡

**涉及文件**
- `com/aiinterview/controller/ResourceController.java`（`GET /api/v1/resources/recommendations/{reportId}` + `POST /feedback`）

**实现内容**
- `GET /api/v1/resources/recommendations/{reportId}`：返回该报告对应的推荐资源列表
- `POST /api/v1/resources/recommendations/{id}/feedback`：接收 `{ "isHelpful": true/false }`，更新 `t_user_recommendation.is_helpful`

**完成标志**：接口返回正确推荐列表；反馈接口不报错，数据库字段正确更新。

**⚠️ 协作节点**：完成后通知**人员 C**，他/她完成 Task 5.7 前端部分。

### Sprint 5 验收标准
- [ ] 成长接口返回时间序列数据，趋势字段正确
- [ ] 资源推荐接口在弱项报告时返回 5 条推荐
- [ ] 反馈接口正常工作

---

## 你的任务汇总

| 任务 | Sprint | 优先级 | 前置依赖 |
|------|--------|--------|---------|
| Task 0.1 | S0 | 🔴 | 无 |
| Task 0.2 | S0 | 🔴 | 0.1 |
| Task 0.3 | S0 | 🔴 | 0.2 |
| Task 0.4 | S0 | 🔴 | 0.1 |
| Task 0.5 | S0 | 🔴 | 0.4 |
| Task 0.6 | S0 | 🔴 | 0.5 |
| Task 0.7 | S0 | 🔴 | 0.6 |
| Task 1.2 | S1 | 🔴 | D 的 Task 1.1（数据）|
| Task 1.4 | S1 | 🔴 | 0.7 |
| Task 1.5 | S1 | 🔴 | 1.4，B 的 1.3（LLM）|
| Task 1.6 | S1 | 🔴 | 1.5 |
| Task 2.3 | S2 | 🔴 | B 的 2.1+2.2 |
| Task 3.4 | S3 | 🔴 | B 的 3.2+3.3 |
| Task 3.5.1 | S3.5 | 🟡 | 0.7 |
| Task 3.5.2 | S3.5 | 🟡 | 3.5.1 |
| Task 3.5.3 | S3.5 | 🟡 | 3.5.1 |
| Task 3.5.4 | S3.5 | 🟡 | 3.5.1 |
| Task 3.5.5 | S3.5 | 🟡 | 3.5.1，B 的 3.5.1 |
| Task 4.5 | S4 | 🟢 | 无 |
| Task 5.4 | S5 | 🟡 | 3.3（GrowthRecord 已写入）|
| Task 5.6 | S5 | 🟡 | D 的 data_resources.sql |
| Task 5.7 | S5 | 🟡 | 5.6 |
