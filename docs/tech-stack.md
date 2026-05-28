# 技术栈说明

## 1. 技术选型总览

| 层次 | 技术 | 版本 | 用途 |
|------|------|------|------|
| 前端框架 | Vue 3 | 3.4.x | SPA 界面 |
| 前端语言 | TypeScript | 5.x | 类型安全 |
| 前端构建 | Vite | 5.x | 开发服务器 + 构建 |
| UI 组件库 | Element Plus | 2.x | 通用 UI 组件 |
| CSS 框架 | Tailwind CSS | 3.x | 原子化样式 |
| 状态管理 | Pinia | 2.x | 全局状态 |
| HTTP 客户端 | Axios | 1.x | API 请求封装 |
| 图表库 | ECharts | 5.x | 成长曲线可视化 |
| 后端语言 | Java | 21 (LTS) | 后端业务逻辑 |
| 后端框架 | Spring Boot | 3.3.x | Web 框架 |
| 安全框架 | Spring Security | 6.x | 认证鉴权 |
| ORM | MyBatis-Plus | 3.5.x | 数据库访问 |
| 主数据库 | MySQL | 8.0 | 持久化存储 |
| 缓存数据库 | Redis | 7.x | 缓存 / 限流 |
| 向量数据库 | Chroma | 0.5.x | RAG 知识检索 |
| 连接池 | HikariCP | 内置 | 数据库连接池 |
| 主 LLM | DeepSeek-V3 | - | 对话 + 评估 + 报告 |
| 备用 LLM | 通义千问-Max | - | 可切换 |
| Embedding | text-embedding-3 / DeepSeek Embed | - | 文本向量化 |
| 语音识别 | Web Speech API | 浏览器原生 | 主 ASR 方案 |
| 语音识别备用 | 讯飞 WebSocket ASR | - | 备用 ASR |
| 反向代理 | Nginx | 1.26.x | 静态托管 + 代理 |
| 容器化 | Docker + Docker Compose | - | 本地开发 + 部署 |

---

## 2. 后端依赖详情

### 2.1 核心依赖（pom.xml）

```xml
<!-- Spring Boot 父 POM -->
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.3.4</version>
</parent>

<!-- Web -->
<dependency>spring-boot-starter-web</dependency>
<!-- WebSocket / SSE（用于流式推送） -->
<dependency>spring-boot-starter-websocket</dependency>
<!-- Spring Security + JWT -->
<dependency>spring-boot-starter-security</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.6</version>
</dependency>
<!-- MyBatis-Plus -->
<dependency>
    <groupId>com.baomidou</groupId>
    <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
    <version>3.5.7</version>
</dependency>
<!-- MySQL 驱动 -->
<dependency>com.mysql:mysql-connector-j</dependency>
<!-- Redis -->
<dependency>spring-boot-starter-data-redis</dependency>
<!-- 参数校验 -->
<dependency>spring-boot-starter-validation</dependency>
<!-- Hutool（工具类） -->
<dependency>
    <groupId>cn.hutool</groupId>
    <artifactId>hutool-all</artifactId>
    <version>5.8.x</version>
</dependency>
<!-- OkHttp（LLM API 调用） -->
<dependency>
    <groupId>com.squareup.okhttp3</groupId>
    <artifactId>okhttp</artifactId>
    <version>4.12.0</version>
</dependency>
<!-- Chroma Java 客户端 -->
<dependency>
    <groupId>io.github.amikos-tech</groupId>
    <artifactId>chromadb-java-client</artifactId>
    <version>0.1.8</version>
</dependency>
<!-- Lombok -->
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <scope>provided</scope>
</dependency>
```

### 2.2 application.yml 关键配置项

```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/ai_interview?useUnicode=true&characterEncoding=utf8mb4&serverTimezone=Asia/Shanghai
    username: ${DB_USERNAME:root}
    password: ${DB_PASSWORD:}
    driver-class-name: com.mysql.cj.jdbc.Driver
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: 6379
      password: ${REDIS_PASSWORD:}
      database: 0

mybatis-plus:
  mapper-locations: classpath*:mapper/**/*.xml
  global-config:
    db-config:
      id-type: auto
      logic-delete-field: isDeleted
      logic-delete-value: 1
      logic-not-delete-value: 0
  configuration:
    map-underscore-to-camel-case: true

# JWT 配置
jwt:
  secret: ${JWT_SECRET:your-256-bit-secret-here}
  expire: 7200        # Access Token 有效期（秒）
  refresh-expire: 604800  # Refresh Token 有效期（秒）

# AI 服务配置
ai:
  llm:
    provider: deepseek         # deepseek | tongyi
    api-key: ${LLM_API_KEY:}
    base-url: https://api.deepseek.com/v1
    model: deepseek-chat
    embed-model: deepseek-embed
    max-tokens: 4096
    temperature: 0.7
  asr:
    provider: web-speech        # web-speech | xunfei
    xunfei:
      app-id: ${XUNFEI_APP_ID:}
      api-key: ${XUNFEI_API_KEY:}
      api-secret: ${XUNFEI_API_SECRET:}
  chroma:
    host: ${CHROMA_HOST:localhost}
    port: 8000
    collection-prefix: ai_interview_

# 文件上传（音频）
spring:
  servlet:
    multipart:
      max-file-size: 10MB
      max-request-size: 10MB
```

---

## 3. 前端依赖详情

### 3.1 package.json 关键依赖

```json
{
  "dependencies": {
    "vue": "^3.4.0",
    "vue-router": "^4.3.0",
    "pinia": "^2.1.0",
    "axios": "^1.7.0",
    "element-plus": "^2.7.0",
    "@element-plus/icons-vue": "^2.3.0",
    "echarts": "^5.5.0",
    "vue-echarts": "^7.0.0",
    "dayjs": "^1.11.0",
    "marked": "^12.0.0"
  },
  "devDependencies": {
    "@vitejs/plugin-vue": "^5.0.0",
    "vite": "^5.3.0",
    "typescript": "^5.4.0",
    "tailwindcss": "^3.4.0",
    "autoprefixer": "^10.4.0",
    "postcss": "^8.4.0",
    "@types/node": "^20.0.0",
    "unplugin-auto-import": "^0.18.0",
    "unplugin-vue-components": "^0.27.0"
  }
}
```

### 3.2 Vite 配置要点

- 配置路径别名 `@` → `src/`
- 配置开发代理：`/api` → `http://localhost:8080`
- 使用 `unplugin-auto-import` 自动导入 Vue/Pinia API
- 使用 `unplugin-vue-components` 自动导入 Element Plus 组件

---

## 4. AI 服务集成说明

### 4.1 DeepSeek API

- 兼容 OpenAI 接口格式（`/v1/chat/completions`）
- 支持流式输出（`stream: true` → SSE）
- Embedding 接口：`/v1/embeddings`（向量维度 1536）
- 官方文档：https://platform.deepseek.com/docs

**调用示例（Java）：**
```java
// 封装在 LlmService 中，外部统一通过此类调用
llmService.chatStream(messages, callback);       // 流式对话
llmService.chat(messages);                        // 同步对话
llmService.embed(text);                           // 获取向量
```

### 4.2 Chroma 向量数据库

- 以 HTTP 模式运行（Docker 启动）
- **推荐**使用 **单一业务 Collection**（如 `ai_interview_kb`），靠 `metadata.position_codes`、`kb_node_id`、`code_path`、`article_id` 过滤；亦可继续按岗位拆分 Collection（与早期文档兼容）
- 旧版 `ai_interview_java_backend` 等与扁平 `t_knowledge_doc` 对齐；**新层级知识库以 article 切块为主**

### 4.3 Web Speech API

- 浏览器原生支持（Chrome/Edge），无需额外费用
- 使用 `SpeechRecognition` 接口，设置 `lang: 'zh-CN'`
- 连续模式 `continuous: true` + 中间结果 `interimResults: true`
- 兼容性检测：如不支持则降级到讯飞 ASR

---

## 5. 数据库版本与字符集要求

```sql
-- 创建数据库
CREATE DATABASE ai_interview
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

-- MySQL 版本要求：>= 8.0
-- 关键特性依赖：JSON 类型、窗口函数、CTE
```

---

## 6. Docker Compose 服务配置

```yaml
# docker-compose.yml（开发环境）
version: '3.8'
services:
  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: dev123456
      MYSQL_DATABASE: ai_interview
    ports:
      - "3306:3306"
    volumes:
      - mysql_data:/var/lib/mysql
      - ./sql/init.sql:/docker-entrypoint-initdb.d/init.sql

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"

  chroma:
    image: chromadb/chroma:0.5.3
    ports:
      - "8000:8000"
    volumes:
      - chroma_data:/chroma/chroma

volumes:
  mysql_data:
  chroma_data:
```

---

## 7. 环境变量清单

| 变量名 | 说明 | 示例值 |
|--------|------|--------|
| `DB_USERNAME` | 数据库用户名 | `root` |
| `DB_PASSWORD` | 数据库密码 | `dev123456` |
| `REDIS_HOST` | Redis 主机 | `localhost` |
| `REDIS_PASSWORD` | Redis 密码 | 空（本地开发） |
| `JWT_SECRET` | JWT 签名密钥（≥32字符） | 随机生成 |
| `LLM_API_KEY` | LLM 服务 API Key | DeepSeek 控制台获取 |
| `CHROMA_HOST` | Chroma 主机 | `localhost` |
| `XUNFEI_APP_ID` | 讯飞 ASR App ID | 讯飞控制台获取 |
| `XUNFEI_API_KEY` | 讯飞 ASR API Key | 讯飞控制台获取 |
| `XUNFEI_API_SECRET` | 讯飞 ASR API Secret | 讯飞控制台获取 |
