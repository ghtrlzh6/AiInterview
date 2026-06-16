#!/usr/bin/env bash
# 在阿里云 ECS 上执行：拉代码、构建前后端、更新静态文件、重启后端
set -euo pipefail

APP_DIR="${APP_DIR:-/opt/ai-interview}"
WEB_ROOT="${WEB_ROOT:-/var/www/ai-interview}"
SERVICE_NAME="${SERVICE_NAME:-ai-interview}"
JAR_NAME="ai-interview-backend-1.0.0-SNAPSHOT.jar"

echo "==> 部署目录: ${APP_DIR}"
cd "${APP_DIR}"

if [[ ! -f .env ]]; then
  echo "错误: ${APP_DIR}/.env 不存在，请先在服务器上创建（勿提交 Git）"
  exit 1
fi

set -a
# shellcheck disable=SC1091
source .env
set +a

echo "==> 构建后端..."
cd "${APP_DIR}/backend"
mvn -q package -DskipTests

echo "==> 构建前端..."
cd "${APP_DIR}/frontend"
npm ci
npm run build

echo "==> 更新前端静态文件..."
sudo mkdir -p "${WEB_ROOT}"
sudo rsync -a --delete dist/ "${WEB_ROOT}/"

echo "==> 安装后端 JAR..."
sudo mkdir -p "${APP_DIR}/runtime"
sudo cp "target/${JAR_NAME}" "${APP_DIR}/runtime/${JAR_NAME}"

echo "==> 重启后端服务..."
if systemctl is-active --quiet "${SERVICE_NAME}"; then
  sudo systemctl restart "${SERVICE_NAME}"
else
  echo "提示: systemd 服务 ${SERVICE_NAME} 未运行，请首次执行:"
  echo "  sudo cp deploy/systemd/ai-interview.service /etc/systemd/system/"
  echo "  sudo systemctl daemon-reload && sudo systemctl enable --now ${SERVICE_NAME}"
fi

if command -v nginx >/dev/null 2>&1; then
  sudo nginx -t && sudo systemctl reload nginx
fi

echo "==> 部署完成"
