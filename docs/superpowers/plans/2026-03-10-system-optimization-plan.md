# QBlog 系统优化实施计划

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现容器化部署、数据库优化、Redis 缓存和 CI/CD 流水线

**Architecture:** 采用渐进式优化策略，分三个阶段：基础设施（Docker + 索引 + 安全）、性能优化（Redis 缓存）、部署流程（CI/CD + 健康检查）

**Tech Stack:** Spring Boot 3.2.0, Vue 3, Redis, MySQL 8.0, Docker, GitHub Actions

---

## File Structure

### 新增文件

| 文件路径 | 职责 |
|---------|------|
| `backend/Dockerfile` | 后端容器镜像构建 |
| `backend/src/main/resources/application-prod.yml` | 生产环境配置 |
| `backend/src/main/java/com/qblog/config/WebMvcConfig.java` | HTTP 缓存拦截器配置 |
| `backend/src/main/java/com/qblog/controller/HealthController.java` | 健康检查端点 |
| `backend/src/main/java/com/qblog/service/CacheService.java` | Redis 缓存服务接口 |
| `backend/src/main/java/com/qblog/service/impl/CacheServiceImpl.java` | Redis 缓存实现 |
| `backend/src/main/java/com/qblog/service/ViewCountService.java` | 浏览量计数服务 |
| `backend/src/main/java/com/qblog/mapper/ArticleMapper.java` | 添加浏览量增量更新方法 |
| `backend/src/main/resources/mapper/ArticleMapper.xml` | MyBatis XML 映射 |
| `docker/docker-compose.prod.yml` | 生产环境 Docker Compose |
| `docker/nginx.conf` | Nginx 反向代理配置 |
| `docker/.env.example` | 环境变量示例文件 |
| `database/migrations/V1__add_indexes_and_constraints.sql` | 数据库迁移脚本 |
| `.github/workflows/ci.yml` | CI/CD 流水线配置 |

### 修改文件

| 文件路径 | 修改内容 |
|---------|---------|
| `backend/src/main/java/com/qblog/service/impl/ArticleServiceImpl.java` | 修复 SQL 注入、添加缓存 |
| `backend/src/main/java/com/qblog/mapper/ArticleMapper.java` | 添加自定义 SQL 方法 |
| `backend/pom.xml` | 添加测试依赖（如需要） |

---

## Chunk 1: 阶段 1 - 基础设施优化

### Task 1: 创建后端 Dockerfile

**Files:**
- Create: `backend/Dockerfile`

- [ ] **Step 1: 创建 Dockerfile**

```dockerfile
# 多阶段构建：构建阶段
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests -B

# 运行阶段
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN addgroup -S qblog && adduser -S qblog -G qblog
USER qblog
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
```

- [ ] **Step 2: 验证 Dockerfile 语法**

Run: `docker run --rm -i hadolint/hadolint < backend/Dockerfile`
Expected: 无错误或仅警告

- [ ] **Step 3: 提交**

```bash
git add backend/Dockerfile
git commit -m "feat: add Dockerfile for backend containerization"
```

---

### Task 2: 创建数据库迁移脚本

**Files:**
- Create: `database/migrations/V1__add_indexes_and_constraints.sql`

- [ ] **Step 1: 创建迁移脚本目录**

Run: `mkdir -p database/migrations`
Expected: 目录创建成功

- [ ] **Step 2: 创建迁移 SQL 文件**

```sql
-- =====================================================
-- V1: 添加索引和唯一约束
-- 执行前请备份数据库
-- =====================================================

USE qblog;

-- 1. 文章表复合索引（优化列表查询）
-- 覆盖场景: WHERE status=1 ORDER BY top DESC, publish_time DESC
ALTER TABLE article
ADD INDEX idx_status_top_publish (status, top, publish_time);

-- 2. 文章标签关联表唯一约束（防止重复关联）
-- 注意：如果已存在重复数据，需要先清理
ALTER TABLE article_tag
ADD UNIQUE INDEX uk_article_tag (article_id, tag_id);

-- 3. 分类名称唯一约束
ALTER TABLE category
ADD UNIQUE INDEX uk_name (name);

-- 4. 标签名称唯一约束
ALTER TABLE tag
ADD UNIQUE INDEX uk_name (name);
```

- [ ] **Step 3: 提交**

```bash
git add database/migrations/V1__add_indexes_and_constraints.sql
git commit -m "feat: add database migration script for indexes and constraints"
```

---

### Task 3: 修复 SQL 注入漏洞 (getArticleList 方法)

**Files:**
- Modify: `backend/src/main/java/com/qblog/service/impl/ArticleServiceImpl.java:62-66`

- [ ] **Step 1: 定位问题代码**

问题位于第 62-66 行，`inSql` 直接拼接参数：

```java
// 当前代码（危险）
if (tagId != null) {
    wrapper.inSql(Article::getId,
        "SELECT article_id FROM article_tag WHERE tag_id = " + tagId);
}
```

- [ ] **Step 2: 修复 SQL 注入**

将以下代码：

```java
if (tagId != null) {
    // 通过 article_tag 关联表筛选指定标签的文章
    wrapper.inSql(Article::getId,
        "SELECT article_id FROM article_tag WHERE tag_id = " + tagId);
}
```

替换为：

```java
if (tagId != null) {
    // 安全：先查询关联的文章ID，再使用 in 查询
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
}
```

- [ ] **Step 3: 验证编译通过**

Run: `cd backend && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 4: 提交**

```bash
git add backend/src/main/java/com/qblog/service/impl/ArticleServiceImpl.java
git commit -m "fix: prevent SQL injection in getArticleList tag filter"
```

---

### Task 4: 修复 SQL 注入漏洞 (getRelatedArticles 方法)

**Files:**
- Modify: `backend/src/main/java/com/qblog/service/impl/ArticleServiceImpl.java:320-337`

- [ ] **Step 1: 定位问题代码**

问题位于第 322-336 行，`inSql` 直接拼接 tagIdList：

```java
String tagIdList = tagIds.stream()
    .map(String::valueOf)
    .collect(Collectors.joining(","));

wrapper.inSql(Article::getId,
    "SELECT at.article_id FROM article_tag at " +
    "WHERE at.tag_id IN (" + tagIdList + ") " +
    "GROUP BY at.article_id " +
    "ORDER BY COUNT(at.tag_id) DESC " +
    "LIMIT " + limit);
```

- [ ] **Step 2: 修复 SQL 注入**

将 `getRelatedArticles` 方法中的代码（第 320-340 行）替换为：

```java
@Override
public List<ArticleListItemVO> getRelatedArticles(Long articleId, Integer limit) {
    // 获取当前文章的标签
    List<ArticleTag> currentArticleTags = articleTagService.list(
        new LambdaQueryWrapper<ArticleTag>().eq(ArticleTag::getArticleId, articleId)
    );

    if (currentArticleTags.isEmpty()) {
        return new ArrayList<>();
    }

    List<Long> tagIds = currentArticleTags.stream()
        .map(ArticleTag::getTagId)
        .collect(Collectors.toList());

    // 安全：使用子查询参数化
    // 1. 先查询有共同标签的文章ID及其共同标签数
    List<ArticleTag> relatedArticleTags = articleTagService.list(
        new LambdaQueryWrapper<ArticleTag>()
            .in(ArticleTag::getTagId, tagIds)
            .ne(ArticleTag::getArticleId, articleId)
    );

    if (relatedArticleTags.isEmpty()) {
        return new ArrayList<>();
    }

    // 2. 按文章ID分组，计算共同标签数，取 top N
    Map<Long, Long> articleTagCount = relatedArticleTags.stream()
        .collect(Collectors.groupingBy(ArticleTag::getArticleId, Collectors.counting()));

    List<Long> relatedArticleIds = articleTagCount.entrySet().stream()
        .sorted(Map.Entry.<Long, Long>comparingByValue().reversed())
        .limit(limit)
        .map(Map.Entry::getKey)
        .collect(Collectors.toList());

    // 3. 查询文章详情
    if (relatedArticleIds.isEmpty()) {
        return new ArrayList<>();
    }

    List<Article> relatedArticles = list(
        new LambdaQueryWrapper<Article>()
            .in(Article::getId, relatedArticleIds)
            .eq(Article::getStatus, 1)
    );

    // 4. 按共同标签数排序返回
    Map<Long, Article> articleMap = relatedArticles.stream()
        .collect(Collectors.toMap(Article::getId, Function.identity()));

    List<Article> sortedArticles = relatedArticleIds.stream()
        .map(articleMap::get)
        .filter(Objects::nonNull)
        .collect(Collectors.toList());

    return convertToListItemVO(sortedArticles);
}
```

需要添加 import: `import java.util.Objects;`

- [ ] **Step 3: 验证编译通过**

Run: `cd backend && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 4: 提交**

```bash
git add backend/src/main/java/com/qblog/service/impl/ArticleServiceImpl.java
git commit -m "fix: prevent SQL injection in getRelatedArticles method"
```

---

### Task 5: 创建生产环境 Docker Compose

**Files:**
- Create: `docker/docker-compose.prod.yml`
- Create: `docker/.env.example`
- Create: `docker/nginx.conf`

- [ ] **Step 1: 创建 .env.example 文件**

```env
# 数据库配置
DB_ROOT_PASSWORD=your_secure_root_password
DB_USERNAME=qblog
DB_PASSWORD=your_secure_password

# JWT 配置
JWT_SECRET=your_jwt_secret_key_here

# 服务器配置
SERVER_HOST=your_server_ip
SERVER_USER=deploy
```

- [ ] **Step 2: 创建 docker-compose.prod.yml**

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
      TZ: Asia/Shanghai
    volumes:
      - mysql_data:/var/lib/mysql
      - ./logs/mysql:/var/log/mysql
    command:
      - --character-set-server=utf8mb4
      - --collation-server=utf8mb4_general_ci
      - --default-time-zone=+8:00
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost", "-u", "root", "-p${DB_ROOT_PASSWORD}"]
      interval: 10s
      timeout: 5s
      retries: 5
      start_period: 30s
    networks:
      - qblog-network

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
    networks:
      - qblog-network

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
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/qblog?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
      SPRING_DATASOURCE_USERNAME: ${DB_USERNAME}
      SPRING_DATASOURCE_PASSWORD: ${DB_PASSWORD}
      SPRING_REDIS_HOST: redis
      JWT_SECRET: ${JWT_SECRET}
    depends_on:
      mysql:
        condition: service_healthy
      redis:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "wget", "-q", "--spider", "http://localhost:8081/api/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 60s
    networks:
      - qblog-network

  frontend:
    image: nginx:alpine
    container_name: qblog-frontend
    restart: always
    ports:
      - "80:80"
    volumes:
      - ../frontend/dist:/usr/share/nginx/html:ro
      - ./nginx.conf:/etc/nginx/nginx.conf:ro
    depends_on:
      - backend
    networks:
      - qblog-network

volumes:
  mysql_data:
  redis_data:

networks:
  qblog-network:
    driver: bridge
```

- [ ] **Step 3: 创建 nginx.conf**

```nginx
worker_processes auto;
error_log /var/log/nginx/error.log warn;
pid /var/run/nginx.pid;

events {
    worker_connections 1024;
}

http {
    include /etc/nginx/mime.types;
    default_type application/octet-stream;

    log_format main '$remote_addr - $remote_user [$time_local] "$request" '
                    '$status $body_bytes_sent "$http_referer" '
                    '"$http_user_agent" "$http_x_forwarded_for"';

    access_log /var/log/nginx/access.log main;

    sendfile on;
    keepalive_timeout 65;
    gzip on;
    gzip_types text/plain text/css application/json application/javascript text/xml application/xml;

    upstream backend {
        server backend:8081;
    }

    server {
        listen 80;
        server_name localhost;

        # 前端静态资源
        location / {
            root /usr/share/nginx/html;
            try_files $uri $uri/ /index.html;

            # 静态资源缓存 30 天
            location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|woff|woff2)$ {
                expires 30d;
                add_header Cache-Control "public, immutable";
            }
        }

        # API 代理
        location /api {
            proxy_pass http://backend;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;

            # API 响应不缓存（由后端控制）
            proxy_cache off;
        }
    }
}
```

- [ ] **Step 4: 提交**

```bash
git add docker/docker-compose.prod.yml docker/.env.example docker/nginx.conf
git commit -m "feat: add production docker-compose and nginx config"
```

---

### Task 6: 创建生产环境配置文件

**Files:**
- Create: `backend/src/main/resources/application-prod.yml`

- [ ] **Step 1: 创建 application-prod.yml**

```yaml
server:
  port: 8081
  servlet:
    context-path: /api

spring:
  application:
    name: qblog

  # 数据库配置（通过环境变量覆盖）
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: ${SPRING_DATASOURCE_URL:jdbc:mysql://localhost:3306/qblog?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true}
    username: ${SPRING_DATASOURCE_USERNAME:root}
    password: ${SPRING_DATASOURCE_PASSWORD:root123}
    hikari:
      minimum-idle: 5
      maximum-pool-size: 20
      idle-timeout: 30000
      pool-name: QBlogHikariCP
      max-lifetime: 1800000
      connection-timeout: 30000

  # Redis 配置
  redis:
    host: ${SPRING_REDIS_HOST:localhost}
    port: 6379
    timeout: 10s

  jackson:
    date-format: yyyy-MM-dd HH:mm:ss
    time-zone: Asia/Shanghai

# MyBatis Plus 配置
mybatis-plus:
  mapper-locations: classpath*:/mapper/**/*.xml
  type-aliases-package: com.qblog.entity
  global-config:
    db-config:
      id-type: auto
  configuration:
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.nologging.NoLoggingImpl

# JWT 配置
jwt:
  secret: ${JWT_SECRET:qblog-secret-key-2024-abcdefghijklmnopqrstuvwxyz-1234567890}
  expiration: 86400000
  header: Authorization
  prefix: "Bearer "

# 日志配置（生产环境精简日志）
logging:
  level:
    root: WARN
    com.qblog: INFO
    org.springframework.security: WARN
  pattern:
    console: '%d{yyyy-MM-dd HH:mm:ss} %-5level %logger{36} - %msg%n'

# Knife4j 配置（生产环境可关闭）
knife4j:
  enable: false
  setting:
    language: zh_cn
```

- [ ] **Step 2: 提交**

```bash
git add backend/src/main/resources/application-prod.yml
git commit -m "feat: add production profile configuration"
```

---

## Chunk 2: 阶段 2 - 性能优化

### Task 7: 创建健康检查端点

**Files:**
- Create: `backend/src/main/java/com/qblog/controller/HealthController.java`

- [ ] **Step 1: 创建 HealthController**

```java
package com.qblog.controller;

import com.qblog.common.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

/**
 * 健康检查控制器
 */
@RestController
@RequestMapping("/health")
@RequiredArgsConstructor
public class HealthController {

    private final DataSource dataSource;
    private final RedisConnectionFactory redisConnectionFactory;

    /**
     * 存活探针 - 检查服务是否运行
     */
    @GetMapping
    public Result<Map<String, String>> health() {
        Map<String, String> status = new HashMap<>();
        status.put("status", "UP");
        return Result.success(status);
    }

    /**
     * 就绪探针 - 检查服务是否准备好接收请求
     */
    @GetMapping("/ready")
    public Result<Map<String, String>> ready() {
        Map<String, String> status = new HashMap<>();

        // 检查数据库连接
        try (Connection conn = dataSource.getConnection()) {
            if (conn.isValid(5)) {
                status.put("database", "UP");
            } else {
                status.put("database", "DOWN");
                status.put("status", "DOWN");
            }
        } catch (Exception e) {
            status.put("database", "DOWN: " + e.getMessage());
            status.put("status", "DOWN");
        }

        // 检查 Redis 连接
        try {
            redisConnectionFactory.getConnection().ping();
            status.put("redis", "UP");
        } catch (Exception e) {
            status.put("redis", "DOWN: " + e.getMessage());
            // Redis 不可用不影响服务运行
        }

        if (!status.containsKey("status")) {
            status.put("status", "UP");
        }

        return Result.success(status);
    }
}
```

- [ ] **Step 2: 验证编译通过**

Run: `cd backend && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: 提交**

```bash
git add backend/src/main/java/com/qblog/controller/HealthController.java
git commit -m "feat: add health check endpoints for k8s/docker probes"
```

---

### Task 8: 创建 Redis 缓存服务

**Files:**
- Create: `backend/src/main/java/com/qblog/service/CacheService.java`
- Create: `backend/src/main/java/com/qblog/service/impl/CacheServiceImpl.java`

- [ ] **Step 1: 创建 CacheService 接口**

```java
package com.qblog.service;

import java.time.Duration;
import java.util.Optional;

/**
 * 缓存服务接口
 */
public interface CacheService {

    /**
     * 获取缓存值
     */
    <T> Optional<T> get(String key, Class<T> type);

    /**
     * 设置缓存值
     */
    <T> void set(String key, T value, Duration ttl);

    /**
     * 删除缓存
     */
    void delete(String key);

    /**
     * 删除匹配模式的所有缓存
     */
    void deleteByPattern(String pattern);

    /**
     * 检查缓存是否存在
     */
    boolean exists(String key);

    /**
     * 增加计数器
     */
    Long increment(String key);

    /**
     * 获取计数器值并重置
     */
    Long getAndReset(String key);
}
```

- [ ] **Step 2: 创建 CacheServiceImpl 实现**

```java
package com.qblog.service.impl;

import cn.hutool.json.JSONUtil;
import com.qblog.service.CacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Redis 缓存服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CacheServiceImpl implements CacheService {

    private final StringRedisTemplate redisTemplate;

    @Override
    public <T> Optional<T> get(String key, Class<T> type) {
        try {
            String value = redisTemplate.opsForValue().get(key);
            if (value == null) {
                return Optional.empty();
            }
            return Optional.of(JSONUtil.toBean(value, type));
        } catch (Exception e) {
            log.warn("Failed to get cache for key: {}", key, e);
            return Optional.empty();
        }
    }

    @Override
    public <T> void set(String key, T value, Duration ttl) {
        try {
            String json = JSONUtil.toJsonStr(value);
            redisTemplate.opsForValue().set(key, json, ttl.toMillis(), TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            log.warn("Failed to set cache for key: {}", key, e);
        }
    }

    @Override
    public void delete(String key) {
        try {
            redisTemplate.delete(key);
        } catch (Exception e) {
            log.warn("Failed to delete cache for key: {}", key, e);
        }
    }

    @Override
    public void deleteByPattern(String pattern) {
        try {
            Set<String> keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        } catch (Exception e) {
            log.warn("Failed to delete cache by pattern: {}", pattern, e);
        }
    }

    @Override
    public boolean exists(String key) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (Exception e) {
            log.warn("Failed to check cache existence for key: {}", key, e);
            return false;
        }
    }

    @Override
    public Long increment(String key) {
        try {
            return redisTemplate.opsForValue().increment(key);
        } catch (Exception e) {
            log.warn("Failed to increment counter for key: {}", key, e);
            return 0L;
        }
    }

    @Override
    public Long getAndReset(String key) {
        try {
            String value = redisTemplate.opsForValue().getAndSet(key, "0");
            return value == null ? 0L : Long.parseLong(value);
        } catch (Exception e) {
            log.warn("Failed to get and reset counter for key: {}", key, e);
            return 0L;
        }
    }
}
```

- [ ] **Step 3: 验证编译通过**

Run: `cd backend && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 4: 提交**

```bash
git add backend/src/main/java/com/qblog/service/CacheService.java backend/src/main/java/com/qblog/service/impl/CacheServiceImpl.java
git commit -m "feat: add Redis cache service implementation"
```

---

### Task 9: 创建浏览量计数服务

**Files:**
- Create: `backend/src/main/java/com/qblog/service/ViewCountService.java`
- Modify: `backend/src/main/java/com/qblog/mapper/ArticleMapper.java`
- Create: `backend/src/main/resources/mapper/ArticleMapper.xml`

- [ ] **Step 1: 修改 ArticleMapper 添加增量更新方法**

在 `ArticleMapper.java` 中添加方法：

```java
package com.qblog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qblog.entity.Article;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 文章 Mapper 接口
 */
@Mapper
public interface ArticleMapper extends BaseMapper<Article> {

    /**
     * 增量更新浏览量
     */
    @Update("UPDATE article SET view_count = view_count + #{increment} WHERE id = #{id}")
    int incrementViewCount(@Param("id") Long id, @Param("increment") Long increment);
}
```

- [ ] **Step 2: 创建 ViewCountService**

```java
package com.qblog.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * 浏览量计数服务
 * 使用 Redis 进行计数，定时同步到数据库
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ViewCountService {

    private final CacheService cacheService;
    private final ArticleMapper articleMapper;

    private static final String VIEW_COUNT_KEY_PREFIX = "article:views:";
    private static final String VIEW_COUNT_PATTERN = "article:views:*";

    /**
     * 增加文章浏览量（写入 Redis）
     */
    public void incrementViewCount(Long articleId) {
        String key = VIEW_COUNT_KEY_PREFIX + articleId;
        cacheService.increment(key);
    }

    /**
     * 获取文章浏览量（数据库值 + Redis 增量）
     */
    public Integer getViewCount(Long articleId, Integer dbViewCount) {
        String key = VIEW_COUNT_KEY_PREFIX + articleId;
        Long increment = cacheService.getAndReset(key);
        // 重置后需要把值加回去，因为 getAndReset 会重置
        if (increment > 0) {
            cacheService.set(key, increment, Duration.ofHours(1));
        }
        return dbViewCount + increment.intValue();
    }

    /**
     * 定时任务：每 5 分钟同步浏览量到数据库
     */
    @Scheduled(fixedRate = 300000)
    public void syncViewCountToDb() {
        log.debug("Starting view count sync to database...");

        try {
            // 使用 StringRedisTemplate 直接操作
            Set<String> keys = stringRedisTemplate.keys(VIEW_COUNT_PATTERN);

            if (keys == null || keys.isEmpty()) {
                return;
            }

            int syncCount = 0;
            for (String key : keys) {
                try {
                    Long articleId = Long.parseLong(key.replace(VIEW_COUNT_KEY_PREFIX, ""));
                    Long count = cacheService.getAndReset(key);

                    if (count != null && count > 0) {
                        articleMapper.incrementViewCount(articleId, count);
                        syncCount++;
                    }
                } catch (Exception e) {
                    log.warn("Failed to sync view count for key: {}", key, e);
                }
            }

            log.info("View count sync completed. Synced {} articles.", syncCount);
        } catch (Exception e) {
            log.error("Failed to sync view counts to database", e);
        }
    }
}
```

注意：需要注入 `StringRedisTemplate`，修改为：

```java
package com.qblog.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Set;

/**
 * 浏览量计数服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ViewCountService {

    private final StringRedisTemplate redisTemplate;
    private final ArticleMapper articleMapper;

    private static final String VIEW_COUNT_KEY_PREFIX = "article:views:";
    private static final Duration SYNC_INTERVAL = Duration.ofMinutes(5);

    /**
     * 增加文章浏览量（写入 Redis）
     */
    public void incrementViewCount(Long articleId) {
        String key = VIEW_COUNT_KEY_PREFIX + articleId;
        redisTemplate.opsForValue().increment(key);
    }

    /**
     * 定时任务：每 5 分钟同步浏览量到数据库
     */
    @Scheduled(fixedRate = 300000)
    public void syncViewCountToDb() {
        log.debug("Starting view count sync...");

        try {
            Set<String> keys = redisTemplate.keys(VIEW_COUNT_KEY_PREFIX + "*");
            if (keys == null || keys.isEmpty()) {
                return;
            }

            int syncCount = 0;
            for (String key : keys) {
                try {
                    String idStr = key.replace(VIEW_COUNT_KEY_PREFIX, "");
                    Long articleId = Long.parseLong(idStr);
                    String countStr = redisTemplate.opsForValue().getAndSet(key, "0");

                    if (countStr != null) {
                        long count = Long.parseLong(countStr);
                        if (count > 0) {
                            articleMapper.incrementViewCount(articleId, count);
                            syncCount++;
                        }
                    }
                } catch (Exception e) {
                    log.warn("Failed to sync view count for key: {}", key, e);
                }
            }

            if (syncCount > 0) {
                log.info("Synced view counts for {} articles", syncCount);
            }
        } catch (Exception e) {
            log.error("View count sync failed", e);
        }
    }
}
```

- [ ] **Step 3: 验证编译通过**

Run: `cd backend && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 4: 提交**

```bash
git add backend/src/main/java/com/qblog/service/ViewCountService.java backend/src/main/java/com/qblog/mapper/ArticleMapper.java
git commit -m "feat: add view count service with Redis and scheduled DB sync"
```

---

### Task 10: 集成缓存到 ArticleService

**Files:**
- Modify: `backend/src/main/java/com/qblog/service/impl/ArticleServiceImpl.java`

- [ ] **Step 1: 添加缓存依赖注入**

在 `ArticleServiceImpl` 类中添加依赖：

```java
private final CacheService cacheService;
private final ViewCountService viewCountService;
```

同时添加缓存常量：

```java
// 缓存 Key 前缀
private static final String CACHE_ARTICLE_DETAIL = "article:detail:";
private static final String CACHE_ARTICLE_HOT = "article:hot";
private static final String CACHE_ARTICLE_LATEST = "article:latest";
private static final Duration TTL_DETAIL = Duration.ofMinutes(10);
private static final Duration TTL_HOT = Duration.ofMinutes(30);
private static final Duration TTL_LATEST = Duration.ofMinutes(5);
```

需要添加 import:

```java
import com.qblog.service.CacheService;
import com.qblog.service.ViewCountService;
import java.time.Duration;
```

- [ ] **Step 2: 修改 getArticleDetail 方法使用缓存**

将 `getArticleDetail` 方法修改为使用缓存和浏览量服务：

```java
@Override
public ArticleVO getArticleDetail(Long id) {
    // 尝试从缓存获取
    String cacheKey = CACHE_ARTICLE_DETAIL + id;
    Optional<ArticleVO> cached = cacheService.get(cacheKey, ArticleVO.class);
    if (cached.isPresent()) {
        ArticleVO vo = cached.get();
        // 更新浏览量（使用 Redis）
        viewCountService.incrementViewCount(id);
        return vo;
    }

    Article article = getById(id);
    if (article == null || article.getStatus() == 2) {
        throw new RuntimeException("文章不存在");
    }

    // 增加浏览量
    if (article.getStatus() == 1) {
        viewCountService.incrementViewCount(id);
    }

    ArticleVO vo = buildArticleVO(article);

    // 写入缓存
    cacheService.set(cacheKey, vo, TTL_DETAIL);

    return vo;
}

// 提取 VO 构建逻辑
private ArticleVO buildArticleVO(Article article) {
    ArticleVO vo = BeanUtil.copyProperties(article, ArticleVO.class);

    if (article.getAuthorId() != null) {
        User author = userService.getById(article.getAuthorId());
        if (author != null) {
            UserVO authorVO = new UserVO();
            BeanUtil.copyProperties(author, authorVO);
            vo.setAuthor(authorVO);
        }
    }

    if (article.getCategoryId() != null) {
        Category category = categoryService.getById(article.getCategoryId());
        if (category != null) {
            CategoryVO categoryVO = new CategoryVO();
            BeanUtil.copyProperties(category, categoryVO);
            vo.setCategory(categoryVO);
        }
    }

    if (article.getId() != null) {
        List<ArticleTag> articleTags = articleTagService.list(
            new LambdaQueryWrapper<ArticleTag>().eq(ArticleTag::getArticleId, article.getId())
        );
        if (!articleTags.isEmpty()) {
            List<Long> tagIds = articleTags.stream()
                .map(ArticleTag::getTagId)
                .collect(Collectors.toList());
            List<Tag> tags = tagService.listByIds(tagIds);
            List<TagVO> tagVOs = tags.stream().map(tag -> {
                TagVO tagVO = new TagVO();
                BeanUtil.copyProperties(tag, tagVO);
                return tagVO;
            }).collect(Collectors.toList());
            vo.setTags(tagVOs);
        }
    }

    return vo;
}
```

- [ ] **Step 3: 修改热门/最新文章方法使用缓存**

```java
@Override
public List<ArticleListItemVO> getHotArticles(Integer limit) {
    // 尝试从缓存获取
    Optional<List<ArticleListItemVO>> cached = cacheService.get(CACHE_ARTICLE_HOT, List.class);
    if (cached.isPresent()) {
        return cached.get();
    }

    List<Article> articles = list(new LambdaQueryWrapper<Article>()
            .eq(Article::getStatus, 1)
            .orderByDesc(Article::getViewCount)
            .last("LIMIT " + limit));

    List<ArticleListItemVO> result = convertToListItemVO(articles);

    // 写入缓存
    cacheService.set(CACHE_ARTICLE_HOT, result, TTL_HOT);

    return result;
}

@Override
public List<ArticleListItemVO> getLatestArticles(Integer limit) {
    // 尝试从缓存获取
    Optional<List<ArticleListItemVO>> cached = cacheService.get(CACHE_ARTICLE_LATEST, List.class);
    if (cached.isPresent()) {
        return cached.get();
    }

    List<Article> articles = list(new LambdaQueryWrapper<Article>()
            .eq(Article::getStatus, 1)
            .orderByDesc(Article::getPublishTime)
            .last("LIMIT " + limit));

    List<ArticleListItemVO> result = convertToListItemVO(articles);

    // 写入缓存
    cacheService.set(CACHE_ARTICLE_LATEST, result, TTL_LATEST);

    return result;
}
```

- [ ] **Step 4: 在文章更新/删除时清除缓存**

修改 `updateArticle` 和 `deleteArticle` 方法：

```java
@Override
@Transactional(rollbackFor = Exception.class)
public Article updateArticle(Long id, ArticleDTO articleDTO) {
    Article article = getById(id);
    if (article == null) {
        throw new RuntimeException("文章不存在");
    }

    BeanUtil.copyProperties(articleDTO, article);

    if (articleDTO.getStatus() == 1 && article.getPublishTime() == null) {
        article.setPublishTime(LocalDateTime.now());
    }

    updateById(article);

    // 清除相关缓存
    clearArticleCache(id);

    // 处理文章标签关联
    articleTagService.remove(new LambdaQueryWrapper<ArticleTag>()
        .eq(ArticleTag::getArticleId, id));

    if (articleDTO.getTagIds() != null && !articleDTO.getTagIds().isEmpty()) {
        List<ArticleTag> articleTags = articleDTO.getTagIds().stream().map(tagId -> {
            ArticleTag articleTag = new ArticleTag();
            articleTag.setArticleId(id);
            articleTag.setTagId(tagId);
            return articleTag;
        }).collect(Collectors.toList());
        articleTagService.saveBatch(articleTags);
    }

    return article;
}

@Override
public void deleteArticle(Long id) {
    Article article = getById(id);
    if (article != null) {
        article.setStatus(2);
        updateById(article);
        // 清除相关缓存
        clearArticleCache(id);
    }
}

private void clearArticleCache(Long articleId) {
    cacheService.delete(CACHE_ARTICLE_DETAIL + articleId);
    cacheService.delete(CACHE_ARTICLE_HOT);
    cacheService.delete(CACHE_ARTICLE_LATEST);
    cacheService.deleteByPattern("article:list:*");
}
```

- [ ] **Step 5: 验证编译通过**

Run: `cd backend && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 6: 提交**

```bash
git add backend/src/main/java/com/qblog/service/impl/ArticleServiceImpl.java
git commit -m "feat: integrate Redis cache into ArticleService"
```

---

### Task 11: 创建 HTTP 缓存拦截器

**Files:**
- Create: `backend/src/main/java/com/qblog/config/WebMvcConfig.java`

- [ ] **Step 1: 创建 WebMvcConfig**

```java
package com.qblog.config;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Web MVC 配置
 */
@Component
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new CacheControlInterceptor())
                .addPathPatterns("/articles/**", "/categories/**", "/tags/**");
    }

    /**
     * HTTP 缓存控制拦截器
     */
    private static class CacheControlInterceptor implements HandlerInterceptor {

        @Override
        public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                    Object handler, Exception ex) {
            // 只对 GET 请求添加缓存头
            if (!"GET".equals(request.getMethod())) {
                return;
            }

            String uri = request.getRequestURI();

            // 文章详情 API: 缓存 2 分钟
            if (uri.matches("/api/articles/\\d+$")) {
                response.setHeader("Cache-Control", "public, max-age=120");
                return;
            }

            // 文章列表 API: 缓存 1 分钟
            if (uri.equals("/api/articles")) {
                response.setHeader("Cache-Control", "public, max-age=60");
                return;
            }

            // 分类和标签列表: 缓存 5 分钟
            if (uri.equals("/api/categories") || uri.equals("/api/tags")) {
                response.setHeader("Cache-Control", "public, max-age=300");
            }
        }
    }
}
```

- [ ] **Step 2: 验证编译通过**

Run: `cd backend && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: 提交**

```bash
git add backend/src/main/java/com/qblog/config/WebMvcConfig.java
git commit -m "feat: add HTTP cache control interceptor"
```

---

## Chunk 3: 阶段 3 - 部署流程完善

### Task 12: 创建 CI/CD 流水线

**Files:**
- Create: `.github/workflows/ci.yml`

- [ ] **Step 1: 创建 CI 工作流文件**

```yaml
name: CI/CD Pipeline

on:
  push:
    branches: [main]
  pull_request:
    branches: [main]

env:
  JAVA_VERSION: '17'
  NODE_VERSION: '20'

jobs:
  # 后端测试和构建
  backend:
    runs-on: ubuntu-latest
    steps:
      - name: Checkout
        uses: actions/checkout@v4

      - name: Set up JDK ${{ env.JAVA_VERSION }}
        uses: actions/setup-java@v4
        with:
          java-version: ${{ env.JAVA_VERSION }}
          distribution: 'temurin'
          cache: maven

      - name: Build with Maven
        working-directory: backend
        run: mvn clean package -DskipTests -B

      - name: Run tests
        working-directory: backend
        run: mvn test -B

      - name: Upload backend artifact
        uses: actions/upload-artifact@v4
        with:
          name: backend-jar
          path: backend/target/*.jar
          retention-days: 1

  # 前端构建
  frontend:
    runs-on: ubuntu-latest
    steps:
      - name: Checkout
        uses: actions/checkout@v4

      - name: Setup Node.js ${{ env.NODE_VERSION }}
        uses: actions/setup-node@v4
        with:
          node-version: ${{ env.NODE_VERSION }}
          cache: 'npm'
          cache-dependency-path: frontend/package-lock.json

      - name: Install dependencies
        working-directory: frontend
        run: npm ci

      - name: Lint
        working-directory: frontend
        run: npm run lint

      - name: Build
        working-directory: frontend
        run: npm run build

      - name: Upload frontend artifact
        uses: actions/upload-artifact@v4
        with:
          name: frontend-dist
          path: frontend/dist
          retention-days: 1

  # 部署到生产环境（仅 main 分支）
  deploy:
    needs: [backend, frontend]
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/main' && github.event_name == 'push'
    steps:
      - name: Checkout
        uses: actions/checkout@v4

      - name: Download artifacts
        uses: actions/download-artifact@v4
        with:
          path: artifacts

      - name: Deploy to server
        uses: appleboy/ssh-action@v1
        with:
          host: ${{ secrets.SERVER_HOST }}
          username: ${{ secrets.SERVER_USER }}
          key: ${{ secrets.SERVER_SSH_KEY }}
          script: |
            cd /opt/qblog

            # Pull latest code
            git pull origin main

            # Rebuild containers
            docker compose -f docker/docker-compose.prod.yml build

            # Restart services
            docker compose -f docker/docker-compose.prod.yml up -d

            # Clean up
            docker system prune -f

            echo "Deployment completed at $(date)"
```

- [ ] **Step 2: 提交**

```bash
git add .github/workflows/ci.yml
git commit -m "feat: add CI/CD pipeline for backend and frontend"
```

---

### Task 13: 更新前端部署配置

**Files:**
- Modify: `.github/workflows/deploy.yml`

- [ ] **Step 1: 更新现有 deploy.yml 支持后端健康检查**

在 `deploy` job 中添加健康检查步骤：

```yaml
# 在 deploy job 中添加
- name: Health check
  run: |
    sleep 30
    curl -f https://${{ steps.deployment.outputs.page_url }}/api/health || exit 1
```

实际上，由于 GitHub Pages 只托管静态文件，后端需要单独部署。保持现有的 deploy.yml 用于前端，CI 流水线用于完整部署。

- [ ] **Step 2: 验证 GitHub Actions 配置**

Run: `cat .github/workflows/ci.yml | head -20`
Expected: 显示 CI 配置内容

- [ ] **Step 3: 提交（如有修改）**

```bash
git add .github/workflows/
git commit -m "chore: update deployment workflows"
```

---

### Task 14: 创建部署文档

**Files:**
- Create: `docs/guides/production-deployment.md`

- [ ] **Step 1: 创建生产部署文档**

```markdown
# 生产环境部署指南

## 前置要求

- Docker 和 Docker Compose
- Git
- 至少 2GB 内存

## 快速部署

### 1. 克隆代码

\`\`\`bash
git clone https://github.com/your-repo/qblog.git
cd qblog
\`\`\`

### 2. 配置环境变量

\`\`\`bash
cd docker
cp .env.example .env
# 编辑 .env 文件，设置安全的密码
\`\`\`

### 3. 构建前端

\`\`\`bash
cd ../frontend
npm install
npm run build
\`\`\`

### 4. 启动服务

\`\`\`bash
cd ../docker
docker compose -f docker-compose.prod.yml up -d
\`\`\`

### 5. 初始化数据库

\`\`\`bash
# 等待 MySQL 启动
sleep 30

# 初始化数据库
docker exec -i qblog-mysql mysql -u root -p${DB_ROOT_PASSWORD} < ../database/mysql-schema.sql
\`\`\`

## 访问地址

- 前端: http://your-server-ip
- 后端 API: http://your-server-ip/api
- 健康检查: http://your-server-ip/api/health

## 常用命令

\`\`\`bash
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
\`\`\`

## 性能优化

- Redis 缓存自动启用
- 浏览量每 5 分钟同步到数据库
- HTTP 缓存头已配置

## 故障排查

### 后端无法启动

\`\`\`bash
# 检查后端日志
docker logs qblog-backend

# 检查数据库连接
docker exec qblog-mysql mysqladmin ping -h localhost -u root -p
\`\`\`

### Redis 连接失败

\`\`\`bash
# 检查 Redis 状态
docker exec qblog-redis redis-cli ping
\`\`\`
```

- [ ] **Step 2: 提交**

```bash
git add docs/guides/production-deployment.md
git commit -m "docs: add production deployment guide"
```

---

### Task 15: 最终验证和提交

- [ ] **Step 1: 验证所有编译**

Run: `cd backend && mvn clean compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 2: 检查文件完整性**

Run: `ls -la backend/Dockerfile docker/docker-compose.prod.yml docker/nginx.conf .github/workflows/ci.yml`
Expected: 所有文件存在

- [ ] **Step 3: 最终提交（如有遗漏）**

```bash
git status
git add -A
git commit -m "feat: complete system optimization - Phase 1, 2, 3"
```

---

## Summary

### 阶段 1 完成
- [x] 后端 Dockerfile
- [x] 数据库迁移脚本
- [x] SQL 注入修复
- [x] 生产环境 Docker Compose
- [x] 生产环境配置文件

### 阶段 2 完成
- [x] 健康检查端点
- [x] Redis 缓存服务
- [x] 浏览量计数优化
- [x] ArticleService 缓存集成
- [x] HTTP 缓存拦截器

### 阶段 3 完成
- [x] CI/CD 流水线
- [x] 部署文档

---

## Notes for Implementation

1. **数据库迁移**: 执行迁移脚本前请备份数据库
2. **Redis 依赖**: 确保生产环境 Redis 正常运行
3. **环境变量**: 不要将 `.env` 文件提交到版本控制
4. **健康检查**: 部署后验证 `/api/health` 和 `/api/health/ready` 端点