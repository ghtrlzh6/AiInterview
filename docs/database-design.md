# 数据库设计

> 数据库名：`ai_interview`  
> 字符集：`utf8mb4`，排序规则：`utf8mb4_unicode_ci`  
> 所有表包含公共字段：`id`, `created_at`, `updated_at`, `is_deleted`

---

## ER 关系概览

```
t_user ──────────────┬── t_interview_session ──── t_interview_question
                     │          │
                     │          ├── t_chat_message
                     │          │
                     │          └── t_evaluation_report ─── t_dimension_score
                     │
                     └── t_user_recommendation ── t_learning_resource

t_position ──────────┬── t_question
                     └── t_knowledge_doc

t_question ──────────── t_question_tag（多对多：t_question_tag_rel）
```

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
 3);
```

---

## 3. 面试题目表（t_question）

```sql
CREATE TABLE t_question (
    id               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '题目ID',
    position_code    VARCHAR(30)  NOT NULL COMMENT '所属岗位编码',
    title            TEXT         NOT NULL COMMENT '题目标题（面试问题）',
    answer_reference TEXT         COMMENT '参考答案（优秀回答示例）',
    difficulty       TINYINT      NOT NULL DEFAULT 2 COMMENT '难度（1简单/2中等/3困难）',
    question_type    VARCHAR(20)  NOT NULL COMMENT '题型：TECH_KNOWLEDGE/PROJECT_DEEP/SCENARIO/BEHAVIOR',
    topic            VARCHAR(50)  DEFAULT '' COMMENT '知识点标签（如JVM、多线程）',
    follow_up_hints  JSON         COMMENT '追问提示词列表（JSON数组，AI追问参考）',
    source           VARCHAR(100) DEFAULT '' COMMENT '题目来源（企业/社区）',
    sort_order       INT DEFAULT 0 COMMENT '排序权重',
    created_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted       TINYINT(1)   NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_position_code (position_code),
    KEY idx_difficulty (difficulty),
    KEY idx_question_type (question_type),
    KEY idx_topic (topic)
) COMMENT='面试题目表';
```

**question_type 枚举值说明：**

| 值 | 含义 |
|----|------|
| `TECH_KNOWLEDGE` | 技术知识题（考察原理、概念） |
| `PROJECT_DEEP` | 项目经历深挖（结合简历追问） |
| `SCENARIO` | 场景设计题（如何设计一个XXX系统） |
| `BEHAVIOR` | 行为题（讲述一次XXX经历） |

---

## 4. 知识库文档表（t_knowledge_doc）

```sql
CREATE TABLE t_knowledge_doc (
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '文档ID',
    position_code VARCHAR(30)  NOT NULL COMMENT '所属岗位编码',
    title         VARCHAR(200) NOT NULL COMMENT '文档标题',
    content       LONGTEXT     NOT NULL COMMENT '文档内容（Markdown格式）',
    doc_type      VARCHAR(20)  NOT NULL COMMENT '文档类型：TECH_POINT/INTERVIEW_TIPS/ANSWER_EXAMPLE',
    topic         VARCHAR(50)  DEFAULT '' COMMENT '知识点分类',
    chroma_ids    JSON         COMMENT '向量化后在Chroma中的chunk ID列表',
    is_vectorized TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否已向量化入库',
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted    TINYINT(1)   NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_position_code (position_code),
    KEY idx_doc_type (doc_type),
    KEY idx_topic (topic)
) COMMENT='知识库文档表（RAG数据源）';
```

---

## 5. 面试会话表（t_interview_session）

```sql
CREATE TABLE t_interview_session (
    id               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '会话ID',
    user_id          BIGINT       NOT NULL COMMENT '用户ID',
    position_code    VARCHAR(30)  NOT NULL COMMENT '面试岗位编码',
    session_status   VARCHAR(20)  NOT NULL DEFAULT 'IN_PROGRESS' COMMENT '状态：IN_PROGRESS/COMPLETED/ABANDONED',
    input_mode       VARCHAR(10)  NOT NULL DEFAULT 'TEXT' COMMENT '输入模式：TEXT/VOICE',
    total_questions  INT          DEFAULT 0 COMMENT '本次面试题目总数',
    answered_count   INT          DEFAULT 0 COMMENT '已回答题目数',
    duration_seconds INT          DEFAULT 0 COMMENT '面试总时长（秒）',
    start_time       DATETIME     COMMENT '面试开始时间',
    end_time         DATETIME     COMMENT '面试结束时间',
    created_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted       TINYINT(1)   NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_user_id (user_id),
    KEY idx_position_code (position_code),
    KEY idx_session_status (session_status),
    KEY idx_user_status (user_id, session_status)
) COMMENT='面试会话表';
```

---

## 6. 面试会话题目表（t_interview_question）

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

## 7. 对话消息表（t_chat_message）

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

## 8. 评估报告表（t_evaluation_report）

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

## 9. 维度得分明细表（t_dimension_score）

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

## 10. 学习资源表（t_learning_resource）

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

## 11. 用户推荐记录表（t_user_recommendation）

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

## 12. 用户成长记录表（t_growth_record）

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

## 13. 系统配置表（t_system_config）

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

## 14. 完整建库脚本入口

建议将以上所有建表 SQL 整理到 `sql/init.sql`，在 Docker Compose 启动时自动初始化。执行顺序：

1. 建库
2. `t_user`（含初始 ADMIN 账号：admin / admin123456）
3. `t_position`（含初始化数据）
4. `t_question`
5. `t_knowledge_doc`
6. `t_interview_session`
7. `t_interview_question`
8. `t_chat_message`
9. `t_evaluation_report`
10. `t_dimension_score`
11. `t_learning_resource`
12. `t_user_recommendation`
13. `t_growth_record`
14. `t_system_config`（含初始化配置数据）
