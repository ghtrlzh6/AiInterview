# B角色API集成指南

## 一、语音识别（ASR）API

### 1.1 语音转文字接口

**接口**: `POST /api/v1/asr/convert`

**参数**:
- `audio` (MultipartFile): 音频文件
- `format` (string, optional): 音频格式，默认 "wav"，支持 "webm", "wav", "mp3", "pcm"
- `sampleRate` (int, optional): 采样率，默认 16000
- `sessionId` (string, optional): 会话ID

**响应**:
```json
{
  "code": 200,
  "data": {
    "text": "识别的文字内容",
    "duration": 12.5,
    "confidence": 0.95,
    "isMock": false
  }
}
```

### 1.2 检查语音识别能力

**接口**: `GET /api/v1/asr/capability`

**响应**:
```json
{
  "code": 200,
  "data": {
    "supported": true,
    "webSpeechAvailable": true,
    "fallbackEnabled": true
  }
}
```

### 1.3 前端集成示例（已实现）

```typescript
// 使用Web Speech API录音，然后调用后端API转写
async function uploadAudio(blob: Blob, sessionId?: number) {
  const form = new FormData()
  form.append('audio', blob, 'recording.webm')
  if (sessionId) form.append('sessionId', String(sessionId))
  const res = await convertAsr(form)
  return res.text
}
```

---

## 二、题库与知识库关联API

### 2.1 获取题目列表

**接口**: `GET /api/v1/questions`

**参数**:
- `positionCode` (string): 岗位代码 (JAVA_BACKEND/WEB_FRONTEND/PYTHON_ALGO/GAME_CLIENT)
- `questionType` (string, optional): 题型 (TECH_KNOWLEDGE/SCENARIO/PROJECT_DEEP/BEHAVIOR)
- `difficulty` (int, optional): 难度 (1/2/3)
- `kbModuleId` (long, optional): 知识库模块ID
- `page` (int, optional): 页码，默认1
- `size` (int, optional): 每页大小，默认20

### 2.2 获取岗位对应的面试官Prompt

**接口**: `GET /api/v1/questions/prompt/{positionCode}`

**响应**:
```json
{
  "code": 200,
  "data": {
    "positionCode": "JAVA_BACKEND",
    "prompt": "你是一位专业严谨的Java后端面试官..."
  }
}
```

### 2.3 获取岗位知识库节点列表

**接口**: `GET /api/v1/questions/kb-nodes/{positionCode}`

**响应**:
```json
{
  "code": 200,
  "data": [
    {
      "id": 12,
      "title": "线程池原理",
      "slug": "thread-pool",
      "codePath": "/java-backend/java-concurrency/thread-pool",
      "summaryExcerpt": "..."
    }
  ]
}
```

### 2.4 关联题目与知识库节点

**接口**: `POST /api/v1/questions/{questionId}/kb-bind`

**请求体**:
```json
[12, 15, 18]
```

### 2.5 自动关联岗位题目与知识库（基于topic匹配）

**接口**: `POST /api/v1/questions/auto-bind/{positionCode}`

**响应**:
```json
{
  "code": 200,
  "data": {
    "positionCode": "JAVA_BACKEND",
    "bindCount": 45
  }
}
```

---

## 三、讯飞ASR配置说明

### 3.1 环境变量配置

```bash
# 启用讯飞ASR
XUNFEI_ENABLED=true

# 讯飞应用配置
XUNFEI_APP_ID=your_app_id
XUNFEI_API_KEY=your_api_key
XUNFEI_API_SECRET=your_api_secret
XUNFEI_ASR_URL=https://api.xf-yun.com/asr
```

### 3.2 降级策略

当前端不支持Web Speech API时，会自动降级调用 `/api/v1/asr/convert` 接口。

---

## 四、岗位差异化Prompt配置

### 4.1 支持的岗位代码

- `JAVA_BACKEND` - Java后端开发工程师
- `WEB_FRONTEND` - Web前端开发工程师
- `PYTHON_ALGO` - Python算法工程师
- `GAME_CLIENT` - 游戏客户端开发工程师

### 4.2 Prompt配置方式

在 `t_system_config` 表中配置：

| config_key | 说明 |
|------------|------|
| `prompt.interview.system.java_backend` | Java后端面试官Prompt |
| `prompt.interview.system.web_frontend` | Web前端面试官Prompt |
| `prompt.interview.system.python_algo` | Python算法面试官Prompt |
| `prompt.interview.system.game_client` | 游戏客户端面试官Prompt |
| `prompt.interview.system` | 默认面试官Prompt |

---

## 五、前端集成检查清单

### 5.1 语音组件集成

- [x] VoiceInput.vue 组件已实现
- [x] 调用 `/api/v1/asr/convert` 接口
- [x] 支持 Web Speech API 录音
- [x] 不支持时自动降级

### 5.2 待前端实现

- [ ] 面试间页面根据岗位显示不同面试官形象
- [ ] 面试间页面根据岗位显示不同欢迎语
- [ ] 面试间页面显示岗位对应的知识库推荐

---

## 六、数据统计

### 6.1 已完成的数据库更新

- **题目数量**: 100道（每岗位25道）
- **手撕题**: 20道
- **Prompt配置**: 4个岗位差异化配置
- **知识库节点**: 4个岗位知识体系
- **学习资源**: 18个

### 6.2 API端点汇总

| 模块 | 接口数 | 状态 |
|------|--------|------|
| ASR | 2 | 已完成 |
| 题库 | 6 | 已完成 |
| Prompt | 1 | 已完成 |
| 合计 | 9 | - |

---

## 七、联系方式

如有问题，请联系角色B开发人员。
