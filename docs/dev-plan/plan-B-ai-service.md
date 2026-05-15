# 开发计划 — AI 服务集成（人员 B）

## 角色定位

**职责范围**：LLM 服务封装 + Prompt 工程 + 面试对话策略 + 评估引擎 + RAG 知识检索 + SystemConfigService 热更新

你是团队的 **AI 能力负责人**。你的核心产出是让 AI"说得好"（Prompt）、"跑得快"（流式）、"评得准"（评估引擎）、"知识丰富"（RAG）。你的工作不直接面向用户，而是以服务层接口的形式被人员 A 的 Controller 层调用。

**Sprint 0 期间**：等待人员 A 完成 Task 0.7（LlmService 接口定义），在此之前你可以：
- 研读 DeepSeek API 文档（`/v1/chat/completions`，注意 stream 参数和 SSE 协议）
- 设计 `interview_system.txt` Prompt 草稿
- 熟悉 OkHttp SSE 接收写法

---

## 与其他成员的依赖关系

| 依赖方向 | 内容 | 时间节点 |
|----------|------|----------|
| **A → B**（你等待） | Task 0.7：`LlmService` 接口定义确定，你才能开始 Task 1.3 编码 | Sprint 0 末 |
| **A → B**（你等待） | Task 3.5.1：`SystemConfigService` 框架由 A 搭建，你的 `DeepSeekLlmService` 需改为从中读取配置 | Sprint 3.5 初 |
| **B → A**（你提供） | Task 1.3：`LlmService.chat()` 实现就绪，A 的 Task 1.5 才能用真 LLM 进行面试 | Sprint 1 中期 |
| **B → A**（你提供） | Task 2.1+2.2：`chatStream()` + `FollowUpStrategy` 就绪，A 才能做 Task 2.3 SSE 改造 | Sprint 2 |
| **B → A**（你提供） | Task 3.2+3.3：`AiEvaluationService` 实现，A 的 Task 3.4 报告接口才有真实数据 | Sprint 3 |
| **B → A**（你提供） | Task 4.2：岗位 Prompt 文件就绪，A 的 `InterviewService.startInterview()` 可按岗位选 Prompt | Sprint 4 |
| **B → A**（你协作） | Task 3.5.5：Prompt 预览渲染逻辑，需与 A 协商 Prompt 存储格式（`t_system_config` vs 文件）| Sprint 3.5 |

---

## 时间线概览

| 周次 | Sprint | 你的主要任务 |
|------|--------|-------------|
| 第 1 周 | Sprint 0 | 研究 DeepSeek API，起草 Prompt，等 A 的接口定义 |
| 第 2-3 周 | Sprint 1 | Task 1.3：DeepSeekLlmService + 基础面试 Prompt |
| 第 4 周 | Sprint 2 | Task 2.1：流式实现；Task 2.2：FollowUpStrategy + 追问 Prompt |
| 第 5 周 | Sprint 3 | Task 3.1：评估 Prompt；Task 3.2：逐题评分；Task 3.3：综合报告 |
| 第 6 周 | Sprint 3.5 | 与 A 协作 SystemConfigService 热更新对接 |
| 第 6-7 周 | Sprint 4 | Task 4.2：前端 + Python 岗位 Prompt 文件 |
| 第 7-8 周 | Sprint 5 | Task 5.2：RAG 向量化；Task 5.3：RAG 检索 + 评估集成 |

---

## Sprint 0：准备阶段（第 1 周）

本 Sprint 你**没有可以合并的代码**，但需要完成以下准备工作：

### 准备清单

1. **研究 DeepSeek API**
   - 确认 Chat API 请求格式（`messages` 数组结构、`model` 参数、`stream` 参数）
   - 研究 SSE 响应格式（`data: {"choices":[{"delta":{"content":"..."}}]}`）
   - 确认 Embedding API 参数（`/v1/embeddings`，`input` 字段，返回 `data[0].embedding`）
   - 确认 API Key 申请状态和调用限额

2. **设计 Prompt 草稿（Task 1.3 提前准备）**
   - 在本地文件中起草 `interview_system.txt`：面试官角色、语气、出题节奏
   - 目标：专业、简洁、不啰嗦，每次提问不超过 2 句话

3. **OkHttp SSE 技术预研（Task 2.1 提前准备）**
   - 确认 OkHttp EventSource API 可正常接收 `data:` 流
   - 备选方案：直接解析 OkHttp ResponseBody（更可控）

4. **等待 A 完成 Task 0.7** 后，即可开始 Task 1.3 编码

---

## Sprint 1：LLM 服务实现（第 2-3 周）

> **前置条件**：人员 A 完成 Task 0.7（`LlmService` 接口定义）

### Task 1.3 — DeepSeekLlmService 实现 + interview_system Prompt 🔴

**涉及文件**
- `com/aiinterview/service/impl/DeepSeekLlmService.java`
- `com/aiinterview/service/AiServiceFactory.java`（注册 DeepSeek 实现）
- `src/main/resources/prompts/interview_system.txt`（基础面试官 System Prompt）

**实现内容**
- 用 OkHttp 调用 DeepSeek Chat API（`/v1/chat/completions`）
- API Key / baseUrl / model / temperature 等参数从配置读取（先用 `application.yml`，Sprint 3.5 后改为从 `SystemConfigService` 读取）
- 同步 `chat()` 方法：拼接 messages 数组，返回 content 字符串
- `chatStream()` 留空桩方法（Sprint 2 实现）
- `interview_system.txt` Prompt 设计要点：
  - 扮演专业技术面试官
  - 每次只提问一道题，等待用户回答后再出下一题
  - 语气专业简洁，每次提问 1-2 句话
  - 接收变量：`{position_name}` / `{question}` / `{question_type}`

**完成标志**：单测传入一条用户消息，能收到 LLM 非空回复；API Key 错误时抛出 `BusinessException` 提示友好信息。

**⚠️ 协作节点**：完成后**立即通知人员 A**，他的 Task 1.5 可以切换为真实 LLM 实现。

### Sprint 1 验收标准
- [ ] `DeepSeekLlmService.chat()` 单测通过，可收到 DeepSeek 非空回复
- [ ] 错误处理：API 不可用时抛出友好异常（非 NPE）
- [ ] `interview_system.txt` Prompt 经过至少 2 轮手动调试

---

## Sprint 2：流式输出 + 追问策略（第 4 周）

### Task 2.1 — LlmService.chatStream() 流式实现 🔴

**涉及文件**
- `com/aiinterview/service/impl/DeepSeekLlmService.java`（新增 `chatStream()` 方法）
- `com/aiinterview/service/LlmService.java`（接口补充 `chatStream()` 签名）

**实现内容**
- 用 OkHttp 发起 SSE 请求到 DeepSeek API（`stream: true`）
- 逐行解析 `data:` 字段，提取 `choices[0].delta.content` token
- 通过回调接口 `StreamHandler.onToken(String)` / `onDone()` / `onError(Throwable)` 通知调用方
- 注意：`data: [DONE]` 时触发 `onDone()`；JSON 解析异常时触发 `onError()`

**完成标志**：单测调用 `chatStream()`，控制台可见逐字打印的流式输出；`[DONE]` 后 `onDone()` 被调用。

**⚠️ 协作节点**：完成后**立即通知人员 A**，他可以开始 Task 2.3 SSE 接口改造。

---

### Task 2.2 — FollowUpStrategy + 追问 Prompt 🔴

**涉及文件**
- `com/aiinterview/service/strategy/FollowUpStrategy.java`
- `src/main/resources/prompts/follow_up_question.txt`

**实现内容**
- `follow_up_question.txt` Prompt 要点：
  - 输入变量：`{question}` / `{user_answer}` / `{follow_up_count}`（已追问次数）
  - 要求 LLM 输出结构化 JSON：`{ "action": "follow_up|next_question|end", "content": "..." }`
  - 追问规则：回答不完整/不准确时追问，每题最多追问 2 次后强制推进
  - `follow_up_count >= 2` 时 Prompt 中说明"请无论如何给出下一题"
- `FollowUpStrategy` 实现 `InterviewStrategy` 接口：
  - 调用 `LlmService.chat()` 获取 JSON 字符串
  - 解析 `action` 字段，决定是追问、出下一题还是结束面试

**完成标志**：
- 传入"不知道"作为用户回答，`action` 为 `follow_up`
- 传入详细技术回答，`action` 为 `next_question`
- `follow_up_count=2` 时，即使回答简单也返回 `next_question` 或 `end`

**Prompt 调试建议**：在 DeepSeek 官方 Playground 先调试 3-5 组用例，确认 JSON 格式稳定后再写入文件。

### Sprint 2 验收标准
- [ ] `chatStream()` 可见逐字流式输出
- [ ] `FollowUpStrategy` 能正确判断追问/推进
- [ ] 追问次数限制生效（不超过 2 次）

---

## Sprint 3：评估引擎（第 5 周）

> **这是赛题评分的核心**，请重点投入 Prompt 设计。

### Task 3.1 — 评估 Prompt 模板设计 🔴

**涉及文件**
- `src/main/resources/prompts/evaluation_question.txt`（逐题评分 Prompt）
- `src/main/resources/prompts/evaluation_final.txt`（综合报告 Prompt）

**实现内容**

`evaluation_question.txt`：
- 输入变量：`{question}` / `{reference_answer}` / `{user_answer}` / `{rag_context}`（Sprint 5 注入，现留占位）
- 要求 LLM 输出 JSON：
  ```json
  {
    "tech_score": 0-100,
    "logic_score": 0-100,
    "depth_score": 0-100,
    "comment": "简短点评（50字以内）"
  }
  ```
- 评分维度说明：技术准确性(tech)、逻辑清晰度(logic)、回答深度(depth)

`evaluation_final.txt`：
- 输入变量：`{all_question_scores}`（所有题目 JSON 汇总）/ `{position_name}`
- 要求 LLM 输出 JSON：
  ```json
  {
    "overall_score": 0-100,
    "expression_score": 0-100,
    "confidence_score": 0-100,
    "summary": "总结文本（支持 Markdown）",
    "highlights": ["亮点1", "亮点2"],
    "weaknesses": ["不足1", "不足2"],
    "suggestions": ["建议1", "建议2", "建议3"]
  }
  ```

**完成标志**：用 curl 手动调 DeepSeek API 带入模板内容（模拟 5 题的评估场景），能得到符合 JSON Schema 的回复，且内容与题目实际相关。

---

### Task 3.2 — AiEvaluationService 逐题评分 🔴

**涉及文件**
- `com/aiinterview/service/AiEvaluationService.java` + `impl/AiEvaluationServiceImpl.java`
- `com/aiinterview/entity/DimensionScore.java`
- `com/aiinterview/mapper/DimensionScoreMapper.java`
- `com/aiinterview/entity/EvaluationReport.java`
- `com/aiinterview/mapper/EvaluationReportMapper.java`

**实现内容**
- `@Async` 方法 `evaluate(sessionId)`
- 先创建 `t_evaluation_report`（status=GENERATING）
- 遍历该 session 所有题目的用户回答：
  - 获取原题 + 参考答案（`answer_reference`）
  - 填充 `evaluation_question.txt` 模板
  - 调 `LlmService.chat()` 获取评分 JSON
  - 解析 JSON 写入 `t_dimension_score`
- 捕获异常：将 report status 置为 FAILED，记录错误信息

**注意**：JSON 解析建议用 Jackson `objectMapper.readTree()`，避免 LLM 偶尔返回多余文本导致解析失败（可用正则提取 `\{.*\}` 再解析）。

**完成标志**：面试结束后触发评估，30 秒内 `t_dimension_score` 有对应条目（每题 3 个维度），`t_evaluation_report` status 为 GENERATING 或 GENERATING→（等 3.3 完成后变 COMPLETED）。

---

### Task 3.3 — AiEvaluationService 综合报告生成 🔴

**涉及文件**
- `com/aiinterview/service/impl/AiEvaluationServiceImpl.java`（续 Task 3.2，补充综合报告逻辑）
- `com/aiinterview/entity/GrowthRecord.java`
- `com/aiinterview/mapper/GrowthRecordMapper.java`

**实现内容**
- 在 Task 3.2 逐题评分完成后，汇总各题分数构建 `{all_question_scores}` 变量
- 调 LLM 生成综合报告（使用 `evaluation_final.txt`）
- 解析 JSON，更新 `t_evaluation_report`（overall_score/expression_score/confidence_score/summary/highlights/weaknesses/suggestions，status=COMPLETED）
- 同步写入 `t_growth_record`（记录本次面试各维度得分 + 时间戳，供 Sprint 5 成长曲线使用）

**完成标志**：面试结束后 60 秒内 `t_evaluation_report` status=COMPLETED；`t_growth_record` 有 1 条新记录；summary 字段内容与实际面试相关。

**⚠️ 协作节点**：完成后**立即通知人员 A**，他的 Task 3.4 ReportController 现在可以返回真实数据。

### Sprint 3 验收标准
- [ ] 逐题评分 JSON 格式正确（tech_score/logic_score/depth_score/comment）
- [ ] 综合报告 JSON 格式正确（7 个字段全部有值）
- [ ] 60 秒内 `t_evaluation_report` status 从 GENERATING 变为 COMPLETED
- [ ] `t_growth_record` 新增记录

---

## Sprint 3.5：热更新对接（第 6 周）

> 人员 A 在 Sprint 3.5 实现 Task 3.5.1 `SystemConfigService`。你需要**协调完成以下改造**：

### SystemConfigService 集成（与 A 协作完成）

**涉及文件**
- `com/aiinterview/service/impl/DeepSeekLlmService.java`（修改配置读取方式）

**需要改动的内容**：
1. 将 `DeepSeekLlmService` 中硬编码的 `@Value("${ai.deepseek.api-key}")` 改为注入 `SystemConfigService`
2. 每次调用前从 `SystemConfigService.get("ai.llm.api_key")` 读取最新配置
3. 同样适用于 `base_url` / `model` / `temperature` / `max_tokens`

**完成标志**：通过 AdminAiConfig 接口修改 temperature 后，下一次 LLM 调用立即使用新值（无需重启）。

---

### Prompt 模板持久化协作

与人员 A 商定 Prompt 存储方案：
- **方案 A**（推荐）：Prompt 存储在 `t_system_config` 中（`config_key` = `prompt.interview_system` 等），可通过管理后台编辑
- **方案 B**：保留文件系统，管理后台只能预览不能编辑文件
- 推荐方案 A，因为这样 Prompt 预览功能（Task 3.5.5）更容易实现

### Sprint 3.5 验收标准
- [ ] `DeepSeekLlmService` 从 `SystemConfigService` 读取配置（热更新生效）
- [ ] Prompt 模板已迁移到 `t_system_config`（或与 A 协商确认存储方案）

---

## Sprint 4：多岗位 Prompt（第 6-7 周）

### Task 4.2 — 各岗位定制 Prompt（你负责 Prompt 文件，SQL 由人员 D 负责）🔴

**你负责的文件**
- `src/main/resources/prompts/interview_system_web.txt`（Web 前端岗位面试官 Prompt）
- `src/main/resources/prompts/interview_system_python.txt`（Python 算法岗位面试官 Prompt）

**实现内容**

`interview_system_web.txt` 设计要点：
- 侧重前端工程化、Vue/React 原理、性能优化、浏览器渲染
- 对代码类题目，要求用户描述思路（而非直接写代码）
- 变量：`{question}` / `{question_type}`

`interview_system_python.txt` 设计要点：
- 侧重数据结构/算法思路表达、时间空间复杂度分析
- 引导用户描述算法步骤
- 鼓励用例验证思考

**还需协调人员 A** 修改 `InterviewService.startInterview()` 逻辑，按 positionCode 加载对应 Prompt 文件（或从 `t_system_config` 读取对应 key）。

**完成标志**：三个岗位各发起一次面试，AI 提问内容和风格差异明显。

### Sprint 4 验收标准
- [ ] 两个新岗位的 Prompt 文件调试完成（至少手动测试 5 轮对话）
- [ ] 岗位 Prompt 路由逻辑与人员 A 协调完成

---

## Sprint 5：RAG 知识检索（第 7-8 周）

> **前置条件**：人员 D 完成 Task 5.1（知识库 SQL 数据录入），文档已在 `t_knowledge_doc` 中且 `is_vectorized=false`。

### Task 5.2 — RagService 向量化（Embedding → Chroma）🟡

**涉及文件**
- `com/aiinterview/service/RagService.java` + `impl/RagServiceImpl.java`
- `com/aiinterview/config/ChromaConfig.java`（Chroma Client 配置）

**实现内容**
- 调用 DeepSeek Embedding API 将文档 content 转为向量（`/v1/embeddings`）
- 写入 Chroma（每个岗位一个 Collection，如 `java_backend_knowledge`）
- 存储时包含 metadata：`{ "docId": 1, "topic": "JVM", "positionCode": "JAVA_BACKEND" }`
- 写入成功后更新 `t_knowledge_doc.is_vectorized=true` 并记录 `chroma_ids`
- `AdminKnowledgeController` 的 `POST /api/v1/admin/knowledge/{id}/vectorize` 接口调用此方法

**完成标志**：触发向量化后 `is_vectorized=true`；Chroma 中对应 Collection 有数据（可通过 Chroma REST API 验证）。

---

### Task 5.3 — RagService 检索 + AiEvaluationService 集成 RAG 🟡

**涉及文件**
- `com/aiinterview/service/impl/RagServiceImpl.java`（新增 `search()` 方法）
- `com/aiinterview/service/impl/AiEvaluationServiceImpl.java`（评分时注入 RAG 结果）

**实现内容**
- `search(userAnswer, positionCode, topK=5)`：将用户回答 Embedding 后在对应 Collection 相似度检索，返回 Top-5 知识片段
- 修改 `AiEvaluationService` 逐题评分逻辑：先调用 `RagService.search()`，将检索到的知识片段注入 `evaluation_question.txt` 的 `{rag_context}` 变量

**完成标志**：评分 Prompt 中可见"相关知识参考"片段；与未集成 RAG 时的点评对比，内容明显更具体（可在报告中对比两次）。

### Sprint 5 验收标准
- [ ] 向量化流程：文档 → Embedding → Chroma → `is_vectorized=true`
- [ ] RAG 检索：用户回答 → 向量检索 → Top-5 片段注入评估 Prompt
- [ ] 评估点评质量提升（与未集成 RAG 时可对比）

---

## 你的任务汇总

| 任务 | Sprint | 优先级 | 前置依赖 |
|------|--------|--------|---------|
| 准备工作（Prompt 草稿 + API 研究） | S0 | — | 无 |
| Task 1.3 DeepSeekLlmService + 基础 Prompt | S1 | 🔴 | A 的 Task 0.7 |
| Task 2.1 chatStream 流式实现 | S2 | 🔴 | Task 1.3 |
| Task 2.2 FollowUpStrategy + 追问 Prompt | S2 | 🔴 | Task 1.3 |
| Task 3.1 评估 Prompt 模板设计 | S3 | 🔴 | Task 1.3 |
| Task 3.2 AiEvaluationService 逐题评分 | S3 | 🔴 | Task 3.1，A 的 Entity/Mapper |
| Task 3.3 AiEvaluationService 综合报告 | S3 | 🔴 | Task 3.2 |
| SystemConfigService 集成（协作 A） | S3.5 | 🟡 | A 的 Task 3.5.1 |
| Task 4.2 岗位 Prompt 文件（Web + Python）| S4 | 🔴 | Task 1.3 |
| Task 5.2 RagService 向量化 | S5 | 🟡 | D 的 Task 5.1 |
| Task 5.3 RagService 检索 + 评估集成 | S5 | 🟡 | Task 5.2 |

---

## Prompt 工程注意事项

1. **JSON 稳定性**：LLM 有时会在 JSON 前后添加 Markdown 代码块（` ```json ` ），解析时需处理：
   ```java
   String cleaned = response.replaceAll("```json\\s*", "").replaceAll("```", "").trim();
   ```

2. **温度设置**：评估 Prompt 建议 temperature=0.3（低随机性，保证分数稳定性）；面试对话建议 temperature=0.7（适度创意）。

3. **错误处理**：LLM 调用失败时，逐题评分应记录 FAILED 状态，但不应阻塞整个评估流程（继续处理其他题目）。

4. **Prompt 版本管理**：每次修改 Prompt 前，先记录当前版本和修改原因，方便对比效果。
