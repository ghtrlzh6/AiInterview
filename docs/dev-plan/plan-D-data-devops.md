# 开发计划 — 数据工程 + 运维支援（人员 D）

## 角色定位

**职责范围**：所有 SQL 数据文件（题库 / 知识库 / 资源 / 演示数据）+ Docker/部署环境 + 集成测试 + 演示脚本准备

你是团队的**数据工程师 + 质量保障**。你的工作有两个显著特点：
1. **高度独立**：题库 SQL 文件只需要看 `database-design.md` 就能开始写，不依赖任何代码完成
2. **全程穿插**：每个 Sprint 末尾需要配合其他人做集成验收测试，发现跨层问题

**Sprint 0 期间**：你可以立即开始写 Java 后端题库数据（Task 1.1），因为表结构已经在 `database-design.md` 中明确定义。你不需要等任何人。

---

## 与其他成员的依赖关系

| 依赖方向 | 内容 | 时间节点 |
|----------|------|----------|
| **A → D**（你等待） | Task 0.2 完成（DDL），才能实际执行你的 SQL 文件验证 | Sprint 0 末 |
| **D → A**（你提供） | Task 1.1 完成：Java 题库 SQL 就绪，A 的面试流程有真实数据测试 | Sprint 1 初 |
| **D → A**（你提供） | Task 4.1/4.2 SQL 完成：其他两岗位数据就绪 | Sprint 4 |
| **D → A**（你提供） | Task 5.6 资源 SQL 完成：A 的 ResourceService 推荐逻辑有数据 | Sprint 5 初 |
| **D → B**（你提供） | Task 5.1 知识库 SQL 完成：B 的 RagService 向量化才有文档可处理 | Sprint 5 初 |
| **D → C**（你提供） | Task 5.8 demo 数据就绪：C 的演示视频录制有完整数据 | Sprint 5 末 |
| **D ↔ 全队**（协调） | 每 Sprint 末集成测试：发现跨层问题并协调解决 | 全程 |

---

## 时间线概览

| 周次 | Sprint | 你的主要任务 |
|------|--------|-------------|
| 第 1 周 | Sprint 0 | 开始写 Java 题库 SQL（Task 1.1）；配合 A 验证 docker-compose |
| 第 2-3 周 | Sprint 1 | 完成 Task 1.1（30 题）；Sprint 1 末集成测试 |
| 第 4 周 | Sprint 2 | Sprint 2 集成测试；整理接口约定文档 |
| 第 5 周 | Sprint 3 | Sprint 3 集成测试（报告流程端到端）|
| 第 6 周 | Sprint 3.5 | Sprint 3.5 集成测试（管理后台）|
| 第 6-7 周 | Sprint 4 | Task 4.1（Web 前端 SQL）；Task 4.2（Python SQL）；Task 4.2 Prompt 路由协作 |
| 第 7-8 周 | Sprint 5 | Task 5.1（知识库 SQL）；Task 5.8（Demo 数据 + 演示脚本）|

---

## Sprint 0：准备阶段（第 1 周）

### 可立即开始的工作

#### Task 1.1 前期准备 — 开始编写 Java 题目数据

`t_question` 表结构（来自 database-design.md）：
```sql
INSERT INTO t_question 
  (position_code, question_type, difficulty, title, content, answer_reference, topic, source)
VALUES
  ('JAVA_BACKEND', 'TECH_KNOWLEDGE', 'MEDIUM', '...', '...', '...', 'JVM', 'MANUAL');
```

**难度分布**（Java 后端 30 题）：
- 简单（EASY）：9 题（30%）
- 中等（MEDIUM）：15 题（50%）
- 困难（HARD）：6 题（20%）

**题型分布**（4 种均有）：
- `TECH_KNOWLEDGE`（技术知识）：15 题 — JVM、并发、Spring、MySQL、Redis 等
- `PROJECT_DEEP`（项目深挖）：5 题 — 设计决策类
- `SCENARIO`（场景题）：5 题 — 系统设计、排障等
- `BEHAVIOR`（行为题）：5 题 — 团队协作、冲突处理等

**题目主题建议**（Java 后端）：
| topic | 推荐题数 |
|-------|---------|
| JVM | 4 |
| 并发 | 4 |
| Spring | 4 |
| MySQL | 4 |
| Redis | 3 |
| 设计模式 | 2 |
| 系统设计 | 3 |
| 行为面试 | 6 |

### 配合工作

#### docker-compose 验证
等人员 A 完成 Task 0.1 后，帮助验证 `docker-compose up` 环境：
- 确认 MySQL 8.0 健康检查通过
- 确认 Redis 7 可连接（`redis-cli ping` 返回 PONG）
- 确认 Chroma 0.5 可访问（`curl http://localhost:8000/api/v1/heartbeat`）
- 记录任何环境问题并帮助排查

#### 接口约定文档维护
在 `docs/api-contract.md` 中（可新建）维护前后端接口约定：
- 每个接口的请求/响应 JSON 示例
- 字段名称和类型约定
- 当前状态（已实现/开发中/待实现）

---

## Sprint 1：Java 题库数据（第 2-3 周）

### Task 1.1 — Java 后端题库 SQL 数据（完成 30 题）🔴

**涉及文件**
- `sql/data_java_backend.sql`（30 条 INSERT INTO t_question）

**实现内容**：覆盖全部 4 种题型，难度分布简单 9 / 中等 15 / 困难 6，每题含 `answer_reference`（参考答案，100-300 字）。

**题目质量要求**：
- `content` 字段：面试官实际提问的话，1-3 句，口语化
- `answer_reference` 字段：关键点列举，面试官评分参考用，不需要非常完整，但核心知识点需覆盖
- `topic` 字段：与知识库文档 `topic` 字段保持一致（Sprint 5 RAG 检索依赖此关联）

**Java 后端题目示例**：
```sql
INSERT INTO t_question (position_code, question_type, difficulty, title, content, answer_reference, topic, source) VALUES
('JAVA_BACKEND', 'TECH_KNOWLEDGE', 'MEDIUM', 
 'Java 内存模型中的可见性问题', 
 '能说说 Java 内存模型中的可见性问题是什么，以及 volatile 关键字是如何解决这个问题的吗？',
 '可见性指一个线程修改共享变量后，其他线程能立即看到最新值。JMM 规定每个线程有工作内存，主内存修改不一定立即同步。volatile 通过写操作触发缓存同步（Store Barrier）、读操作强制从主内存读（Load Barrier），保证可见性但不保证原子性。适用于标志位场景，不适用于 i++ 等复合操作。',
 'JMM', 'MANUAL');
```

**完成标志**：执行后 `SELECT COUNT(*) FROM t_question WHERE position_code='JAVA_BACKEND';` = 30；分题型/难度 COUNT 分布正确。

### Sprint 1 集成测试

Sprint 1 末（人员 A 完成 Task 1.6，C 完成 Task 1.8 后），执行以下集成验收：

**端到端测试清单**：
- [ ] 注册新用户 → 登录 → 进入岗位选择
- [ ] 选 Java 后端 → 开始面试 → 确认第 1 题出现
- [ ] 回答 5 题 → 确认题号进度更新
- [ ] 点击结束面试 → 看到结束占位页
- [ ] 数据库检查：`t_interview_session` 1 条，`t_chat_message` 10+ 条

**常见问题记录**：
| 问题 | 排查方向 |
|------|---------|
| CORS 错误 | 检查 `SecurityConfig` 的 CORS 配置，或 Vite 代理配置 |
| 401 Unauthorized | 检查前端 Axios 拦截器是否正确注入 Bearer Token |
| 接口返回 500 | 检查后端日志，通常是 SQL 报错或空指针 |
| 题目抽取失败 | 确认 Task 1.1 的 SQL 已执行，数据存在 |

---

## Sprint 2：集成测试（第 4 周）

### Sprint 2 集成测试

Sprint 2 末（A 完成 2.3，C 完成 2.5 后），执行 SSE 联调验收：

**SSE 流式测试清单**：
- [ ] 开始面试 → 回答第一题 → AI 回复有打字机效果
- [ ] 回答"不知道" → AI 应发出追问（B 的 Task 2.2 FollowUpStrategy）
- [ ] 连续追问 3 次后 → AI 应推进到下一题（不应无限追问）
- [ ] 所有题目答完 → 自动跳转结束页
- [ ] 模拟断网（Chrome DevTools → Network → Offline）→ 3 秒后自动重连

**性能观察**：
- AI 第一个 token 到达时间应 < 3 秒（记录慢响应情况）
- 整个 10 题面试总时长参考（估计 15-25 分钟）

---

## Sprint 3：集成测试（第 5 周）

### Sprint 3 集成测试

Sprint 3 末（B 完成 3.3，A 完成 3.4，C 完成 3.6 后），执行报告流程端到端验收：

**报告流程测试清单**：
- [ ] 完成一次面试 → 结束页出现轮询 Loading
- [ ] 等待 < 60 秒 → 自动跳转报告详情页
- [ ] 报告页显示：综合得分 / 等级标签 / 雷达图 / 总结 / 亮点不足建议 / 逐题点评
- [ ] 雷达图 5 个维度数值与数据库 `t_dimension_score` 一致
- [ ] 历史报告列表显示刚刚生成的报告
- [ ] 数据库检查：`t_evaluation_report` status=COMPLETED，`t_growth_record` 有记录

**评估质量检查**（主观判断）：
- 点评内容与用户实际回答是否相关（非通用模板）
- 各维度分数是否有差异（不应全部相同）

---

## Sprint 3.5：集成测试（第 6 周）

### Sprint 3.5 集成测试

**管理后台测试清单**：
- [ ] 普通用户访问 `/admin` → 被拦截，显示提示
- [ ] admin 账号访问 `/admin` → 仪表盘加载统计数据
- [ ] 题目管理：新增一题 → 立即在列表看到 → 发起新面试（可能抽到该题）
- [ ] 停用 Java 后端岗位 → 前台岗位选择页 Java 后端消失（或变灰）
- [ ] AI 配置：修改 temperature=0.3 → 保存 → 测试连通性 → 应正常
- [ ] Prompt 编辑：修改 interview_system Prompt → 保存 → 发起新面试 → 确认 Prompt 已变

---

## Sprint 4：Web 前端 + Python 题库数据（第 6-7 周）

### Task 4.1 — Web 前端题库 SQL 数据 🔴

**涉及文件**
- `sql/data_web_frontend.sql`（30 条 INSERT INTO t_question，positionCode='WEB_FRONTEND'）

**题目主题建议**（Web 前端 30 题）：
| topic | 推荐题数 |
|-------|---------|
| Vue | 5 |
| React | 3 |
| JavaScript | 5 |
| CSS | 3 |
| 浏览器 | 3 |
| 工程化 | 3 |
| 性能优化 | 3 |
| 行为面试 | 5 |

**完成标志**：`SELECT COUNT(*) FROM t_question WHERE position_code='WEB_FRONTEND';` = 30。

---

### Task 4.2（SQL 部分）— Python 算法题库 SQL 数据 🔴

**涉及文件**
- `sql/data_python_algo.sql`（20 条 INSERT INTO t_question，positionCode='PYTHON_ALGO'）

> 注意：Task 4.2 中的 Prompt 文件（interview_system_web.txt / interview_system_python.txt）由**人员 B** 负责；SQL 数据文件由你负责；`InterviewService` 按 positionCode 选择 Prompt 的路由逻辑由**人员 A** 负责。

**题目主题建议**（Python 算法 20 题）：
| topic | 推荐题数 |
|-------|---------|
| 数据结构 | 5 |
| 算法 | 5 |
| Python 特性 | 4 |
| 机器学习基础 | 3 |
| 行为面试 | 3 |

**完成标志**：`SELECT COUNT(*) FROM t_question WHERE position_code='PYTHON_ALGO';` = 20。

### Sprint 4 集成测试

**三岗位验收清单**：
- [ ] Web 前端岗位：发起面试 → 题目涵盖 Vue/JS/CSS 等前端内容
- [ ] Python 算法岗位：发起面试 → 题目涵盖数据结构/算法等内容
- [ ] 三岗位各完成一次完整面试并生成报告
- [ ] 各岗位面试官 Prompt 风格差异明显（与 B 验证）

---

## Sprint 5：知识库 + 演示数据（第 7-8 周）

### Task 5.1 — 知识库文档内容录入 🟡

**涉及文件**
- `sql/data_knowledge_docs.sql`（每岗位 10+ 篇，共 35+ 条，INSERT INTO t_knowledge_doc）

**`t_knowledge_doc` 表结构**：
```sql
INSERT INTO t_knowledge_doc 
  (position_code, doc_type, topic, title, content, tags, is_vectorized)
VALUES
  ('JAVA_BACKEND', 'KNOWLEDGE', 'JVM', 
   'JVM 内存模型详解', 
   '## JVM 内存区域\n...',  -- Markdown 格式，500-2000字
   'JVM,内存,GC', 
   0);
```

**文档规划**（Java 后端 12 篇 + Web 前端 12 篇 + Python 算法 11 篇 = 35 篇）：

Java 后端知识库：
- JVM 内存模型与 GC 机制（KNOWLEDGE）
- volatile 和 synchronized 原理（KNOWLEDGE）
- Spring IoC/AOP 核心原理（KNOWLEDGE）
- MySQL 索引优化实战（KNOWLEDGE）
- Redis 缓存设计模式（KNOWLEDGE）
- 分布式锁实现方案（KNOWLEDGE）
- 优秀 JVM 问题回答示例（ANSWER_EXAMPLE）× 3
- 系统设计场景优秀回答示例（ANSWER_EXAMPLE）× 3

> `topic` 字段**必须与 `t_question` 中的 `topic` 字段一致**（如都用 "JVM"），RAG 检索依赖此关联。

**完成标志**：三个岗位各有 10+ 条知识库文档，`is_vectorized=false`；`SELECT COUNT(*) FROM t_knowledge_doc;` ≥ 35。

---

### Task 5.6（数据部分）— 学习资源 SQL 数据 🟡

**涉及文件**
- `sql/data_resources.sql`（每岗位 15 条，共 45 条，INSERT INTO t_learning_resource）

> 注意：`ResourceService` 推荐逻辑由**人员 A**（Task 5.6 代码部分）负责；数据文件由你负责。

**`t_learning_resource` 表结构**：
```sql
INSERT INTO t_learning_resource
  (position_code, resource_type, topic, title, url, description, difficulty_level)
VALUES
  ('JAVA_BACKEND', 'ARTICLE', 'JVM', 
   '深入理解 Java 虚拟机 - GC 篇',
   'https://example.com/jvm-gc',
   'JVM 垃圾收集机制的原理与调优实践',
   'ADVANCED');
```

**资源规划**（每岗位 15 条）：
- ARTICLE（文章）：8 条
- QUESTION（练习题/LeetCode）：4 条
- VIDEO（视频）：3 条

**URL 建议**：使用真实可访问的链接（如掘金文章、LeetCode 题目、B站视频），避免填写虚假链接。

**完成标志**：三岗位各 15 条，`SELECT COUNT(*) FROM t_learning_resource;` = 45。

---

### Task 5.8 — 演示数据预置 + 演示脚本验证 🔴

**涉及文件**
- `sql/data_demo.sql`（demo 账号 + 3 次历史面试完整数据）
- `docs/demo-script.md`（5 分钟演示脚本详细步骤）

**实现内容**

`data_demo.sql` 内容：
1. **demo 账号**：
   ```sql
   INSERT INTO t_user (username, password, nickname, role, target_position)
   VALUES ('demo_student', '{bcrypt}$2a$10$...', '小明同学', 'USER', 'JAVA_BACKEND');
   ```

2. **3 次历史面试数据**（成长曲线需要 3+ 数据点）：
   - 第 1 次（2 周前）：综合得分 62，各维度 60-65
   - 第 2 次（1 周前）：综合得分 75，各维度 70-78
   - 第 3 次（3 天前）：综合得分 83，各维度 80-87
   - 对应 `t_interview_session`、`t_interview_question`、`t_chat_message`、`t_evaluation_report`、`t_dimension_score`、`t_growth_record` 全部预置

3. **确保演示第一题为经典 JVM 题**（调整 demo 演示用的题目 SQL seed）

**`docs/demo-script.md` 演示脚本结构**：
```markdown
## 5 分钟演示脚本

### 准备工作（演示前）
- [ ] demo_student 账号登录正常
- [ ] 3 次历史成长数据可见
- [ ] 所有 Docker 服务健康

### 第 1 分钟：用户注册/登录
- 打开浏览器，访问 http://localhost:5173
- （可选）展示注册流程（15s）
- 使用 demo_student / demo123456 登录
- 首页展示成长曲线"最近得分 83"

### 第 2-3 分钟：发起 Java 后端面试
- 点击"开始面试" → 选择 Java 后端
- AI 开场白 + 第一题（JVM 相关）
- 展示打字机效果（约 15s）
- 回答"JVM 内存分为堆、栈、方法区…"
- AI 追问一次（展示追问功能）
- 再回答，AI 推进到第二题

### 第 4 分钟：查看评估报告
- 切换到历史报告页，点击第 3 次面试报告
- 展示雷达图（5 维度）
- 展示综合总结（Markdown 渲染）
- 展示逐题点评（展开 2-3 题）
- 展示推荐资源区块

### 第 5 分钟：成长曲线 + 管理后台
- 切换到成长曲线页，展示 3 次上升趋势
- （可选）切换到管理后台，展示 Prompt 编辑功能（30s）
- 总结：系统核心功能演示完毕
```

**完成标志**：5 分钟演示脚本完整演练通过（全队至少演练 2 遍）；demo_student 成长曲线有 3 个数据点；演示无卡顿。

### Sprint 5 验收标准
- [ ] 知识库 35+ 条文档录入完成，topic 与题库一致
- [ ] 推荐资源 45 条录入，URL 可访问
- [ ] demo 数据预置，成长曲线 3 数据点可见
- [ ] 5 分钟演示脚本演练通过（无卡顿）

---

## 集成测试总览

每个 Sprint 末你需要执行端到端集成测试，记录问题并推动解决：

| Sprint | 集成测试重点 | 预计时间 |
|--------|------------|---------|
| S0 末 | 注册/登录流程，docker 环境 | 30 min |
| S1 末 | 面试全流程（选岗→答题→结束）| 1 h |
| S2 末 | SSE 打字机 + 追问场景 + 断线重连 | 1 h |
| S3 末 | 报告生成 + 详情页 + 历史列表 | 1 h |
| S3.5 末 | 管理后台全功能 | 1 h |
| S4 末 | 三岗位完整面试 + 语音输入 | 1.5 h |
| S5 末 | 全流程演练 + 演示脚本通过 | 2 h |

---

## 你的任务汇总

| 任务 | Sprint | 优先级 | 前置依赖 |
|------|--------|--------|---------|
| Task 1.1 Java 后端题库（30 题）| S0→S1 | 🔴 | 无（可立即开始）|
| Sprint 1 集成测试 | S1 末 | 🔴 | A+C 完成 S1 任务 |
| Sprint 2 集成测试 | S2 末 | 🔴 | A+B+C 完成 S2 任务 |
| Sprint 3 集成测试 | S3 末 | 🔴 | A+B+C 完成 S3 任务 |
| Sprint 3.5 集成测试 | S3.5 末 | 🟡 | A+C 完成 S3.5 任务 |
| Task 4.1 Web 前端题库（30 题）| S4 | 🔴 | 无 |
| Task 4.2 Python 算法题库（20 题）| S4 | 🔴 | 无 |
| Sprint 4 集成测试 | S4 末 | 🔴 | A+B+C+D 完成 S4 任务 |
| Task 5.1 知识库文档（35+ 篇）| S5 初 | 🟡 | 无 |
| Task 5.6 学习资源 SQL（45 条）| S5 初 | 🟡 | 无 |
| Task 5.8 Demo 数据 + 演示脚本 | S5 末 | 🔴 | 全部功能完成后 |

---

## 题库数据编写指南

### 答案参考（answer_reference）写法

`answer_reference` 是供 AI 评分参考的，**不需要给用户看**，写法应该是：
- 列出 3-5 个核心知识点
- 关键字/概念要准确
- 100-300 字即可，不需要完整文章

**示例（差）**：
```
volatile关键字可以保证变量的可见性。
```

**示例（好）**：
```
核心要点：①JMM工作内存/主内存模型 ②volatile写操作插入StoreStore和StoreLoad屏障确保写后刷新主内存 ③volatile读操作插入LoadLoad和LoadStore屏障确保从主内存读 ④保证可见性但不保证原子性（区别于synchronized）⑤适用场景：状态标志位、双重检查锁（DCL）
```

### topic 字段一致性

**非常重要**：`t_question.topic` 和 `t_knowledge_doc.topic` 的值必须完全一致，RAG 检索功能通过这两个字段做关联。

建议维护一个 topic 枚举表（可在此文档中记录）：

| 岗位 | topic 值 |
|------|---------|
| JAVA_BACKEND | JVM / 并发 / Spring / MySQL / Redis / 设计模式 / 系统设计 / 行为面试 |
| WEB_FRONTEND | Vue / React / JavaScript / CSS / 浏览器 / 工程化 / 性能优化 / 行为面试 |
| PYTHON_ALGO | 数据结构 / 算法 / Python特性 / 机器学习 / 行为面试 |

### SQL 文件格式规范

每个 SQL 文件开头加注释：
```sql
-- =============================================
-- 文件：data_java_backend.sql
-- 说明：Java 后端岗位题库数据
-- 题目数量：30 条
-- 题型分布：TECH_KNOWLEDGE(15) / PROJECT_DEEP(5) / SCENARIO(5) / BEHAVIOR(5)
-- 难度分布：EASY(9) / MEDIUM(15) / HARD(6)
-- 最后更新：yyyy-mm-dd
-- =============================================

SET NAMES utf8mb4;
```
