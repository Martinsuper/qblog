#!/bin/bash
# QBlog 后端容器化部署脚本
set -e

cd "$(dirname "$0")/../"

echo "===== 部署 QBlog 后端 ====="

# 1. 更新代码
git pull

# 2. 确保网络存在
docker network create qblog-network 2>/dev/null || true

# 3. 启动中间件（MySQL + Redis）
cd docker && docker compose up -d mysql redis

# 4. 等待中间件就绪
echo "等待中间件启动..."
sleep 10

# 5. 启动后端
docker compose -f docker-compose.backend.yml up -d --build

# 6. 验证
sleep 5
curl -s http://localhost:8081/api/health && echo ""

echo ""
echo "✅ 部署完成 - API: http://localhost:8081/api"