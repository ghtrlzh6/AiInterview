# 系统架构设计

## 1. 整体架构概览

```
┌─────────────────────────────────────────────────────────────────┐
│                          客户端层                                │
│   浏览器（Vue 3 SPA）                                            │
│   - 面试交互界面  - 报告展示  - 成长曲线  - 题库浏览  - 【知识库目录与正文阅览】 │
└──────────────────────┬──────────────────────────────────────────┘
                       │ HTTPS / WebSocket
┌──────────────────────▼──────────────────────────────────────────┐
│                       接入层（Nginx）                            │
│   静态资源托管 / 反向代理 / SSL 终止 / 限流                      │
└──────────────────────┬──────────────────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────────────────┐
│                    应用层（Spring Boot 3.x）                     │
│                                                                   │
│  ┌────────────┐ ┌────────────┐ ┌────────────┐ ┌──────────────┐  │
│  │  认证模块  │ │  面试模块  │ │  评估模块  │ │  用户模块    │  │
│  │  AuthModule│ │ InterviewM │ │  EvalModule│ │  UserModule  │  │
│  └────────────┘ └────────────┘ └────────────┘ └──────────────┘  │
│                                                                   │
│  ┌────────────┐ ┌────────────┐ ┌────────────┐ ┌──────────────┐  │
│  │  题库模块  │ │ 【知识仓库】 │ │ 【简历模块】 │ │ 【AI出题管线】 │
│  │ QuestionM  │ │  KbModule  │ │ ResumeMod  │ │ AiGenModule  │  │
│  └────────────┘ └────┬───────┘ └─────┬──────┘ └───────┬────────┘  │
│                       │向量切片入库     │PDF/LLM结构化   │写 t_question │
│  ┌────────────┐ ┌────────────┐ ┌────────────┐ ┌──────────────┐  │
│  │  报告模块  │ │ 【手撕快照】 │ │  资源模块  │ │  AI 服务层   │  │
│  │ ReportMod  │ │ CodingJob  │ │ ResourceM │ │ AiService/Llm│  │
│  └────────────┘ └────────────┘ └────────────┘ └──────────────┘  │
└───────────┬───────────┬──────────────┬──────────────┬───────────┘
            │           │              │              │
     ┌──────▼───┐ ┌─────▼────┐ ┌──────▼──────┐ ┌───▼──────────┐
     │  MySQL   │ │  Redis   │ │  Chroma     │ │  外部 AI 服务│
     │  8.0     │ │  7.x     │ │  向量数据库 │ │              │
     │（主存储）│ │（缓存/会话）│（RAG 检索）│ │ DeepSeek API │
     └──────────┘ └──────────┘ └─────────────┘ │ 讯飞 ASR     │
                                                └──────────────┘
```

---

## 2. 模块职责划分

### 2.1 认证模块（AuthModule）

- 用户注册、登录、Token 刷新
- JWT 生成与校验（无状态认证）
- 密码 BCrypt 加密
- Spring Security 过滤器链配置

### 2.2 用户模块（UserModule）

- 用户档案 CRUD（昵称、头像、目标岗位等）
- 能力成长曲线数据聚合
- 历史面试记录查询

### 2.3 题库模块（QuestionModule）

- 面试题目 `t_question`：保留四种题型语义（见库表注释）；**TECH / SCENARIO** 可由题库池或 AI 按需生成并入表；**PROJECT_DEEP** 多与本场 `resume_snapshot` 绑定会话专属记录；**BEHAVIOR** = 手撕题，关联 `t_coding_challenge`（Hot100 等）。
- 题目与 **`t_kb_node`**：通过 `primary_kb_module_id` 声明「这道题挂在哪个知识大模块」（如数据结构）；通过 **`t_question_kb_point`** M:N 关联 **0～多个 TOPIC_POINT 知识点**。
- 题目抽取策略：`binding_session_id IS NULL` 的全局池 + 会话内插入的会话专属题；按岗位 / 题型 / 难度配比随机或策略抽取。
- 对外题目列表不落 `answer_reference`。

### 2.3.a 知识库模块（KbModule）

- **`t_kb_node` 类目树**：支持无限层级分组；仅 `TOPIC_POINT` 叶子挂载 **`t_kb_article` 整块 Markdown**。
- **前台**：侧边树 + 正文渲染（与用户举例的层级展示一致）。
- **RAG**：正文切块 → Embedding → Chroma，`metadata` 含 `kb_node_id`、`article_id`、`code_path`、`position_codes`。
- **管理**：后台与普通接口维护节点与正文，“向量化”按钮刷新向量索引。

### 2.3.b 简历模块（ResumeModule）

- 用户上传 PDF → 落 `t_user_resume` → 异步解析全文 → LLM/规则抽取 `t_resume_project`。
- `POST /interviews/start` 可携带 `resume_id`；会话写入 `resume_snapshot_id` 供项目题生成器读取。

### 2.3.c AI 题目生成（AiQuestionGenerator）

- **输入**：岗位、`kb_module_id` 或子树范围、题型（仅 `TECH_KNOWLEDGE` / `SCENARIO`）、可选难度与数量。
- **输出**：LLM 生成题干 → 校验 JSON → **INSERT `t_question`** + 可选写 `t_question_kb_point`；`source=AI_TECH_SCENARIO`。
- **项目题**：读取 `t_resume_project` + 选定知识点 → 生成 `PROJECT_DEEP` 行并设 `binding_session_id`。
- **手撕题**：不调 LLM 造新题，由 **`t_coding_challenge` 池** 抽题后组装 `t_question` 或直接引用池 ID。

### 2.4 面试模块（InterviewModule）

- 创建面试会话（选岗位 → 【可选绑定简历快照】→ 分配题目序列，含会话专属项目题插入）
- WebSocket / SSE 长连接维护实时对话流
- 对话轮次管理（题目推进 + 追问逻辑）
- **手撕题轮次**：内置 IDE 提交代码 → `t_session_coding_submit`（无 OJ 时仅快照 + LLM 评审与追问）
- 语音转文字（接收前端 Base64 音频 → 调用 ASR）
- 面试状态机（未开始 → 进行中 → 已结束）

### 2.5 评估模块（EvalModule）

- 面试结束后触发异步评估任务
- RAG 检索：将用户回答向量化，从 Chroma 按 **知识树元数据** 召回 `t_kb_article` 切块
- **手撕题**：合并最近代码提交快照 + 题干，走独立评分 Prompt（正确性/算法选择/明显逻辑或语法问题）
- 调用 LLM 进行多维度评分（技术正确性、逻辑性、深度、表达）
- 评估结果持久化到 `t_evaluation_report` + `t_dimension_score`

### 2.6 报告模块（ReportModule）

- 查询评估报告详情
- 生成改进建议（基于维度短板调用 LLM）
- 报告分享链接生成

### 2.7 资源模块（ResourceModule）

- 学习资源（文章、题目、视频链接）管理
- 基于用户短板的智能推荐（弱点维度 → 匹配资源标签）
- 推荐记录存储与反馈收集

### 2.8 AI 服务层（AiService）

- LLM 调用封装（统一接口，支持切换模型）
- Prompt 模板渲染引擎
- Embedding 生成（文本 → 向量）
- ASR 调用封装（语音 → 文字）
- 流式响应处理（SSE 推送 LLM token 流）

---

## 3. 前端架构

```
src/
├── api/              # Axios 封装的 API 调用层（按模块分文件）
├── assets/           # 静态资源
├── components/       # 通用组件
│   ├── common/       # 按钮、表单、弹窗等基础组件
│   └── charts/       # ECharts 图表组件（成长曲线等）
├── layouts/          # 布局组件（AuthLayout, MainLayout）
├── pages/            # 页面级组件
│   ├── auth/         # 登录、注册
│   ├── home/         # 首页 / 仪表盘
│   ├── interview/    # 面试选择 + 面试间 + 结束等待
│   ├── report/       # 报告详情、历史报告列表
│   ├── growth/       # 成长曲线可视化
│   ├── knowledge/    # 知识库树 + 正文阅读（与 t_kb_node / t_kb_article 对应）
│   └── resources/    # 推荐资源页
├── router/           # Vue Router 路由配置
├── stores/           # Pinia 状态管理
│   ├── auth.ts       # 认证状态（token、用户信息）
│   ├── interview.ts  # 面试会话状态
│   └── report.ts     # 报告状态
├── types/            # TypeScript 类型定义
└── utils/            # 工具函数（请求拦截、语音工具等）
```

---

## 4. 后端目录结构

```
src/main/java/com/aiinterview/
├── config/                  # Spring 配置类
│   ├── SecurityConfig.java  # Spring Security + JWT
│   ├── RedisConfig.java
│   └── WebSocketConfig.java
├── controller/              # REST 控制器
│   ├── AuthController.java
│   ├── UserController.java
│   ├── KbController.java              # 知识库树/正文只读
│   ├── ResumeController.java          # 简历上传与项目条目
│   ├── InterviewController.java
│   ├── QuestionController.java
│   ├── ReportController.java
│   ├── ResourceController.java
│   └── admin/               # 管理员专属接口（需 ADMIN 角色）
│       ├── AdminStatsController.java
│       ├── AdminPositionController.java
│       ├── AdminQuestionController.java
│       ├── AdminKnowledgeController.java  # 【替换为 AdminKbController：节点+正文 CRUD】
│       ├── AdminAiQuestionGenController.java
│       ├── AdminAiConfigController.java
│       └── AdminUserController.java
├── service/                 # 业务逻辑
│   ├── impl/                # 接口实现
│   └── ai/                  # AI 服务
│       ├── LlmService.java
│       ├── AiEvaluationService.java
│       ├── AsrService.java
│       └── RagService.java
├── mapper/                  # MyBatis-Plus Mapper 接口
├── entity/                  # 数据库实体类
├── dto/                     # 数据传输对象（Request/Response）
├── vo/                      # 视图对象
├── enums/                   # 枚举（岗位类型、题型、面试状态等）
├── exception/               # 全局异常处理
├── util/                    # 工具类（JWT、加密等）
└── AiInterviewApplication.java
src/main/resources/
├── mapper/                  # MyBatis XML
├── prompts/                 # LLM Prompt 模板
│   ├── interview_system.txt      # 面试官系统提示词
│   ├── evaluation_report.txt     # 评估报告生成提示词
│   └── follow_up_question.txt    # 追问生成提示词
└── application.yml
```

---

## 5. 核心数据流

### 5.1 面试流程数据流

```
用户选择岗位
    ↓
【可选】上传/选用简历快照（PDF → t_user_resume）
    ↓
POST /api/v1/interviews/start （可带 resume_snapshot_id）
    ↓ 创建 t_interview_session；若启用项目深挖：流水线生成 SESSION_BOUND 的 PROJECT_DEEP 题并入队
    ↓ TECH/SCENARIO 从全局 pool 抽出；BEHAVIOR 从 t_coding_challenge 抽一题并生成占位 t_question（若未有预置）
    ↓ 按岗位抽取题目序列，存入 t_interview_question
    ↓ 返回 sessionId + 第一道题目
    ↓
AI 面试官发出第一个问题（由 LLM 基于题目生成口语化提问）
    ↓
用户回答（文字 or 语音；若本题 BEHAVIOR：先 IDE 提交代码）
    ↓ 【手撕分支】 POST /coding-submit → 写 t_session_coding_submit → 可走一轮 LLM 「代码审稿」SSE
    ↓ 若语音：前端录音 → Base64 → POST /api/v1/asr/convert → 返回文字
    ↓
POST /api/v1/interviews/{id}/message（携带用户文字回答）
    ↓ 保存 t_chat_message（role=USER）
    ↓ RagService 检索相关知识片段
    ↓ LlmService 判断：是否需要追问？生成下一轮问题 or 推进下一题
    ↓ 保存 AI 回复 t_chat_message（role=ASSISTANT）
    ↓ SSE 流式推送 AI 回复给前端
    ↓
（多轮对话循环...）
    ↓
用户/系统触发结束
POST /api/v1/interviews/{id}/end
    ↓ 更新 t_interview_session（status=COMPLETED）
    ↓ 发布异步评估任务
    ↓ 返回 reportId（状态=GENERATING）
    ↓
异步评估任务
    ↓ 遍历所有对话轮次
    ↓ 逐题调用 LLM 生成各维度分数
    ↓ 汇总生成综合报告 + 改进建议
    ↓ 存入 t_evaluation_report + t_dimension_score
    ↓ 调用 ResourceModule 生成个性化推荐
    ↓ 更新报告状态=COMPLETED
    ↓
前端轮询 GET /api/v1/reports/{id} 获取完整报告
```

### 5.2 RAG 知识检索流程

```
知识库构建（离线）：
t_kb_article.body_markdown → 文本分块（512 token，重叠 64）
    → Embedding API → 向量
    → 存入 Chroma（metadata：kb_node_id、article_id、code_path、position_codes）

在线检索：
用户回答文本（或对话上下文）
    → Embedding API → 查询向量
    → Chroma 相似度检索（Top-K），可按岗位、bread-crumb/code_path 过滤
    → 召回知识片段
    → 注入 LLM（评估 Prompt 或出题 Prompt）
```

---

## 6. 非功能性设计

### 6.1 安全性
- JWT 有效期 2 小时，Refresh Token 7 天
- API 接口统一鉴权（除 /auth/** 白名单）
- SQL 注入防御：MyBatis-Plus 参数化查询
- XSS 防御：前端输出转义 + CSP Header
- 敏感配置通过环境变量注入，不硬编码

### 6.2 性能
- Redis 缓存：面试题目列表、用户档案（TTL 10 分钟）
- LLM 调用采用 SSE 流式响应，减少首字节延迟
- 评估任务异步化，不阻塞面试结束响应
- 数据库关键字段建索引（user_id、session_id、position_code）

### 6.3 可扩展性
- AI 服务层抽象为接口，支持无缝切换 LLM 提供商
- 岗位通过数据库 + 枚举驱动，新增岗位只需加数据无需改代码
- 前端 API 层统一封装，后端接口变更影响范围可控

---

## 7. 部署架构

```
开发环境（本地）：
  前端 Vite Dev Server :5173
  后端 Spring Boot :8080
  MySQL :3306 （Docker）
  Redis :6379  （Docker）
  Chroma :8000 （Docker）

生产环境（单机 Docker Compose）：
  Nginx :80/:443
  Spring Boot JAR（容器）:8080
  MySQL 容器 :3306
  Redis 容器 :6379
  Chroma 容器 :8000
```
