# QBlog 系统优化设计文档

**日期**: 2026-03-10
**作者**: Claude Code
**状态**: 已批准

## 概述

本文档描述了 QBlog 博客系统的优化方案，包括容器化部署、数据库表结构优化、性能改进和安全加固。

## 目标

- **部署方式**: 容器化部署 (Docker/K8s)
- **访问规模**: 小型 (<1000 日访问量)
- **优化范围**: 性能、表结构、部署流程、安全性

## 设计决策

采用**渐进式优化**策略，分三个阶段实施：

1. **阶段 1 - 基础设施**: 容器化配置、数据库索引、安全修复
2. **阶段 2 - 性能优化**: Redis 缓存、浏览量优化、HTTP 缓存
3. **阶段 3 - 部署流程**: CI/CD 流水线、健康检查、生产配置

---

## 阶段 1：基础设施优化

### 1.1 容器化部署

#### 后端 Dockerfile

采用多阶段构建，减小镜像体积：

```dockerfile
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
```

#### 生产环境 docker-compose.prod.yml

```yaml
version: '3.8'

services:
  mysql:
    image: mysql:8.0
    container_name: qblog-mysql
    restart: always
    environment:
      MYSQL_ROOT_PASSWORD: ${DB_ROOT_PASSWORD}
      MYSQL_DATABASE: qblog
      MYSQL_USER: ${DB_USERNAME}
      MYSQL_PASSWORD: ${DB_PASSWORD}
    volumes:
      - mysql_data:/var/lib/mysql
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
      interval: 10s
      timeout: 5s
      retries: 5

  redis:
    image: redis:7-alpine
    container_name: qblog-redis
    restart: always
    volumes:
      - redis_data:/data
    command: redis-server --appendonly yes
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 5s
      retries: 5

  backend:
    build:
      context: ../backend
      dockerfile: Dockerfile
    container_name: qblog-backend
    restart: always
    ports:
      - "8081:8081"
    environment:
      SPRING_PROFILES_ACTIVE: prod
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/qblog
      SPRING_DATASOURCE_USERNAME: ${DB_USERNAME}
      SPRING_DATASOURCE_PASSWORD: ${DB_PASSWORD}
      SPRING_REDIS_HOST: redis
    depends_on:
      mysql:
        condition: service_healthy
      redis:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8081/api/health"]
      interval: 30s
      timeout: 10s
      retries: 3

  frontend:
    image: nginx:alpine
    container_name: qblog-frontend
    restart: always
    ports:
      - "80:80"
    volumes:
      - ../frontend/dist:/usr/share/nginx/html
      - ./nginx.conf:/etc/nginx/nginx.conf:ro
    depends_on:
      - backend

volumes:
  mysql_data:
  redis_data:
```

### 1.2 数据库索引优化

#### 新增索引

```sql
-- 文章表复合索引（覆盖常用查询场景）
-- 查询条件: status=1, ORDER BY top DESC, publish_time DESC
ALTER TABLE article ADD INDEX idx_status_top_publish (status, top, publish_time);

-- 文章标签关联表唯一约束（防止重复关联）
ALTER TABLE article_tag ADD UNIQUE INDEX uk_article_tag (article_id, tag_id);

-- 分类名称唯一约束
ALTER TABLE category ADD UNIQUE INDEX uk_name (name);

-- 标签名称唯一约束
ALTER TABLE tag ADD UNIQUE INDEX uk_name (name);
```

#### 索引设计说明

| 表名 | 索引名 | 字段 | 用途 |
|-----|-------|-----|------|
| article | idx_status_top_publish | (status, top, publish_time) | 文章列表查询 |
| article_tag | uk_article_tag | (article_id, tag_id) | 防止重复关联 |
| category | uk_name | (name) | 防止重复分类 |
| tag | uk_name | (name) | 防止重复标签 |

### 1.3 SQL 注入修复

#### 问题代码位置

`ArticleServiceImpl.java` 第 64-66 行和第 331-336 行

#### 修复方案

使用参数化查询替代 `inSql` 直接拼接：

```java
// 修复前（危险）
wrapper.inSql(Article::getId,
    "SELECT article_id FROM article_tag WHERE tag_id = " + tagId);

// 修复后（安全）
List<Long> articleIds = articleTagService.list(
    new LambdaQueryWrapper<ArticleTag>()
        .select(ArticleTag::getArticleId)
        .eq(ArticleTag::getTagId, tagId)
).stream()
 .map(ArticleTag::getArticleId)
 .collect(Collectors.toList());

if (!articleIds.isEmpty()) {
    wrapper.in(Article::getId, articleIds);
} else {
    // 没有匹配的文章，返回空结果
    wrapper.apply("1 = 0");
}
```

---

## 阶段 2：性能优化

### 2.1 Redis 缓存策略

#### 缓存键设计

| 数据类型 | 缓存 Key 模式 | TTL | 说明 |
|---------|-------------|-----|------|
| 文章列表 | `article:list:{hash}` | 5分钟 | hash 基于 query params |
| 文章详情 | `article:detail:{id}` | 10分钟 | 单篇文章 |
| 热门文章 | `article:hot` | 30分钟 | Top 10 |
| 最新文章 | `article:latest` | 5分钟 | 最新 10 篇 |
| 分类列表 | `category:list` | 1小时 | 全部分类 |
| 标签列表 | `tag:list` | 1小时 | 全部标签 |

#### 缓存实现要点

1. **读缓存**: 先查 Redis，未命中再查数据库
2. **写缓存**: 数据库更新后立即删除相关缓存
3. **缓存穿透**: 对不存在的数据缓存空值（短 TTL）
4. **缓存雪崩**: TTL 添加随机偏移

### 2.2 浏览量计数优化

#### 当前问题

- 每次文章访问都直接 UPDATE 数据库
- 高并发下造成数据库压力
- 可能出现数据不一致

#### 优化方案

采用 **Redis 计数 + 定时批量同步**：

1. 浏览量增加时只更新 Redis 计数器
2. 定时任务（每 5 分钟）将 Redis 计数同步到数据库
3. 文章详情查询时合并 Redis 和数据库的计数值

```java
// 浏览量增加
public void incrementViewCount(Long articleId) {
    redisTemplate.opsForValue().increment("article:views:" + articleId);
}

// 获取浏览量（数据库值 + Redis增量）
public Integer getViewCount(Long articleId) {
    Article article = articleMapper.selectById(articleId);
    String incr = redisTemplate.opsForValue().get("article:views:" + articleId);
    long increment = incr == null ? 0 : Long.parseLong(incr);
    return article.getViewCount() + (int) increment;
}

// 定时同步到数据库
@Scheduled(fixedRate = 300000) // 5分钟
public void syncViewCountToDb() {
    Set<String> keys = redisTemplate.keys("article:views:*");
    for (String key : keys) {
        Long articleId = Long.parseLong(key.split(":")[2]);
        String count = redisTemplate.opsForValue().getAndSet(key, "0");
        if (count != null && !"0".equals(count)) {
            articleMapper.incrementViewCount(articleId, Long.parseLong(count));
        }
    }
}
```

### 2.3 HTTP 缓存

#### 缓存策略

| 资源类型 | Cache-Control | 说明 |
|---------|--------------|------|
| 静态资源 (js/css/images) | `public, max-age=2592000` | 30天强缓存 |
| 文章列表 API | `public, max-age=60` | 60秒共享缓存 |
| 文章详情 API | `public, max-age=120` | 2分钟共享缓存 |
| 用户相关 API | `private, no-cache` | 不缓存 |

#### 实现

通过 Spring MVC 拦截器添加响应头：

```java
@Component
public class CacheInterceptor implements HandlerInterceptor {
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response,
                          Object handler, ModelAndView modelAndView) {
        String uri = request.getRequestURI();

        if (uri.matches("/api/articles/\\d+$")) {
            response.setHeader("Cache-Control", "public, max-age=120");
        } else if (uri.equals("/api/articles") && "GET".equals(request.getMethod())) {
            response.setHeader("Cache-Control", "public, max-age=60");
        }
    }
}
```

---

## 阶段 3：部署流程完善

### 3.1 CI/CD 流水线

#### 流水线阶段

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   Test      │────▶│   Build     │────▶│   Deploy    │
└─────────────┘     └─────────────┘     └─────────────┘
  - 后端单元测试      - 后端 JAR 构建      - Docker 镜像推送
  - 前端 Lint        - 前端 Vite 构建     - SSH 部署执行
                     - Docker 镜像构建
```

#### 触发条件

- **PR**: 运行测试和构建，不部署
- **main 分支 push**: 运行完整流水线并部署

### 3.2 生产环境配置

#### 环境变量

| 变量名 | 说明 | 示例 |
|-------|------|------|
| DB_HOST | 数据库主机 | mysql |
| DB_USERNAME | 数据库用户名 | qblog |
| DB_PASSWORD | 数据库密码 | (密文) |
| DB_ROOT_PASSWORD | Root 密码 | (密文) |
| REDIS_HOST | Redis 主机 | redis |

#### 配置文件分离

- `application.yml`: 默认配置（开发环境）
- `application-prod.yml`: 生产环境配置（覆盖默认值）

### 3.3 健康检查

#### 端点设计

| 端点 | 用途 | 响应 |
|-----|------|------|
| `GET /api/health` | 存活探针 | `{"status":"UP"}` |
| `GET /api/health/ready` | 就绪探针 | 检查 DB + Redis 连接 |

#### Docker 健康检查

```yaml
healthcheck:
  test: ["CMD", "curl", "-f", "http://localhost:8081/api/health"]
  interval: 30s
  timeout: 10s
  retries: 3
  start_period: 60s
```

---

## 风险与缓解

| 风险 | 影响 | 缓解措施 |
|-----|------|---------|
| 数据库迁移失败 | 服务不可用 | 先备份，使用事务 |
| Redis 缓存雪崩 | 数据库压力 | TTL 随机化，限流 |
| Docker 镜像拉取失败 | 部署失败 | 使用私有镜像仓库 |
| 环境变量泄露 | 安全风险 | 使用 secrets 管理 |

## 实施计划

详见 `writing-plans` 技能生成的实施计划文档。

## 参考

- 项目结构: `STRUCTURE.md`
- 数据库设计: `database/mysql-schema.sql`
- 部署配置: `docker/docker-compose.yml`