#!/bin/bash
# QBlog 生产环境部署脚本 - 分离域名版本
# 前端: qlog.younote.top
# 后端 API: bqlog.younote.top

set -e

echo "===== QBlog 生产环境部署 ====="

# 1. 拉取最新代码
echo "[1/6] 拉取最新代码..."
git pull

# 2. 更新 CORS 配置
echo "[2/6] 更新 CORS 配置..."
cd docker
if ! grep -q "CORS_ALLOWED_ORIGINS" .env; then
    echo "CORS_ALLOWED_ORIGINS=https://qlog.younote.top" >> .env
    echo "已添加 CORS 配置"
else
    sed -i 's|CORS_ALLOWED_ORIGINS=.*|CORS_ALLOWED_ORIGINS=https://qlog.younote.top|' .env
    echo "已更新 CORS 配置"
fi

# 3. 构建前端
echo "[3/6] 构建前端..."
cd ../frontend
npm install --production=false
npm run build
echo "前端构建完成: dist/ 目录"

# 4. 停止旧服务
echo "[4/6] 停止旧服务..."
cd ../docker
docker compose -f docker-compose.caddy.yml down

# 5. 启动新服务
echo "[5/6] 启动新服务..."
docker compose -f docker-compose.caddy.yml up -d --build

# 6. 等待服务就绪并验证
echo "[6/6] 验证服务状态..."
sleep 10

echo ""
echo "===== 验证结果 ====="

# 前端验证
FRONTEND_STATUS=$(curl -s -o /dev/null -w "%{http_code}" https://qlog.younote.top)
if [ "$FRONTEND_STATUS" = "200" ]; then
    echo "✅ 前端 (qlog.younote.top): HTTP $FRONTEND_STATUS"
else
    echo "❌ 前端 (qlog.younote.top): HTTP $FRONTEND_STATUS"
fi

# 前端 SPA 验证（文章详情页）
ARTICLE_STATUS=$(curl -s -o /dev/null -w "%{http_code}" https://qlog.younote.top/article/1)
if [ "$ARTICLE_STATUS" = "200" ]; then
    echo "✅ 前端 SPA (qlog.younote.top/article/1): HTTP $ARTICLE_STATUS"
else
    echo "❌ 前端 SPA (qlog.younote.top/article/1): HTTP $ARTICLE_STATUS"
fi

# 后端 API 验证
API_STATUS=$(curl -s -o /dev/null -w "%{http_code}" https://bqlog.younote.top/api/health)
if [ "$API_STATUS" = "200" ]; then
    echo "✅ 后端 API (bqlog.younote.top/api/health): HTTP $API_STATUS"
else
    echo "❌ 后端 API (bqlog.younote.top/api/health): HTTP $API_STATUS"
fi

echo ""
echo "===== 服务地址 ====="
echo "前端首页:    https://qlog.younote.top"
echo "文章详情:    https://qlog.younote.top/article/1"
echo "API 地址:    https://bqlog.younote.top/api"
echo "API 文档:    https://bqlog.younote.top/api/doc.html"
echo ""
echo "部署完成！"