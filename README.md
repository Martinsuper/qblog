# Q 博客系统

基于 Spring Boot 3.x 和 Vue.js 3.0 的前后端分离博客系统

## 📚 文档导航

- **[生产部署指南](docs/guides/production-deployment.md)** - 生产环境部署说明
- **[系统优化设计](docs/superpowers/specs/2026-03-10-system-optimization-design.md)** - 系统优化设计文档

## 技术栈

### 后端
- **框架**: Spring Boot 3.2.0
- **ORM**: MyBatis-Plus 3.5.5
- **数据库**: MySQL 8.0+
- **缓存**: Redis
- **认证**: JWT + Spring Security
- **API 文档**: Knife4j (Swagger)
- **工具**: Lombok, Hutool

### 前端
- **框架**: Vue.js 3.0
- **构建工具**: Vite 5.0
- **状态管理**: Pinia
- **路由**: Vue Router 4
- **UI 组件**: Element Plus
- **HTTP 客户端**: Axios
- **Markdown**: markdown-it

## 快速开始

### 方式一：使用启动脚本（推荐）
```bash
./start.sh
```

### 方式二：手动启动

#### 1. 启动 Docker 服务
```bash
cd docker
docker-compose up -d
```

#### 2. 初始化数据库
```bash
docker exec -i qblog-mysql mysql -u root -proot123 < ../database/mysql-schema.sql
```

#### 3. 启动后端
```bash
cd backend
mvn spring-boot:run
```

#### 4. 启动前端
```bash
cd frontend
npm install
npm run dev
```

### 访问地址

| 服务 | 地址 | 说明 |
|------|------|------|
| 前端 | http://localhost:3001 | 博客首页 |
| 后端 API | http://localhost:8081/api | API 接口 |
| API 文档 | http://localhost:8081/api/doc.html | Swagger 文档 |
| 健康检查 | http://localhost:8081/api/health | 服务状态 |

### 默认账号

- 管理员账号：`admin` / `admin123`

## 生产部署

### 使用 Docker Compose 部署

```bash
# 1. 配置环境变量
cd docker
cp .env.example .env
# 编辑 .env 文件设置密码

# 2. 构建前端
cd ../frontend
npm install && npm run build

# 3. 启动服务
cd ../docker
docker compose -f docker-compose.prod.yml up -d

# 4. 初始化数据库
docker exec -i qblog-mysql mysql -u root -p${DB_ROOT_PASSWORD} < ../database/mysql-schema.sql
```

详细部署说明请查看 [生产部署指南](docs/guides/production-deployment.md)

## 项目结构概览

```
qblog/
├── backend/                    # Spring Boot 后端
│   ├── Dockerfile             # 容器镜像构建
│   └── src/main/
│       ├── java/com/qblog/
│       │   ├── config/        # 配置类
│       │   ├── controller/    # 控制器
│       │   ├── service/       # 服务层
│       │   ├── mapper/        # MyBatis Mapper
│       │   └── entity/        # 实体类
│       └── resources/
│           ├── application.yml        # 开发环境配置
│           └── application-prod.yml   # 生产环境配置
├── frontend/                   # Vue.js 前端
├── docker/                     # Docker 配置
│   ├── docker-compose.yml     # 开发环境
│   ├── docker-compose.prod.yml # 生产环境
│   ├── nginx.conf             # Nginx 配置
│   └── .env.example           # 环境变量模板
├── database/                   # 数据库脚本
│   ├── mysql-schema.sql       # 数据库结构
│   └── migrations/            # 迁移脚本
├── docs/                       # 文档目录
├── start.sh                    # 启动脚本
└── stop.sh                     # 停止脚本
```

## API 接口

| 模块 | 前缀 | 说明 |
|------|------|------|
| 认证 | /api/auth | 登录、注册 |
| 文章 | /api/articles | 文章 CRUD |
| 分类 | /api/categories | 分类管理 |
| 标签 | /api/tags | 标签管理 |
| 健康检查 | /api/health | 服务状态探针 |

## 功能特性

### 已实现
- [x] 用户注册/登录
- [x] JWT 认证
- [x] 文章列表/详情
- [x] 文章发布/编辑
- [x] 分类/标签管理
- [x] Markdown 编辑器
- [x] 响应式设计
- [x] Redis 缓存
- [x] 浏览量统计
- [x] Docker 容器化部署
- [x] CI/CD 流水线
- [x] 健康检查端点

### 待实现
- [ ] 评论功能
- [ ] 点赞/收藏
- [ ] 文件上传
- [ ] 搜索功能
- [ ] 数据统计

## 性能优化

### 缓存策略
- 文章详情：Redis 缓存 10 分钟
- 热门文章：Redis 缓存 30 分钟
- 最新文章：Redis 缓存 5 分钟
- 分类/标签列表：Redis 缓存 1 小时

### 数据库优化
- 文章列表复合索引 `(status, top, publish_time)`
- 分类/标签名称唯一约束
- 文章标签关联唯一约束

### HTTP 缓存
- 文章详情 API：Cache-Control 2 分钟
- 文章列表 API：Cache-Control 1 分钟
- 静态资源：Cache-Control 30 天

## 停止服务

```bash
./stop.sh
```

## License

MIT