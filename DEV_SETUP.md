# QBlog 本地开发环境配置指南

## 数据库配置

### 方案一：使用 Docker 运行 MySQL（推荐）

1. 安装 Docker
2. 启动 MySQL 服务：
```bash
docker run --name qblog-mysql -p 3306:3306 -e MYSQL_ROOT_PASSWORD=root123 -e MYSQL_DATABASE=qblog -d mysql:8.0
```

### 方案二：本地安装 MySQL

1. 使用 Homebrew 安装 MySQL：
```bash
brew install mysql
```

2. 启动 MySQL 服务：
```bash
brew services start mysql
```

3. 设置 MySQL root 密码：
```bash
mysql_secure_installation
```

### 初始化数据库

无论使用哪种方案，都需要运行以下命令初始化数据库结构：

1. 使用 root 用户连接数据库：
```bash
mysql -u root -p
```

2. 输入密码（如果使用 Docker，则密码是 `root123`；如果本地安装，则输入您设置的密码）

3. 在 MySQL 命令行中执行初始化脚本：
```sql
SOURCE /path/to/init_db.sql;
```

## 修改数据库配置（如需要）

如果您的 MySQL 配置与默认不同，请修改 `backend/src/main/resources/application.yml` 中的数据库配置：

```yaml
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/qblog?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
    username: root      # 修改为您的用户名
    password: root123   # 修改为您的密码
```

## Redis 配置（可选）

如果您想要完整的功能，也可以配置 Redis：

1. 安装 Redis：
```bash
brew install redis
```

2. 启动 Redis：
```bash
brew services start redis
```

3. 或者使用 Docker：
```bash
docker run --name qblog-redis -p 6379:6379 -d redis:alpine
```

## 启动应用

1. 启动后端：
```bash
cd backend
mvn spring-boot:run
```

2. 启动前端：
```bash
cd frontend
npm install
npm run dev
```

## 访问应用

- 前端地址：http://localhost:3001
- 后端 API 地址：http://localhost:8081/api
- API 文档：http://localhost:8081/api/doc.html

## 默认账号

- 用户名：admin
- 密码：admin123