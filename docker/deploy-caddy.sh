#!/bin/bash

# QBlog 生产环境部署脚本
# 使用 Caddy 作为反向代理

set -e

echo "=== QBlog 生产环境部署 ==="

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

# 检查 Docker 是否安装
if ! command -v docker &> /dev/null; then
    echo -e "${RED}Docker 未安装，请先安装 Docker${NC}"
    exit 1
fi

if ! command -v docker-compose &> /dev/null; then
    echo -e "${RED}Docker Compose 未安装，请先安装 Docker Compose${NC}"
    exit 1
fi

# 进入 docker 目录
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# 检查 .env 文件
if [ ! -f .env ]; then
    echo -e "${YELLOW}.env 文件不存在，请复制 .env.example 并修改配置${NC}"
    echo "执行: cp .env.example .env"
    cp .env.example .env
    echo -e "${YELLOW}请修改 .env 文件中的配置后重新运行此脚本${NC}"
    exit 1
fi

# 加载环境变量
source .env

# 验证必要的环境变量
if [ -z "$DB_ROOT_PASSWORD" ] || [ -z "$DB_PASSWORD" ] || [ -z "$JWT_SECRET" ]; then
    echo -e "${RED}请确保 .env 文件中设置了以下变量:${NC}"
    echo "  - DB_ROOT_PASSWORD"
    echo "  - DB_PASSWORD"
    echo "  - JWT_SECRET"
    exit 1
fi

echo -e "${GREEN}环境变量检查通过${NC}"

# 停止并清理旧容器
echo "停止现有容器..."
docker-compose -f docker-compose.caddy.yml down 2>/dev/null || true

# 构建并启动服务
echo "构建并启动服务..."
docker-compose -f docker-compose.caddy.yml up -d --build

# 等待服务启动
echo "等待服务启动..."
sleep 30

# 检查服务状态
echo "检查服务状态..."
docker-compose -f docker-compose.caddy.yml ps

# 健康检查
echo "健康检查..."
for i in {1..10}; do
    if wget -q --spider http://localhost/api/health 2>/dev/null; then
        echo -e "${GREEN}后端服务健康检查通过${NC}"
        break
    fi
    if [ $i -eq 10 ]; then
        echo -e "${YELLOW}健康检查未通过，请检查日志${NC}"
        docker-compose -f docker-compose.caddy.yml logs backend
    fi
    sleep 5
done

echo -e "${GREEN}=== 部署完成 ===${NC}"
echo "API 地址: https://bqlog.younote.top/api"
echo "API 文档: https://bqlog.younote.top/api/doc.html"
echo ""
echo "查看日志: docker-compose -f docker-compose.caddy.yml logs -f"
echo "停止服务: docker-compose -f docker-compose.caddy.yml down"