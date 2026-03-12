# 移除点赞和评论功能实施计划

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 从 QBlog 系统中完全移除点赞和评论功能,优化文章管理列表操作体验

**Architecture:** 采用 TDD 方法,先修改后端实体和 VO 移除字段,删除点赞 API,然后更新前端 UI 移除点赞/评论显示并添加查看按钮,最后执行数据库迁移

**Tech Stack:** Spring Boot 3.2, MyBatis-Plus, Vue 3, Element Plus, MySQL 8.0

---

## 文件结构

### 后端文件

**修改的文件:**
- `backend/src/main/java/com/qblog/entity/Article.java` - 删除 likeCount 字段
- `backend/src/main/java/com/qblog/model/vo/ArticleVO.java` - 删除 likeCount 和 commentCount 字段
- `backend/src/main/java/com/qblog/model/vo/ArticleListItemVO.java` - 删除 likeCount 和 commentCount 字段
- `backend/src/main/java/com/qblog/controller/ArticleController.java` - 删除点赞接口
- `backend/src/main/java/com/qblog/service/ArticleService.java` - 删除点赞方法声明
- `backend/src/main/java/com/qblog/service/impl/ArticleServiceImpl.java` - 删除点赞方法实现

**创建的文件:**
- `database/migrations/V2__remove_like_comment_features.sql` - 数据库迁移脚本

### 前端文件

**修改的文件:**
- `frontend/src/views/Admin/ArticleManage.vue` - 删除点赞/评论列,添加查看按钮
- `frontend/src/components/ArticleCard.vue` - 删除点赞显示
- `frontend/src/views/Article/Detail.vue` - 删除点赞按钮和显示
- `frontend/src/api/article.js` - 删除点赞 API 方法

**删除的文件:**
- `frontend/src/api/comment.js` - 删除未实现的评论 API 文件

---

## Chunk 1: 后端实体和 VO 清理

### Task 1: 修改 Article 实体类

**Files:**
- Modify: `backend/src/main/java/com/qblog/entity/Article.java:59-63`

- [ ] **Step 1: 删除 likeCount 字段**

在 Article.java 中删除以下代码:

```java
/**
 * 点赞数
 */
private Integer likeCount;
```

- [ ] **Step 2: 验证编译通过**

Run: `cd backend && mvn clean compile`
Expected: BUILD SUCCESS

- [ ] **Step 3: 提交更改**

```bash
git add backend/src/main/java/com/qblog/entity/Article.java
git commit -m "refactor: remove likeCount field from Article entity

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

### Task 2: 修改 ArticleVO

**Files:**
- Modify: `backend/src/main/java/com/qblog/model/vo/ArticleVO.java`

- [ ] **Step 1: 删除 likeCount 和 commentCount 字段**

删除以下字段及其注释:

```java
private Integer likeCount;
private Integer commentCount;
```

- [ ] **Step 2: 验证编译通过**

Run: `cd backend && mvn clean compile`
Expected: BUILD SUCCESS

- [ ] **Step 3: 提交更改**

```bash
git add backend/src/main/java/com/qblog/model/vo/ArticleVO.java
git commit -m "refactor: remove likeCount and commentCount from ArticleVO

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

### Task 3: 修改 ArticleListItemVO

**Files:**
- Modify: `backend/src/main/java/com/qblog/model/vo/ArticleListItemVO.java:27-29`

- [ ] **Step 1: 删除 likeCount 和 commentCount 字段**

在 ArticleListItemVO.java 中删除以下代码:

```java
private Integer likeCount;

private Integer commentCount;
```

- [ ] **Step 2: 验证编译通过**

Run: `cd backend && mvn clean compile`
Expected: BUILD SUCCESS

- [ ] **Step 3: 提交更改**

```bash
git add backend/src/main/java/com/qblog/model/vo/ArticleListItemVO.java
git commit -m "refactor: remove likeCount and commentCount from ArticleListItemVO

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

### Task 4: 删除后端点赞功能(Controller + Service)

**Files:**
- Modify: `backend/src/main/java/com/qblog/controller/ArticleController.java:129-145`
- Modify: `backend/src/main/java/com/qblog/service/ArticleService.java`
- Modify: `backend/src/main/java/com/qblog/service/impl/ArticleServiceImpl.java`

- [ ] **Step 1: 删除 ArticleController 中的点赞接口**

在 ArticleController.java 中删除以下代码:

```java
/**
 * 点赞文章
 */
@PostMapping("/{id}/like")
public Result<Void> likeArticle(@PathVariable Long id) {
    articleService.likeArticle(id);
    return Result.success();
}

/**
 * 取消点赞
 */
@DeleteMapping("/{id}/like")
public Result<Void> unlikeArticle(@PathVariable Long id) {
    articleService.unlikeArticle(id);
    return Result.success();
}
```

- [ ] **Step 2: 删除 ArticleService 接口中的点赞方法声明**

删除以下方法声明:

```java
void likeArticle(Long id);
void unlikeArticle(Long id);
```

- [ ] **Step 3: 删除 ArticleServiceImpl 中的点赞方法实现**

在 ArticleServiceImpl.java 中找到并删除以下两个方法的完整实现:
- `public void likeArticle(Long id)` 方法
- `public void unlikeArticle(Long id)` 方法

包括方法签名、注解和方法体。

- [ ] **Step 4: 验证编译通过**

Run: `cd backend && mvn clean compile`
Expected: BUILD SUCCESS

- [ ] **Step 5: 运行后端测试**

Run: `cd backend && mvn test`
Expected: 所有测试通过

- [ ] **Step 6: 验证没有遗留的点赞相关代码**

Run: `cd backend && grep -r "likeArticle\|unlikeArticle" src/`
Expected: 无输出(所有引用已删除)

- [ ] **Step 7: 提交更改**

```bash
git add backend/src/main/java/com/qblog/controller/ArticleController.java \
        backend/src/main/java/com/qblog/service/ArticleService.java \
        backend/src/main/java/com/qblog/service/impl/ArticleServiceImpl.java
git commit -m "refactor: remove like functionality from backend API

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

## Chunk 2: 前端 UI 清理

### Task 5: 修改 ArticleManage.vue - 删除点赞和评论列

**Files:**
- Modify: `frontend/src/views/Admin/ArticleManage.vue:39-41`

- [ ] **Step 1: 删除点赞列**

在 ArticleManage.vue 中删除以下代码:

```vue
<el-table-column prop="likeCount" label="点赞" width="80" />
```

- [ ] **Step 2: 删除评论列**

删除以下代码:

```vue
<el-table-column prop="commentCount" label="评论" width="80" />
```

- [ ] **Step 3: 验证前端编译**

Run: `cd frontend && npm run build`
Expected: Build successful

- [ ] **Step 4: 提交更改**

```bash
git add frontend/src/views/Admin/ArticleManage.vue
git commit -m "refactor: remove like and comment columns from article management list

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

### Task 6: 修改 ArticleManage.vue - 添加查看按钮

**Files:**
- Modify: `frontend/src/views/Admin/ArticleManage.vue:50-59`

- [ ] **Step 1: 修改操作列宽度**

将操作列的 width 从 "180" 改为 "220":

```vue
<el-table-column label="操作" width="220" fixed="right">
```

- [ ] **Step 2: 添加查看按钮**

在操作列模板中,在编辑按钮之前添加查看按钮:

```vue
<template #default="{ row }">
  <el-button link type="primary" @click="$router.push(`/article/${row.id}`)">
    查看
  </el-button>
  <el-button link type="primary" @click="$router.push(`/admin/create?id=${row.id}`)">
    编辑
  </el-button>
  <el-button link type="danger" @click="handleDelete(row.id)">
    删除
  </el-button>
</template>
```

- [ ] **Step 3: 验证前端编译**

Run: `cd frontend && npm run build`
Expected: Build successful

- [ ] **Step 4: 启动开发服务器测试**

Run: `cd frontend && npm run dev`
访问 http://localhost:3001/admin/articles 验证:
- 点赞和评论列已删除
- 查看按钮显示在第一位
- 点击查看按钮跳转到文章详情页

- [ ] **Step 5: 提交更改**

```bash
git add frontend/src/views/Admin/ArticleManage.vue
git commit -m "feat: add view button to article management list

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

### Task 7: 修改 ArticleCard.vue - 删除点赞显示

**Files:**
- Modify: `frontend/src/components/ArticleCard.vue:36-39`

- [ ] **Step 1: 删除点赞图标和数量显示**

在 ArticleCard.vue 中删除以下代码:

```vue
<span class="flex items-center gap-1">
  <el-icon><Star /></el-icon>
  {{ article.likeCount }}
</span>
```

- [ ] **Step 2: 验证前端编译**

Run: `cd frontend && npm run build`
Expected: Build successful

- [ ] **Step 3: 提交更改**

```bash
git add frontend/src/components/ArticleCard.vue
git commit -m "refactor: remove like count display from ArticleCard

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

### Task 8: 修改 Article/Detail.vue - 删除点赞功能

**Files:**
- Modify: `frontend/src/views/Article/Detail.vue`

- [ ] **Step 1: 删除元数据中的点赞数显示**

在 Detail.vue 的元数据区域删除以下代码(第 26-29 行):

```vue
<span class="meta-item">
  <el-icon><Star /></el-icon>
  {{ article.likeCount }}
</span>
```

- [ ] **Step 2: 删除点赞按钮区域**

删除整个 article-actions 区域(第 55-61 行):

```vue
<!-- 操作按钮 -->
<div class="article-actions">
  <el-button type="primary" @click="handleLike">
    <el-icon><Star /></el-icon>
    点赞 ({{ article.likeCount }})
  </el-button>
</div>
```

- [ ] **Step 3: 删除 handleLike 方法**

在 script 部分删除 handleLike 方法(第 132-139 行):

```javascript
const handleLike = async () => {
  try {
    await likeArticle(article.value.id)
    article.value.likeCount++
    ElMessage.success('点赞成功')
  } catch (error) {
    console.error('点赞失败:', error)
  }
}
```

- [ ] **Step 4: 删除 likeArticle 导入**

在 import 语句中删除 likeArticle:

```javascript
// 修改前
import { getArticleDetail, likeArticle, getRelatedArticles } from '@/api/article'

// 修改后
import { getArticleDetail, getRelatedArticles } from '@/api/article'
```

- [ ] **Step 5: 删除 article-actions 样式**

在 style 部分删除 article-actions 相关样式(第 233-239 行):

```scss
.article-actions {
  margin-top: 30px;
  padding-top: 20px;
  border-top: 1px solid var(--border-color);
  display: flex;
  gap: 10px;
}
```

- [ ] **Step 6: 验证前端编译**

Run: `cd frontend && npm run build`
Expected: Build successful

- [ ] **Step 7: 提交更改**

```bash
git add frontend/src/views/Article/Detail.vue
git commit -m "refactor: remove like functionality from article detail page

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

### Task 9: 修改 article.js - 删除点赞 API 方法

**Files:**
- Modify: `frontend/src/api/article.js:71-83`

- [ ] **Step 1: 删除 likeArticle 和 unlikeArticle 方法**

在 article.js 中删除以下代码:

```javascript
export function likeArticle(id) {
  return request({
    url: `/articles/${id}/like`,
    method: 'post'
  })
}

export function unlikeArticle(id) {
  return request({
    url: `/articles/${id}/like`,
    method: 'delete'
  })
}
```

- [ ] **Step 2: 验证前端编译**

Run: `cd frontend && npm run build`
Expected: Build successful

- [ ] **Step 3: 提交更改**

```bash
git add frontend/src/api/article.js
git commit -m "refactor: remove like API methods from article.js

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

### Task 10: 删除 comment.js 文件

**Files:**
- Delete: `frontend/src/api/comment.js`

- [ ] **Step 1: 删除 comment.js 文件**

Run: `rm frontend/src/api/comment.js`

- [ ] **Step 2: 验证前端编译**

Run: `cd frontend && npm run build`
Expected: Build successful

- [ ] **Step 3: 提交更改**

```bash
git add frontend/src/api/comment.js
git commit -m "refactor: remove unused comment.js API file

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

## Chunk 3: 数据库迁移脚本

### Task 11: 创建数据库迁移脚本

**Files:**
- Create: `database/migrations/V2__remove_like_comment_features.sql`

- [ ] **Step 1: 创建迁移脚本文件**

创建文件 `database/migrations/V2__remove_like_comment_features.sql` 并写入以下内容:

```sql
-- =====================================================
-- 移除点赞和评论功能
-- 日期: 2026-03-12
-- 说明: 删除 article 表的 like_count 列
-- 注意: comment_count 字段仅存在于 VO 中,数据库表中不存在此列
-- =====================================================

-- 删除 like_count 列
ALTER TABLE article DROP COLUMN like_count;
```

- [ ] **Step 2: 提交迁移脚本**

```bash
git add database/migrations/V2__remove_like_comment_features.sql
git commit -m "feat: add database migration to remove like_count column

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

## Chunk 4: 集成测试和验证

**注意:** 数据库迁移脚本已创建,但实际执行应在部署阶段进行,不在开发实施阶段执行。

### Task 12: 后端集成测试

**Files:**
- Test: Backend API endpoints

- [ ] **Step 1: 启动后端服务(手动操作)**

在单独的终端窗口中启动后端服务:
```bash
cd backend && mvn spring-boot:run
```
等待应用启动成功,端口 8081

- [ ] **Step 2: 测试文章详情接口**

Run: `curl -s http://localhost:8081/api/articles/1 | jq`
Expected: 响应中不包含 likeCount 和 commentCount 字段

- [ ] **Step 3: 测试管理后台文章列表接口**

首先获取 JWT token (使用默认管理员账户 admin/admin123):
```bash
TOKEN=$(curl -s -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' | jq -r '.data.token')
```

然后测试接口:
```bash
curl -s "http://localhost:8081/api/articles/admin/list?page=1&size=10" \
  -H "Authorization: Bearer $TOKEN" | jq
```
Expected: 响应中不包含 likeCount 和 commentCount 字段

- [ ] **Step 4: 测试点赞接口返回 404**

Run: `curl -s -X POST http://localhost:8081/api/articles/1/like -w "\n%{http_code}"`
Expected: HTTP 404

- [ ] **Step 5: 测试取消点赞接口返回 404**

Run: `curl -s -X DELETE http://localhost:8081/api/articles/1/like -w "\n%{http_code}"`
Expected: HTTP 404

---

### Task 13: 前端集成测试

**Files:**
- Test: Frontend UI components

- [ ] **Step 1: 启动前端开发服务器(手动操作)**

在单独的终端窗口中启动前端开发服务器:
```bash
cd frontend && npm run dev
```
等待服务启动成功,端口 3001

- [ ] **Step 2: 测试文章管理列表**

访问 http://localhost:3001/admin/articles
验证:
- ✓ 不显示"点赞"列
- ✓ 不显示"评论"列
- ✓ "查看"按钮显示在第一位
- ✓ 操作按钮顺序: 查看 - 编辑 - 删除

- [ ] **Step 3: 测试查看按钮功能**

点击任意文章的"查看"按钮
验证:
- ✓ 跳转到 `/article/{id}` 路径
- ✓ 显示文章详情页

- [ ] **Step 4: 测试文章详情页**

访问 http://localhost:3001/article/1
验证:
- ✓ 元数据区域不显示点赞数
- ✓ 底部不显示点赞按钮
- ✓ 文章内容正常显示
- ✓ 相关文章推荐正常工作

- [ ] **Step 5: 测试文章卡片**

访问 http://localhost:3001
验证:
- ✓ 文章卡片不显示点赞图标和数量
- ✓ 显示作者、时间、浏览量

---

### Task 14: 回归测试

**Files:**
- Test: Core functionality

- [ ] **Step 1: 测试文章创建**

访问 http://localhost:3001/admin/create
创建一篇新文章
验证: ✓ 创建成功

- [ ] **Step 2: 测试文章编辑**

点击"编辑"按钮修改文章
验证: ✓ 编辑成功

- [ ] **Step 3: 测试文章删除**

点击"删除"按钮删除文章
验证: ✓ 删除成功

- [ ] **Step 4: 测试文章列表查询**

访问 http://localhost:3001/articles
验证: ✓ 列表正常显示

- [ ] **Step 5: 测试分类和标签功能**

点击分类和标签筛选
验证: ✓ 筛选功能正常

- [ ] **Step 6: 测试浏览量统计**

访问文章详情页多次
验证: ✓ 浏览量正常增加

---

### Task 15: 最终提交和文档更新

**Files:**
- Update: Documentation (optional)

- [ ] **Step 1: 检查 API 设计文档是否存在**

Run: `test -f docs/design/api-design.md && echo "文件存在" || echo "文件不存在"`

- [ ] **Step 2: 更新 API 设计文档(如果存在)**

如果上一步显示"文件存在",则更新文档删除点赞相关接口说明,然后提交:
```bash
git add docs/design/api-design.md
git commit -m "docs: update API documentation after removing like feature

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

如果显示"文件不存在",跳过此步骤。

- [ ] **Step 3: 推送到远程仓库**

Run: `git push origin main`
Expected: 推送成功

---

## 部署检查清单

在生产环境部署前,确认以下事项:

- [ ] ✓ 所有测试通过
- [ ] ✓ 代码已提交到 main 分支
- [ ] ✓ 生产数据库已备份
- [ ] ✓ 迁移脚本已在测试环境验证
- [ ] ✓ 准备好回滚方案
- [ ] ✓ 选择低峰期执行部署

## 回滚方案

如果部署后发现问题,执行以下步骤:

1. 回滚代码到上一个版本
2. 执行数据库回滚脚本:
   ```sql
   ALTER TABLE article ADD COLUMN like_count INT DEFAULT 0 COMMENT '点赞数';
   ```
3. 重启应用服务
4. 清理 Redis 缓存

## 总结

本实施计划包含 15 个任务,分为 4 个主要部分:
1. 后端实体和 VO 清理 (Task 1-4)
2. 前端 UI 清理 (Task 5-10)
3. 数据库迁移脚本 (Task 11)
4. 集成测试和验证 (Task 12-15)

预计完成时间: 2-3 小时
风险等级: 中等(涉及数据库结构变更)
