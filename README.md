# Q 博客系统

基于 Spring Boot 3.x 和 Vue.js 3.0 的前后端分离博客系统

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

## 项目结构

```
qblog/
├── backend/                 # 后端项目
│   ├── src/main/java/com/qblog/
│   │   ├── common/          # 公共类
│   │   ├── config/          # 配置类
│   │   ├── controller/      # 控制器
│   │   ├── entity/          # 实体类
│   │   ├── filter/          # 过滤器
│   │   ├── mapper/          # Mapper 接口
│   │   ├── model/           # DTO/VO
│   │   ├── service/         # 服务层
│   │   └── QblogApplication.java
│   ├── src/main/resources/
│   │   ├── application.yml  # 配置文件
│   │   └── mapper/          # MyBatis XML
│   └── pom.xml
│
├── frontend/                # 前端项目
│   ├── src/
│   │   ├── api/             # API 接口
│   │   ├── assets/          # 静态资源
│   │   ├── components/      # 组件
│   │   ├── router/          # 路由
│   │   ├── stores/          # Pinia 状态
│   │   ├── views/           # 页面
│   │   ├── App.vue
│   │   └── main.js
│   ├── index.html
│   ├── package.json
│   └── vite.config.js
│
├── database/
│   └── schema.sql           # 数据库脚本
│
└── docs/
    ├── api-design.md        # API 设计文档
    └── frontend-design.md   # 前端设计文档
```

## 快速开始

### 环境要求
- JDK 17+
- MySQL 8.0+
- Redis
- Node.js 18+

### 数据库配置

1. 创建数据库
```sql
CREATE DATABASE qblog DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

2. 导入数据表结构
```bash
mysql -u root -p qblog < database/schema.sql
```

### 后端启动

1. 修改配置文件 `backend/src/main/resources/application.yml`
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/qblog?...
    username: your_username
    password: your_password
  redis:
    host: localhost
    port: 6379
```

2. 启动后端
```bash
cd backend
mvn spring-boot:run
```

访问 API 文档：http://localhost:8080/api/doc.html

### 前端启动

1. 安装依赖
```bash
cd frontend
npm install
```

2. 启动开发服务器
```bash
npm run dev
```

访问前端：http://localhost:3000

## 默认账号

- 管理员账号：admin / admin123

## API 接口

| 模块 | 前缀 | 说明 |
|------|------|------|
| 认证 | /api/v1/auth | 登录、注册、退出 |
| 文章 | /api/v1/articles | 文章 CRUD |
| 分类 | /api/v1/categories | 分类管理 |
| 标签 | /api/v1/tags | 标签管理 |
| 评论 | /api/v1/comments | 评论管理 |
| 用户 | /api/v1/users | 用户信息 |

详细 API 文档请查看：[API 设计文档](docs/api-design.md)

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
- [ ] 邮件通知

## 开发计划

1. **第一阶段**: 核心功能开发
   - 完善文章管理
   - 实现评论系统
   - 文件上传功能

2. **第二阶段**: 用户体验优化
   - 搜索优化
   - 性能优化
   - 移动端适配

3. **第三阶段**: 高级功能
   - 数据统计
   - SEO 优化
   - 主题切换

## License

MIT
