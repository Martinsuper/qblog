<template>
  <div class="dashboard" v-loading="loading">
    <!-- 统计卡片 -->
    <el-row :gutter="20" class="mb-6">
      <el-col :xs="24" :sm="12" :md="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%)">
              <el-icon><Document /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.articleCount }}</div>
              <div class="stat-label">文章总数</div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="12" :md="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" style="background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%)">
              <el-icon><View /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.viewCount.toLocaleString() }}</div>
              <div class="stat-label">总浏览量</div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="12" :md="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" style="background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)">
              <el-icon><User /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.userCount }}</div>
              <div class="stat-label">用户总数</div>
            </div>
          </div>
        </el-card>
      </el-col>

    </el-row>

    <!-- 图表区域 -->
    <el-row :gutter="20" class="mb-6">
      <el-col :xs="24" :md="16">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span class="card-title">📈 访问量趋势</span>
              <el-radio-group v-model="dateRange" size="small">
                <el-radio-button label="week">近 7 天</el-radio-button>
                <el-radio-button label="month">近 30 天</el-radio-button>
              </el-radio-group>
            </div>
          </template>
          <div class="chart-placeholder">
            <el-empty description="图表组件待集成" :image-size="80" />
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :md="8">
        <el-card shadow="hover">
          <template #header>
            <span class="card-title">📊 分类统计</span>
          </template>
          <div class="category-list">
            <div v-for="cat in categories" :key="cat.id" class="category-item">
              <div class="category-info">
                <span class="category-name">{{ cat.name }}</span>
                <span class="category-count">{{ cat.count }} 篇</span>
              </div>
              <el-progress :percentage="cat.percentage" :color="cat.color" />
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getArticleList, getHotArticles } from '@/api/article'
import { getCategoryList } from '@/api/category'
import { getCommentList } from '@/api/comment'

const dateRange = ref('week')
const loading = ref(false)

// 统计数据
const stats = ref({
  articleCount: 0,
  viewCount: 0,
  userCount: 0,
  commentCount: 0
})

// 分类统计
const categories = ref([])

// 热门文章
const hotArticles = ref([])

// 最新评论
const comments = ref([])

// 获取统计数据
const fetchStats = async () => {
  try {
    // 获取文章总数和总浏览量
    const articleRes = await getArticleList({ page: 1, size: 1 })
    stats.value.articleCount = articleRes.data?.total || 0
    
    // 计算总浏览量（需要后端提供统计接口，这里先估算）
    const allArticlesRes = await getArticleList({ page: 1, size: 100 })
    const articles = allArticlesRes.data?.records || []
    stats.value.viewCount = articles.reduce((sum, article) => sum + (article.viewCount || 0), 0)
  } catch (error) {
    console.error('获取统计数据失败:', error)
  }
}

// 获取分类统计
const fetchCategoryStats = async () => {
  try {
    const res = await getCategoryList()
    const categoryList = res.data || []
    
    // 获取每个分类的文章数量
    const categoryStats = await Promise.all(
      categoryList.map(async (cat) => {
        const articleRes = await getArticleList({ page: 1, size: 1, categoryId: cat.id })
        const count = articleRes.data?.total || 0
        return {
          id: cat.id,
          name: cat.name,
          count,
          percentage: 0,
          color: getCategoryColor(cat.id)
        }
      })
    )
    
    // 计算百分比
    const total = categoryStats.reduce((sum, cat) => sum + cat.count, 0)
    categories.value = categoryStats.map(cat => ({
      ...cat,
      percentage: total > 0 ? Math.round((cat.count / total) * 100) : 0
    }))
  } catch (error) {
    console.error('获取分类统计失败:', error)
  }
}

// 获取热门文章
const fetchHotArticles = async () => {
  try {
    const res = await getHotArticles({ limit: 5 })
    hotArticles.value = res.data || []
  } catch (error) {
    console.error('获取热门文章失败:', error)
  }
}



// 分类颜色
const categoryColors = ['#67c23a', '#409eff', '#e6a23c', '#909399', '#f56c6c', '#a0cfa1']
const getCategoryColor = (id) => {
  return categoryColors[(id - 1) % categoryColors.length]
}

// 格式化时间
const formatTime = (time) => {
  if (!time) return ''
  const date = new Date(time)
  const now = new Date()
  const diff = now - date
  
  const minute = 60 * 1000
  const hour = 60 * minute
  const day = 24 * hour
  
  if (diff < minute) {
    return '刚刚'
  } else if (diff < hour) {
    return Math.floor(diff / minute) + '分钟前'
  } else if (diff < day) {
    return Math.floor(diff / hour) + '小时前'
  } else if (diff < 7 * day) {
    return Math.floor(diff / day) + '天前'
  } else {
    return date.toLocaleDateString()
  }
}

onMounted(() => {
  fetchStats()
  fetchCategoryStats()
  fetchHotArticles()
  // fetchComments() // 如果后端没有评论接口，先注释
})
</script>

<style lang="scss" scoped>
.dashboard {
  padding: 0;
  min-height: 100%;
}

.mb-6 {
  margin-bottom: 24px;
}

// 统计卡片
.stat-card {
  border-radius: var(--border-radius-lg);
  transition: all var(--transition-fast);

  &:hover {
    box-shadow: var(--shadow-md);
  }

  :deep(.el-card__body) {
    padding: var(--spacing-lg);
  }

  .stat-content {
    display: flex;
    align-items: center;
    gap: 16px;
    margin-bottom: 16px;

    .stat-icon {
      width: 56px;
      height: 56px;
      border-radius: var(--border-radius);
      display: flex;
      align-items: center;
      justify-content: center;
      flex-shrink: 0;

      .el-icon {
        font-size: 26px;
        color: #fff;
      }
    }

    .stat-info {
      flex: 1;

      .stat-value {
        font-size: 28px;
        font-weight: 700;
        color: var(--text-primary);
        line-height: 1.2;
      }

      .stat-label {
        font-size: 13px;
        color: var(--text-secondary);
        margin-top: 4px;
      }
    }
  }

  .stat-trend {
    display: flex;
    align-items: center;
    gap: 4px;
    font-size: 13px;
    padding-top: 12px;
    border-top: 1px solid var(--border-color);
    color: var(--text-secondary);

    .el-icon {
      font-size: 14px;
    }

    &.positive {
      color: #10b981;
    }

    &.negative {
      color: #ef4444;
    }
  }
}

// 卡片通用样式
:deep(.el-card) {
  border-radius: var(--border-radius-lg);
  border: 1px solid var(--border-color);
  background: var(--bg-secondary);
  box-shadow: var(--shadow-sm);
  transition: all var(--transition-fast);

  &:hover {
    box-shadow: var(--shadow-md);
  }

  .el-card__header {
    padding: var(--spacing-md) var(--spacing-lg);
    border-bottom: 1px solid var(--border-color);
    background: var(--bg-secondary);
  }

  .el-card__body {
    padding: var(--spacing-lg);
  }
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
}

// 分类列表
.category-list {
  .category-item {
    margin-bottom: 20px;

    &:last-child {
      margin-bottom: 0;
    }

    .category-info {
      display: flex;
      justify-content: space-between;
      margin-bottom: 8px;
      font-size: 14px;

      .category-name {
        color: var(--text-primary);
        font-weight: 500;
      }

      .category-count {
        color: var(--text-secondary);
      }
    }

    :deep(.el-progress) {
      .el-progress__bar {
        border-radius: 4px;
      }

      .el-progress__text {
        font-size: 12px !important;
      }
    }
  }
}

// 图表占位
.chart-placeholder {
  height: 300px;
  display: flex;
  align-items: center;
  justify-content: center;

  :deep(.el-empty) {
    .el-empty__description {
      color: var(--text-secondary);
    }
  }
}

// 热门文章
.rank {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 600;
  color: #fff;

  &.rank-1 {
    background: #ef4444;
  }

  &.rank-2 {
    background: #f97316;
  }

  &.rank-3 {
    background: #f59e0b;
  }

  &.rank-4,
  &.rank-5 {
    background: var(--bg-tertiary);
    color: var(--text-secondary);
  }
}

:deep(.el-table) {
  --el-table-tr-bg-color: transparent;
  --el-table-header-bg-color: var(--bg-tertiary);
  --el-table-text-color: var(--text-primary);
  --el-table-header-text-color: var(--text-secondary);
  --el-table-border-color: var(--border-color);

  .el-table__header th {
    font-weight: 600;
    font-size: 13px;
  }

  .el-table__row {
    td {
      padding: 12px 0;
      border-bottom: 1px solid var(--border-color);
    }

    &:hover {
      background: var(--bg-tertiary) !important;
    }
  }

  .el-table__cell {
    font-size: 14px;
  }
}

// 文章链接
.article-link {
  color: var(--text-primary);
  text-decoration: none;
  transition: color var(--transition-fast);

  &:hover {
    color: var(--color-primary);
  }
}

// 评论列表
.comment-list {
  .comment-item {
    display: flex;
    gap: 12px;
    padding: 12px 0;
    border-bottom: 1px solid var(--border-color);

    &:last-child {
      border-bottom: none;
    }

    :deep(.el-avatar) {
      border: 2px solid var(--bg-tertiary);
    }

    .comment-content {
      flex: 1;
      min-width: 0;

      .comment-user {
        font-size: 14px;
        font-weight: 500;
        color: var(--text-primary);
        margin-bottom: 4px;
      }

      .comment-text {
        font-size: 13px;
        color: var(--text-secondary);
        margin-bottom: 6px;
        line-height: 1.5;
      }

      .comment-meta {
        font-size: 12px;
        color: var(--text-tertiary);
        display: flex;
        gap: 12px;
      }
    }
  }
}

// 响应式
@media (max-width: 768px) {
  .stat-card {
    margin-bottom: 16px;
  }

  .card-header {
    flex-direction: column;
    gap: 12px;
    align-items: flex-start;
  }
}
</style>
