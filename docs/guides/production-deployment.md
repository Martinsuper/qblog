# 生产环境部署指南

## 前置要求

- Docker 和 Docker Compose
- Git
- 至少 2GB 内存

## 快速部署

### 1. 克隆代码

```bash
git clone https://github.com/your-repo/qblog.git
cd qblog
```

### 2. 配置环境变量

```bash
cd docker
cp .env.example .env
# 编辑 .env 文件，设置安全的密码
```

### 3. 构建前端

```bash
cd ../frontend
npm install
npm run build
```

### 4. 启动服务

```bash
cd ../docker
docker compose -f docker-compose.prod.yml up -d
```

### 5. 初始化数据库

```bash
# 等待 MySQL 启动
sleep 30

# 初始化数据库
docker exec -i qblog-mysql mysql -u root -p${DB_ROOT_PASSWORD} < ../database/mysql-schema.sql
```

## 访问地址

- 前端: http://your-server-ip
- 后端 API: http://your-server-ip/api
- 健康检查: http://your-server-ip/api/health

## 默认账号

- 用户名：admin
- 密码：admin123

## 常用命令

```bash
# 查看日志
docker compose -f docker-compose.prod.yml logs -f

# 重启服务
docker compose -f docker-compose.prod.yml restart

# 停止服务
docker compose -f docker-compose.prod.yml down

# 更新部署
git pull
docker compose -f docker-compose.prod.yml build
docker compose -f docker-compose.prod.yml up -d
```

## 性能优化

- Redis 缓存自动启用
- 浏览量每 5 分钟同步到数据库
- HTTP 缓存头已配置

## 故障排查

### 后端无法启动

```bash
# 检查后端日志
docker logs qblog-backend

# 检查数据库连接
docker exec qblog-mysql mysqladmin ping -h localhost -u root -p
```

### Redis 连接失败

```bash
# 检查 Redis 状态
docker exec qblog-redis redis-cli ping
```

## 环境变量说明

| 变量名 | 说明 | 必填 |
|-------|------|------|
| DB_ROOT_PASSWORD | MySQL root 密码 | 是 |
| DB_USERNAME | 数据库用户名 | 是 |
| DB_PASSWORD | 数据库密码 | 是 |
| JWT_SECRET | JWT 密钥 | 是 |
| SPRING_REDIS_PASSWORD | Redis 密码 | 否 |