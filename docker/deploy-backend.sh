#!/bin/bash
# QBlog 后端容器化部署脚本
set -e

cd "$(dirname "$0")/../"

echo "===== 部署 QBlog 后端 ====="

# 1. 更新代码
git pull

# 2. 加载环境变量
cd docker
set -a && source .env && set +a

# 3. 确保网络存在
docker network create qblog-network 2>/dev/null || true

# 4. 启动中间件（MySQL + Redis）
docker compose up -d mysql redis

# 5. 等待中间件就绪
echo "等待中间件启动..."
sleep 10

# 6. 启动后端
docker compose -f docker-compose.backend.yml up -d --build

# 7. 验证
sleep 5
echo ""
echo "===== 验证服务 ====="

# 健康检查
if curl -sf http://localhost:8081/api/health | grep -q "UP"; then
    echo "✅ 后端服务正常"
else
    echo "❌ 后端服务异常，查看日志: docker logs qblog-backend"
fi

# 容器状态
if docker ps --format '{{.Names}} {{.Status}}' | grep -E "qblog-(mysql|redis|backend).*Up"; then
    echo "✅ 所有容器运行中"
else
    echo "❌ 有容器未启动"
    docker ps -a --format 'table {{.Names}}\t{{.Status}}'
fi

echo ""
echo "API: http://localhost:8081/api"
echo "日志: docker logs -f qblog-backend"