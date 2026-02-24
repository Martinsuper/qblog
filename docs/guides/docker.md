# Q 博客 - Docker 一键启动

## 快速启动

### macOS/Linux
```bash
# 启动所有服务
./scripts/start.sh

# 停止所有服务
./scripts/stop.sh
```

### Windows (PowerShell)
```powershell
# 启动所有服务
scripts\start.bat

# 停止所有服务
scripts\stop.bat
```

## 架构说明

```
┌─────────────────┐
│    前端服务     │
│  localhost:3001 │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│    后端服务     │
│  localhost:8080 │
└────────┬────────┘
         │
    ┌────┴────┐
    ▼         ▼
┌──────┐  ┌──────┐
│MySQL │  │Redis │
│:3306 │  │:6379 │
└──────┘  └──────┘
```

## 服务说明

### Docker 服务
| 服务 | 容器名 | 端口 | 说明 |
|------|--------|------|------|
| MySQL | qblog-mysql | 3306 | 主数据库 |
| Redis | qblog-redis | 6379 | 缓存服务 |

### 应用服务
| 服务 | 地址 | 说明 |
|------|------|------|
| 前端 | http://localhost:3001 | 博客前台 |
| 后台 | http://localhost:3001/admin/dashboard | 管理后台 |
| 后端 API | http://localhost:8080 | REST API |
| API 文档 | http://localhost:8080/api/doc.html | Swagger UI |

## 默认账户

**管理员账户：**
- 用户名：`admin`
- 密码：`admin123`

**MySQL 账户：**
- Root 密码：`root123`
- 普通用户：`qblog` / `qblog123`

## 手动操作

### 1. 启动 Docker 服务
```bash
docker compose up -d
```

### 2. 初始化数据库（如果需要）
```bash
docker exec -i qblog-mysql mysql -u root -proot123 qblog < database/mysql-schema.sql
```

### 3. 启动后端
```bash
cd backend
mvn spring-boot:run
```

### 4. 启动前端（新终端）
```bash
cd frontend
npm run dev
```

## 常用命令

### Docker 相关
```bash
# 查看服务状态
docker compose ps

# 查看日志
docker compose logs -f

# 重启服务
docker compose restart

# 停止服务
docker compose down

# 停止并删除数据（谨慎使用）
docker compose down -v
```

### MySQL 相关
```bash
# 进入 MySQL 容器
docker exec -it qblog-mysql mysql -u root -p

# 查看数据库
docker exec qblog-mysql mysql -u root -proot123 -e "SHOW DATABASES;"

# 查看表
docker exec qblog-mysql mysql -u root -proot123 -e "USE qblog; SHOW TABLES;"

# 导入 SQL
docker exec -i qblog-mysql mysql -u root -proot123 qblog < database/mysql-schema.sql
```

### Redis 相关
```bash
# 进入 Redis 容器
docker exec -it qblog-redis redis-cli

# 查看 Redis 信息
docker exec qblog-redis redis-cli info
```

## 数据持久化

数据存储在以下目录：
- `docker/data/mysql` - MySQL 数据文件
- `docker/logs/mysql` - MySQL 日志
- `docker/data/redis` - Redis 数据文件

## 故障排查

### 端口被占用
```bash
# 查看端口占用
lsof -i :3306
lsof -i :6379
lsof -i :8080
lsof -i :3001

# 停止占用端口的进程
kill -9 <PID>
```

### Docker 启动失败
```bash
# 查看日志
docker compose logs mysql
docker compose logs redis

# 重新创建容器
docker compose down
docker compose up -d
```

### 数据库连接失败
```bash
# 测试 MySQL 连接
docker exec qblog-mysql mysqladmin -u root -proot123 ping

# 重启 MySQL
docker compose restart mysql
```

### 后端启动失败
```bash
# 清理编译
cd backend && mvn clean

# 重新编译
mvn compile

# 检查数据库连接
curl http://localhost:8080/api/categories
```

## 环境变量配置

创建 `.env` 文件配置环境变量：
```bash
# MySQL 配置
MYSQL_ROOT_PASSWORD=root123
MYSQL_DATABASE=qblog
MYSQL_USER=qblog
MYSQL_PASSWORD=qblog123

# 后端配置
DB_HOST=localhost
DB_PORT=3306
```

## 生产部署

生产环境请使用专门的配置文件：

```bash
# 使用生产配置
docker compose -f docker-compose.prod.yml up -d
```

## 备份与恢复

### 备份数据库
```bash
docker exec qblog-mysql mysqldump -u root -proot123 qblog > backup.sql
```

### 恢复数据库
```bash
docker exec -i qblog-mysql mysql -u root -proot123 qblog < backup.sql
```

## 性能优化

### MySQL 优化
编辑 `docker-compose.yml` 添加配置：
```yaml
command:
  - --innodb-buffer-pool-size=1G
  - --max-connections=200
```

### Redis 优化
```yaml
command: redis-server --appendonly yes --maxmemory 256mb
```

## 监控

### 查看资源使用
```bash
docker stats
```

### 查看 MySQL 状态
```bash
docker exec qblog-mysql mysql -u root -proot123 -e "SHOW STATUS;"
```

### 查看慢查询
```bash
docker exec qblog-mysql mysql -u root -proot123 -e "SHOW SLOW LOG;"
```
