# AI 模拟面试平台 — 前端

Vue 3 + TypeScript + Vite 5 单页应用。

## 快速开始

```bash
cd frontend
npm install
npm run dev
```

开发服务器：`http://localhost:5173`，API 代理 `/api` → `http://localhost:8080`。

## 技术栈

- Vue 3.4、TypeScript、Vite 5
- Pinia、Vue Router、Element Plus、Tailwind CSS
- Axios、ECharts、marked

## 目录结构

见 `docs/architecture.md` 前端章节：`src/api`、`stores`、`layouts`、`pages`、`components`、`router`。

## 环境变量

| 变量 | 说明 |
|------|------|
| `VITE_API_BASE_URL` | API 前缀，默认 `/api/v1` |

## 构建

```bash
npm run build
```

产物在 `dist/`。
