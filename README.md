# Q 博客系统

基于 Spring Boot 3.x 和 Vue.js 3.0 的前后端分离博客系统

## 📚 文档导航

- **[项目结构](STRUCTURE.md)** - 详细的目录结构说明
- **[启动指南](docs/guides/startup.md)** - 快速启动指南
- **[Docker 部署](docs/guides/docker.md)** - Docker 部署指南
- **[数据库配置](docs/guides/database.md)** - 数据库配置说明

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
| 后端 API | http://localhost:8080/api | API 接口 |
| API 文档 | http://localhost:8080/api/doc.html | Swagger 文档 |

### 默认账号

- 管理员账号：`admin` / `admin123`

## 项目结构概览

```
qblog/
├── backend/           # Spring Boot 后端
├── frontend/          # Vue.js 前端
├── docker/            # Docker 配置和数据
├── database/          # 数据库脚本
├── docs/              # 文档目录
├── logs/              # 日志目录
├── start.sh           # 启动脚本
└── stop.sh            # 停止脚本
```

详细结构请查看 [STRUCTURE.md](STRUCTURE.md)

## API 接口

| 模块 | 前缀 | 说明 |
|------|------|------|
| 认证 | /api/auth | 登录、注册 |
| 文章 | /api/articles | 文章 CRUD |
| 分类 | /api/categories | 分类管理 |
| 标签 | /api/tags | 标签管理 |
| 评论 | /api/comments | 评论管理 |

## 功能特性

### 已实现
- [x] 用户注册/登录
- [x] JWT 认证
- [x] 文章列表/详情
- [x] 文章发布/编辑
- [x] 分类/标签管理
- [x] Markdown 编辑器
- [x] 响应式设计

### 待实现
- [ ] 评论功能
- [ ] 点赞/收藏
- [ ] 文件上传
- [ ] 搜索功能
- [ ] 数据统计

## 停止服务

```bash
./stop.sh
```

## License

MIT
