#!/usr/bin/env bash
# 从 CI 上传的 release.tar.gz 安装 JAR + 前端静态文件（不在服务器上构建）
set -euo pipefail

RELEASE_TAR="${1:-/tmp/release.tar.gz}"
APP_DIR="${APP_DIR:-/opt/ai-interview}"
WEB_ROOT="${WEB_ROOT:-/var/www/ai-interview}"
SERVICE_NAME="${SERVICE_NAME:-ai-interview}"
JAR_NAME="ai-interview-backend-1.0.0-SNAPSHOT.jar"
STAGING="/tmp/ai-interview-release-$$"

if [[ ! -f "${RELEASE_TAR}" ]]; then
  echo "错误: 找不到 ${RELEASE_TAR}"
  exit 1
fi

if [[ ! -f "${APP_DIR}/.env" ]]; then
  echo "错误: ${APP_DIR}/.env 不存在，请先在服务器上创建"
  exit 1
fi

echo "==> 解压发布包..."
mkdir -p "${STAGING}"
tar xzf "${RELEASE_TAR}" -C "${STAGING}"

echo "==> 安装后端 JAR..."
sudo mkdir -p "${APP_DIR}/runtime"
sudo cp "${STAGING}/${JAR_NAME}" "${APP_DIR}/runtime/${JAR_NAME}"

echo "==> 更新前端静态文件..."
sudo mkdir -p "${WEB_ROOT}"
sudo rsync -a --delete "${STAGING}/dist/" "${WEB_ROOT}/"

echo "==> 重启后端..."
if systemctl is-active --quiet "${SERVICE_NAME}"; then
  sudo systemctl restart "${SERVICE_NAME}"
else
  echo "提示: systemd 服务 ${SERVICE_NAME} 未运行，首次请执行:"
  echo "  sudo cp ${APP_DIR}/deploy/systemd/ai-interview.service /etc/systemd/system/"
  echo "  sudo systemctl daemon-reload && sudo systemctl enable --now ${SERVICE_NAME}"
fi

if command -v nginx >/dev/null 2>&1; then
  sudo nginx -t && sudo systemctl reload nginx
fi

rm -rf "${STAGING}" "${RELEASE_TAR}"
echo "==> 安装完成"
