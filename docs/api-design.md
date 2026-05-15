# API 接口设计文档

> 基础路径：`/api/v1`  
> 鉴权方式：JWT Bearer Token（除标注"无需鉴权"外，所有接口均需在 Header 中携带 `Authorization: Bearer <token>`）  
> 统一响应格式见 `ReadForAi.md` 的"接口约定"部分

---

## 1. 认证模块 `/api/v1/auth`

### 1.1 用户注册

```
POST /api/v1/auth/register
无需鉴权
```

**Request Body：**
```json
{
  "username": "zhangsan",
  "password": "Abc12345!",
  "nickname": "张三",
  "email": "zhangsan@example.com"
}
```

**Response Data：**
```json
{
  "userId": 1001,
  "username": "zhangsan",
  "nickname": "张三"
}
```

**校验规则：**
- `username`：4-20 位字母数字下划线，唯一
- `password`：8-20 位，需包含字母和数字
- `email`：合法邮箱格式（可选字段）

---

### 1.2 用户登录

```
POST /api/v1/auth/login
无需鉴权
```

**Request Body：**
```json
{
  "username": "zhangsan",
  "password": "Abc12345!"
}
```

**Response Data：**
```json
{
  "accessToken": "eyJhbGci...",
  "refreshToken": "eyJhbGci...",
  "expiresIn": 7200,
  "userInfo": {
    "userId": 1001,
    "username": "zhangsan",
    "nickname": "张三",
    "avatarUrl": "",
    "targetPositionCode": "JAVA_BACKEND"
  }
}
```

---

### 1.3 刷新 Token

```
POST /api/v1/auth/refresh
无需鉴权
```

**Request Body：**
```json
{
  "refreshToken": "eyJhbGci..."
}
```

**Response Data：**
```json
{
  "accessToken": "eyJhbGci...",
  "expiresIn": 7200
}
```

---

### 1.4 登出

```
POST /api/v1/auth/logout
需要鉴权
```

将当前 Token 加入 Redis 黑名单。

**Response Data：** `null`

---

## 2. 用户模块 `/api/v1/users`

### 2.1 获取当前用户档案

```
GET /api/v1/users/me
需要鉴权
```

**Response Data：**
```json
{
  "userId": 1001,
  "username": "zhangsan",
  "nickname": "张三",
  "avatarUrl": "https://...",
  "email": "zhangsan@example.com",
  "school": "某某大学",
  "major": "计算机科学与技术",
  "targetPositionCode": "JAVA_BACKEND",
  "targetPositionName": "Java后端开发工程师",
  "totalInterviews": 5,
  "createdAt": "2026-01-01T10:00:00"
}
```

---

### 2.2 更新用户档案

```
PUT /api/v1/users/me
需要鉴权
```

**Request Body（所有字段均为可选，仅更新提供的字段）：**
```json
{
  "nickname": "新昵称",
  "school": "某某大学",
  "major": "软件工程",
  "targetPositionCode": "WEB_FRONTEND"
}
```

**Response Data：** 返回更新后的完整用户档案（同 2.1）

---

### 2.3 获取用户面试历史列表

```
GET /api/v1/users/me/interviews?page=1&size=10&positionCode=JAVA_BACKEND
需要鉴权
```

**Query Params：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `page` | int | 否 | 页码，默认 1 |
| `size` | int | 否 | 每页条数，默认 10，最大 50 |
| `positionCode` | string | 否 | 按岗位筛选 |

**Response Data：**
```json
{
  "total": 5,
  "page": 1,
  "size": 10,
  "list": [
    {
      "sessionId": 20001,
      "positionCode": "JAVA_BACKEND",
      "positionName": "Java后端开发工程师",
      "sessionStatus": "COMPLETED",
      "overallScore": 78.5,
      "durationSeconds": 1800,
      "startTime": "2026-05-01T14:00:00",
      "endTime": "2026-05-01T14:30:00",
      "reportId": 30001
    }
  ]
}
```

---

## 3. 岗位模块 `/api/v1/positions`

### 3.1 获取所有岗位列表

```
GET /api/v1/positions
无需鉴权
```

**Response Data：**
```json
[
  {
    "id": 1,
    "code": "JAVA_BACKEND",
    "name": "Java后端开发工程师",
    "description": "负责服务端业务逻辑开发...",
    "techStack": ["Java", "Spring Boot", "MySQL", "Redis"],
    "iconUrl": ""
  }
]
```

---

### 3.2 获取岗位详情

```
GET /api/v1/positions/{code}
无需鉴权
```

返回单个岗位的完整信息，包含 `techStack` 完整列表。

---

## 4. 题库模块 `/api/v1/questions`

### 4.1 按岗位获取题目列表

```
GET /api/v1/questions?positionCode=JAVA_BACKEND&questionType=TECH_KNOWLEDGE&difficulty=2&page=1&size=20
无需鉴权
```

**Query Params：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `positionCode` | string | 是 | 岗位编码 |
| `questionType` | string | 否 | 题型 |
| `difficulty` | int | 否 | 难度 1/2/3 |
| `topic` | string | 否 | 知识点标签 |
| `page` | int | 否 | 默认 1 |
| `size` | int | 否 | 默认 20 |

**Response Data：**
```json
{
  "total": 100,
  "list": [
    {
      "id": 1,
      "positionCode": "JAVA_BACKEND",
      "title": "请解释 Java 中 HashMap 的底层实现原理，以及 JDK8 相比 JDK7 做了哪些优化？",
      "difficulty": 2,
      "questionType": "TECH_KNOWLEDGE",
      "topic": "集合框架"
    }
  ]
}
```

> 注意：`answerReference` 和 `followUpHints` 字段不在列表接口返回（防止作弊），仅在管理端或面试结束后报告中展示。

---

## 5. 面试模块 `/api/v1/interviews`

### 5.1 开始面试

```
POST /api/v1/interviews/start
需要鉴权
```

**Request Body：**
```json
{
  "positionCode": "JAVA_BACKEND",
  "inputMode": "TEXT",
  "questionCount": 8
}
```

**字段说明：**
- `positionCode`：岗位编码（必填）
- `inputMode`：`TEXT`（文字）或 `VOICE`（语音），默认 `TEXT`
- `questionCount`：本次面试题目数量，默认 8，范围 3-15

**Response Data：**
```json
{
  "sessionId": 20001,
  "positionCode": "JAVA_BACKEND",
  "positionName": "Java后端开发工程师",
  "totalQuestions": 8,
  "firstMessage": {
    "messageId": 1,
    "role": "ASSISTANT",
    "content": "你好！我是今天的面试官，很高兴认识你。我们今天进行的是 Java 后端开发工程师岗位的模拟面试，共有 8 道题目，请放松心态，我们开始吧。\n\n**第一题：**请介绍一下你对 Java 虚拟机（JVM）内存模型的理解？",
    "messageType": "QUESTION",
    "questionOrder": 1
  }
}
```

---

### 5.2 发送消息（对话核心接口）

```
POST /api/v1/interviews/{sessionId}/message
需要鉴权
Content-Type: application/json
```

**Path Params：** `sessionId` — 面试会话 ID

**Request Body：**
```json
{
  "content": "Java 虚拟机内存模型主要分为堆、栈、方法区、程序计数器和本地方法栈五个部分...",
  "messageType": "NORMAL"
}
```

**Response：** SSE（Server-Sent Events）流式响应

```
Content-Type: text/event-stream

data: {"type":"token","content":"好的"}
data: {"type":"token","content":"，你提到了"}
data: {"type":"token","content":"几个关键区域"}
data: {"type":"done","messageId":2,"messageType":"FOLLOW_UP","questionOrder":1}
```

**SSE 事件类型说明：**

| `type` | 说明 |
|--------|------|
| `token` | AI 回复的流式 token |
| `done` | 本轮回复结束，携带完整元信息 |
| `next_question` | 推进到下一题，`content` 为新题目内容 |
| `interview_end` | 面试结束，`reportId` 为生成中的报告 ID |
| `error` | 服务端错误 |

---

### 5.3 结束面试

```
POST /api/v1/interviews/{sessionId}/end
需要鉴权
```

可由用户主动触发，或由系统在答完所有题目后自动触发。

**Response Data：**
```json
{
  "sessionId": 20001,
  "reportId": 30001,
  "reportStatus": "GENERATING",
  "message": "面试已结束，正在生成评估报告，请稍候..."
}
```

---

### 5.4 获取面试会话详情

```
GET /api/v1/interviews/{sessionId}
需要鉴权（只能查询自己的会话）
```

**Response Data：**
```json
{
  "sessionId": 20001,
  "positionCode": "JAVA_BACKEND",
  "sessionStatus": "COMPLETED",
  "totalQuestions": 8,
  "answeredCount": 8,
  "durationSeconds": 1800,
  "startTime": "2026-05-01T14:00:00",
  "endTime": "2026-05-01T14:30:00"
}
```

---

### 5.5 获取面试对话记录

```
GET /api/v1/interviews/{sessionId}/messages
需要鉴权（只能查询自己的会话）
```

**Response Data：**
```json
{
  "sessionId": 20001,
  "messages": [
    {
      "messageId": 1,
      "role": "ASSISTANT",
      "content": "你好！我是今天的面试官...",
      "messageType": "QUESTION",
      "questionOrder": 1,
      "createdAt": "2026-05-01T14:00:05"
    },
    {
      "messageId": 2,
      "role": "USER",
      "content": "Java 虚拟机内存模型主要分为...",
      "messageType": "NORMAL",
      "questionOrder": 1,
      "createdAt": "2026-05-01T14:01:30"
    }
  ]
}
```

---

## 6. 语音识别模块 `/api/v1/asr`

### 6.1 语音转文字

```
POST /api/v1/asr/convert
需要鉴权
Content-Type: multipart/form-data
```

**Form Data：**

| 字段 | 类型 | 说明 |
|------|------|------|
| `audio` | File | 音频文件（WAV/WebM/OGG，≤10MB） |
| `sessionId` | string | 面试会话 ID（用于记录） |

**Response Data：**
```json
{
  "text": "Java 虚拟机内存模型主要分为堆、栈、方法区...",
  "duration": 12.5,
  "confidence": 0.95
}
```

---

## 7. 报告模块 `/api/v1/reports`

### 7.1 获取评估报告详情

```
GET /api/v1/reports/{reportId}
需要鉴权（只能查询自己的报告，或有分享token时可公开访问）
```

**Response Data：**
```json
{
  "reportId": 30001,
  "sessionId": 20001,
  "positionCode": "JAVA_BACKEND",
  "positionName": "Java后端开发工程师",
  "reportStatus": "COMPLETED",
  "overallScore": 78.5,
  "scores": {
    "tech": 80.0,
    "expression": 75.0,
    "logic": 82.0,
    "depth": 76.0,
    "confidence": 70.0
  },
  "summary": "## 综合评估\n\n本次面试整体表现良好...",
  "highlights": [
    "对 JVM 内存模型有扎实的理论基础",
    "能够结合实际项目经验阐述问题"
  ],
  "weaknesses": [
    "对并发编程的底层原理掌握尚浅",
    "系统设计题目缺乏全局思维"
  ],
  "suggestions": [
    "建议深入学习 Java 并发编程，重点掌握 AQS 原理",
    "推荐练习系统设计题，培养全局视角"
  ],
  "questionScores": [
    {
      "questionOrder": 1,
      "questionTitle": "请解释 JVM 内存模型...",
      "techScore": 85.0,
      "logicScore": 80.0,
      "depthScore": 78.0,
      "comment": "对基础概念掌握扎实，但未提及元空间（Metaspace）的变化..."
    }
  ],
  "durationSeconds": 1800,
  "createdAt": "2026-05-01T14:35:00"
}
```

---

### 7.2 获取报告列表（当前用户）

```
GET /api/v1/reports?page=1&size=10&positionCode=JAVA_BACKEND
需要鉴权
```

返回当前用户的所有已完成报告，支持分页和岗位筛选。

---

### 7.3 生成报告分享链接

```
POST /api/v1/reports/{reportId}/share
需要鉴权
```

**Response Data：**
```json
{
  "shareUrl": "http://localhost/share/abc123def456",
  "shareToken": "abc123def456"
}
```

---

### 7.4 通过分享 Token 访问报告

```
GET /api/v1/reports/share/{shareToken}
无需鉴权
```

返回精简版报告（隐藏部分敏感维度数据）。

---

## 8. 成长曲线模块 `/api/v1/growth`

### 8.1 获取能力成长数据

```
GET /api/v1/growth?positionCode=JAVA_BACKEND&days=90
需要鉴权
```

**Query Params：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `positionCode` | string | 否 | 岗位筛选（空则返回所有岗位） |
| `days` | int | 否 | 最近 N 天，默认 90 |

**Response Data：**
```json
{
  "positionCode": "JAVA_BACKEND",
  "records": [
    {
      "recordDate": "2026-04-01",
      "overallScore": 65.0,
      "techScore": 62.0,
      "expressionScore": 70.0,
      "logicScore": 68.0,
      "depthScore": 60.0,
      "confidenceScore": 65.0,
      "sessionId": 19990,
      "reportId": 29990
    },
    {
      "recordDate": "2026-05-01",
      "overallScore": 78.5,
      "techScore": 80.0,
      "expressionScore": 75.0,
      "logicScore": 82.0,
      "depthScore": 76.0,
      "confidenceScore": 70.0,
      "sessionId": 20001,
      "reportId": 30001
    }
  ],
  "trend": {
    "overallChange": 13.5,
    "strongestDimension": "logic",
    "weakestDimension": "confidence"
  }
}
```

---

## 9. 学习资源模块 `/api/v1/resources`

### 9.1 获取推荐资源（基于最新报告）

```
GET /api/v1/resources/recommendations?reportId=30001
需要鉴权
```

**Response Data：**
```json
{
  "reportId": 30001,
  "recommendations": [
    {
      "recommendationId": 40001,
      "resource": {
        "id": 501,
        "title": "深入理解 Java 并发编程 - AQS 原理详解",
        "description": "全面讲解 AbstractQueuedSynchronizer 的实现原理...",
        "resourceType": "ARTICLE",
        "url": "https://example.com/aqs-deep-dive",
        "topic": "并发编程",
        "difficulty": 3
      },
      "reason": "本次面试在并发编程维度得分较低，推荐补充学习"
    }
  ]
}
```

---

### 9.2 资源反馈（是否有帮助）

```
POST /api/v1/resources/recommendations/{recommendationId}/feedback
需要鉴权
```

**Request Body：**
```json
{
  "isHelpful": true
}
```

**Response Data：** `null`

---

### 9.3 搜索学习资源

```
GET /api/v1/resources?positionCode=JAVA_BACKEND&topic=并发编程&type=ARTICLE&page=1&size=10
无需鉴权
```

---

## 10. 错误响应示例

```json
{
  "code": 401,
  "message": "Token 已过期，请重新登录",
  "data": null,
  "timestamp": 1715000000000
}
```

```json
{
  "code": 400,
  "message": "参数校验失败",
  "data": {
    "errors": [
      { "field": "username", "message": "用户名长度必须在4-20个字符之间" }
    ]
  },
  "timestamp": 1715000000000
}
```

```json
{
  "code": 600,
  "message": "AI 服务暂时不可用，请稍后重试",
  "data": null,
  "timestamp": 1715000000000
}
```

---

## 11. 管理员模块 `/api/v1/admin`

> **所有 `/api/v1/admin/**` 接口均需要 ADMIN 角色**，普通用户访问返回 403。  
> Spring Security 配置：`.requestMatchers("/api/v1/admin/**").hasRole("ADMIN")`

---

### 11.1 管理员仪表盘统计

```
GET /api/v1/admin/stats
需要鉴权（ADMIN）
```

**Response Data：**
```json
{
  "totalUsers": 256,
  "totalInterviews": 1024,
  "todayInterviews": 38,
  "completedReports": 980,
  "positionStats": [
    { "positionCode": "JAVA_BACKEND", "count": 520 },
    { "positionCode": "WEB_FRONTEND", "count": 310 },
    { "positionCode": "PYTHON_ALGO",  "count": 194 }
  ]
}
```

---

### 11.2 岗位管理

```
GET    /api/v1/admin/positions              获取岗位列表（含已停用）
POST   /api/v1/admin/positions              新增岗位
PUT    /api/v1/admin/positions/{id}         修改岗位信息
PUT    /api/v1/admin/positions/{id}/status  启用/停用岗位
DELETE /api/v1/admin/positions/{id}         逻辑删除岗位
全部需要鉴权（ADMIN）
```

**POST/PUT Request Body：**
```json
{
  "code": "JAVA_BACKEND",
  "name": "Java后端开发工程师",
  "description": "...",
  "techStack": ["Java", "Spring Boot"],
  "sortOrder": 1
}
```

**PUT /status Request Body：**
```json
{ "isActive": true }
```

---

### 11.3 题目管理

```
GET    /api/v1/admin/questions              题目列表（含全部岗位，支持分页和过滤）
POST   /api/v1/admin/questions              新增题目
PUT    /api/v1/admin/questions/{id}         修改题目
DELETE /api/v1/admin/questions/{id}         删除题目（逻辑删除）
POST   /api/v1/admin/questions/batch-import 批量导入（JSON 数组）
全部需要鉴权（ADMIN）
```

**POST/PUT Request Body：**
```json
{
  "positionCode": "JAVA_BACKEND",
  "title": "请解释 HashMap 的底层实现...",
  "answerReference": "HashMap 底层是数组+链表/红黑树...",
  "difficulty": 2,
  "questionType": "TECH_KNOWLEDGE",
  "topic": "集合框架",
  "followUpHints": ["追问：什么情况下链表转红黑树？", "追问：HashCode 碰撞如何处理？"]
}
```

---

### 11.4 知识库管理

```
GET    /api/v1/admin/knowledge-docs           知识库文档列表
POST   /api/v1/admin/knowledge-docs           新增文档
PUT    /api/v1/admin/knowledge-docs/{id}      修改文档
DELETE /api/v1/admin/knowledge-docs/{id}      删除文档
POST   /api/v1/admin/knowledge-docs/{id}/vectorize  手动触发向量化入库
POST   /api/v1/admin/knowledge-docs/vectorize-all   批量向量化所有未入库文档
全部需要鉴权（ADMIN）
```

**POST/PUT Request Body：**
```json
{
  "positionCode": "JAVA_BACKEND",
  "title": "Java 并发编程 - AQS 原理详解",
  "content": "## AQS 简介\n\n...",
  "docType": "TECH_POINT",
  "topic": "并发编程"
}
```

**POST /vectorize Response Data：**
```json
{
  "docId": 101,
  "chunksCount": 8,
  "message": "向量化完成，已写入 Chroma collection: ai_interview_java_backend"
}
```

---

### 11.5 AI 配置管理

```
GET  /api/v1/admin/ai-config        获取所有 AI 相关配置（敏感字段掩码）
PUT  /api/v1/admin/ai-config        批量更新 AI 配置
GET  /api/v1/admin/ai-config/test   测试 LLM 连通性
全部需要鉴权（ADMIN）
```

**GET Response Data（`is_sensitive=1` 的值掩码）：**
```json
[
  { "key": "ai.llm.provider",    "value": "deepseek",                    "type": "STRING", "sensitive": false },
  { "key": "ai.llm.api-key",     "value": "sk-**********************key", "type": "STRING", "sensitive": true  },
  { "key": "ai.llm.temperature", "value": "0.7",                         "type": "STRING", "sensitive": false }
]
```

**PUT Request Body（仅更新提供的 key）：**
```json
[
  { "key": "ai.llm.api-key",     "value": "sk-新的APIKey" },
  { "key": "ai.llm.temperature", "value": "0.8" }
]
```

**GET /test Response Data：**
```json
{
  "success": true,
  "model": "deepseek-chat",
  "latencyMs": 320,
  "message": "LLM 服务连接正常"
}
```

---

### 11.6 Prompt 模板管理

```
GET  /api/v1/admin/prompts          获取所有 Prompt 模板列表
GET  /api/v1/admin/prompts/{key}    获取单个 Prompt 模板
PUT  /api/v1/admin/prompts/{key}    更新 Prompt 模板内容
POST /api/v1/admin/prompts/{key}/preview  预览 Prompt 渲染结果（填充示例变量）
全部需要鉴权（ADMIN）
```

**PUT Request Body：**
```json
{
  "value": "你是一位专业严肃的技术面试官，正在对{positionName}岗位候选人进行面试...",
  "description": "面试官系统提示词"
}
```

**POST /preview Request Body：**
```json
{
  "variables": {
    "positionName": "Java后端开发工程师",
    "totalQuestions": "8",
    "currentOrder": "1",
    "questionTitle": "请解释 JVM 内存模型"
  }
}
```

**POST /preview Response Data：**
```json
{
  "rendered": "你是一位专业严肃的技术面试官，正在对Java后端开发工程师岗位候选人进行面试..."
}
```

---

### 11.7 用户管理

```
GET  /api/v1/admin/users                获取用户列表（支持分页、按用户名搜索）
GET  /api/v1/admin/users/{id}           获取用户详情
PUT  /api/v1/admin/users/{id}/role      修改用户角色（USER/ADMIN）
全部需要鉴权（ADMIN）
```

**PUT /role Request Body：**
```json
{ "role": "ADMIN" }
```

---

### 11.8 学习资源管理

```
GET    /api/v1/admin/resources          资源列表
POST   /api/v1/admin/resources          新增资源
PUT    /api/v1/admin/resources/{id}     修改资源
DELETE /api/v1/admin/resources/{id}     删除资源
全部需要鉴权（ADMIN）
```

---

## 附录：接口汇总

### 普通用户接口

| 模块 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 认证 | POST | /auth/register | 注册 |
| 认证 | POST | /auth/login | 登录 |
| 认证 | POST | /auth/refresh | 刷新 Token |
| 认证 | POST | /auth/logout | 登出 |
| 用户 | GET | /users/me | 获取个人档案 |
| 用户 | PUT | /users/me | 更新个人档案 |
| 用户 | GET | /users/me/interviews | 面试历史 |
| 岗位 | GET | /positions | 岗位列表 |
| 岗位 | GET | /positions/{code} | 岗位详情 |
| 题库 | GET | /questions | 题目列表 |
| 面试 | POST | /interviews/start | 开始面试 |
| 面试 | POST | /interviews/{id}/message | 发送消息（SSE） |
| 面试 | POST | /interviews/{id}/end | 结束面试 |
| 面试 | GET | /interviews/{id} | 会话详情 |
| 面试 | GET | /interviews/{id}/messages | 对话记录 |
| ASR | POST | /asr/convert | 语音转文字 |
| 报告 | GET | /reports/{id} | 报告详情 |
| 报告 | GET | /reports | 报告列表 |
| 报告 | POST | /reports/{id}/share | 生成分享链接 |
| 报告 | GET | /reports/share/{token} | 分享报告访问 |
| 成长 | GET | /growth | 成长曲线数据 |
| 资源 | GET | /resources/recommendations | 推荐资源 |
| 资源 | POST | /resources/recommendations/{id}/feedback | 资源反馈 |
| 资源 | GET | /resources | 搜索资源 |

### 管理员接口（ADMIN 角色）

| 模块 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 统计 | GET | /admin/stats | 仪表盘统计 |
| 岗位管理 | GET/POST | /admin/positions | 列表/新增 |
| 岗位管理 | PUT/DELETE | /admin/positions/{id} | 修改/删除 |
| 题目管理 | GET/POST | /admin/questions | 列表/新增 |
| 题目管理 | PUT/DELETE | /admin/questions/{id} | 修改/删除 |
| 题目管理 | POST | /admin/questions/batch-import | 批量导入 |
| 知识库 | GET/POST | /admin/knowledge-docs | 列表/新增 |
| 知识库 | PUT/DELETE | /admin/knowledge-docs/{id} | 修改/删除 |
| 知识库 | POST | /admin/knowledge-docs/{id}/vectorize | 向量化入库 |
| AI 配置 | GET/PUT | /admin/ai-config | 查看/更新 AI 配置 |
| AI 配置 | GET | /admin/ai-config/test | 测试 LLM 连通性 |
| Prompt | GET/PUT | /admin/prompts/{key} | 查看/更新 Prompt |
| Prompt | POST | /admin/prompts/{key}/preview | 预览渲染结果 |
| 用户管理 | GET | /admin/users | 用户列表 |
| 用户管理 | PUT | /admin/users/{id}/role | 修改用户角色 |
| 资源管理 | GET/POST | /admin/resources | 列表/新增 |
| 资源管理 | PUT/DELETE | /admin/resources/{id} | 修改/删除 |
