# 数据库设计

> 数据库名：`ai_interview`  
> 字符集：`utf8mb4`，排序规则：`utf8mb4_unicode_ci`  
> 所有表包含公共字段：`id`, `created_at`, `updated_at`, `is_deleted`

---

## ER 关系概览

```
t_kb_node ─────┬── t_kb_article（整块 Markdown，挂载在「知识点」叶节点）
               │           └── Chroma（按 article 切块向量化）
               │
               └── t_question ── M:N ── （仅关联 TOPIC_POINT 叶节点）

t_kb_node ──（可选 FK）──── t_question.primary_kb_module_id（题干归属的知识「大模块」锚点）

t_coding_challenge —─（可选 FK）──── t_question.coding_challenge_id（BEHAVIOR=手撕题）

t_question ────────── t_question_kb_point（question_id ↔ kb_node_id）

t_user_resume ──┬── t_resume_project（解析出的项目条目）
                  └── （可选 FK）──── t_interview_session.resume_snapshot_id

t_position ────────── t_question

t_user ──────────────┬── t_interview_session ──── t_interview_question
                     │          │
                     │          ├── t_chat_message
                     │          │
                     │          ├── t_session_coding_submit（一道手撕题的代码提交快照）
                     │          │
                     │          └── t_evaluation_report ─── t_dimension_score
                     │
                     └── t_user_recommendation ── t_learning_resource
```

> **`t_knowledge_doc`（旧扁平知识库文档表）已由 `t_kb_node` + `t_kb_article` 替代**，本节不再列出建表 DDL；存量数据可按 `topic`/`position_code` 迁移到树上的叶节点文档。

---

## 1. 用户表（t_user）

```sql
CREATE TABLE t_user (
    id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    username     VARCHAR(50)  NOT NULL COMMENT '用户名（登录账号）',
    password     VARCHAR(255) NOT NULL COMMENT '密码（BCrypt 加密）',
    nickname     VARCHAR(50)  DEFAULT '' COMMENT '昵称',
    avatar_url   VARCHAR(500) DEFAULT '' COMMENT '头像 URL',
    email        VARCHAR(100) DEFAULT '' COMMENT '邮箱',
    school       VARCHAR(100) DEFAULT '' COMMENT '学校',
    major        VARCHAR(100) DEFAULT '' COMMENT '专业',
    role         VARCHAR(10)  NOT NULL DEFAULT 'USER' COMMENT '角色：USER / ADMIN',
    target_position_code VARCHAR(30) DEFAULT '' COMMENT '目标岗位编码（对应 t_position.code）',
    total_interviews INT DEFAULT 0 COMMENT '累计面试次数',
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted   TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '逻辑删除（0正常/1删除）',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username),
    KEY idx_target_position (target_position_code)
) COMMENT='用户表';
```

---

## 2. 岗位表（t_position）

```sql
CREATE TABLE t_position (
    id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '岗位ID',
    code         VARCHAR(30)  NOT NULL COMMENT '岗位编码（唯一标识，如 JAVA_BACKEND）',
    name         VARCHAR(50)  NOT NULL COMMENT '岗位名称（如 Java后端开发工程师）',
    description  TEXT         COMMENT '岗位描述',
    tech_stack   JSON         COMMENT '核心技术栈（JSON数组）',
    icon_url     VARCHAR(500) DEFAULT '' COMMENT '岗位图标',
    sort_order   INT DEFAULT 0 COMMENT '排序',
    is_active    TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '是否启用',
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted   TINYINT(1)   NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_code (code)
) COMMENT='岗位表';

-- 初始化数据
INSERT INTO t_position (code, name, description, tech_stack, sort_order) VALUES
('JAVA_BACKEND', 'Java后端开发工程师', 
 '负责服务端业务逻辑开发，熟悉 Java 生态和分布式架构',
 '["Java","Spring Boot","Spring Cloud","MySQL","Redis","JVM","设计模式","多线程","Kafka","MyBatis"]',
 1),
('WEB_FRONTEND', 'Web前端开发工程师',
 '负责 Web 端界面开发，熟悉现代前端框架和工程化体系',
 '["HTML","CSS","JavaScript","TypeScript","Vue3","React","Webpack","Vite","性能优化","浏览器原理"]',
 2),
('PYTHON_ALGO', 'Python算法工程师',
 '负责算法研究与实现，熟悉机器学习和数据结构',
 '["Python","数据结构与算法","机器学习","深度学习","NumPy","Pandas","LeetCode","系统设计"]',
 3),
('GAME_CLIENT', '游戏客户端开发工程师',
 '负责游戏客户端功能开发，熟悉游戏引擎架构与渲染管线',
 '["C++","C#","Unity","Unreal Engine","游戏引擎","图形渲染","内存管理","网络同步","帧同步","性能优化","ECS","Lua"]',
 4);
```

---

## 3. 知识类目树节点（t_kb_node）

> **设计目标**：用「无限层级」的树表达：计算机通识 → 大模块（如编程语言）→ 子模块（如 C++）→ … → **知识点叶节点**；仅 **叶节点**（`node_type=TOPIC_POINT`）挂载正文（见 `t_kb_article`）。中间节点（`GROUP`）用于网页侧目录展示与 AI 按模块理解考纲。  
> **约定**：建议插入一条「虚拟根」`id=1, parent_id=NULL, title='知识库根', slug='root', node_type='GROUP'`，所有一级栏目（如「计算机通识基础」）的 `parent_id=1`，便于树查询。

```sql
CREATE TABLE t_kb_node (
    id               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '节点ID',
    parent_id        BIGINT                DEFAULT NULL COMMENT '父节点ID，NULL 仅允许用于虚拟根',
    title            VARCHAR(200) NOT NULL COMMENT '栏目/知识点标题（与用户示例中的各级名称对应）',
    slug             VARCHAR(120) NOT NULL COMMENT '同级 URL 友好标识（同 parent_id 下唯一）',
    code_path        VARCHAR(600) DEFAULT '' COMMENT '物化路径，如 /cs-foundations/programming/cpp/memory（便于检索与面包屑）',
    depth            INT          NOT NULL DEFAULT 0 COMMENT '根为0',
    sort_order       INT          NOT NULL DEFAULT 0 COMMENT '同级排序（小在前）',
    node_type        VARCHAR(30)  NOT NULL COMMENT 'GROUP=仅目录容器；TOPIC_POINT=知识点叶节点（可挂正文）',
    position_codes   JSON                  DEFAULT NULL COMMENT '适用岗位编码数组；NULL或空数组=全系通用面试知识',
    summary_excerpt  VARCHAR(600) DEFAULT '' COMMENT '树上展示的短摘要（可选）',
    is_active        TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '是否在前台知识库栏目展示',
    created_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted       TINYINT(1)   NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_parent_id (parent_id),
    KEY idx_node_type (node_type),
    UNIQUE KEY uk_parent_slug (parent_id, slug),
    CONSTRAINT fk_kb_node_parent FOREIGN KEY (parent_id) REFERENCES t_kb_node(id)
) COMMENT='知识库类目树节点（人机共享同一棵树）';
```

**node_type 说明：**

| 值 | 含义 |
|----|------|
| `GROUP` | 仅目录层级，不挂载 `t_kb_article`（可提供 `summary_excerpt` 导读） |
| `TOPIC_POINT` | **知识点**：必须 **0或1篇** `t_kb_article`（整块资料）；向量检索粒度以正文切块为准 |

---

## 4. 知识正文块（t_kb_article）

> **整块内容**：由运营/团队在库里维护一篇 Markdown（或按需拆多个块：`display_order`）。RAG **切分 Embedding** 时以 `body_markdown` 为准写入 Chroma，元数据中携带 `kb_node_id`、`article_id`。

```sql
CREATE TABLE t_kb_article (
    id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '正文块ID',
    kb_node_id      BIGINT       NOT NULL COMMENT '必须为 node_type=TOPIC_POINT 的节点',
    title           VARCHAR(200) NOT NULL DEFAULT '' COMMENT '篇名（可与节点标题略有不同）',
    body_markdown   LONGTEXT     NOT NULL COMMENT '整块教学内容（Markdown）',
    display_order   INT          NOT NULL DEFAULT 0 COMMENT '同节点多块正文时的排序',
    chroma_ids      JSON                  DEFAULT NULL COMMENT '向量化后 chunk 对应的 Chroma 记录',
    is_vectorized   TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否完成向量化',
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted      TINYINT(1)   NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_kb_node_id (kb_node_id),
    CONSTRAINT fk_kb_article_node FOREIGN KEY (kb_node_id) REFERENCES t_kb_node(id)
) COMMENT='知识点下的整块讲义/范例（人机阅读 + RAG 源）';
```

---

## 5. LeetCode / 手撕编程题池（t_coding_challenge）

> **`BEHAVIOR`（手撕编程）**：从 Hot100 等你方维护的题目池中 **每场面试抽一道**；题干存在本表，`t_question` 通过 `coding_challenge_id` 引用。不做在线判题时可仅保存用户提交的代码快照，由 **LLM 评审**。

```sql
CREATE TABLE t_coding_challenge (
    id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '题目池ID',
    external_ref    VARCHAR(64)  DEFAULT '' COMMENT '如 LeetCode 题号/别名（Hot100-003）',
    title           VARCHAR(300) NOT NULL COMMENT '题干标题',
    problem_md      LONGTEXT     NOT NULL COMMENT '题干描述 Markdown（constraints、示例可复制）',
    difficulty      TINYINT      NOT NULL DEFAULT 2 COMMENT '1简单 2中等 3困难',
    canonical_tags  JSON                  DEFAULT NULL COMMENT '标签数组，如["数组","哈希"]',
    answer_hint_md  LONGTEXT COMMENT '内部参考解法要点（不向考生展示）',
    is_active       TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '是否可抽取',
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted      TINYINT(1)   NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_external_ref (external_ref),
    KEY idx_difficulty (difficulty)
) COMMENT='手撕编程题池（对齐 LeetCode Hot100 等你方导入的数据）';
```

---

## 6. 面试题目表（t_question）

> **与知识树**：每题仍可保留 `topic` **冗余标签**便于旧接口；结构化关联通过 `primary_kb_module_id`（所属大模块锚点）+ `t_question_kb_point`（0~多个具体知识点）。

```sql
CREATE TABLE t_question (
    id                    BIGINT       NOT NULL AUTO_INCREMENT COMMENT '题目ID',
    position_code         VARCHAR(30)  NOT NULL COMMENT '所属岗位编码',
    primary_kb_module_id  BIGINT       DEFAULT NULL COMMENT '题干归属的知识模块锚点（t_kb_node 上任意祖先或 GROUP，例：数据结构）',
    coding_challenge_id   BIGINT       DEFAULT NULL COMMENT '题型为 BEHAVIOR 且为手撕时关联 t_coding_challenge',
    binding_session_id    BIGINT       DEFAULT NULL COMMENT '非空：本条仅用于本场面试可见（会话专属题，常见于 AI 生成的项目深挖）',
    title                 TEXT         NOT NULL COMMENT '题干',
    answer_reference      TEXT         COMMENT '参考答案/要点（不向考生端列表泄露）',
    difficulty            TINYINT      NOT NULL DEFAULT 2 COMMENT '难度（1简单/2中等/3困难）',
    question_type         VARCHAR(25)  NOT NULL COMMENT 'TECH_KNOWLEDGE/PROJECT_DEEP/SCENARIO/BEHAVIOR',
    topic                 VARCHAR(100) DEFAULT '' COMMENT '冗余展示标签（与模块/知识点可读性一致时可手填）',
    follow_up_hints       JSON         COMMENT '追问提示词列表（JSON数组，AI追问参考）',
    source                VARCHAR(100) DEFAULT '' COMMENT '来源：MANUAL/AI_TECH_SCENARIO/AI_PROJECT/LC_HOT100 …',
    generation_meta       JSON         DEFAULT NULL COMMENT 'AI出题时记录的模块路径、prompt、简历项目ID等',
    sort_order            INT DEFAULT 0 COMMENT '排序权重',
    created_at            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted            TINYINT(1)   NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_position_code (position_code),
    KEY idx_difficulty (difficulty),
    KEY idx_question_type (question_type),
    KEY idx_topic (topic),
    KEY idx_primary_module (primary_kb_module_id),
    KEY idx_binding_session (binding_session_id),
    CONSTRAINT fk_question_primary_module FOREIGN KEY (primary_kb_module_id) REFERENCES t_kb_node(id),
    CONSTRAINT fk_question_coding FOREIGN KEY (coding_challenge_id) REFERENCES t_coding_challenge(id)
) COMMENT='题库主表（与知识树及手撕题目池可选关联）';
```

**question_type 枚举值说明（在你们方案上的语义对齐）：**

| 值 | 含义 |
|----|------|
| `TECH_KNOWLEDGE` | 技术原理/基础题 |
| `PROJECT_DEEP` | 项目深挖题（题干常由简历解析 → AI，按知识点生成；多存为 `binding_session_id`） |
| `SCENARIO` | 场景设计与开放题 |
| `BEHAVIOR` | **手撕编程题**（本节设计：从 Hot100 池中抽一道 + 内置 IDE 提交代码 + AI 评语与追问）；**区别于**传统 STAR 行为面 |

---

## 7. 题目—知识点关联（t_question_kb_point）

> **多对多**：同一题可不关联知识点，也可挂 **多条** TOPIC_POINT 节点。

```sql
CREATE TABLE t_question_kb_point (
    id               BIGINT NOT NULL AUTO_INCREMENT,
    question_id      BIGINT NOT NULL,
    kb_node_id       BIGINT NOT NULL COMMENT '须为 TOPIC_POINT 叶节点',
    relevance_weight DECIMAL(5,4) DEFAULT 1.0000 COMMENT '可扩展：主绑定=1，弱相关=0.5',
    created_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted       TINYINT(1) NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_q_kb (question_id, kb_node_id),
    KEY idx_question_id (question_id),
    KEY idx_kb_node_id (kb_node_id),
    CONSTRAINT fk_qkp_question FOREIGN KEY (question_id) REFERENCES t_question(id),
    CONSTRAINT fk_qkp_kb FOREIGN KEY (kb_node_id) REFERENCES t_kb_node(id)
) COMMENT='题库题目关联到的具体知识点叶节点（0或多个）';
```

---

## 8. 简历快照（t_user_resume）

> 用户在**开始项目类面试前**上传 PDF；解析后的文本与原文 URL 存本表，供 AI 生成 `PROJECT_DEEP` 题。

```sql
CREATE TABLE t_user_resume (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    user_id         BIGINT       NOT NULL,
    file_url        VARCHAR(500) NOT NULL COMMENT '对象存储或本地路径',
    file_name       VARCHAR(255) DEFAULT '' COMMENT '原始文件名',
    parse_status    VARCHAR(20)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/SUCCESS/FAILED',
    resume_text_md  LONGTEXT COMMENT 'PDF 抽取后的全文（Markdown/纯文本）',
    remark          VARCHAR(500) DEFAULT '' COMMENT '解析失败原因等',
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted      TINYINT(1)   NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_user_id (user_id),
    CONSTRAINT fk_resume_user FOREIGN KEY (user_id) REFERENCES t_user(id)
) COMMENT='用户简历上传与解析快照';
```

---

## 9. 简历结构化项目条目（t_resume_project）

```sql
CREATE TABLE t_resume_project (
    id                   BIGINT NOT NULL AUTO_INCREMENT,
    resume_id            BIGINT NOT NULL,
    project_name         VARCHAR(200) NOT NULL,
    summary_md           TEXT COMMENT '项目简介（可由 LLM 从简历截取）',
    tech_stack_tokens    JSON   COMMENT '用到的技术关键字',
    kb_point_ids_hint    JSON   COMMENT 'LLM估计关联的知识点 TOPIC_POINT id（可空）',
    sort_order           INT NOT NULL DEFAULT 0,
    created_at           DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted           TINYINT(1) NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_resume_id (resume_id),
    CONSTRAINT fk_rp_resume FOREIGN KEY (resume_id) REFERENCES t_user_resume(id)
) COMMENT='从简历中提取的项目块，便于按项目出题';
```

---

## 10. 面试会话表（t_interview_session）

```sql
CREATE TABLE t_interview_session (
    id                 BIGINT       NOT NULL AUTO_INCREMENT COMMENT '会话ID',
    user_id            BIGINT       NOT NULL COMMENT '用户ID',
    resume_snapshot_id BIGINT       DEFAULT NULL COMMENT '本场面试绑定的简历快照（项目深挖题依赖；可空）',
    position_code      VARCHAR(30)  NOT NULL COMMENT '面试岗位编码',
    session_status     VARCHAR(20)  NOT NULL DEFAULT 'IN_PROGRESS' COMMENT '状态：IN_PROGRESS/COMPLETED/ABANDONED',
    input_mode         VARCHAR(10)  NOT NULL DEFAULT 'TEXT' COMMENT '输入模式：TEXT/VOICE',
    total_questions    INT          DEFAULT 0 COMMENT '本次面试题目总数',
    answered_count     INT          DEFAULT 0 COMMENT '已回答题目数',
    duration_seconds   INT          DEFAULT 0 COMMENT '面试总时长（秒）',
    start_time         DATETIME     COMMENT '面试开始时间',
    end_time           DATETIME     COMMENT '面试结束时间',
    created_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted         TINYINT(1)   NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_user_id (user_id),
    KEY idx_resume_snapshot (resume_snapshot_id),
    KEY idx_position_code (position_code),
    KEY idx_session_status (session_status),
    KEY idx_user_status (user_id, session_status),
    CONSTRAINT fk_session_resume FOREIGN KEY (resume_snapshot_id) REFERENCES t_user_resume(id)
) COMMENT='面试会话表';
```

---

## 11. 面试会话题目表（t_interview_question）

```sql
CREATE TABLE t_interview_question (
    id              BIGINT   NOT NULL AUTO_INCREMENT COMMENT 'ID',
    session_id      BIGINT   NOT NULL COMMENT '面试会话ID',
    question_id     BIGINT   NOT NULL COMMENT '题目ID（关联 t_question）',
    question_order  INT      NOT NULL COMMENT '题目序号（第几题）',
    is_answered     TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已作答',
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted      TINYINT(1) NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_session_id (session_id),
    UNIQUE KEY uk_session_question (session_id, question_order)
) COMMENT='面试会话与题目的关联表（记录本次面试的题目序列）';
```

---

## 12. 对话消息表（t_chat_message）

```sql
CREATE TABLE t_chat_message (
    id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '消息ID',
    session_id   BIGINT       NOT NULL COMMENT '面试会话ID',
    question_id  BIGINT       DEFAULT NULL COMMENT '关联的题目ID（NULL表示非题目相关消息）',
    role         VARCHAR(10)  NOT NULL COMMENT '角色：USER/ASSISTANT/SYSTEM',
    content      TEXT         NOT NULL COMMENT '消息内容（文字）',
    audio_url    VARCHAR(500) DEFAULT '' COMMENT '语音消息原始音频URL（用户语音输入时记录）',
    message_type VARCHAR(20)  NOT NULL DEFAULT 'NORMAL' COMMENT '消息类型：NORMAL/QUESTION/FOLLOW_UP/EVALUATION/CLOSING',
    token_count  INT          DEFAULT 0 COMMENT '该消息的 token 数量（用于成本统计）',
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted   TINYINT(1)   NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_session_id (session_id),
    KEY idx_session_question (session_id, question_id),
    KEY idx_role (role)
) COMMENT='面试对话消息表';
```

---

## 13. 手撕题代码提交（t_session_coding_submit）

> **BEHAVIOR / 手撕**：用户在内置 IDE 中提交代码（**不做 OJ 自测**）。每次提交落库，评估阶段由 LLM 判断思路、边界与明显语法/逻辑问题，并驱动追问（如「为何选该算法」）。

```sql
CREATE TABLE t_session_coding_submit (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    session_id      BIGINT       NOT NULL,
    question_id     BIGINT       NOT NULL COMMENT '本场手撕题对应的 t_question.id',
    code_body       MEDIUMTEXT   NOT NULL COMMENT '用户提交的代码全文',
    language        VARCHAR(32)  NOT NULL DEFAULT 'cpp' COMMENT 'IDE 所选语言',
    submit_order    INT          NOT NULL DEFAULT 1 COMMENT '同一题多次提交递增',
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted      TINYINT(1)   NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_session_question (session_id, question_id),
    CONSTRAINT fk_scs_session FOREIGN KEY (session_id) REFERENCES t_interview_session(id),
    CONSTRAINT fk_scs_question FOREIGN KEY (question_id) REFERENCES t_question(id)
) COMMENT='手撕编程题提交快照（无判题机时以 LLM 评审为主）';
```

---

## 14. 评估报告表（t_evaluation_report）

```sql
CREATE TABLE t_evaluation_report (
    id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '报告ID',
    session_id      BIGINT       NOT NULL COMMENT '关联的面试会话ID',
    user_id         BIGINT       NOT NULL COMMENT '用户ID',
    position_code   VARCHAR(30)  NOT NULL COMMENT '岗位编码',
    report_status   VARCHAR(20)  NOT NULL DEFAULT 'GENERATING' COMMENT '状态：GENERATING/COMPLETED/FAILED',
    overall_score   DECIMAL(5,2) DEFAULT NULL COMMENT '综合得分（0-100）',
    tech_score      DECIMAL(5,2) DEFAULT NULL COMMENT '技术能力得分（0-100）',
    expression_score DECIMAL(5,2) DEFAULT NULL COMMENT '语言表达得分（0-100）',
    logic_score     DECIMAL(5,2) DEFAULT NULL COMMENT '逻辑思维得分（0-100）',
    depth_score     DECIMAL(5,2) DEFAULT NULL COMMENT '知识深度得分（0-100）',
    confidence_score DECIMAL(5,2) DEFAULT NULL COMMENT '自信度得分（0-100）',
    summary         TEXT         COMMENT '报告总结（LLM生成，Markdown格式）',
    highlights      JSON         COMMENT '亮点列表（JSON字符串数组）',
    weaknesses      JSON         COMMENT '不足列表（JSON字符串数组）',
    suggestions     JSON         COMMENT '改进建议列表（JSON字符串数组）',
    share_token     VARCHAR(64)  DEFAULT '' COMMENT '分享链接 token（空表示未分享）',
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted      TINYINT(1)   NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_session_id (session_id),
    KEY idx_user_id (user_id),
    KEY idx_user_position (user_id, position_code),
    KEY idx_report_status (report_status)
) COMMENT='评估报告表';
```

---

## 15. 维度得分明细表（t_dimension_score）

```sql
CREATE TABLE t_dimension_score (
    id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT 'ID',
    report_id    BIGINT       NOT NULL COMMENT '关联报告ID',
    session_id   BIGINT       NOT NULL COMMENT '面试会话ID',
    question_id  BIGINT       NOT NULL COMMENT '题目ID',
    question_order INT        NOT NULL COMMENT '该题序号',
    tech_score   DECIMAL(5,2) COMMENT '本题技术得分',
    logic_score  DECIMAL(5,2) COMMENT '本题逻辑得分',
    depth_score  DECIMAL(5,2) COMMENT '本题深度得分',
    comment      TEXT         COMMENT '本题点评（LLM生成）',
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted   TINYINT(1)   NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_report_id (report_id),
    KEY idx_session_id (session_id)
) COMMENT='题目维度得分明细表';
```

---

## 16. 学习资源表（t_learning_resource）

```sql
CREATE TABLE t_learning_resource (
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '资源ID',
    position_code VARCHAR(30)  DEFAULT '' COMMENT '适用岗位（空表示通用）',
    title         VARCHAR(200) NOT NULL COMMENT '资源标题',
    description   TEXT         COMMENT '资源描述',
    resource_type VARCHAR(20)  NOT NULL COMMENT '类型：ARTICLE/QUESTION/VIDEO/BOOK',
    url           VARCHAR(500) NOT NULL COMMENT '资源链接',
    topic         VARCHAR(50)  DEFAULT '' COMMENT '知识点标签',
    difficulty    TINYINT      DEFAULT 2 COMMENT '难度（1简单/2中等/3困难）',
    quality_score INT DEFAULT 0 COMMENT '质量评分（用于推荐排序）',
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted    TINYINT(1)   NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_position_code (position_code),
    KEY idx_resource_type (resource_type),
    KEY idx_topic (topic)
) COMMENT='学习资源表';
```

---

## 17. 用户推荐记录表（t_user_recommendation）

```sql
CREATE TABLE t_user_recommendation (
    id           BIGINT   NOT NULL AUTO_INCREMENT COMMENT 'ID',
    user_id      BIGINT   NOT NULL COMMENT '用户ID',
    report_id    BIGINT   DEFAULT NULL COMMENT '关联的报告ID（触发推荐的来源报告）',
    resource_id  BIGINT   NOT NULL COMMENT '推荐的资源ID',
    reason       VARCHAR(200) DEFAULT '' COMMENT '推荐理由（简要说明）',
    is_clicked   TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已点击',
    is_helpful   TINYINT(1) DEFAULT NULL COMMENT '是否有帮助（用户反馈：NULL未反馈/1有帮助/0没帮助）',
    created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted   TINYINT(1) NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_user_id (user_id),
    KEY idx_report_id (report_id),
    KEY idx_user_created (user_id, created_at)
) COMMENT='用户推荐记录表';
```

---

## 18. 用户成长记录表（t_growth_record）

```sql
CREATE TABLE t_growth_record (
    id               BIGINT       NOT NULL AUTO_INCREMENT COMMENT 'ID',
    user_id          BIGINT       NOT NULL COMMENT '用户ID',
    report_id        BIGINT       NOT NULL COMMENT '关联报告ID',
    session_id       BIGINT       NOT NULL COMMENT '关联会话ID',
    position_code    VARCHAR(30)  NOT NULL COMMENT '岗位编码',
    overall_score    DECIMAL(5,2) COMMENT '综合得分',
    tech_score       DECIMAL(5,2) COMMENT '技术得分',
    expression_score DECIMAL(5,2) COMMENT '表达得分',
    logic_score      DECIMAL(5,2) COMMENT '逻辑得分',
    depth_score      DECIMAL(5,2) COMMENT '深度得分',
    confidence_score DECIMAL(5,2) COMMENT '自信度得分',
    record_date      DATE         NOT NULL COMMENT '记录日期',
    created_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted       TINYINT(1)   NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_user_id (user_id),
    KEY idx_user_position_date (user_id, position_code, record_date),
    KEY idx_report_id (report_id)
) COMMENT='用户能力成长记录表（用于可视化成长曲线）';
```

---

## 19. 系统配置表（t_system_config）

> 管理员通过后台界面修改此表来控制 AI 服务参数和 Prompt 模板，无需重启应用。

```sql
CREATE TABLE t_system_config (
    id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT 'ID',
    config_key   VARCHAR(100) NOT NULL COMMENT '配置键（唯一）',
    config_value TEXT         NOT NULL COMMENT '配置值',
    config_type  VARCHAR(20)  NOT NULL DEFAULT 'STRING' COMMENT '值类型：STRING/JSON/TEXT（TEXT用于Prompt）',
    description  VARCHAR(200) DEFAULT '' COMMENT '配置说明',
    is_sensitive TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否敏感（1=API Key 等，查询时掩码处理）',
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted   TINYINT(1)   NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_config_key (config_key)
) COMMENT='系统配置表（管理员可修改AI参数和Prompt）';

-- 初始化配置数据
INSERT INTO t_system_config (config_key, config_value, config_type, description, is_sensitive) VALUES
-- LLM 服务配置
('ai.llm.provider',      'deepseek',                          'STRING', 'LLM提供商：deepseek / tongyi',          0),
('ai.llm.base-url',      'https://api.deepseek.com/v1',       'STRING', 'LLM API 基础地址',                      0),
('ai.llm.model',         'deepseek-chat',                     'STRING', '对话模型名称',                          0),
('ai.llm.embed-model',   'deepseek-embed',                    'STRING', 'Embedding 模型名称',                    0),
('ai.llm.api-key',       '',                                  'STRING', 'LLM API Key（部署时填写）',              1),
('ai.llm.temperature',   '0.7',                               'STRING', '生成温度（0.0-1.0）',                   0),
('ai.llm.max-tokens',    '4096',                              'STRING', '单次最大输出 token 数',                 0),
-- Prompt 模板（TEXT 类型，支持多行）
('prompt.interview.system',   '你是一位专业严肃的技术面试官，正在对{positionName}岗位的候选人进行面试。
面试共{totalQuestions}题，当前是第{currentOrder}题。
规则：
1. 如果候选人的回答不完整或存在明显错误，可以追问，但同一题最多追问2次后必须推进下一题
2. 追问时请直接针对回答的不足点发问
3. 推进下一题时，自然过渡，不要生硬
4. 所有回复用JSON格式输出：{"action":"follow_up|next_question|end","content":"..."}
当前题目：{questionTitle}',
                              'TEXT',  '面试官系统提示词模板', 0),
('prompt.evaluation.question','请根据以下信息，对候选人的回答进行评分：
岗位：{positionName}
题目：{questionTitle}
参考答案要点：{answerReference}
候选人回答：{userAnswer}

请从以下三个维度评分（0-100分）并给出点评，以JSON格式输出：
{"tech_score":分数,"logic_score":分数,"depth_score":分数,"comment":"点评内容"}',
                              'TEXT',  '逐题评分提示词模板',   0),
('prompt.evaluation.final',   '以下是候选人本次面试的各题评分汇总：
{scoreSummary}

请生成一份综合评估报告，以JSON格式输出：
{"overall_score":综合分,"expression_score":表达分,"confidence_score":自信度分,"summary":"总结（Markdown格式）","highlights":["亮点1","亮点2"],"weaknesses":["不足1","不足2"],"suggestions":["建议1","建议2"]}',
                              'TEXT',  '综合报告提示词模板',   0);
```

**config_key 命名约定：**
- `ai.llm.*` — LLM 服务配置
- `ai.asr.*` — 语音识别配置
- `prompt.*` — Prompt 模板
- `system.*` — 系统参数（如面试默认题目数量）

> **安全说明**：`is_sensitive=1` 的配置（如 API Key）在 GET 接口中返回值掩码为 `****`，仅 PUT 时可覆盖写入。

---

## 20. 完整建库脚本入口

建议将以上所有建表 SQL 整理到 `sql/init.sql`，在 Docker Compose 启动时自动初始化。执行顺序（**按外键依赖**）：

1. 建库 `ai_interview`
2. `t_user`（含初始 ADMIN：admin / admin123456）
3. `t_position`（含初始化数据）
4. `t_kb_node`（含虚拟根 + 一级栏目种子，可选）
5. `t_kb_article`
6. `t_coding_challenge`（Hot100 等你方导入）
7. `t_question`（依赖 `t_kb_node`、`t_coding_challenge`）
8. `t_question_kb_point`
9. `t_user_resume` → `t_resume_project`
10. `t_interview_session`（依赖 `t_user`、`t_user_resume` 可选）
11. `t_interview_question`
12. `t_chat_message`
13. `t_session_coding_submit`
14. `t_evaluation_report`
15. `t_dimension_score`
16. `t_learning_resource`
17. `t_user_recommendation`
18. `t_growth_record`
19. `t_system_config`（含初始化配置数据）

> **说明**：若存在历史 `t_knowledge_doc` 表，先迁移到 `t_kb_node` + `t_kb_article` 后再删表，避免与 `t_question` 外键冲突。
