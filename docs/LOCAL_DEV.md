# 本地开发快速指南

## 1. 环境文件

项目根目录 `.env` 已配置（**勿提交 Git**）：

| 变量 | 说明 |
|------|------|
| `DB_HOST` | 远端 MySQL `8.139.252.135` |
| `DB_USERNAME` / `DB_PASSWORD` | 数据库账号 |
| `LLM_API_KEY` | DeepSeek API Key |
| `REDIS_HOST` | 本地 Redis `localhost` |

## 2. 启动 Redis（本地必需）

后端登出黑名单等功能依赖 Redis，请先启动：

```powershell
# 项目根目录，需已安装 Docker
docker-compose up -d redis
```

## 3. 测试数据库连接

```powershell
powershell -ExecutionPolicy Bypass -File scripts/test-db.ps1
```

若报错 `Access denied for user 'root'@'你的公网IP'`：说明远端 MySQL 只允许 root 在本机登录。在**远端服务器** MySQL 中执行（root 登录后）：

```sql
-- 方案 A：允许 root 从任意 IP（开发用，生产请收紧）
CREATE USER IF NOT EXISTS 'root'@'%' IDENTIFIED BY '050103';
GRANT ALL PRIVILEGES ON ai_interview.* TO 'root'@'%';
FLUSH PRIVILEGES;

-- 方案 B：仅允许你的公网 IP（更安全，把 IP 换成 test-db 报错里显示的 IP）
CREATE USER IF NOT EXISTS 'root'@'111.187.62.20' IDENTIFIED BY '050103';
GRANT ALL PRIVILEGES ON ai_interview.* TO 'root'@'111.187.62.20';
FLUSH PRIVILEGES;
```

同时确认云服务器**安全组已放行 3306**。

## 4. 启动后端

**终端 1** — 先 `cd` 到项目根目录，再执行：

```powershell
Get-Content .env | ForEach-Object {
    $line = $_.Trim()
    if ($line -eq '' -or $line.StartsWith('#')) { return }
    $idx = $line.IndexOf('=')
    if ($idx -lt 1) { return }
    $name = $line.Substring(0, $idx).Trim()
    $value = $line.Substring($idx + 1).Trim()
    [Environment]::SetEnvironmentVariable($name, $value, 'Process')
}
cd backend
mvn spring-boot:run
```

- 地址：`http://localhost:8080`
- Swagger：`http://localhost:8080/swagger-ui.html`

## 5. 启动前端

**终端 2** — 先 `cd` 到项目根目录，再执行：

```powershell
cd frontend
npm install
npm run dev
```

> `npm install` 只需在第一次拉代码后执行一次；之后直接 `npm run dev` 即可。

- 地址：`http://localhost:5173`
- API 自动代理到 `localhost:8080`

## 6. 登录

| 账号 | 密码 | 角色 |
|------|------|------|
| admin | admin123456 | 管理员 |
