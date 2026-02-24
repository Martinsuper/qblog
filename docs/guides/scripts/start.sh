#!/bin/bash

# Q 博客自动启动脚本
# 使用 Docker 启动 MySQL 和 Redis，然后启动后端和前端服务

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "========================================"
echo "  Q 博客 - 自动启动脚本"
echo "========================================"
echo ""

# 检查 Docker 是否安装
if ! command -v docker &> /dev/null; then
    echo "❌ 错误：Docker 未安装"
    echo "请先安装 Docker: https://docs.docker.com/get-docker/"
    exit 1
fi

# 检查 Docker Compose 是否安装
if ! command -v docker-compose &> /dev/null; then
    echo "❌ 错误：Docker Compose 未安装"
    echo "请先安装 Docker Compose"
    exit 1
fi

# 检查 Docker 是否运行
if ! docker info &> /dev/null; then
    echo "❌ 错误：Docker 未运行"
    echo "请启动 Docker Desktop 或 Docker 服务"
    exit 1
fi

echo "✅ Docker 检查通过"
echo ""

# 启动 Docker 服务
echo "📦 正在启动 Docker 服务 (MySQL + Redis)..."
docker-compose up -d

echo ""
echo "⏳ 等待数据库初始化完成..."

# 等待 MySQL 启动
MAX_ATTEMPTS=30
ATTEMPT=1
while [ $ATTEMPT -le $MAX_ATTEMPTS ]; do
    if docker exec qblog-mysql mysqladmin ping -h localhost -u root -proot123 &> /dev/null; then
        echo "✅ MySQL 已启动"
        break
    fi
    echo "   等待中... ($ATTEMPT/$MAX_ATTEMPTS)"
    sleep 2
    ATTEMPT=$((ATTEMPT + 1))
done

if [ $ATTEMPT -gt $MAX_ATTEMPTS ]; then
    echo "❌ MySQL 启动超时"
    docker-compose logs mysql
    exit 1
fi

# 等待 Redis 启动
if docker exec qblog-redis redis-cli ping &> /dev/null; then
    echo "✅ Redis 已启动"
else
    echo "❌ Redis 启动失败"
    docker-compose logs redis
    exit 1
fi

# 检查并初始化数据库
echo ""
echo "📦 检查数据库初始化状态..."
TABLE_COUNT=$(docker exec qblog-mysql mysql -u root -proot123 --default-character-set=utf8mb4 -N -e "USE qblog; SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='qblog';" 2>/dev/null)

if [ "$TABLE_COUNT" -eq "0" ] 2>/dev/null; then
    echo "📦 正在初始化数据库..."
    docker exec -i qblog-mysql mysql -u root -proot123 --default-character-set=utf8mb4 qblog < "$SCRIPT_DIR/database/mysql-schema.sql"
    echo "✅ 数据库初始化完成"
else
    echo "✅ 数据库已初始化 ($TABLE_COUNT 张表)"
fi

echo ""
echo "✅ Docker 服务启动完成"
echo ""

# 显示 Docker 容器状态
echo "📊 Docker 容器状态:"
docker-compose ps
echo ""

# 启动后端
echo "🚀 正在启动后端服务..."
cd "$SCRIPT_DIR/backend"

# 检查 Maven 是否安装
if ! command -v mvn &> /dev/null; then
    echo "❌ 错误：Maven 未安装"
    echo "请先安装 Maven: https://maven.apache.org/download.cgi"
    exit 1
fi

echo "📦 编译后端项目..."
mvn clean compile -q

echo "🚀 启动后端服务..."
echo "   访问地址：http://localhost:8080"
echo "   Swagger:  http://localhost:8080/api/doc.html"
echo ""
echo "   按 Ctrl+C 停止后端服务"
echo ""

# 启动后端（前台运行）
mvn spring-boot:run &
BACKEND_PID=$!

# 等待后端启动
echo "⏳ 等待后端服务启动..."
sleep 15

# 检查后端是否启动成功
if curl -s http://localhost:8080/api/categories &> /dev/null; then
    echo "✅ 后端服务启动成功"
else
    echo "⚠️  后端服务可能还未完全启动，请稍后手动检查"
fi

echo ""

# 启动前端
echo "🚀 正在启动前端服务..."
cd "$SCRIPT_DIR/frontend"

# 检查 Node.js 是否安装
if ! command -v node &> /dev/null; then
    echo "❌ 错误：Node.js 未安装"
    echo "请先安装 Node.js: https://nodejs.org/"
    exit 1
fi

# 检查依赖是否安装
if [ ! -d "node_modules" ]; then
    echo "📦 安装前端依赖..."
    npm install
fi

echo "🚀 启动前端服务..."
echo "   访问地址：http://localhost:3001"
echo ""

# 启动前端（后台运行）
npm run dev &
FRONTEND_PID=$!

echo ""
echo "========================================"
echo "  ✅ 所有服务启动完成！"
echo "========================================"
echo ""
echo "📝 服务信息:"
echo "   前端：http://localhost:3001"
echo "   后端：http://localhost:8080"
echo "   Swagger: http://localhost:8080/api/doc.html"
echo ""
echo "🔐 默认管理员账户:"
echo "   用户名：admin"
echo "   密码：admin123"
echo ""
echo "🛑 停止所有服务:"
echo "   按 Ctrl+C 或运行：./scripts/stop.sh"
echo ""
echo "📊 查看日志:"
echo "   Docker: docker-compose logs -f"
echo "   后端：查看后端控制台输出"
echo "   前端：查看前端控制台输出"
echo ""

# 等待用户中断
wait
