# Q 博客 - 快速启动指南

## 一键启动（推荐）

### 使用脚本启动（macOS/Linux）
```bash
# 进入项目目录
cd /Users/duanluyao/code/qblog

# 启动所有服务
./scripts/start.sh

# 停止所有服务
./scripts/stop.sh
```

### 使用脚本启动（Windows）
```bash
# 启动所有服务
scripts\start.bat

# 停止所有服务
scripts\stop.bat
```

## 手动启动

### 1. 启动 Docker 服务
```bash
cd /Users/duanluyao/code/qblog
docker-compose up -d
```

### 2. 验证服务
```bash
# 检查 MySQL
docker exec qblog-mysql mysqladmin ping -h localhost -u root -proot123

# 检查 Redis
docker exec qblog-redis redis-cli ping
```

### 3. 启动后端
```bash
cd backend
mvn spring-boot:run
```

### 4. 启动前端（新终端窗口）
```bash
cd frontend
npm run dev
```

## 访问地址

| 服务 | 地址 | 说明 |
|------|------|------|
| 前端 | http://localhost:3001 | 博客前台 |
| 后台 | http://localhost:3001/admin/dashboard | 管理后台 |
| 后端 | http://localhost:8080 | API 服务 |
| Swagger | http://localhost:8080/api/doc.html | API 文档 |

## 默认账户

**管理员账户：**
- 用户名：`admin`
- 密码：`admin123`

## Docker 服务

| 服务 | 容器名 | 端口 | 说明 |
|------|--------|------|------|
| MySQL | qblog-mysql | 3306 | 数据库 |
| Redis | qblog-redis | 6379 | 缓存 |

## 常用命令

```bash
# 查看 Docker 服务状态
docker-compose ps

# 查看 Docker 服务日志
docker-compose logs -f

# 重启 Docker 服务
docker-compose restart

# 停止并删除所有数据（谨慎使用）
docker-compose down -v

# 进入 MySQL 容器
docker exec -it qblog-mysql mysql -u root -p

# 进入 Redis 容器
docker exec -it qblog-redis redis-cli
```

## 数据持久化

数据存储在以下目录：
- `docker/data/mysql` - MySQL 数据
- `docker/data/redis` - Redis 数据

## 故障排查

### 端口被占用
```bash
# 查看端口占用
lsof -i :3306
lsof -i :6379
lsof -i :8080
lsof -i :3001

# 停止占用端口的服务或修改配置
```

### Docker 启动失败
```bash
# 查看日志
docker-compose logs

# 重新创建容器
docker-compose down
docker-compose up -d
```

### 后端启动失败
```bash
# 清理编译
cd backend
mvn clean

# 重新编译
mvn compile

# 查看日志
tail -f backend/logs/*.log
```

### 前端启动失败
```bash
# 清理依赖
cd frontend
rm -rf node_modules package-lock.json

# 重新安装
npm install

# 启动
npm run dev
```

## 环境变量配置

复制 `.env.example` 为 `.env` 并修改配置：
```bash
cp .env.example .env
```

可配置的环境变量：
- `MYSQL_ROOT_PASSWORD` - MySQL root 密码
- `MYSQL_DATABASE` - 数据库名
- `MYSQL_USER` - 数据库用户
- `MYSQL_PASSWORD` - 数据库密码

## 生产部署

生产环境请使用专门的配置文件：
```bash
docker-compose -f docker-compose.prod.yml up -d
```
