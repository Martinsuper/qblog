# 移除点赞和评论功能设计文档

**日期:** 2026-03-12
**状态:** 待审核
**类型:** 功能简化重构

## 一、需求概述

### 1.1 目标

从 QBlog 系统中完全移除点赞和评论功能,并优化文章管理列表的操作体验。

### 1.2 具体需求

1. **移除点赞功能**: 删除所有点赞相关的 API、业务逻辑、数据字段和 UI
2. **移除评论数据**: 删除评论数量字段(评论功能本身未实现,仅清理占位字段)
3. **优化管理列表**: 从文章管理列表中删除"点赞"和"评论"列
4. **新增查看操作**: 在文章管理列表添加"查看文章详情"按钮,跳转到公开文章详情页
5. **操作按钮顺序**: 查看 - 编辑 - 删除

## 二、架构设计

### 2.1 影响范围

#### 后端改动

**控制器层 (ArticleController.java)**
- 删除 `POST /api/articles/{id}/like` - 点赞文章接口
- 删除 `DELETE /api/articles/{id}/like` - 取消点赞接口

**服务层 (ArticleService/ArticleServiceImpl)**
- 删除 `likeArticle(Long id)` 方法
- 删除 `unlikeArticle(Long id)` 方法
- 修改查询方法,不再查询和填充 likeCount/commentCount

**实体和 VO**
- `Article.java`: 删除 `likeCount` 字段
- `ArticleVO.java`: 删除 `likeCount` 和 `commentCount` 字段
- `ArticleListItemVO.java`: 删除 `likeCount` 和 `commentCount` 字段

**数据库**
- 创建迁移脚本删除 `article.like_count` 列

#### 前端改动

**管理页面 (ArticleManage.vue)**
- 删除"点赞"列 (`<el-table-column prop="likeCount">`)
- 删除"评论"列 (`<el-table-column prop="commentCount">`)
- 添加"查看"按钮,跳转到 `/article/${row.id}`
- 调整操作列宽度以容纳三个按钮

**文章卡片 (ArticleCard.vue)**
- 删除点赞图标和数量显示

**文章详情页 (Article/Detail.vue)**
- 删除元数据中的点赞数显示
- 删除底部的点赞按钮和相关逻辑
- 删除 `handleLike` 方法

**API 层**
- `article.js`: 删除 `likeArticle` 和 `unlikeArticle` 方法
- `comment.js`: 删除整个文件(评论功能未实现,仅有占位代码)

### 2.2 数据流设计

#### 数据库迁移

创建迁移脚本 `database/migrations/V2__remove_like_comment_features.sql`:

```sql
-- 移除点赞和评论功能
-- 删除 article 表的 like_count 列
ALTER TABLE article DROP COLUMN like_count;
```

**迁移注意事项:**
1. 在生产环境执行前先备份数据库
2. 确认所有应用实例已更新到新版本代码
3. 在低峰期执行迁移
4. 验证迁移后应用正常运行

#### API 变更

**删除的接口:**
- `POST /api/articles/{id}/like` - 点赞文章
- `DELETE /api/articles/{id}/like` - 取消点赞

**修改的接口:**
- `GET /api/articles/{id}` - 获取文章详情
  - 移除响应中的 `likeCount` 和 `commentCount` 字段
- `GET /api/articles/admin/list` - 管理后台文章列表
  - 移除响应中的 `likeCount` 和 `commentCount` 字段
- `GET /api/articles` - 公开文章列表
  - 移除响应中的 `likeCount` 和 `commentCount` 字段

#### 前端路由变更

ArticleManage.vue 中新增"查看"按钮:

```javascript
// 点击查看按钮跳转到公开文章详情页
$router.push(`/article/${row.id}`)
```

这会打开与普通用户看到的相同的文章详情页面。

## 三、UI 设计

### 3.1 文章管理列表变更

**当前列结构:**
```
ID | 标题 | 分类 | 浏览 | 点赞 | 评论 | 状态 | 创建时间 | 操作(编辑/删除)
```

**变更后列结构:**
```
ID | 标题 | 分类 | 浏览 | 状态 | 创建时间 | 操作(查看/编辑/删除)
```

**操作按钮变更:**

```vue
<!-- 当前实现 -->
<el-table-column label="操作" width="180" fixed="right">
  <template #default="{ row }">
    <el-button link type="primary" @click="$router.push(`/admin/create?id=${row.id}`)">
      编辑
    </el-button>
    <el-button link type="danger" @click="handleDelete(row.id)">
      删除
    </el-button>
  </template>
</el-table-column>

<!-- 变更后实现 -->
<el-table-column label="操作" width="220" fixed="right">
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
</el-table-column>
```

**列宽调整:**
- 删除"点赞"列 (width="80")
- 删除"评论"列 (width="80")
- 操作列从 width="180" 调整为 width="220" (容纳三个按钮)

### 3.2 文章卡片变更

ArticleCard.vue 移除点赞显示:

```vue
<!-- 删除这部分代码 -->
<span class="flex items-center gap-1">
  <el-icon><Star /></el-icon>
  {{ article.likeCount }}
</span>
```

保留的元数据显示:
- 作者信息
- 发布时间
- 浏览量

### 3.3 文章详情页变更

Article/Detail.vue 需要移除:

**元数据区域:**
```vue
<!-- 删除点赞数显示 -->
<span class="meta-item">
  <el-icon><Star /></el-icon>
  {{ article.likeCount }}
</span>
```

**操作按钮区域:**
```vue
<!-- 删除整个点赞按钮 -->
<div class="article-actions">
  <el-button type="primary" @click="handleLike">
    <el-icon><Star /></el-icon>
    点赞 ({{ article.likeCount }})
  </el-button>
</div>
```

**脚本部分:**
```javascript
// 删除 handleLike 方法
// 删除 likeArticle 的导入
```

## 四、错误处理

### 4.1 向后兼容

**场景:** 前端缓存或旧版本仍然调用点赞接口

**处理方案:**
- 后端返回 `404 Not Found`
- 前端优雅降级,不显示错误提示
- 建议在部署时清理前端缓存

### 4.2 数据迁移安全

**执行前检查清单:**
1. ✓ 确认没有正在运行的查询依赖 `like_count` 列
2. ✓ 确认所有应用实例已更新到新版本代码
3. ✓ 备份生产数据库
4. ✓ 在测试环境验证迁移脚本
5. ✓ 选择低峰期执行

**回滚方案:**

如果迁移后发现问题,可以通过以下步骤回滚:

```sql
-- 恢复 like_count 列
ALTER TABLE article ADD COLUMN like_count INT DEFAULT 0 COMMENT '点赞数';
```

然后回滚到旧版本代码。

### 4.3 缓存处理

**Redis 缓存清理:**

由于文章详情和列表的缓存结构发生变化,需要清理相关缓存:

```bash
# 清理文章相关缓存
redis-cli --scan --pattern "article:*" | xargs redis-cli del
```

或在应用启动时自动清理旧缓存。

## 五、测试策略

### 5.1 后端测试

**单元测试:**
- ✓ 验证 ArticleVO 不包含 likeCount/commentCount 字段
- ✓ 验证 ArticleListItemVO 不包含 likeCount/commentCount 字段
- ✓ 验证文章查询方法不报错

**集成测试:**
- ✓ 验证 `GET /api/articles/{id}` 返回正确的数据结构
- ✓ 验证 `GET /api/articles/admin/list` 返回正确的数据结构
- ✓ 验证 `POST /api/articles/{id}/like` 返回 404
- ✓ 验证 `DELETE /api/articles/{id}/like` 返回 404

**数据库测试:**
- ✓ 验证迁移脚本在测试环境执行成功
- ✓ 验证 article 表不包含 like_count 列
- ✓ 验证现有数据完整性

### 5.2 前端测试

**管理后台测试:**
- ✓ 验证文章管理列表不显示"点赞"和"评论"列
- ✓ 验证"查看"按钮存在且位于第一位
- ✓ 验证"查看"按钮跳转到正确的文章详情页 (`/article/{id}`)
- ✓ 验证操作按钮顺序: 查看 - 编辑 - 删除
- ✓ 验证列表数据正常加载

**文章卡片测试:**
- ✓ 验证文章卡片不显示点赞图标和数量
- ✓ 验证保留的元数据正常显示(作者、时间、浏览量)

**文章详情页测试:**
- ✓ 验证元数据区域不显示点赞数
- ✓ 验证底部不显示点赞按钮
- ✓ 验证文章内容正常显示
- ✓ 验证相关文章推荐正常工作

**跨浏览器测试:**
- ✓ Chrome
- ✓ Firefox
- ✓ Safari
- ✓ Edge

### 5.3 回归测试

**核心功能验证:**
- ✓ 文章创建功能正常
- ✓ 文章编辑功能正常
- ✓ 文章删除功能正常
- ✓ 文章列表查询正常
- ✓ 文章详情查询正常
- ✓ 分类和标签功能正常
- ✓ 浏览量统计正常

## 六、部署计划

### 6.1 部署顺序

1. **准备阶段**
   - 备份生产数据库
   - 在测试环境验证所有改动
   - 准备回滚方案

2. **后端部署**
   - 部署新版本后端代码
   - 验证应用启动成功
   - 验证健康检查通过

3. **数据库迁移**
   - 执行迁移脚本删除 like_count 列
   - 验证迁移成功
   - 清理 Redis 缓存

4. **前端部署**
   - 构建新版本前端代码
   - 部署到生产环境
   - 清理 CDN 缓存

5. **验证阶段**
   - 验证文章管理列表显示正确
   - 验证文章详情页显示正确
   - 验证所有核心功能正常

### 6.2 回滚方案

如果部署后发现严重问题:

1. 回滚前端到旧版本
2. 回滚后端到旧版本
3. 执行数据库回滚脚本恢复 like_count 列
4. 重启应用服务

### 6.3 监控指标

部署后需要监控:
- 应用错误率
- API 响应时间
- 数据库查询性能
- 用户访问量
- 404 错误数量(旧版本调用已删除接口)

## 七、风险评估

### 7.1 技术风险

| 风险 | 影响 | 概率 | 缓解措施 |
|------|------|------|----------|
| 数据库迁移失败 | 高 | 低 | 在测试环境充分验证,准备回滚脚本 |
| 缓存数据结构不兼容 | 中 | 中 | 部署时清理所有相关缓存 |
| 旧版本前端调用已删除接口 | 低 | 中 | 后端返回 404,前端优雅降级 |
| 遗漏清理某些点赞相关代码 | 低 | 低 | 代码审查和全局搜索验证 |

### 7.2 业务风险

| 风险 | 影响 | 概率 | 缓解措施 |
|------|------|------|----------|
| 用户期望保留点赞功能 | 低 | 低 | 这是明确的需求,已确认移除 |
| 历史点赞数据丢失 | 低 | 低 | 数据库备份可恢复 |

## 八、后续优化建议

### 8.1 代码清理

完成此次重构后,建议进一步清理:
- 删除未使用的导入语句
- 删除未使用的图标组件引用
- 更新 API 文档

### 8.2 性能优化

移除点赞和评论字段后:
- 文章列表查询减少两个字段,性能略有提升
- 缓存数据体积减小
- 数据库表结构更简洁

### 8.3 未来扩展

如果将来需要重新添加点赞或评论功能:
- 建议使用独立的点赞表和评论表
- 避免在 article 表中存储聚合数据
- 使用 Redis 缓存点赞和评论数量

## 九、总结

本次重构将完全移除 QBlog 系统中的点赞和评论功能,简化代码结构,优化文章管理体验。主要改动包括:

1. 删除后端点赞 API 和业务逻辑
2. 删除数据库 like_count 列
3. 删除前端所有点赞和评论相关 UI
4. 在文章管理列表添加"查看"按钮

改动范围较大但逻辑清晰,通过充分的测试和谨慎的部署流程,可以安全地完成此次重构。
