<template>
  <div class="dashboard">
    <!-- 统计卡片 -->
    <el-row :gutter="20" class="mb-6">
      <el-col :xs="24" :sm="12" :md="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%)">
              <el-icon><Document /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">128</div>
              <div class="stat-label">文章总数</div>
            </div>
          </div>
          <div class="stat-trend positive">
            <el-icon><Top /></el-icon>
            <span>较上周 +12%</span>
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
              <div class="stat-value">10,234</div>
              <div class="stat-label">总浏览量</div>
            </div>
          </div>
          <div class="stat-trend positive">
            <el-icon><Top /></el-icon>
            <span>较上周 +8.5%</span>
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
              <div class="stat-value">542</div>
              <div class="stat-label">用户总数</div>
            </div>
          </div>
          <div class="stat-trend positive">
            <el-icon><Top /></el-icon>
            <span>较上周 +15%</span>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="12" :md="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" style="background: linear-gradient(135deg, #fa709a 0%, #fee140 100%)">
              <el-icon><ChatDotRound /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">1,024</div>
              <div class="stat-label">评论总数</div>
            </div>
          </div>
          <div class="stat-trend negative">
            <el-icon><Bottom /></el-icon>
            <span>较上周 -3.2%</span>
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

    <!-- 最新动态 -->
    <el-row :gutter="20">
      <el-col :xs="24" :md="12">
        <el-card shadow="hover">
          <template #header>
            <span class="card-title">🔥 热门文章</span>
          </template>
          <el-table :data="hotArticles" style="width: 100%" :show-header="false">
            <el-table-column type="index" width="50" align="center">
              <template #default="{ $index }">
                <span :class="['rank', `rank-${$index + 1}`]">{{ $index + 1 }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="title" label="标题" />
            <el-table-column prop="viewCount" label="浏览" width="80" align="right" />
          </el-table>
        </el-card>
      </el-col>

      <el-col :xs="24" :md="12">
        <el-card shadow="hover">
          <template #header>
            <span class="card-title">💬 最新评论</span>
          </template>
          <div class="comment-list">
            <div v-for="comment in comments" :key="comment.id" class="comment-item">
              <el-avatar :size="40" :src="comment.avatar" />
              <div class="comment-content">
                <div class="comment-user">{{ comment.user }}</div>
                <div class="comment-text">{{ comment.content }}</div>
                <div class="comment-meta">
                  <span>{{ comment.time }}</span>
                  <span>来自：{{ comment.article }}</span>
                </div>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const dateRange = ref('week')

const categories = ref([
  { id: 1, name: '技术', count: 45, percentage: 45, color: '#67c23a' },
  { id: 2, name: '生活', count: 30, percentage: 30, color: '#409eff' },
  { id: 3, name: '随笔', count: 15, percentage: 15, color: '#e6a23c' },
  { id: 4, name: '其他', count: 10, percentage: 10, color: '#909399' }
])

const hotArticles = ref([
  { id: 1, title: 'Spring Boot 入门教程', viewCount: 1234 },
  { id: 2, title: 'Vue.js 3.0 新特性', viewCount: 987 },
  { id: 3, title: 'MySQL 性能优化实战', viewCount: 756 },
  { id: 4, title: 'Docker 容器化部署', viewCount: 543 },
  { id: 5, title: 'Redis 缓存最佳实践', viewCount: 432 }
])

const comments = ref([
  {
    id: 1,
    user: '张三',
    avatar: '',
    content: '这篇文章写得很好，学到了很多！',
    time: '10 分钟前',
    article: 'Spring Boot 入门教程'
  },
  {
    id: 2,
    user: '李四',
    avatar: '',
    content: '感谢分享，已收藏',
    time: '30 分钟前',
    article: 'Vue.js 3.0 新特性'
  },
  {
    id: 3,
    user: '王五',
    avatar: '',
    content: '请问有源码吗？',
    time: '1 小时前',
    article: 'MySQL 性能优化实战'
  },
  {
    id: 4,
    user: '赵六',
    avatar: '',
    content: '非常实用的教程',
    time: '2 小时前',
    article: 'Docker 容器化部署'
  }
])
</script>

<style lang="scss" scoped>
.dashboard {
  padding: 0;
}

.mb-6 {
  margin-bottom: 24px;
}

// 统计卡片
.stat-card {
  border-radius: 12px;
  
  :deep(.el-card__body) {
    padding: 20px;
  }

  .stat-content {
    display: flex;
    align-items: center;
    gap: 16px;
    margin-bottom: 16px;

    .stat-icon {
      width: 60px;
      height: 60px;
      border-radius: 12px;
      display: flex;
      align-items: center;
      justify-content: center;
      
      .el-icon {
        font-size: 28px;
        color: #fff;
      }
    }

    .stat-info {
      flex: 1;

      .stat-value {
        font-size: 28px;
        font-weight: 700;
        color: #333;
        line-height: 1;
      }

      .stat-label {
        font-size: 13px;
        color: #999;
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
    border-top: 1px solid #f0f0f0;

    &.positive {
      color: #67c23a;
    }

    &.negative {
      color: #f56c6c;
    }
  }
}

// 卡片通用样式
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  color: #333;
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
        color: #333;
        font-weight: 500;
      }

      .category-count {
        color: #999;
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
    background: linear-gradient(135deg, #ff6b6b 0%, #ee5a6f 100%);
  }

  &.rank-2 {
    background: linear-gradient(135deg, #ffa940 0%, #ff8c00 100%);
  }

  &.rank-3 {
    background: linear-gradient(135deg, #ffd666 0%, #faad14 100%);
  }

  &.rank-4,
  &.rank-5 {
    background: #d9d9d9;
  }
}

:deep(.el-table) {
  .el-table__row:hover {
    background-color: #f5f5f5;
  }
}

// 评论列表
.comment-list {
  .comment-item {
    display: flex;
    gap: 12px;
    padding: 12px 0;
    border-bottom: 1px solid #f0f0f0;

    &:last-child {
      border-bottom: none;
    }

    .comment-content {
      flex: 1;
      min-width: 0;

      .comment-user {
        font-size: 14px;
        font-weight: 500;
        color: #333;
        margin-bottom: 4px;
      }

      .comment-text {
        font-size: 13px;
        color: #666;
        margin-bottom: 6px;
        line-height: 1.5;
      }

      .comment-meta {
        font-size: 12px;
        color: #999;
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
}
</style>
