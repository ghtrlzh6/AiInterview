# AI 模拟面试与能力提升平台 — 文档导航指南

> 本文件是给 AI Agent 阅读的入口文件。请先阅读本文件，再按需读取其他文档。

## 项目概述

**项目名称**：AI 模拟面试与能力提升平台  
**赛题来源**：锐捷网络 · 2025 大学生软件设计竞赛  
**目标用户**：计算机相关专业在校学生  
**核心价值**：通过 AI 技术模拟真实企业面试场景，提供多维度即时反馈，帮助学生提升面试能力与就业竞争力

---

## 文档目录

| 文件 | 内容 | 阅读优先级 |
|------|------|------------|
| `ReadForAi.md`（本文件）| 文档导航、项目全局概述、开发约定 | 必读·第一 |
| `architecture.md` | 系统整体架构、模块划分、数据流向、部署拓扑 | 必读·第二 |
| `tech-stack.md` | 前后端技术选型、AI 服务选型、版本要求、依赖说明 | 必读·第三 |
| `database-design.md` | 所有数据库表结构、字段说明、索引设计、ER 关系 | 按需读取 |
| `api-design.md` | 全部 RESTful API 接口定义、请求/响应格式、鉴权规则 | 按需读取 |
| `development-plan.md` | 开发阶段划分、每阶段任务、里程碑节点、优先级排序 | 按需读取 |

---

## 快速上下文摘要

### 核心功能模块（4 个）

1. **岗位化题库与知识库** — 支持 Java 后端、Web 前端、Python 算法三个岗位，题型涵盖技术知识、项目深挖、场景题、行为题；知识库作为 RAG 检索基础
2. **多模态交互面试** — 支持语音 + 文字双输入，AI 面试官具备多轮对话、动态追问能力
3. **多维度评估分析** — 评估技术正确性、知识深度、逻辑严谨性、语言表达、自信度等，生成结构化评估报告
4. **个性化能力提升** — 基于历史面试数据分析短板，推荐学习资源，可视化成长曲线

### 目标岗位（3 个）

| 岗位 | 标识符 | 核心技术栈 |
|------|--------|------------|
| Java 后端开发工程师 | `JAVA_BACKEND` | Java、Spring Boot、MySQL、Redis、JVM、设计模式 |
| Web 前端开发工程师 | `WEB_FRONTEND` | HTML/CSS/JS、Vue3/React、工程化、性能优化 |
| Python 算法工程师 | `PYTHON_ALGO` | Python、数据结构与算法、机器学习、LeetCode 类题目 |

### 用户角色（2 种）

| 角色 | `t_user.role` 值 | 权限 |
|------|-----------------|------|
| 普通用户 | `USER` | 使用面试功能、查看自己的报告 |
| 管理员 | `ADMIN` | 全部用户权限 + 后台管理（题库/岗位/知识库/AI 配置/Prompt） |

- 管理员接口路径前缀：`/api/v1/admin/**`
- Spring Security 配置：`.requestMatchers("/api/v1/admin/**").hasRole("ADMIN")`
- 初始管理员账号：用户名 `admin`，密码 `admin123456`（通过 `sql/init.sql` 创建）

### 技术架构简述

```
前端（Vue 3）→ Nginx → Spring Boot 3.x 后端
后端 → MySQL（持久化）+ Redis（缓存/会话）
后端 → LLM API（DeepSeek / 通义千问）→ 对话管理 + 报告生成
后端 → ASR 服务（讯飞/WebSpeech）→ 语音识别
后端 → 向量数据库（Chroma）→ RAG 知识检索
```

---

## 开发约定（重要，所有代码生成必须遵守）

### 命名规范

- **Java 包名**：`com.aiinterview`
- **数据库表名**：统一前缀 `t_`，小写下划线命名，例如 `t_user`、`t_interview_session`
- **API 路径前缀**：`/api/v1/`
- **前端组件**：PascalCase，例如 `InterviewRoom.vue`
- **前端 store**：camelCase，例如 `useInterviewStore`

### 技术约定

- 后端 Java 版本：**JDK 21**
- Spring Boot 版本：**3.3.x**
- 数据库：**MySQL 8.0**（utf8mb4 字符集）
- ORM：**MyBatis-Plus 3.5.x**
- 认证：**JWT（无状态）**，Token 放 Authorization Header，格式 `Bearer <token>`
- 前端构建：**Vite 5.x**
- 前端框架：**Vue 3.4.x + TypeScript 5.x**
- CSS 方案：**Tailwind CSS + Element Plus**

### 接口约定

所有接口统一响应格式：
```json
{
  "code": 200,
  "message": "success",
  "data": { ... },
  "timestamp": 1715000000000
}
```

错误码规范：
- `200` — 成功
- `400` — 参数错误
- `401` — 未认证
- `403` — 无权限
- `404` — 资源不存在
- `500` — 服务器内部错误
- `600` — AI 服务调用失败

### 数据库约定

- 所有表必须包含：`id` (BIGINT, 主键自增)、`created_at` (DATETIME)、`updated_at` (DATETIME)、`is_deleted` (TINYINT, 逻辑删除)
- 使用 MyBatis-Plus 逻辑删除注解 `@TableLogic`
- 敏感数据（密码）使用 BCrypt 加密

---

## AI 服务集成说明

### LLM 调用
- 使用 OpenAI 兼容接口格式，便于切换模型
- 主模型：DeepSeek-V3（`deepseek-chat`）
- 所有 LLM 调用封装在 `com.aiinterview.service.ai.LlmService` 中
- Prompt 模板统一放在 `resources/prompts/` 目录下

### RAG 流程
1. 知识库文档 → 文本分块 → Embedding → 存入 Chroma
2. 用户提问 → Embedding → 向量检索 → 召回相关知识片段
3. 知识片段 + 用户回答 + 岗位上下文 → LLM → 生成评估结果

### 语音识别
- 浏览器端使用 Web Speech API（主方案）
- 备用：讯飞语音识别 WebSocket API
- 音频格式：WAV/PCM，采样率 16000Hz

---

## 阅读建议（针对不同开发任务）

| 任务类型 | 建议阅读的文档 |
|----------|----------------|
| 初始化项目结构 | `tech-stack.md` → `architecture.md` |
| 创建数据库 | `database-design.md` |
| 开发后端接口 | `api-design.md` → `database-design.md` |
| 开发前端页面 | `architecture.md` → `api-design.md` |
| 集成 AI 功能 | `architecture.md`（AI 模块部分）→ `api-design.md` |
| 制定开发计划 | `development-plan.md` |
| 全局理解项目 | 按顺序阅读全部文档 |
