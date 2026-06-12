# AI 模拟面试与能力提升平台

> 锐捷网络 · 2025 大学生软件设计竞赛  
> 通过 AI 技术模拟真实企业面试，提供多维度即时反馈与个性化能力提升建议。

---

## 项目结构

```
AiInterview/
├── backend/          # Spring Boot 3.3 后端（Java 21）
├── frontend/         # Vue 3 + TypeScript 前端
├── sql/              # 数据库建表与种子数据
│   └── init.sql      # 完整 DDL + DML（19 张表）
├── docs/             # 设计文档（架构/API/数据库/开发计划）
├── docker-compose.yml # 本地 MySQL + Redis + Chroma
└── .env.example      # 环境变量模板（远端/密钥留空）
```

---

## 快速启动（本地开发）

### 前置要求

| 工具 | 版本 | 说明 |
|------|------|------|
| JDK | 21 | 后端编译运行 |
| Maven | 3.9+ | 后端构建 |
| Node.js | 18+ | 前端构建 |
| Docker | 可选 | 一键启动 MySQL/Redis/Chroma |

### 第一步：启动基础设施

**方式 A — Docker Compose（推荐）**

```bash
# 在项目根目录
docker-compose up -d

# 等待 MySQL 健康检查通过后，数据库已自动执行 sql/init.sql
```

**方式 B — 手动连接远端 MySQL（租服务器后）**

1. 在远端 MySQL 执行 `sql/init.sql`
2. 复制 `.env.example` 为 `.env`，填写 `DB_HOST`、`DB_USERNAME`、`DB_PASSWORD`
3. 启动 Redis（登出黑名单功能需要）

### 第二步：启动后端

```bash
cd backend

# Windows PowerShell 示例（本地 Docker MySQL）
$env:DB_PASSWORD="dev123456"
# LLM_API_KEY 留空时使用模拟 AI 回复，不影响流程演示
# $env:LLM_API_KEY="sk-你的DeepSeek密钥"

mvn spring-boot:run
```

后端地址：`http://localhost:8080`  
Swagger 文档：`http://localhost:8080/swagger-ui.html`  
OpenAPI JSON：`http://localhost:8080/v3/api-docs`

### 第三步：启动前端

```bash
cd frontend
npm install
npm run dev
```

前端地址：`http://localhost:5173`（开发模式自动代理 `/api` → `8080`）

### 默认账号

| 角色 | 用户名 | 密码 | 说明 |
|------|--------|------|------|
| 管理员 | `admin` | `admin123456` | 可访问 `/admin` 管理后台 |
| 普通用户 | 自行注册 | — | 注册页创建 |

---

## 使用指南

### 普通用户流程

1. **注册 / 登录** → 首页仪表盘
2. **选择岗位** → 四岗位可选（Java 后端 / Web 前端 / Python 算法 / 游戏客户端）
3. **开始模拟面试** → AI 面试官逐题提问，支持文字 / 语音输入
4. **对话交互** → AI 可追问（每题最多 2 次），答完自动推进下一题
5. **结束面试** → 异步生成评估报告（约 10~60 秒）
6. **查看报告** → 综合得分、五维雷达图、逐题点评、改进建议
7. **成长曲线** → 历次面试能力变化折线图
8. **知识库** → 侧边目录树 + Markdown 正文阅读
9. **学习资源** → 报告页底部个性化推荐

### 管理员流程

1. 使用 `admin` 账号登录
2. 访问 **管理后台**（侧边栏或 `/admin`）
3. 可管理：岗位、题库、知识库、学习资源、AI 配置与 Prompt、用户角色
4. **AI 配置页**：填写 DeepSeek API Key 后保存，点击「测试连通性」验证（配置热更新，无需重启后端）
5. **学习资源管理**：`/admin/resources` 对学习资源增删改查

### 未配置 DeepSeek API 时

- 面试对话：使用内置模拟回复（规则引擎 + 模板），流程可完整跑通
- 评估报告：基于规则生成模拟分数与点评
- 配置 API Key 后（环境变量 `LLM_API_KEY` 或管理后台）自动切换为真实 LLM

---

## Swagger / API 接口

### 文档地址

| 地址 | 说明 |
|------|------|
| `http://localhost:8080/swagger-ui.html` | 可视化 Swagger UI |
| `http://localhost:8080/v3/api-docs` | OpenAPI 3.0 JSON |

### 鉴权方式

除标注「无需鉴权」外，请求头携带：

```
Authorization: Bearer <accessToken>
```

登录接口返回 `accessToken`（有效期 2 小时）和 `refreshToken`（7 天）。

### 统一响应格式

```json
{
  "code": 200,
  "message": "success",
  "data": { },
  "timestamp": 1715000000000
}
```

| code | 含义 |
|------|------|
| 200 | 成功 |
| 400 | 参数错误 |
| 401 | 未认证 |
| 403 | 无权限 |
| 404 | 资源不存在 |
| 500 | 服务器错误 |
| 600 | AI 服务调用失败 |

### 接口汇总

#### 用户端

| 模块 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 认证 | POST | `/api/v1/auth/register` | 注册 |
| 认证 | POST | `/api/v1/auth/login` | 登录 |
| 认证 | POST | `/api/v1/auth/refresh` | 刷新 Token |
| 认证 | POST | `/api/v1/auth/logout` | 登出 |
| 用户 | GET | `/api/v1/users/me` | 个人档案 |
| 用户 | PUT | `/api/v1/users/me` | 更新档案 |
| 用户 | GET | `/api/v1/users/me/interviews` | 面试历史 |
| 岗位 | GET | `/api/v1/positions` | 岗位列表 |
| 岗位 | GET | `/api/v1/positions/{code}` | 岗位详情 |
| 知识库 | GET | `/api/v1/kb/tree` | 类目子树 |
| 知识库 | GET | `/api/v1/kb/nodes/{id}` | 节点详情 |
| 知识库 | GET | `/api/v1/kb/articles/{id}` | 正文 Markdown |
| 简历 | POST | `/api/v1/resumes/upload` | 上传 PDF |
| 简历 | GET | `/api/v1/resumes/{id}` | 解析状态 |
| 简历 | GET | `/api/v1/resumes/{id}/projects` | 项目条目 |
| 题库 | GET | `/api/v1/questions` | 题目列表 |
| 面试 | POST | `/api/v1/interviews/start` | 开始面试 |
| 面试 | POST | `/api/v1/interviews/{id}/message` | 发送消息（SSE 流式） |
| 面试 | POST | `/api/v1/interviews/{id}/coding-submit` | 手撕代码提交 |
| 面试 | POST | `/api/v1/interviews/{id}/end` | 结束面试 |
| 面试 | GET | `/api/v1/interviews/{id}` | 会话详情 |
| 面试 | GET | `/api/v1/interviews/{id}/messages` | 对话记录 |
| ASR | POST | `/api/v1/asr/convert` | 语音转文字 |
| 报告 | GET | `/api/v1/reports/{id}` | 报告详情 |
| 报告 | GET | `/api/v1/reports` | 报告列表 |
| 报告 | POST | `/api/v1/reports/{id}/share` | 生成分享链接 |
| 报告 | GET | `/api/v1/reports/share/{token}` | 分享访问 |
| 成长 | GET | `/api/v1/growth` | 成长曲线数据 |
| 资源 | GET | `/api/v1/resources/recommendations` | 推荐资源 |
| 资源 | POST | `/api/v1/resources/recommendations/{id}/feedback` | 反馈 |
| 资源 | GET | `/api/v1/resources` | 搜索资源 |

#### 管理员（需 ADMIN 角色）

| 模块 | 方法 | 路径 |
|------|------|------|
| 统计 | GET | `/api/v1/admin/stats` |
| 岗位 | CRUD | `/api/v1/admin/positions/**` |
| 题目 | CRUD | `/api/v1/admin/questions/**` |
| 知识库 | CRUD | `/api/v1/admin/kb/**` |
| AI 出题 | POST | `/api/v1/admin/ai/questions/generate` |
| AI 配置 | GET/PUT | `/api/v1/admin/ai-config` |
| AI 测试 | GET | `/api/v1/admin/ai-config/test` |
| Prompt | GET/PUT | `/api/v1/admin/prompts/{key}` |
| 用户 | GET/PUT | `/api/v1/admin/users/**` |
| 资源 | CRUD | `/api/v1/admin/resources/**` |

> 完整请求/响应字段见 `docs/api-design.md`

---

## 架构与实现逻辑

### 整体架构

```
浏览器 (Vue 3 SPA)
    ↓ HTTP / SSE
Spring Boot 后端
    ├── MySQL 8.0（持久化）
    ├── Redis 7（Token 黑名单 / 缓存）
    ├── Chroma（RAG 向量检索，可选）
    └── DeepSeek API（LLM，Key 留空时降级为模拟）
```

### 模块划分（低耦合设计）

后端按 **垂直模块** 拆分，每层只依赖下层接口：

```
controller/     → 接收 HTTP，参数校验，调用 Service
service/        → 业务逻辑，模块间通过接口通信
mapper/         → MyBatis-Plus 数据访问
entity/         → 数据库实体
dto/ / vo/      → 请求响应对象，与 Entity 隔离
service/ai/     → AI 能力独立封装（LlmService / RagService / AiEvaluationService）
```

| 模块 | 职责 | 核心类 |
|------|------|--------|
| AuthModule | 注册登录 JWT | `AuthController`, `JwtUtil` |
| UserModule | 用户档案、面试历史 | `UserController`, `UserService` |
| QuestionModule | 题库查询与抽取 | `QuestionController`, `QuestionService` |
| KbModule | 知识库树与正文 | `KbController`, `KbService` |
| InterviewModule | 会话、SSE 对话、手撕提交 | `InterviewController`, `FollowUpStrategy` |
| EvalModule | 异步多维度评分 | `AiEvaluationService` |
| ReportModule | 报告查询与分享 | `ReportController` |
| GrowthModule | 成长曲线聚合 | `GrowthController` |
| ResourceModule | 学习资源推荐 | `ResourceController` |
| Admin* | 管理后台 CRUD | `controller/admin/*` |

**解耦要点：**
- AI 调用统一走 `LlmService` 接口，切换模型只需换实现类
- 面试策略可插拔（`FollowUpStrategy`），与 Controller 解耦
- 评估异步执行（`@Async`），不阻塞面试结束响应
- 前端 API 按模块分文件（`src/api/*.ts`），与页面组件分离

### 核心业务流程

#### 1. 面试流程

```
选岗位 → POST /interviews/start
  → 按难度比例抽题（TECH/SCENARIO/PROJECT/BEHAVIOR）
  → 创建 session + 题目序列
  → LLM 生成开场白 + 第一题
  → 用户回答 → POST /message（SSE 流式）
  → FollowUpStrategy 判断：追问 / 下一题 / 结束
  → POST /end → 触发异步评估
  → 前端轮询 GET /reports/{id}
```

#### 2. SSE 流式对话

`POST /interviews/{id}/message` 返回 `text/event-stream`：

```
data: {"type":"token","content":"好的"}
data: {"type":"done","messageId":2,"messageType":"FOLLOW_UP","questionOrder":1}
data: {"type":"next_question","content":"第二题：..."}
data: {"type":"interview_end","reportId":30001}
```

#### 3. 评估报告生成

```
面试结束 → AiEvaluationService.evaluate() [@Async]
  → 逐题 LLM 评分 → t_dimension_score
  → 汇总综合报告 → t_evaluation_report
  → 写入 t_growth_record
  → 匹配弱项 → t_user_recommendation
```

### 前端架构

```
src/
├── api/          # Axios 封装，按模块分文件
├── stores/       # Pinia 全局状态（auth / interview）
├── layouts/      # 布局（Main / Admin / Auth）
├── pages/        # 页面组件
├── components/   # 可复用组件（图表、语音、连接状态）
├── router/       # 路由 + 守卫
└── utils/        # SSE 工具、请求拦截
```

---

## 数据库

- **库名**：`ai_interview`
- **字符集**：`utf8mb4_unicode_ci`
- **表数量**：19 张（前缀 `t_`）
- **脚本**：`sql/init.sql`（DDL + 种子数据）

### 初始化

```bash
mysql -u root -p < sql/init.sql
```

### 远端部署（租服务器后）

1. 在远端 MySQL 执行 `sql/init.sql`
2. 修改 `.env` 或环境变量：

```env
DB_HOST=你的远端IP
DB_USERNAME=你的用户名
DB_PASSWORD=你的密码
LLM_API_KEY=sk-你的DeepSeek密钥
```

3. 后端 `application.yml` 已通过 `${DB_HOST}` 等占位符读取，无需改代码

---

## 生产部署（远端 MySQL + Nginx）

完整步骤见 **[docs/deployment.md](docs/deployment.md)**，包含远端 MySQL、`.env` 配置、Nginx 反向代理与 systemd 守护。

```
Nginx (:80/:443)
  ├── /          → 前端静态文件 (frontend/dist)
  └── /api       → 反向代理 → Spring Boot (:8080)
```

Nginx 配置模板：`deploy/nginx/ai-interview.conf`

快速构建：

```bash
# 后端
cd backend && mvn package -DskipTests
java -jar target/ai-interview-backend-1.0.0-SNAPSHOT.jar

# 前端
cd frontend && npm run build
# 将 dist/ 复制到 Nginx root（见 deployment.md）
```

---

## 环境变量清单

| 变量 | 必填 | 说明 | 当前状态 |
|------|------|------|----------|
| `DB_HOST` | 是 | MySQL 地址 | 本地 `localhost` |
| `DB_USERNAME` | 是 | 数据库用户 | `root` |
| `DB_PASSWORD` | 是 | 数据库密码 | 见 `.env.example` |
| `REDIS_HOST` | 是 | Redis 地址 | 本地 `localhost` |
| `JWT_SECRET` | 是 | JWT 签名密钥 | 有默认值 |
| `LLM_API_KEY` | 否 | DeepSeek API Key | **留空，使用模拟** |
| `CHROMA_HOST` | 否 | 向量库地址 | 可选 |
| `XUNFEI_*` | 否 | 讯飞 ASR | 可选 |

---

## 设计文档

| 文件 | 内容 |
|------|------|
| `docs/ReadProject.md` | 项目导航与开发约定 |
| `docs/architecture.md` | 系统架构与模块划分 |
| `docs/api-design.md` | 完整 API 定义 |
| `docs/database-design.md` | 数据库表结构 |
| `docs/tech-stack.md` | 技术选型 |
| `docs/development-plan.md` | 开发计划与 Sprint 任务 |
| `docs/team-task-allocation.md` | 四人全栈分工与剩余任务（各管各模块前后端） |
| `docs/deployment.md` | 远端 MySQL + Nginx 生产部署指南 |

---

## 已知限制与后续计划

- [x] 远端 MySQL / Nginx 部署文档与配置模板（见 `docs/deployment.md`）
- [x] 管理后台学习资源管理页 + AI 配置热更新与连通性测试
- [ ] 实际上线租服务器部署（按 deployment.md 执行）
- [ ] DeepSeek API Key 配置（待提供密钥，可在管理后台填写）
- [ ] Chroma RAG 向量化（接口已预留，待接真实 Embedding）
- [ ] 讯飞 ASR 备用方案（主方案为浏览器 Web Speech API）

---

## 常见问题

**Q: 后端启动报 Redis 连接失败？**  
A: 启动 Redis：`docker-compose up -d redis` 或本地 `redis-server`

**Q: 面试 AI 回复是固定模板？**  
A: 未配置 `LLM_API_KEY` 时使用模拟模式。在管理后台 AI 配置页填入 Key 即可切换真实 LLM

**Q: 如何重新初始化数据库？**  
A: `docker-compose down -v && docker-compose up -d`（会清空数据并重新执行 init.sql）
