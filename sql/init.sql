-- AI 模拟面试平台 - 完整建库脚本
-- 字符集: utf8mb4 | MySQL 8.0+

SET NAMES utf8mb4;

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
    education_experience   TEXT COMMENT '教育经历（简历提取）',
    personal_skills        TEXT COMMENT '个人能力（简历提取）',
    project_experience     TEXT COMMENT '项目经历（简历提取）',
    internship_experience  TEXT COMMENT '实习/工作经历（简历提取）',
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
('prompt.interview.system', '你是一位专业严肃的技术面试官，正在对{positionName}岗位的候选人进行面试。面试共{totalQuestions}题，当前是第{currentOrder}题。规则：1.回答不完整可追问，每题最多2次追问后推进下一题 2.所有回复用JSON：{"action":"follow_up|next_question|end","reply":"..."} 当前题目：{questionTitle}', 'TEXT', '面试官系统提示词', 0),
('prompt.evaluation.question', '岗位：{positionName} 题目：{questionTitle} 参考要点：{answerReference} 候选人回答：{userAnswer} 请JSON输出：{"tech_score":0-100,"logic_score":0-100,"depth_score":0-100,"comment":"..."}', 'TEXT', '逐题评分提示词', 0),
('prompt.evaluation.final', '各题评分汇总：{scoreSummary} 请JSON输出综合报告：{"overall_score":0-100,"expression_score":0-100,"confidence_score":0-100,"summary":"Markdown总结","highlights":[],"weaknesses":[],"suggestions":[]}', 'TEXT', '综合报告提示词', 0);

-- ========================================
-- 扩展：完整手撕题池（20题）
-- ========================================
INSERT INTO t_coding_challenge (external_ref, title, problem_md, difficulty, canonical_tags) VALUES
('Hot100-001', '两数之和', '给定一个整数数组 nums 和一个目标值 target，请你在该数组中找出和为目标值的那两个整数。\n\n**示例：** nums = [2,7,11,15], target = 9 → [0,1]', 1, '["数组","哈希"]'),
('Hot100-003', '无重复字符的最长子串', '给定一个字符串 s，请你找出其中不含有重复字符的最长子串的长度。', 2, '["字符串","滑动窗口"]'),
('Hot100-005', '最长回文子串', '给定一个字符串 s，找到 s 中最长的回文子串。', 2, '["字符串","动态规划"]'),
('Hot100-020', '有效的括号', '给定一个只包括 ''('' , '')'' , ''{'' , ''}'' , ''['' , '']'' 的字符串 s，判断字符串是否有效。', 1, '["栈","字符串"]'),
('Hot100-021', '合并两个有序链表', '将两个升序链表合并为一个新的升序链表并返回。', 1, '["链表","递归"]'),
('Hot100-053', '最大子数组和', '给定一个整数数组 nums，找到一个具有最大和的连续子数组。', 1, '["数组","动态规划"]'),
('Hot100-070', '爬楼梯', '假设你正在爬楼梯。需要 n 阶你才能到达楼顶。每次你可以爬 1 或 2 个台阶。有多少种不同的方法？', 1, '["动态规划"]'),
('Hot100-101', '对称二叉树', '给定一个二叉树，检查它是否是镜像对称的。', 1, '["树","深度优先搜索"]'),
('Hot100-102', '二叉树的层序遍历', '给你二叉树的根节点 root，返回其节点值的层序遍历。', 2, '["树","广度优先搜索"]'),
('Hot100-104', '二叉树的最大深度', '给定一个二叉树，找出其最大深度。', 1, '["树","深度优先搜索"]'),
('Hot100-121', '买卖股票的最佳时机', '给定一个数组 prices，它的第 i 个元素表示一支给定股票第 i 天的价格。你只能选择某一天买入，并在未来的某一天卖出。', 1, '["数组","贪心"]'),
('Hot100-141', '环形链表', '给定一个链表，判断链表中是否有环。', 1, '["链表","双指针"]'),
('Hot100-146', 'LRU缓存机制', '请你设计并实现一个满足 LRU (最近最少使用) 缓存约束的数据结构。', 3, '["设计","哈希表","双向链表"]'),
('Hot100-155', '最小栈', '设计一个支持 push，pop，top 操作，并能在常数时间内检索到最小元素的栈。', 2, '["栈","设计"]'),
('Hot100-198', '打家劫舍', '你是一个专业的小偷，计划偷窃沿街的房屋。每间房内都藏有一定的现金，影响你偷窃的唯一制约因素就是相邻的房屋装有相互连通的防盗系统。', 1, '["动态规划"]'),
('Hot100-206', '反转链表', '给你单链表的头节点 head，请你反转链表，并返回反转后的链表。', 1, '["链表","递归"]'),
('Hot100-226', '翻转二叉树', '给你一棵二叉树的根节点 root，翻转这棵二叉树，并返回其根节点。', 1, '["树","深度优先搜索"]'),
('Hot100-300', '最长递增子序列', '给你一个整数数组 nums，找到其中最长严格递增子序列的长度。', 2, '["数组","动态规划","二分查找"]'),
('Hot100-322', '零钱兑换', '给你一个整数数组 coins，表示不同面额的硬币；以及一个整数 amount，表示总金额。计算并返回可以凑成总金额所需的最少的硬币个数。', 2, '["动态规划","贪心"]'),
('Hot100-437', '路径总和 III', '给定一个二叉树的根节点 root，和一个整数 targetSum，求该二叉树里节点值之和等于 targetSum 的路径的数目。', 2, '["树","深度优先搜索","前缀和"]');

-- ========================================
-- 扩展：完整四岗位题库（每岗位25题）
-- ========================================
-- JAVA_BACKEND 25题
INSERT INTO t_question (position_code, primary_kb_module_id, title, answer_reference, difficulty, question_type, topic, source) VALUES
('JAVA_BACKEND', 3, '请解释 Java 虚拟机（JVM）内存模型的组成及各区域的作用？', '堆、栈、方法区、程序计数器、本地方法栈；JDK8 元空间替代永久代', 2, 'TECH_KNOWLEDGE', 'JVM', 'MANUAL'),
('JAVA_BACKEND', 5, '请说明 HashMap 的底层实现原理，以及 JDK 8 的优化？', '数组+链表+红黑树；负载因子0.75；树化阈值8', 2, 'TECH_KNOWLEDGE', '集合框架', 'MANUAL'),
('JAVA_BACKEND', NULL, 'Java 中 String、StringBuffer、StringBuilder 的区别？', 'String 不可变；StringBuffer 线程安全但性能低；StringBuilder 线程不安全但性能高', 1, 'TECH_KNOWLEDGE', '基础', 'MANUAL'),
('JAVA_BACKEND', NULL, '请解释 Spring Bean 的生命周期？', '实例化 → 属性注入 → 初始化前 → 初始化 → 初始化后 → 使用 → 销毁', 2, 'TECH_KNOWLEDGE', 'Spring', 'MANUAL'),
('JAVA_BACKEND', NULL, '什么是 Spring AOP？应用场景有哪些？', '面向切面编程；日志、事务、权限、性能监控', 2, 'TECH_KNOWLEDGE', 'Spring', 'MANUAL'),
('JAVA_BACKEND', NULL, 'MySQL 索引的数据结构？为什么用 B+树？', 'B+树；数据只在叶子节点；范围查询快；页分裂优化', 2, 'TECH_KNOWLEDGE', 'MySQL', 'MANUAL'),
('JAVA_BACKEND', NULL, 'Redis 数据类型有哪些？各适用于什么场景？', 'String/List/Hash/Set/ZSet/BitMap/Geo/HyperLogLog/Stream', 2, 'TECH_KNOWLEDGE', 'Redis', 'MANUAL'),
('JAVA_BACKEND', NULL, '什么是 Java 内存模型（JMM）？volatile 的作用？', '主内存与工作内存；可见性、禁止指令重排、不保证原子性', 3, 'TECH_KNOWLEDGE', '并发', 'MANUAL'),
('JAVA_BACKEND', NULL, 'synchronized 和 ReentrantLock 的区别？', '关键字vs类；可重入；可中断；公平锁；Condition', 2, 'TECH_KNOWLEDGE', '并发', 'MANUAL'),
('JAVA_BACKEND', NULL, '请解释 JVM 垃圾回收算法？CMS 和 G1 的区别？', '标记清除、复制、标记整理；CMS 低延迟 vs G1 可预测停顿', 3, 'TECH_KNOWLEDGE', 'JVM', 'MANUAL'),
('JAVA_BACKEND', NULL, '如何设计一个高并发的秒杀系统？请从架构层面阐述。', '限流、缓存、异步、库存扣减、消息队列、分布式锁', 3, 'SCENARIO', '系统设计', 'MANUAL'),
('JAVA_BACKEND', NULL, '如何保证接口的幂等性？请列举常见方案。', '唯一ID、token机制、数据库唯一约束、乐观锁、redis setnx', 2, 'SCENARIO', '分布式', 'MANUAL'),
('JAVA_BACKEND', NULL, '如何防止重复提交？前端和后端分别怎么做？', '前端：防抖/状态禁用；后端：token/幂等键/乐观锁', 2, 'SCENARIO', '系统设计', 'MANUAL'),
('JAVA_BACKEND', NULL, '系统出现 OOM 如何排查？', 'heap dump、jmap/jhat、jstat、jvisualvm、MAT分析', 3, 'SCENARIO', '问题排查', 'MANUAL'),
('JAVA_BACKEND', NULL, '如何设计分布式系统的限流方案？', '滑动窗口、令牌桶、漏桶算法；Redis+Lua；Sentinel', 3, 'SCENARIO', '系统设计', 'MANUAL'),
('JAVA_BACKEND', NULL, '请结合你简历中的项目，深入讲解你在项目中遇到的最大技术挑战及解决方案。', 'STAR 法则，结合具体技术细节', 2, 'PROJECT_DEEP', '项目经验', 'MANUAL'),
('JAVA_BACKEND', NULL, '请描述一个你负责的复杂模块的设计与实现过程。', '需求分析、技术选型、架构设计、编码实现、测试上线', 2, 'PROJECT_DEEP', '项目经验', 'MANUAL'),
('JAVA_BACKEND', NULL, '你在项目中是如何做性能优化的？请举具体例子说明。', '定位瓶颈（慢SQL/日志/锁）、优化方案、效果验证', 3, 'PROJECT_DEEP', '项目经验', 'MANUAL'),
('JAVA_BACKEND', NULL, '你有参与过线上问题排查吗？请描述一次印象深刻的排查经历。', '问题现象、排查思路、定位过程、解决方案、复盘总结', 2, 'PROJECT_DEEP', '项目经验', 'MANUAL'),
('JAVA_BACKEND', NULL, '请讲讲你做过的最有成就感的项目，你在其中扮演了什么角色？', '项目背景、个人贡献、遇到困难、解决过程、成果与收获', 2, 'PROJECT_DEEP', '项目经验', 'MANUAL'),
('JAVA_BACKEND', NULL, '请实现「两数之和」算法题', '哈希表 O(n) 解法', 1, 'BEHAVIOR', '算法', 'LC_HOT100'),
('JAVA_BACKEND', NULL, '请实现「无重复字符的最长子串」', '滑动窗口 O(n)', 2, 'BEHAVIOR', '算法', 'LC_HOT100'),
('JAVA_BACKEND', NULL, '请实现「二叉树的层序遍历」', 'BFS 使用队列', 2, 'BEHAVIOR', '算法', 'LC_HOT100'),
('JAVA_BACKEND', NULL, '请实现「LRU缓存机制」', '哈希表 + 双向链表', 3, 'BEHAVIOR', '算法', 'LC_HOT100'),
('JAVA_BACKEND', NULL, '请实现「反转链表」', '迭代或递归', 1, 'BEHAVIOR', '算法', 'LC_HOT100');

-- WEB_FRONTEND 25题
INSERT INTO t_question (position_code, primary_kb_module_id, title, answer_reference, difficulty, question_type, topic, source) VALUES
('WEB_FRONTEND', NULL, '请解释 JavaScript 的事件循环机制？', '调用栈、宏任务/微任务队列、事件循环过程', 2, 'TECH_KNOWLEDGE', 'JS基础', 'MANUAL'),
('WEB_FRONTEND', NULL, 'ES6 有哪些新特性？请列举并简要说明。', 'let/const、箭头函数、解构、Promise、async/await、Map/Set、Class', 2, 'TECH_KNOWLEDGE', 'ES6', 'MANUAL'),
('WEB_FRONTEND', NULL, 'Vue 2 和 Vue 3 的区别？Composition API 的优势？', '响应式原理、虚拟DOM、Composition API、性能优化', 2, 'TECH_KNOWLEDGE', 'Vue', 'MANUAL'),
('WEB_FRONTEND', NULL, '请解释 Vue 的双向绑定原理？', 'Vue2：Object.defineProperty；Vue3：Proxy + Reflect', 2, 'TECH_KNOWLEDGE', 'Vue', 'MANUAL'),
('WEB_FRONTEND', NULL, '什么是 Virtual DOM？Diff 算法的原理？', '虚拟DOM对比；同层比较、key作用、双端对比', 3, 'TECH_KNOWLEDGE', 'Vue/React', 'MANUAL'),
('WEB_FRONTEND', NULL, 'CSS 盒模型？标准盒与怪异盒的区别？', 'content-box vs border-box', 1, 'TECH_KNOWLEDGE', 'CSS', 'MANUAL'),
('WEB_FRONTEND', NULL, '请列举 CSS 居中的方案？', 'flex、grid、margin auto、absolute+transform、table-cell', 2, 'TECH_KNOWLEDGE', 'CSS', 'MANUAL'),
('WEB_FRONTEND', NULL, '浏览器渲染流程？如何优化首屏加载？', '解析DOM/CSS → 渲染树 → 布局 → 绘制；资源优化、预加载、CDN', 2, 'TECH_KNOWLEDGE', '性能', 'MANUAL'),
('WEB_FRONTEND', NULL, 'HTTP 和 HTTPS 的区别？HTTP/2 的特性？', '加密、CA证书、端口；二进制分帧、多路复用、头部压缩、服务器推送', 2, 'TECH_KNOWLEDGE', '网络', 'MANUAL'),
('WEB_FRONTEND', NULL, '请解释 Webpack 的构建流程？', '入口、Loader、Plugin、Module、Chunk、Output', 2, 'TECH_KNOWLEDGE', '工程化', 'MANUAL'),
('WEB_FRONTEND', NULL, '如何设计一个复杂的中后台系统？', '布局设计、权限管理、组件封装、状态管理、工程化', 3, 'SCENARIO', '系统设计', 'MANUAL'),
('WEB_FRONTEND', NULL, '前端性能优化有哪些策略？', '资源加载、渲染优化、缓存策略、代码优化、监控体系', 2, 'SCENARIO', '性能优化', 'MANUAL'),
('WEB_FRONTEND', NULL, '如何做前端监控与埋点？', '错误监控、性能监控、用户行为埋点；sentry/自研', 2, 'SCENARIO', '监控', 'MANUAL'),
('WEB_FRONTEND', NULL, '如何解决跨域问题？', 'CORS、JSONP、代理、Nginx、postMessage', 2, 'SCENARIO', '网络', 'MANUAL'),
('WEB_FRONTEND', NULL, '请讲讲你是如何做组件设计与封装的？', '单一职责、可复用性、可扩展性、Props设计、插槽、文档', 3, 'SCENARIO', '系统设计', 'MANUAL'),
('WEB_FRONTEND', NULL, '请结合你简历中的项目，深入讲解你在项目中遇到的最大技术挑战及解决方案。', 'STAR 法则，结合具体技术细节', 2, 'PROJECT_DEEP', '项目经验', 'MANUAL'),
('WEB_FRONTEND', NULL, '请描述一个你负责的复杂组件的设计与实现过程。', '需求分析、技术方案、编码实现、测试、优化', 2, 'PROJECT_DEEP', '项目经验', 'MANUAL'),
('WEB_FRONTEND', NULL, '你在项目中是如何做性能优化的？请举具体例子说明。', '定位瓶颈、优化方案、效果验证、数据对比', 3, 'PROJECT_DEEP', '项目经验', 'MANUAL'),
('WEB_FRONTEND', NULL, '你有参与过线上问题排查吗？请描述一次印象深刻的排查经历。', '问题现象、排查思路、定位过程、解决方案、复盘', 2, 'PROJECT_DEEP', '项目经验', 'MANUAL'),
('WEB_FRONTEND', NULL, '请讲讲你做过的最有成就感的项目，你在其中扮演了什么角色？', '项目背景、个人贡献、遇到困难、解决过程、成果与收获', 2, 'PROJECT_DEEP', '项目经验', 'MANUAL'),
('WEB_FRONTEND', NULL, '请实现「两数之和」', '哈希表 O(n) 解法', 1, 'BEHAVIOR', '算法', 'LC_HOT100'),
('WEB_FRONTEND', NULL, '请实现「无重复字符的最长子串」', '滑动窗口 O(n)', 2, 'BEHAVIOR', '算法', 'LC_HOT100'),
('WEB_FRONTEND', NULL, '请实现「有效的括号」', '栈匹配', 1, 'BEHAVIOR', '算法', 'LC_HOT100'),
('WEB_FRONTEND', NULL, '请实现「最大子数组和」', 'Kadane 算法 O(n)', 1, 'BEHAVIOR', '算法', 'LC_HOT100'),
('WEB_FRONTEND', NULL, '请实现「爬楼梯」', '动态规划 / 斐波那契', 1, 'BEHAVIOR', '算法', 'LC_HOT100');

-- PYTHON_ALGO 25题
INSERT INTO t_question (position_code, primary_kb_module_id, title, answer_reference, difficulty, question_type, topic, source) VALUES
('PYTHON_ALGO', NULL, 'Python 的 GIL 是什么？对多线程有什么影响？', '全局解释器锁；同一时刻只有一个线程执行CPU密集代码', 2, 'TECH_KNOWLEDGE', 'Python', 'MANUAL'),
('PYTHON_ALGO', NULL, '请解释 *args 和 **kwargs 的用法？', '可变位置参数、可变关键字参数', 1, 'TECH_KNOWLEDGE', 'Python', 'MANUAL'),
('PYTHON_ALGO', NULL, '什么是装饰器？请举例子说明。', '闭包实现、@语法糖、函数增强', 2, 'TECH_KNOWLEDGE', 'Python', 'MANUAL'),
('PYTHON_ALGO', NULL, '迭代器、生成器、可迭代对象的区别？', '__iter__/__next__；yield；iter()', 2, 'TECH_KNOWLEDGE', 'Python', 'MANUAL'),
('PYTHON_ALGO', NULL, '请解释 Python 的深浅拷贝？', 'copy.copy vs copy.deepcopy；可变对象嵌套', 2, 'TECH_KNOWLEDGE', 'Python', 'MANUAL'),
('PYTHON_ALGO', NULL, '什么是动态规划？能举个例子吗？', '重叠子问题、最优子结构；背包问题、爬楼梯', 2, 'TECH_KNOWLEDGE', '算法', 'MANUAL'),
('PYTHON_ALGO', NULL, '请解释二分查找及其时间复杂度？', '有序数组、O(log n)、边界条件', 1, 'TECH_KNOWLEDGE', '算法', 'MANUAL'),
('PYTHON_ALGO', NULL, '什么是快速排序？请说明其原理和时间复杂度？', '分治、基准选择、O(n log n)平均、O(n²)最坏', 2, 'TECH_KNOWLEDGE', '算法', 'MANUAL'),
('PYTHON_ALGO', NULL, '请解释二叉树的前/中/后序遍历？', '根左右、左根右、左右根', 1, 'TECH_KNOWLEDGE', '数据结构', 'MANUAL'),
('PYTHON_ALGO', NULL, '什么是哈希冲突？有哪些解决方法？', '链地址法、开放寻址法、再哈希法', 2, 'TECH_KNOWLEDGE', '数据结构', 'MANUAL'),
('PYTHON_ALGO', NULL, '如何设计一个推荐系统？请从算法和工程层面阐述。', '协同过滤、内容推荐、召回+排序、特征工程、冷启动', 3, 'SCENARIO', '系统设计', 'MANUAL'),
('PYTHON_ALGO', NULL, '如果模型在训练集上效果很好，但测试集上效果很差，你怎么分析？', '过拟合、数据分布差异、特征泄露、模型选择', 2, 'SCENARIO', 'ML', 'MANUAL'),
('PYTHON_ALGO', NULL, '如何处理数据集中的缺失值？', '删除、均值/中位数/众数填充、模型预测、KNN填充', 2, 'SCENARIO', '数据处理', 'MANUAL'),
('PYTHON_ALGO', NULL, '请说明如何优化一个慢查询或慢算法？', '算法复杂度分析、空间换时间、并行计算、剪枝', 3, 'SCENARIO', '性能优化', 'MANUAL'),
('PYTHON_ALGO', NULL, '如何设计一个爬虫系统？', '请求池、去重、并发、反爬应对、数据存储', 2, 'SCENARIO', '系统设计', 'MANUAL'),
('PYTHON_ALGO', NULL, '请结合你简历中的项目，深入讲解你在项目中遇到的最大技术挑战及解决方案。', 'STAR 法则，结合具体技术细节', 2, 'PROJECT_DEEP', '项目经验', 'MANUAL'),
('PYTHON_ALGO', NULL, '请描述一个你做过的算法或模型优化项目，说明优化前后的效果。', '问题背景、基线方案、优化思路、实施过程、效果评估', 2, 'PROJECT_DEEP', '项目经验', 'MANUAL'),
('PYTHON_ALGO', NULL, '你在项目中是如何做特征工程的？请举具体例子。', '特征选择、特征构造、特征变换、特征重要性', 3, 'PROJECT_DEEP', '项目经验', 'MANUAL'),
('PYTHON_ALGO', NULL, '你有遇到过模型上线效果不如离线训练的情况吗？如何解决？', '线上线下数据差异、特征不一致、概念漂移', 2, 'PROJECT_DEEP', '项目经验', 'MANUAL'),
('PYTHON_ALGO', NULL, '请讲讲你做过的最有成就感的项目，你在其中扮演了什么角色？', '项目背景、个人贡献、遇到困难、解决过程、成果与收获', 2, 'PROJECT_DEEP', '项目经验', 'MANUAL'),
('PYTHON_ALGO', NULL, '请实现「两数之和」', '哈希表 O(n)', 1, 'BEHAVIOR', '算法', 'LC_HOT100'),
('PYTHON_ALGO', NULL, '请实现「最长回文子串」', '动态规划或中心扩散', 2, 'BEHAVIOR', '算法', 'LC_HOT100'),
('PYTHON_ALGO', NULL, '请实现「二叉树的最大深度」', '递归或 BFS', 1, 'BEHAVIOR', '算法', 'LC_HOT100'),
('PYTHON_ALGO', NULL, '请实现「买卖股票的最佳时机」', '一次遍历记录最小值', 1, 'BEHAVIOR', '算法', 'LC_HOT100'),
('PYTHON_ALGO', NULL, '请实现「最长递增子序列」', 'DP O(n²) 或二分 O(n log n)', 2, 'BEHAVIOR', '算法', 'LC_HOT100');

-- GAME_CLIENT 25题
INSERT INTO t_question (position_code, primary_kb_module_id, title, answer_reference, difficulty, question_type, topic, source) VALUES
('GAME_CLIENT', NULL, '请解释 Unity 的 GameObject 和 Component 关系？', '实体-组件模式，GameObject 是容器，Component 是功能模块', 2, 'TECH_KNOWLEDGE', 'Unity', 'MANUAL'),
('GAME_CLIENT', NULL, 'Unity 的生命周期函数有哪些？执行顺序是怎样的？', 'Awake、Start、Update、LateUpdate、FixedUpdate、OnDestroy', 2, 'TECH_KNOWLEDGE', 'Unity', 'MANUAL'),
('GAME_CLIENT', NULL, '请解释什么是 Draw Call？如何优化 Draw Call？', 'GPU 绘制命令；合批、图集、静态批处理、SRP Batcher', 3, 'TECH_KNOWLEDGE', '渲染', 'MANUAL'),
('GAME_CLIENT', NULL, '什么是对象池？为什么需要对象池？', '预分配对象复用；减少 GC、避免频繁创建销毁', 2, 'TECH_KNOWLEDGE', '性能', 'MANUAL'),
('GAME_CLIENT', NULL, '请解释游戏中的碰撞检测原理？', 'AABB、OBB、Sphere、Raycast；宽相位+窄相位', 2, 'TECH_KNOWLEDGE', '物理', 'MANUAL'),
('GAME_CLIENT', NULL, 'Unity 的协程是什么？与线程有什么区别？', '迭代器实现、主线程分帧执行、非多线程', 2, 'TECH_KNOWLEDGE', 'Unity', 'MANUAL'),
('GAME_CLIENT', NULL, '请解释 Shader 的基本结构？Vertex Shader 和 Fragment Shader 的作用？', '顶点变换、片元着色；顶点位置、像素颜色', 3, 'TECH_KNOWLEDGE', '渲染', 'MANUAL'),
('GAME_CLIENT', NULL, '游戏中的动画系统有哪些？', 'Legacy、Animator、Playable、Animation Rigging', 2, 'TECH_KNOWLEDGE', '动画', 'MANUAL'),
('GAME_CLIENT', NULL, '请解释什么是 ECS？与传统 OOP 的区别？', '实体-组件-系统；数据驱动、缓存友好、高性能', 3, 'TECH_KNOWLEDGE', '架构', 'MANUAL'),
('GAME_CLIENT', NULL, '什么是帧同步和状态同步？各适用于什么场景？', '输入同步vs状态同步；MOBA vs RPG/FPS', 3, 'TECH_KNOWLEDGE', '网络', 'MANUAL'),
('GAME_CLIENT', NULL, '如何设计一个灵活的技能系统？', '技能配置、Buff系统、事件机制、状态机', 3, 'SCENARIO', '系统设计', 'MANUAL'),
('GAME_CLIENT', NULL, '游戏卡顿如何排查与优化？', 'Profiler、CPU/GPU分析、定位瓶颈、分步优化', 2, 'SCENARIO', '性能优化', 'MANUAL'),
('GAME_CLIENT', NULL, '如何做游戏内存优化？', '资源压缩、对象池、资源卸载、纹理格式优化', 3, 'SCENARIO', '内存', 'MANUAL'),
('GAME_CLIENT', NULL, '请讲讲你是如何设计游戏中的 UI 架构的？', 'UI管理、分层设计、事件系统、MVVM/MVC', 2, 'SCENARIO', '系统设计', 'MANUAL'),
('GAME_CLIENT', NULL, '如何做游戏热更新？', 'Lua/ILRuntime/CLR、AssetBundle、热更方案选择', 3, 'SCENARIO', '热更新', 'MANUAL'),
('GAME_CLIENT', NULL, '请结合你简历中的项目，深入讲解你在项目中遇到的最大技术挑战及解决方案。', 'STAR 法则，结合具体技术细节', 2, 'PROJECT_DEEP', '项目经验', 'MANUAL'),
('GAME_CLIENT', NULL, '请描述一个你负责的复杂游戏系统的设计与实现过程。', '需求分析、技术方案、架构设计、编码实现、测试', 2, 'PROJECT_DEEP', '项目经验', 'MANUAL'),
('GAME_CLIENT', NULL, '你在游戏项目中是如何做性能优化的？请举具体例子。', '性能分析、定位瓶颈、优化方案、效果验证', 3, 'PROJECT_DEEP', '项目经验', 'MANUAL'),
('GAME_CLIENT', NULL, '你有参与过游戏线上问题排查吗？请描述一次印象深刻的排查经历。', '问题现象、排查思路、定位过程、解决方案、复盘', 2, 'PROJECT_DEEP', '项目经验', 'MANUAL'),
('GAME_CLIENT', NULL, '请讲讲你做过的最有成就感的游戏项目，你在其中扮演了什么角色？', '项目背景、个人贡献、遇到困难、解决过程、成果与收获', 2, 'PROJECT_DEEP', '项目经验', 'MANUAL'),
('GAME_CLIENT', NULL, '请实现「反转链表」', '迭代或递归', 1, 'BEHAVIOR', '算法', 'LC_HOT100'),
('GAME_CLIENT', NULL, '请实现「环形链表」', '快慢指针 / Floyd', 1, 'BEHAVIOR', '算法', 'LC_HOT100'),
('GAME_CLIENT', NULL, '请实现「二叉树的最大深度」', '递归 DFS 或 BFS', 1, 'BEHAVIOR', '算法', 'LC_HOT100'),
('GAME_CLIENT', NULL, '请实现「打家劫舍」', '动态规划', 1, 'BEHAVIOR', '算法', 'LC_HOT100'),
('GAME_CLIENT', NULL, '请实现「最长递增子序列」', 'DP 或二分搜索', 2, 'BEHAVIOR', '算法', 'LC_HOT100');

-- 关联手撕题与 Hot100 题池（仅 LC_HOT100 来源的 BEHAVIOR 题）
UPDATE t_question q JOIN t_coding_challenge c ON c.external_ref = 'Hot100-001' SET q.coding_challenge_id = c.id WHERE q.question_type = 'BEHAVIOR' AND q.source = 'LC_HOT100' AND q.title LIKE '%两数之和%';
UPDATE t_question q JOIN t_coding_challenge c ON c.external_ref = 'Hot100-003' SET q.coding_challenge_id = c.id WHERE q.question_type = 'BEHAVIOR' AND q.source = 'LC_HOT100' AND q.title LIKE '%无重复字符的最长子串%';
UPDATE t_question q JOIN t_coding_challenge c ON c.external_ref = 'Hot100-005' SET q.coding_challenge_id = c.id WHERE q.question_type = 'BEHAVIOR' AND q.source = 'LC_HOT100' AND q.title LIKE '%最长回文子串%';
UPDATE t_question q JOIN t_coding_challenge c ON c.external_ref = 'Hot100-020' SET q.coding_challenge_id = c.id WHERE q.question_type = 'BEHAVIOR' AND q.source = 'LC_HOT100' AND q.title LIKE '%有效的括号%';
UPDATE t_question q JOIN t_coding_challenge c ON c.external_ref = 'Hot100-053' SET q.coding_challenge_id = c.id WHERE q.question_type = 'BEHAVIOR' AND q.source = 'LC_HOT100' AND q.title LIKE '%最大子数组和%';
UPDATE t_question q JOIN t_coding_challenge c ON c.external_ref = 'Hot100-070' SET q.coding_challenge_id = c.id WHERE q.question_type = 'BEHAVIOR' AND q.source = 'LC_HOT100' AND q.title LIKE '%爬楼梯%';
UPDATE t_question q JOIN t_coding_challenge c ON c.external_ref = 'Hot100-102' SET q.coding_challenge_id = c.id WHERE q.question_type = 'BEHAVIOR' AND q.source = 'LC_HOT100' AND q.title LIKE '%二叉树的层序遍历%';
UPDATE t_question q JOIN t_coding_challenge c ON c.external_ref = 'Hot100-104' SET q.coding_challenge_id = c.id WHERE q.question_type = 'BEHAVIOR' AND q.source = 'LC_HOT100' AND q.title LIKE '%二叉树的最大深度%';
UPDATE t_question q JOIN t_coding_challenge c ON c.external_ref = 'Hot100-121' SET q.coding_challenge_id = c.id WHERE q.question_type = 'BEHAVIOR' AND q.source = 'LC_HOT100' AND q.title LIKE '%买卖股票的最佳时机%';
UPDATE t_question q JOIN t_coding_challenge c ON c.external_ref = 'Hot100-141' SET q.coding_challenge_id = c.id WHERE q.question_type = 'BEHAVIOR' AND q.source = 'LC_HOT100' AND q.title LIKE '%环形链表%';
UPDATE t_question q JOIN t_coding_challenge c ON c.external_ref = 'Hot100-146' SET q.coding_challenge_id = c.id WHERE q.question_type = 'BEHAVIOR' AND q.source = 'LC_HOT100' AND q.title LIKE '%LRU缓存%';
UPDATE t_question q JOIN t_coding_challenge c ON c.external_ref = 'Hot100-198' SET q.coding_challenge_id = c.id WHERE q.question_type = 'BEHAVIOR' AND q.source = 'LC_HOT100' AND q.title LIKE '%打家劫舍%';
UPDATE t_question q JOIN t_coding_challenge c ON c.external_ref = 'Hot100-206' SET q.coding_challenge_id = c.id WHERE q.question_type = 'BEHAVIOR' AND q.source = 'LC_HOT100' AND q.title LIKE '%反转链表%';
UPDATE t_question q JOIN t_coding_challenge c ON c.external_ref = 'Hot100-300' SET q.coding_challenge_id = c.id WHERE q.question_type = 'BEHAVIOR' AND q.source = 'LC_HOT100' AND q.title LIKE '%最长递增子序列%';
UPDATE t_question q JOIN t_coding_challenge c ON c.external_ref = 'Hot100-322' SET q.coding_challenge_id = c.id WHERE q.question_type = 'BEHAVIOR' AND q.source = 'LC_HOT100' AND q.title LIKE '%零钱兑换%';

INSERT INTO t_question_kb_point (question_id, kb_node_id)
SELECT q.id, 4 FROM t_question q WHERE q.position_code = 'JAVA_BACKEND' AND q.title LIKE '%JVM 内存模型%' LIMIT 1;

INSERT INTO t_question_kb_point (question_id, kb_node_id)
SELECT q.id, 6 FROM t_question q WHERE q.position_code = 'JAVA_BACKEND' AND q.title LIKE '%HashMap%' LIMIT 1;

-- ========================================
-- 扩展：四岗位差异化 Prompt 配置
-- ========================================
INSERT INTO t_system_config (config_key, config_value, config_type, description, is_sensitive) VALUES
('prompt.interview.system.java_backend', '你是一位专业严谨的Java后端面试官，正在对Java后端开发工程师岗位的候选人进行面试。面试共{totalQuestions}题，当前是第{currentOrder}题。角色定位：你是资深Java后端架构师，对Spring生态、分布式系统、JVM调优、数据库设计有深入理解。注重考察候选人的基础扎实度、技术深度、项目经验、问题排查能力。适当追问，引导候选人展示真实水平。面试重点：1.Java基础：集合、并发、JVM 2.Spring生态：Spring Boot、Spring Cloud、IOC/AOP 3.数据库：MySQL索引、事务、锁 4.中间件：Redis、消息队列 5.分布式系统：CAP、限流、熔断、分布式锁 6.项目经验：技术选型、架构设计、问题排查。规则：1.回答不完整可追问，每题最多2次追问后推进下一题 2.追问针对不足点发问 3.推进下一题自然过渡 4.所有回复用JSON：{"action":"follow_up|next_question|end","reply":"..."} 当前题目：{questionTitle}', 'TEXT', 'Java后端面试官提示词', 0),
('prompt.interview.system.web_frontend', '你是一位专业资深的前端面试官，正在对Web前端开发工程师岗位的候选人进行面试。面试共{totalQuestions}题，当前是第{currentOrder}题。角色定位：你是资深前端架构师，对JavaScript/TypeScript、Vue/React、工程化、性能优化有深入理解。注重考察候选人的基础扎实度、工程实践、解决问题能力。适当追问，引导候选人展示真实水平。面试重点：1.JS基础：原型、闭包、异步、事件循环 2.框架原理：Vue/React响应式、虚拟DOM、Diff算法 3.CSS：盒模型、布局、动画、性能 4.工程化：Webpack/Vite、构建优化、CI/CD 5.性能优化：首屏、渲染、资源加载 6.项目经验：组件设计、架构、技术选型。规则：1.回答不完整可追问，每题最多2次追问后推进下一题 2.追问针对不足点发问 3.推进下一题自然过渡 4.所有回复用JSON：{"action":"follow_up|next_question|end","reply":"..."} 当前题目：{questionTitle}', 'TEXT', 'Web前端面试官提示词', 0),
('prompt.interview.system.python_algo', '你是一位专业资深的算法/AI面试官，正在对Python算法工程师岗位的候选人进行面试。面试共{totalQuestions}题，当前是第{currentOrder}题。角色定位：你是资深算法工程师/数据科学家，对数据结构、算法、机器学习有深入理解。注重考察候选人的逻辑思维、算法能力、建模能力、项目落地能力。适当追问，引导候选人展示真实水平。面试重点：1.Python基础：语言特性、高级用法、性能 2.数据结构与算法：数组、链表、树、图、动态规划 3.机器学习：模型原理、特征工程、调优、评估 4.数据处理：NumPy、Pandas、数据清洗 5.系统设计：推荐系统、爬虫、工程化 6.项目经验：问题建模、方案选择、效果评估。规则：1.回答不完整可追问，每题最多2次追问后推进下一题 2.追问针对不足点发问 3.推进下一题自然过渡 4.所有回复用JSON：{"action":"follow_up|next_question|end","reply":"..."} 当前题目：{questionTitle}', 'TEXT', 'Python算法工程师提示词', 0),
('prompt.interview.system.game_client', '你是一位专业资深的游戏客户端面试官，正在对游戏客户端开发工程师岗位的候选人进行面试。面试共{totalQuestions}题，当前是第{currentOrder}题。角色定位：你是资深游戏客户端架构师，对Unity/Unreal引擎、渲染、物理、性能优化有深入理解。注重考察候选人的基础扎实度、技术深度、项目经验、优化能力。适当追问，引导候选人展示真实水平。面试重点：1.引擎：Unity/Unreal核心机制、生命周期、组件系统 2.渲染：Shader、Draw Call、渲染管线、性能优化 3.物理：碰撞检测、物理引擎、刚体 4.性能：内存优化、对象池、GC、性能分析 5.架构：ECS、框架设计、模块划分 6.项目经验：系统设计、问题排查、优化经历。规则：1.回答不完整可追问，每题最多2次追问后推进下一题 2.追问针对不足点发问 3.推进下一题自然过渡 4.所有回复用JSON：{"action":"follow_up|next_question|end","reply":"..."} 当前题目：{questionTitle}', 'TEXT', '游戏客户端面试官提示词', 0);

-- ========================================
-- 扩展：四岗位知识库示例
-- ========================================
INSERT INTO t_kb_node (id, parent_id, title, slug, code_path, depth, sort_order, node_type, position_codes) VALUES
(10, 1, 'Java后端知识体系', 'java-backend', '/java-backend', 1, 2, 'GROUP', '["JAVA_BACKEND"]'),
(11, 10, 'Java并发编程', 'java-concurrency', '/java-backend/java-concurrency', 2, 1, 'GROUP', '["JAVA_BACKEND"]'),
(12, 11, '线程池原理', 'thread-pool', '/java-backend/java-concurrency/thread-pool', 3, 1, 'TOPIC_POINT', '["JAVA_BACKEND"]'),
(20, 1, '前端知识体系', 'frontend', '/frontend', 1, 3, 'GROUP', '["WEB_FRONTEND"]'),
(21, 20, 'Vue3核心原理', 'vue3-core', '/frontend/vue3-core', 2, 1, 'GROUP', '["WEB_FRONTEND"]'),
(22, 21, '响应式原理', 'reactivity', '/frontend/vue3-core/reactivity', 3, 1, 'TOPIC_POINT', '["WEB_FRONTEND"]'),
(30, 1, '算法与数据结构', 'algorithm-dsa', '/algorithm-dsa', 1, 4, 'GROUP', '["PYTHON_ALGO"]'),
(31, 30, '动态规划入门', 'dp-intro', '/algorithm-dsa/dp-intro', 2, 1, 'GROUP', '["PYTHON_ALGO"]'),
(32, 31, '背包问题', 'knapsack', '/algorithm-dsa/dp-intro/knapsack', 3, 1, 'TOPIC_POINT', '["PYTHON_ALGO"]'),
(40, 1, '游戏开发知识体系', 'game-dev', '/game-dev', 1, 5, 'GROUP', '["GAME_CLIENT"]'),
(41, 40, 'Unity核心', 'unity-core', '/game-dev/unity-core', 2, 1, 'GROUP', '["GAME_CLIENT"]'),
(42, 41, 'Unity渲染管线', 'render-pipeline', '/game-dev/unity-core/render-pipeline', 3, 1, 'TOPIC_POINT', '["GAME_CLIENT"]');

INSERT INTO t_kb_article (kb_node_id, title, body_markdown) VALUES
(12, '线程池原理与最佳实践', '# 线程池\n\n## 核心参数\n\n- **corePoolSize**：核心线程数\n- **maximumPoolSize**：最大线程数\n- **keepAliveTime**：空闲线程存活时间\n- **workQueue**：任务队列\n- **threadFactory**：线程工厂\n- **handler**：拒绝策略\n\n## 拒绝策略\n\n1. **AbortPolicy**：直接抛出异常（默认）\n2. **CallerRunsPolicy**：调用者线程执行\n3. **DiscardPolicy**：直接丢弃\n4. **DiscardOldestPolicy**：丢弃最老任务'),
(22, 'Vue3响应式原理解析', '# Vue3响应式\n\n## Proxy 优势\n\n相比 Vue2 的 Object.defineProperty：\n\n- 监听整个对象而非单个属性\n- 可监听数组下标变化\n- 性能更好\n- 支持 Map/Set 等新类型\n\n## 核心流程\n\n1. reactive() 将对象转为 Proxy\n2. 访问属性时触发 track 收集依赖\n3. 修改属性时触发 trigger 通知更新'),
(32, '动态规划：背包问题', '# 背包问题\n\n## 0-1 背包\n\n```python\ndp[i][w] = max(dp[i-1][w], dp[i-1][w-w[i]] + v[i])\n```\n\n## 完全背包\n\n物品可重复选，循环顺序调整即可。'),
(42, 'Unity渲染管线基础', '# Unity渲染管线\n\n## SRP\n\n- **Built-in RP**：内置渲染管线\n- **URP**：通用渲染管线（跨平台、轻量）\n- **HDRP**：高清渲染管线（高质量PC/主机）\n\n## 渲染流程\n\n1. 剔除 Culling\n2. 渲染物体\n3. 后处理 Post-processing');

-- ========================================
-- 扩展：更多学习资源
-- ========================================
INSERT INTO t_learning_resource (position_code, title, description, resource_type, url, topic, difficulty) VALUES
('JAVA_BACKEND', '深入理解 Java 并发编程', 'AQS 原理与线程池', 'ARTICLE', 'https://javaguide.cn/java/concurrent/', '并发编程', 3),
('JAVA_BACKEND', 'JVM 调优实战指南', 'GC 算法与参数调优', 'ARTICLE', 'https://cloud.tencent.com/developer/article/2560537', 'JVM', 3),
('JAVA_BACKEND', 'Spring Cloud 微服务架构', '服务注册、配置中心、熔断限流', 'ARTICLE', 'http://icyfenix.cn/exploration/projects/microservice_arch_springcloud.html', 'Spring Cloud', 3),
('JAVA_BACKEND', 'MySQL索引优化实战', '索引原理、慢查询优化', 'ARTICLE', 'https://tech.meituan.com/2014/06/30/mysql-index.html', 'MySQL', 2),
('JAVA_BACKEND', 'Redis 深度历险', 'Redis 原理与应用', 'ARTICLE', 'https://pegasuswang.readthedocs.io/zh/latest/database/redis%E6%B7%B1%E5%BA%A6%E5%8E%86%E9%99%A9%E6%A0%B8%E5%BF%83%E5%8E%9F%E7%90%86%E5%92%8C%E5%BA%94%E7%94%A8%E5%AE%9E%E8%B7%B5/book/', 'Redis', 3),
('WEB_FRONTEND', 'Vue 3 组合式 API 最佳实践', 'Composition API 深入', 'ARTICLE', 'https://vuejs.org/guide/extras/composition-api-faq.html', 'Vue3', 2),
('WEB_FRONTEND', 'React Hooks 完全指南', 'useState, useEffect, useCallback 等', 'ARTICLE', 'https://zh-hans.react.dev/reference/react/hooks', 'React', 2),
('WEB_FRONTEND', '前端工程化与构建优化', 'Webpack/Vite、性能优化', 'ARTICLE', 'https://developer.aliyun.com/article/1636709', '工程化', 3),
('WEB_FRONTEND', '深入理解浏览器原理', '渲染流程、事件循环、安全策略', 'ARTICLE', 'https://developer.mozilla.org/zh-CN/docs/Web/Performance/Guides/How_browsers_work', '浏览器原理', 3),
('WEB_FRONTEND', 'CSS 布局技巧大全', 'Flex、Grid、居中方案', 'ARTICLE', 'https://www.ruanyifeng.com/blog/2019/03/grid-layout-tutorial.html', 'CSS', 1),
('PYTHON_ALGO', 'LeetCode 刷题指南', '数据结构与算法经典题目', 'ARTICLE', 'https://github.com/youngyangyang04/leetcode-master', '算法', 2),
('PYTHON_ALGO', '机器学习入门实战', 'Scikit-learn、模型训练、评估', 'ARTICLE', 'https://zhuanlan.zhihu.com/p/592174336', '机器学习', 2),
('PYTHON_ALGO', 'Python 高级特性详解', '装饰器、迭代器、生成器、元编程', 'ARTICLE', 'https://my.oschina.net/emacs_9551789/blog/18778773', 'Python', 2),
('PYTHON_ALGO', '推荐系统入门', '协同过滤、内容推荐、召回排序', 'ARTICLE', 'https://github.com/datawhalechina/fun-rec', '推荐系统', 3),
('PYTHON_ALGO', 'Python 数据结构与算法进阶', '常用算法与LeetCode实战', 'ARTICLE', 'https://datawhalechina.github.io/leetcode-notes/', '算法', 2),
('GAME_CLIENT', 'Unity 游戏开发实战', 'Unity引擎、场景搭建、脚本开发', 'ARTICLE', 'https://learn.unity.com/', 'Unity', 2),
('GAME_CLIENT', '游戏性能优化指南', '内存优化、Draw Call优化、GC优化', 'ARTICLE', 'https://unity.com/cn/how-to/best-practices-for-profiling-game-performance', '性能优化', 3),
('GAME_CLIENT', 'Shader 编程入门', 'Unity Shader、图形渲染基础', 'ARTICLE', 'https://onevcat.com/2013/07/shader-tutorial-1/', 'Shader', 3),
('GAME_CLIENT', '游戏网络同步技术', '帧同步、状态同步、预测回滚', 'ARTICLE', 'https://zhuanlan.zhihu.com/p/336869551', '网络同步', 3),
('GAME_CLIENT', 'Unity 2D/3D 游戏完整开发流程', '从零到项目实战', 'ARTICLE', 'https://learn.u3d.cn/', 'Unity', 2),
('GAME_CLIENT', 'Unity 高级渲染与特效', 'Shader Graph 与粒子系统', 'ARTICLE', 'https://www.bilibili.com/video/BV1sh41147zb/', '渲染', 3),
('GAME_CLIENT', 'Unity 移动端性能优化', '帧率与内存实战', 'ARTICLE', 'https://unity.com/cn/blog/engine-platform/updated-2022-lts-best-practice-guides', '性能优化', 3),
('GAME_CLIENT', 'Unity 网络游戏开发', '多人联机与同步机制', 'ARTICLE', 'https://developer.unity.cn/projects/5e09b1beedbc2a7c529491b3', '网络', 3),
('GAME_CLIENT', 'Unity 物理与动画系统', 'Rigidbody 与 Animator 进阶', 'ARTICLE', 'https://learn.unity.com/pathway/game-development', '物理动画', 2),
('GAME_CLIENT', 'Unity Shader 进阶与图形学', '自定义渲染管线基础', 'ARTICLE', 'https://zhuanlan.zhihu.com/p/46745694', 'Shader', 3),
('GAME_CLIENT', 'Unity 游戏开发零基础入门', 'Unity 下载安装、界面操作、简单游戏制作', 'VIDEO', 'https://www.youtube.com/watch?v=vOCCtfZtlzk', 'Unity入门', 1),
('GAME_CLIENT', 'Unity 3小时制作一个完整游戏', '初学者从零制作小游戏实战', 'VIDEO', 'https://www.youtube.com/watch?v=nPW6tKeapsM', 'Unity实战', 2),
('GAME_CLIENT', 'Unity Shader 入门精要', 'Shader基础、渲染流水线与编程', 'VIDEO', 'https://www.bilibili.com/video/BV1sh41147zb/', 'Shader', 3),
('GAME_CLIENT', 'Unity 性能优化全攻略', 'Profiler使用、DrawCall优化、内存管理', 'VIDEO', 'https://www.bilibili.com/video/BV11h4y117wW/', '性能优化', 3),
('GAME_CLIENT', 'Unity 多人联机网络同步框架', 'C#联机同步、Mirror等实战', 'VIDEO', 'https://www.bilibili.com/video/BV1oL4y1c7P9/', '网络同步', 3);
-- ========================================
-- 扩展：更多知识库文章（四岗位）
-- ========================================
INSERT INTO t_kb_article (kb_node_id, title, body_markdown) VALUES
(4, 'JVM 垃圾回收调优入门', '# GC 调优\n\n## 常见收集器\n\n- Serial / Parallel\n- CMS\n- G1\n- ZGC\n\n## 调优思路\n\n1. 明确停顿目标\n2. 观察 GC 日志\n3. 调整堆大小与比例\n4. 验证吞吐与延迟'),
(6, 'ConcurrentHashMap 原理', '# ConcurrentHashMap\n\nJDK8 采用分段 CAS + synchronized 桶锁，读操作基本无锁，写操作只锁单个桶。'),
(12, 'ThreadLocal 使用与内存泄漏', '# ThreadLocal\n\n每个线程维护独立副本。线程池场景必须 remove，否则可能引发内存泄漏。'),
(22, 'Vue Router 导航守卫', '# 路由守卫\n\n- `beforeEach` 全局前置\n- `beforeEnter` 路由独享\n- 组件内 `beforeRouteEnter`'),
(21, 'Pinia 状态管理实践', '# Pinia\n\n轻量、类型友好，推荐替代 Vuex。按模块拆分 store，避免巨型状态树。'),
(32, 'LeetCode 动态规划模板', '# DP 模板\n\n1. 定义状态\n2. 状态转移方程\n3. 初始化\n4. 返回目标状态'),
(31, '时间复杂度分析方法', '# 复杂度\n\n关注循环嵌套、递归深度、数据结构操作均摊成本。'),
(42, 'Draw Call 优化清单', '# Draw Call\n\n- 合并材质\n- 静态/动态批处理\n- 图集合批\n- LOD 与遮挡剔除');

-- ========================================
-- 演示账号：demo_student / demo123456
-- ========================================
INSERT INTO t_user (username, password, nickname, school, major, role, target_position_code, total_interviews) VALUES
('demo_student', '$2b$10$6S2zCfpoSMe2c8v22/p5uuMpj4oAAK/TbunDr7fJHK7oykce10IgO', '演示学生', '示例大学', '软件工程', 'USER', 'JAVA_BACKEND', 3);

SET @demo_user_id = LAST_INSERT_ID();

INSERT INTO t_interview_session (user_id, position_code, session_status, input_mode, total_questions, answered_count, duration_seconds, start_time, end_time) VALUES
(@demo_user_id, 'JAVA_BACKEND', 'COMPLETED', 'TEXT', 8, 8, 1800, DATE_SUB(NOW(), INTERVAL 14 DAY), DATE_SUB(NOW(), INTERVAL 14 DAY)),
(@demo_user_id, 'JAVA_BACKEND', 'COMPLETED', 'TEXT', 8, 8, 2100, DATE_SUB(NOW(), INTERVAL 7 DAY), DATE_SUB(NOW(), INTERVAL 7 DAY)),
(@demo_user_id, 'JAVA_BACKEND', 'COMPLETED', 'VOICE', 8, 8, 2400, DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY));

INSERT INTO t_evaluation_report (session_id, user_id, position_code, report_status, overall_score, tech_score, expression_score, logic_score, depth_score, confidence_score, summary, highlights, weaknesses, suggestions) VALUES
(1, @demo_user_id, 'JAVA_BACKEND', 'COMPLETED', 72.00, 70.00, 68.00, 74.00, 71.00, 69.00,
 '## 综合评估\n\n首次模拟面试整体达标，基础概念掌握尚可，表达与深度仍需加强。',
 '["Java 基础概念回答较完整", "场景题思路基本正确"]',
 '["并发与 JVM 深度不足", "项目描述缺乏量化结果"]',
 '["复习 JVM 与并发专题", "用 STAR 法则重写项目介绍"]'),
(2, @demo_user_id, 'JAVA_BACKEND', 'COMPLETED', 78.50, 76.00, 75.00, 80.00, 79.00, 77.00,
 '## 综合评估\n\n第二次面试较首次有明显进步，逻辑性提升明显。',
 '["追问响应更快", "系统设计题结构更清晰"]',
 '["Redis 与 MySQL 细节仍可加强"]',
 '["补充中间件实战案例", "继续练习模拟面试"]'),
(3, @demo_user_id, 'JAVA_BACKEND', 'COMPLETED', 85.00, 84.00, 82.00, 86.00, 85.00, 83.00,
 '## 综合评估\n\n第三次面试表现稳定，具备较好的岗位匹配度。',
 '["技术深度明显提升", "表达更自信有条理"]',
 '["极端场景下的排查经验仍可补充"]',
 '["保持练习节奏", "针对薄弱点做专项突破"]');

INSERT INTO t_growth_record (user_id, report_id, session_id, position_code, overall_score, tech_score, expression_score, logic_score, depth_score, confidence_score, record_date) VALUES
(@demo_user_id, 1, 1, 'JAVA_BACKEND', 72.00, 70.00, 68.00, 74.00, 71.00, 69.00, DATE_SUB(CURDATE(), INTERVAL 14 DAY)),
(@demo_user_id, 2, 2, 'JAVA_BACKEND', 78.50, 76.00, 75.00, 80.00, 79.00, 77.00, DATE_SUB(CURDATE(), INTERVAL 7 DAY)),
(@demo_user_id, 3, 3, 'JAVA_BACKEND', 85.00, 84.00, 82.00, 86.00, 85.00, 83.00, CURDATE());
