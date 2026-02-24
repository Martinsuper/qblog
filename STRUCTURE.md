# Q 博客项目结构说明

## 📁 目录结构

```
qblog/
├── 📄 README.md                 # 项目说明文档
├── 📄 STRUCTURE.md              # 项目结构说明（本文档）
├── 📄 .gitignore                # Git 忽略配置
│
├── 🚀 start.sh                  # 项目启动脚本
├── 🛑 stop.sh                   # 项目停止脚本
│
├── 📂 backend/                  # 后端项目（Spring Boot）
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/qblog/
│   │   │   │   ├── QblogApplication.java    # 主启动类
│   │   │   │   ├── common/                  # 公共模块（Result、JwtUtil 等）
│   │   │   │   ├── config/                  # 配置类（Security、Knife4j 等）
│   │   │   │   ├── controller/              # 控制器层
│   │   │   │   ├── service/                 # 服务层
│   │   │   │   │   ├── impl/                # 服务实现
│   │   │   │   ├── entity/                  # 实体类
│   │   │   │   ├── mapper/                  # MyBatis Mapper
│   │   │   │   ├── model/                   # 数据模型
│   │   │   │   │   ├── vo/                  # 视图对象
│   │   │   │   │   └── dto/                 # 数据传输对象
│   │   │   │   ├── filter/                  # 过滤器
│   │   │   │   └── exception/               # 异常处理
│   │   │   └── resources/
│   │   │       ├── application.yml          # 应用配置
│   │   │       └── mapper/                  # MyBatis XML
│   │   └── test/                            # 测试代码
│   ├── pom.xml                              # Maven 配置
│   └── target/                              # 编译输出（可删除）
│
├── 📂 frontend/                 # 前端项目（Vue 3 + Vite）
│   ├── src/
│   │   ├── api/                 # API 接口
│   │   ├── assets/              # 静态资源
│   │   ├── components/          # 公共组件
│   │   ├── router/              # 路由配置
│   │   ├── stores/              # 状态管理（Pinia）
│   │   ├── styles/              # 全局样式
│   │   ├── views/               # 页面视图
│   │   ├── App.vue              # 根组件
│   │   └── main.js              # 入口文件
│   ├── index.html               # HTML 模板
│   ├── package.json             # 依赖配置
│   ├── vite.config.js           # Vite 配置
│   └── uno.config.js            # UnoCSS 配置
│
├── 📂 docker/                   # Docker 相关
│   ├── docker-compose.yml       # Docker Compose 配置
│   ├── data/                    # 数据持久化
│   │   ├── mysql/               # MySQL 数据
│   │   └── redis/               # Redis 数据
│   └── logs/                    # 日志目录
│       └── mysql/               # MySQL 日志
│
├── 📂 database/                 # 数据库脚本
│   └── mysql-schema.sql         # 数据库初始化脚本
│
├── 📂 docs/                     # 文档目录
│   ├── design/                  # 设计文档
│   │   ├── api-design.md        # API 设计
│   │   └── frontend-design.md   # 前端设计
│   ├── guides/                  # 指南文档
│   │   ├── database.md          # 数据库配置指南
│   │   ├── docker.md            # Docker 部署指南
│   │   ├── startup.md           # 启动指南
│   │   └── scripts/             # 脚本文件
│   └── plans/                   # 计划文档
│
├── 📂 logs/                     # 应用日志（运行时生成）
│   └── qblog.log
│
└── 📂 .github/                  # GitHub 相关配置
    ├── ISSUE_TEMPLATE/          # Issue 模板
    ├── PULL_REQUEST_TEMPLATE/   # PR 模板
    └── workflows/               # GitHub Actions
```

## 📊 技术栈

### 后端
- **框架**: Spring Boot 3.x
- **安全**: Spring Security + JWT
- **ORM**: MyBatis Plus
- **数据库**: MySQL 8.0
- **缓存**: Redis
- **文档**: Knife4j (Swagger)

### 前端
- **框架**: Vue 3 (Composition API)
- **构建**: Vite
- **UI**: Element Plus
- **CSS**: UnoCSS (原子化 CSS)
- **路由**: Vue Router
- **状态**: Pinia
- **HTTP**: Axios

## 🔧 快速开始

### 1. 启动 Docker 服务
```bash
cd docker
docker-compose up -d
```

### 2. 初始化数据库
```bash
docker exec -i qblog-mysql mysql -u root -proot123 < ../database/mysql-schema.sql
```

### 3. 启动后端
```bash
cd backend
mvn spring-boot:run
```

### 4. 启动前端
```bash
cd frontend
npm run dev
```

### 或使用启动脚本
```bash
./start.sh
```

## 📝 开发规范

### 目录命名
- 使用小写字母
- 多个单词使用连字符 `-` 分隔
- 例如：`article-management`, `user-center`

### 代码组织
- **后端**: 按功能模块分包（controller, service, entity）
- **前端**: 按功能模块组织视图（views/Article, views/User）

### 配置文件
- `application.yml` - 应用配置
- `docker-compose.yml` - Docker 配置
- `.gitignore` - Git 忽略规则

## 🗂️ 数据流向

```
用户请求 → Controller → Service → Mapper → Database
              ↓                        ↑
           Response                Redis Cache
```

## 📦 部署结构

生产环境建议结构：
```
/opt/qblog/
├── backend/          # 后端程序
├── frontend/dist/    # 前端构建产物
├── docker/           # Docker 配置
├── database/         # 数据库脚本
└── logs/             # 日志目录
```

## 🔒 敏感文件

以下文件不应提交到版本控制：
- `.env` - 环境变量
- `application-local.yml` - 本地配置
- `*.log` - 日志文件
- `node_modules/` - 前端依赖
- `target/` - 编译产物
