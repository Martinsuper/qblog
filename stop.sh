#!/bin/bash

# Q 博客 - 停止所有服务脚本

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "========================================"
echo "  停止所有服务..."
echo "========================================"
echo ""

# 停止前端和后端进程
echo "🛑 停止应用进程..."
pkill -f "npm run dev" 2>/dev/null || true
pkill -f "mvn spring-boot:run" 2>/dev/null || true
echo "✅ 应用进程已停止"
echo ""

# 停止 Docker 服务
echo "🛑 停止 Docker 服务..."
cd "$SCRIPT_DIR/docker"
docker compose down
echo "✅ Docker 服务已停止"
echo ""

echo "========================================"
echo "  ✅ 所有服务已停止"
echo "========================================"
echo ""
echo "📝 重新启动：./start.sh"
echo ""
