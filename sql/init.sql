-- AI 模拟面试平台 - 完整建库脚本
-- 字符集: utf8mb4 | MySQL 8.0+

CREATE DATABASE IF NOT EXISTS ai_interview
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE ai_interview;

-- ==================== 1. 用户表 ====================
CREATE TABLE IF NOT EXISTS t_user (
    id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    username     VARCHAR(50)  NOT NULL COMMENT '用户名',
    password     VARCHAR(255) NOT NULL COMMENT '密码 BCrypt',
    nickname     VARCHAR(50)  DEFAULT '' COMMENT '昵称',
    avatar_url   VARCHAR(500) DEFAULT '' COMMENT '头像',
    email        VARCHAR(100) DEFAULT '' COMMENT '邮箱',
    school       VARCHAR(100) DEFAULT '' COMMENT '学校',
    major        VARCHAR(100) DEFAULT '' COMMENT '专业',
    role         VARCHAR(10)  NOT NULL DEFAULT 'USER' COMMENT 'USER/ADMIN',
    target_position_code VARCHAR(30) DEFAULT '' COMMENT '目标岗位',
    total_interviews INT DEFAULT 0 COMMENT '累计面试次数',
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted   TINYINT(1)   NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username),
    KEY idx_target_position (target_position_code)
) COMMENT='用户表';

-- ==================== 2. 岗位表 ====================
CREATE TABLE IF NOT EXISTS t_position (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    code         VARCHAR(30)  NOT NULL,
    name         VARCHAR(50)  NOT NULL,
    description  TEXT,
    tech_stack   JSON,
    icon_url     VARCHAR(500) DEFAULT '',
    sort_order   INT DEFAULT 0,
    is_active    TINYINT(1)   NOT NULL DEFAULT 1,
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted   TINYINT(1)   NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_code (code)
) COMMENT='岗位表';

-- ==================== 3. 知识库节点 ====================
CREATE TABLE IF NOT EXISTS t_kb_node (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    parent_id        BIGINT                DEFAULT NULL,
    title            VARCHAR(200) NOT NULL,
    slug             VARCHAR(120) NOT NULL,
    code_path        VARCHAR(600) DEFAULT '',
    depth            INT          NOT NULL DEFAULT 0,
    sort_order       INT          NOT NULL DEFAULT 0,
    node_type        VARCHAR(30)  NOT NULL COMMENT 'GROUP/TOPIC_POINT',
    position_codes   JSON                  DEFAULT NULL,
    summary_excerpt  VARCHAR(600) DEFAULT '',
    is_active        TINYINT(1)   NOT NULL DEFAULT 1,
    created_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted       TINYINT(1)   NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_parent_id (parent_id),
    KEY idx_node_type (node_type),
    UNIQUE KEY uk_parent_slug (parent_id, slug)
) COMMENT='知识库类目树';

-- ==================== 4. 知识正文 ====================
CREATE TABLE IF NOT EXISTS t_kb_article (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    kb_node_id      BIGINT       NOT NULL,
    title           VARCHAR(200) NOT NULL DEFAULT '',
    body_markdown   LONGTEXT     NOT NULL,
    display_order   INT          NOT NULL DEFAULT 0,
    chroma_ids      JSON                  DEFAULT NULL,
    is_vectorized   TINYINT(1)   NOT NULL DEFAULT 0,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted      TINYINT(1)   NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_kb_node_id (kb_node_id)
) COMMENT='知识点正文';

-- ==================== 5. 手撕题池 ====================
CREATE TABLE IF NOT EXISTS t_coding_challenge (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    external_ref    VARCHAR(64)  DEFAULT '',
    title           VARCHAR(300) NOT NULL,
    problem_md      LONGTEXT     NOT NULL,
    difficulty      TINYINT      NOT NULL DEFAULT 2,
    canonical_tags  JSON                  DEFAULT NULL,
    answer_hint_md  LONGTEXT,
    is_active       TINYINT(1)   NOT NULL DEFAULT 1,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted      TINYINT(1)   NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_external_ref (external_ref),
    KEY idx_difficulty (difficulty)
) COMMENT='手撕编程题池';

-- ==================== 6. 题库 ====================
CREATE TABLE IF NOT EXISTS t_question (
    id                    BIGINT       NOT NULL AUTO_INCREMENT,
    position_code         VARCHAR(30)  NOT NULL,
    primary_kb_module_id  BIGINT       DEFAULT NULL,
    coding_challenge_id   BIGINT       DEFAULT NULL,
    binding_session_id    BIGINT       DEFAULT NULL,
    title                 TEXT         NOT NULL,
    answer_reference      TEXT,
    difficulty            TINYINT      NOT NULL DEFAULT 2,
    question_type         VARCHAR(25)  NOT NULL,
    topic                 VARCHAR(100) DEFAULT '',
    follow_up_hints       JSON,
    source                VARCHAR(100) DEFAULT '',
    generation_meta       JSON         DEFAULT NULL,
    sort_order            INT DEFAULT 0,
    created_at            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted            TINYINT(1)   NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_position_code (position_code),
    KEY idx_difficulty (difficulty),
    KEY idx_question_type (question_type),
    KEY idx_topic (topic),
    KEY idx_primary_module (primary_kb_module_id),
    KEY idx_binding_session (binding_session_id)
) COMMENT='面试题目';

-- ==================== 7. 题目-知识点关联 ====================
CREATE TABLE IF NOT EXISTS t_question_kb_point (
    id               BIGINT NOT NULL AUTO_INCREMENT,
    question_id      BIGINT NOT NULL,
    kb_node_id       BIGINT NOT NULL,
    relevance_weight DECIMAL(5,4) DEFAULT 1.0000,
    created_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted       TINYINT(1) NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_q_kb (question_id, kb_node_id),
    KEY idx_question_id (question_id),
    KEY idx_kb_node_id (kb_node_id)
) COMMENT='题目知识点关联';

-- ==================== 8. 简历 ====================
CREATE TABLE IF NOT EXISTS t_user_resume (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    user_id         BIGINT       NOT NULL,
    file_url        VARCHAR(500) NOT NULL,
    file_name       VARCHAR(255) DEFAULT '',
    parse_status    VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    resume_text_md  LONGTEXT,
    remark          VARCHAR(500) DEFAULT '',
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted      TINYINT(1)   NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_user_id (user_id)
) COMMENT='用户简历';

-- ==================== 9. 简历项目 ====================
CREATE TABLE IF NOT EXISTS t_resume_project (
    id                   BIGINT NOT NULL AUTO_INCREMENT,
    resume_id            BIGINT NOT NULL,
    project_name         VARCHAR(200) NOT NULL,
    summary_md           TEXT,
    tech_stack_tokens    JSON,
    kb_point_ids_hint    JSON,
    sort_order           INT NOT NULL DEFAULT 0,
    created_at           DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted           TINYINT(1) NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_resume_id (resume_id)
) COMMENT='简历项目条目';

-- ==================== 10. 面试会话 ====================
CREATE TABLE IF NOT EXISTS t_interview_session (
    id                 BIGINT       NOT NULL AUTO_INCREMENT,
    user_id            BIGINT       NOT NULL,
    resume_snapshot_id BIGINT       DEFAULT NULL,
    position_code      VARCHAR(30)  NOT NULL,
    session_status     VARCHAR(20)  NOT NULL DEFAULT 'IN_PROGRESS',
    input_mode         VARCHAR(10)  NOT NULL DEFAULT 'TEXT',
    total_questions    INT          DEFAULT 0,
    answered_count     INT          DEFAULT 0,
    duration_seconds   INT          DEFAULT 0,
    start_time         DATETIME,
    end_time           DATETIME,
    created_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted         TINYINT(1)   NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_user_id (user_id),
    KEY idx_resume_snapshot (resume_snapshot_id),
    KEY idx_position_code (position_code),
    KEY idx_session_status (session_status),
    KEY idx_user_status (user_id, session_status)
) COMMENT='面试会话';

-- ==================== 11. 会话题目 ====================
CREATE TABLE IF NOT EXISTS t_interview_question (
    id              BIGINT   NOT NULL AUTO_INCREMENT,
    session_id      BIGINT   NOT NULL,
    question_id     BIGINT   NOT NULL,
    question_order  INT      NOT NULL,
    is_answered     TINYINT(1) NOT NULL DEFAULT 0,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted      TINYINT(1) NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_session_id (session_id),
    UNIQUE KEY uk_session_question (session_id, question_order)
) COMMENT='会话题目序列';

-- ==================== 12. 对话消息 ====================
CREATE TABLE IF NOT EXISTS t_chat_message (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    session_id   BIGINT       NOT NULL,
    question_id  BIGINT       DEFAULT NULL,
    role         VARCHAR(10)  NOT NULL,
    content      TEXT         NOT NULL,
    audio_url    VARCHAR(500) DEFAULT '',
    message_type VARCHAR(20)  NOT NULL DEFAULT 'NORMAL',
    token_count  INT          DEFAULT 0,
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted   TINYINT(1)   NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_session_id (session_id),
    KEY idx_session_question (session_id, question_id),
    KEY idx_role (role)
) COMMENT='对话消息';

-- ==================== 13. 手撕代码提交 ====================
CREATE TABLE IF NOT EXISTS t_session_coding_submit (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    session_id      BIGINT       NOT NULL,
    question_id     BIGINT       NOT NULL,
    code_body       MEDIUMTEXT   NOT NULL,
    language        VARCHAR(32)  NOT NULL DEFAULT 'cpp',
    submit_order    INT          NOT NULL DEFAULT 1,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted      TINYINT(1)   NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_session_question (session_id, question_id)
) COMMENT='手撕代码提交';

-- ==================== 14. 评估报告 ====================
CREATE TABLE IF NOT EXISTS t_evaluation_report (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    session_id      BIGINT       NOT NULL,
    user_id         BIGINT       NOT NULL,
    position_code   VARCHAR(30)  NOT NULL,
    report_status   VARCHAR(20)  NOT NULL DEFAULT 'GENERATING',
    overall_score   DECIMAL(5,2) DEFAULT NULL,
    tech_score      DECIMAL(5,2) DEFAULT NULL,
    expression_score DECIMAL(5,2) DEFAULT NULL,
    logic_score     DECIMAL(5,2) DEFAULT NULL,
    depth_score     DECIMAL(5,2) DEFAULT NULL,
    confidence_score DECIMAL(5,2) DEFAULT NULL,
    summary         TEXT,
    highlights      JSON,
    weaknesses      JSON,
    suggestions     JSON,
    share_token     VARCHAR(64)  DEFAULT '',
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted      TINYINT(1)   NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_session_id (session_id),
    KEY idx_user_id (user_id),
    KEY idx_user_position (user_id, position_code),
    KEY idx_report_status (report_status)
) COMMENT='评估报告';

-- ==================== 15. 维度得分 ====================
CREATE TABLE IF NOT EXISTS t_dimension_score (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    report_id    BIGINT       NOT NULL,
    session_id   BIGINT       NOT NULL,
    question_id  BIGINT       NOT NULL,
    question_order INT        NOT NULL,
    tech_score   DECIMAL(5,2),
    logic_score  DECIMAL(5,2),
    depth_score  DECIMAL(5,2),
    comment      TEXT,
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted   TINYINT(1)   NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_report_id (report_id),
    KEY idx_session_id (session_id)
) COMMENT='逐题维度得分';

-- ==================== 16. 学习资源 ====================
CREATE TABLE IF NOT EXISTS t_learning_resource (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    position_code VARCHAR(30)  DEFAULT '',
    title         VARCHAR(200) NOT NULL,
    description   TEXT,
    resource_type VARCHAR(20)  NOT NULL,
    url           VARCHAR(500) NOT NULL,
    topic         VARCHAR(50)  DEFAULT '',
    difficulty    TINYINT      DEFAULT 2,
    quality_score INT DEFAULT 0,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted    TINYINT(1)   NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_position_code (position_code),
    KEY idx_resource_type (resource_type),
    KEY idx_topic (topic)
) COMMENT='学习资源';

-- ==================== 17. 用户推荐 ====================
CREATE TABLE IF NOT EXISTS t_user_recommendation (
    id           BIGINT   NOT NULL AUTO_INCREMENT,
    user_id      BIGINT   NOT NULL,
    report_id    BIGINT   DEFAULT NULL,
    resource_id  BIGINT   NOT NULL,
    reason       VARCHAR(200) DEFAULT '',
    is_clicked   TINYINT(1) NOT NULL DEFAULT 0,
    is_helpful   TINYINT(1) DEFAULT NULL,
    created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted   TINYINT(1) NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_user_id (user_id),
    KEY idx_report_id (report_id),
    KEY idx_user_created (user_id, created_at)
) COMMENT='用户推荐记录';

-- ==================== 18. 成长记录 ====================
CREATE TABLE IF NOT EXISTS t_growth_record (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    user_id          BIGINT       NOT NULL,
    report_id        BIGINT       NOT NULL,
    session_id       BIGINT       NOT NULL,
    position_code    VARCHAR(30)  NOT NULL,
    overall_score    DECIMAL(5,2),
    tech_score       DECIMAL(5,2),
    expression_score DECIMAL(5,2),
    logic_score      DECIMAL(5,2),
    depth_score      DECIMAL(5,2),
    confidence_score DECIMAL(5,2),
    record_date      DATE         NOT NULL,
    created_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted       TINYINT(1)   NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_user_id (user_id),
    KEY idx_user_position_date (user_id, position_code, record_date),
    KEY idx_report_id (report_id)
) COMMENT='成长记录';

-- ==================== 19. 系统配置 ====================
CREATE TABLE IF NOT EXISTS t_system_config (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    config_key   VARCHAR(100) NOT NULL,
    config_value TEXT         NOT NULL,
    config_type  VARCHAR(20)  NOT NULL DEFAULT 'STRING',
    description  VARCHAR(200) DEFAULT '',
    is_sensitive TINYINT(1)   NOT NULL DEFAULT 0,
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted   TINYINT(1)   NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_config_key (config_key)
) COMMENT='系统配置';

-- ==================== 初始化数据 ====================

-- 管理员 admin / admin123456
INSERT INTO t_user (username, password, nickname, role) VALUES
('admin', '$2b$10$YdWI3RPGFmQKT5nmH3/7A.1Hr8N5AORZwytE39bN0BzTDBRJ3i/eu', '系统管理员', 'ADMIN');

-- 岗位
INSERT INTO t_position (code, name, description, tech_stack, sort_order) VALUES
('JAVA_BACKEND', 'Java后端开发工程师', '负责服务端业务逻辑开发，熟悉 Java 生态和分布式架构',
 '["Java","Spring Boot","Spring Cloud","MySQL","Redis","JVM","设计模式","多线程","Kafka","MyBatis"]', 1),
('WEB_FRONTEND', 'Web前端开发工程师', '负责 Web 端界面开发，熟悉现代前端框架和工程化体系',
 '["HTML","CSS","JavaScript","TypeScript","Vue3","React","Webpack","Vite","性能优化","浏览器原理"]', 2),
('PYTHON_ALGO', 'Python算法工程师', '负责算法研究与实现，熟悉机器学习和数据结构',
 '["Python","数据结构与算法","机器学习","深度学习","NumPy","Pandas","LeetCode","系统设计"]', 3),
('GAME_CLIENT', '游戏客户端开发工程师', '负责游戏客户端功能开发，熟悉游戏引擎架构与渲染管线',
 '["C++","C#","Unity","Unreal Engine","游戏引擎","图形渲染","内存管理","网络同步","帧同步","性能优化","ECS","Lua"]', 4);

-- 知识库虚拟根 + 示例节点
INSERT INTO t_kb_node (id, parent_id, title, slug, code_path, depth, sort_order, node_type) VALUES
(1, NULL, '知识库根', 'root', '/root', 0, 0, 'GROUP'),
(2, 1, '计算机通识基础', 'cs-foundations', '/cs-foundations', 1, 1, 'GROUP'),
(3, 2, 'Java 语言基础', 'java-basics', '/cs-foundations/java-basics', 2, 1, 'GROUP'),
(4, 3, 'JVM 内存模型', 'jvm-memory', '/cs-foundations/java-basics/jvm-memory', 3, 1, 'TOPIC_POINT'),
(5, 2, '数据结构与算法', 'dsa', '/cs-foundations/dsa', 2, 2, 'GROUP'),
(6, 5, 'HashMap 原理', 'hashmap', '/cs-foundations/dsa/hashmap', 3, 1, 'TOPIC_POINT');

INSERT INTO t_kb_article (kb_node_id, title, body_markdown) VALUES
(4, 'JVM 内存模型详解', '# JVM 内存模型\n\n## 运行时数据区\n\n- **堆（Heap）**：存放对象实例，GC 主要区域\n- **栈（Stack）**：线程私有，存放局部变量表、操作数栈\n- **方法区（Method Area）**：类信息、常量、静态变量\n- **程序计数器**：当前线程执行字节码行号\n- **本地方法栈**：Native 方法服务\n\n## 元空间（Metaspace）\n\nJDK 8 起永久代被元空间取代，使用本地内存。'),
(6, 'HashMap 底层实现', '# HashMap 底层原理\n\n## 数据结构\n\nJDK 8 采用 **数组 + 链表 + 红黑树**。\n\n- 默认容量 16，负载因子 0.75\n- 链表长度 > 8 且数组长度 >= 64 时转红黑树\n- hash 碰撞通过链地址法解决');

-- 手撕题示例
INSERT INTO t_coding_challenge (external_ref, title, problem_md, difficulty, canonical_tags) VALUES
('Hot100-001', '两数之和', '给定一个整数数组 nums 和一个目标值 target，请你在该数组中找出和为目标值的那两个整数。\n\n**示例：** nums = [2,7,11,15], target = 9 → [0,1]', 1, '["数组","哈希"]'),
('Hot100-003', '无重复字符的最长子串', '给定一个字符串 s，请你找出其中不含有重复字符的最长子串的长度。', 2, '["字符串","滑动窗口"]');

-- Java 后端示例题目
INSERT INTO t_question (position_code, primary_kb_module_id, title, answer_reference, difficulty, question_type, topic, source) VALUES
('JAVA_BACKEND', 3, '请解释 Java 虚拟机（JVM）内存模型的组成及各区域的作用？', '堆、栈、方法区、程序计数器、本地方法栈；JDK8 元空间替代永久代', 2, 'TECH_KNOWLEDGE', 'JVM', 'MANUAL'),
('JAVA_BACKEND', 5, '请说明 HashMap 的底层实现原理，以及 JDK 8 的优化？', '数组+链表+红黑树；负载因子0.75；树化阈值8', 2, 'TECH_KNOWLEDGE', '集合框架', 'MANUAL'),
('JAVA_BACKEND', NULL, '如何设计一个高并发的秒杀系统？请从架构层面阐述。', '限流、缓存、异步、库存扣减、消息队列', 3, 'SCENARIO', '系统设计', 'MANUAL'),
('JAVA_BACKEND', NULL, '请结合你简历中的项目，深入讲解你在项目中遇到的最大技术挑战及解决方案。', 'STAR 法则，结合具体技术细节', 2, 'PROJECT_DEEP', '项目经验', 'MANUAL'),
('JAVA_BACKEND', NULL, '请实现「两数之和」算法题（可在 IDE 中编写代码）。', '哈希表 O(n) 解法', 1, 'BEHAVIOR', '算法', 'LC_HOT100');

UPDATE t_question SET coding_challenge_id = 1 WHERE question_type = 'BEHAVIOR' AND position_code = 'JAVA_BACKEND' LIMIT 1;

INSERT INTO t_question_kb_point (question_id, kb_node_id) VALUES (1, 4), (2, 6);

-- 系统配置
INSERT INTO t_system_config (config_key, config_value, config_type, description, is_sensitive) VALUES
('ai.llm.provider', 'deepseek', 'STRING', 'LLM提供商', 0),
('ai.llm.base-url', 'https://api.deepseek.com/v1', 'STRING', 'LLM API 基础地址', 0),
('ai.llm.model', 'deepseek-chat', 'STRING', '对话模型', 0),
('ai.llm.embed-model', 'deepseek-embed', 'STRING', 'Embedding 模型', 0),
('ai.llm.api-key', '', 'STRING', 'LLM API Key（部署时填写）', 1),
('ai.llm.temperature', '0.7', 'STRING', '生成温度', 0),
('ai.llm.max-tokens', '4096', 'STRING', '最大输出 token', 0),
('system.interview.default-question-count', '8', 'STRING', '默认面试题数', 0),
('prompt.interview.system', '你是一位专业严肃的技术面试官，正在对{positionName}岗位的候选人进行面试。面试共{totalQuestions}题，当前是第{currentOrder}题。规则：1.回答不完整可追问，每题最多2次追问后推进下一题 2.所有回复用JSON：{"action":"follow_up|next_question|end","content":"..."} 当前题目：{questionTitle}', 'TEXT', '面试官系统提示词', 0),
('prompt.evaluation.question', '岗位：{positionName} 题目：{questionTitle} 参考要点：{answerReference} 候选人回答：{userAnswer} 请JSON输出：{"tech_score":0-100,"logic_score":0-100,"depth_score":0-100,"comment":"..."}', 'TEXT', '逐题评分提示词', 0),
('prompt.evaluation.final', '各题评分汇总：{scoreSummary} 请JSON输出综合报告：{"overall_score":0-100,"expression_score":0-100,"confidence_score":0-100,"summary":"Markdown总结","highlights":[],"weaknesses":[],"suggestions":[]}', 'TEXT', '综合报告提示词', 0);

-- 学习资源示例
INSERT INTO t_learning_resource (position_code, title, description, resource_type, url, topic, difficulty) VALUES
('JAVA_BACKEND', '深入理解 Java 并发编程', 'AQS 原理与线程池', 'ARTICLE', 'https://example.com/java-concurrency', '并发编程', 3),
('JAVA_BACKEND', 'JVM 调优实战指南', 'GC 算法与参数调优', 'ARTICLE', 'https://example.com/jvm-tuning', 'JVM', 3),
('WEB_FRONTEND', 'Vue 3 组合式 API 最佳实践', 'Composition API 深入', 'ARTICLE', 'https://example.com/vue3-composition', 'Vue3', 2);
