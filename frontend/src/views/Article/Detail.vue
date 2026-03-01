<template>
  <div class="article-detail-page">
    <article v-if="article" class="article-content">
      <el-card>
        <!-- 文章头部 -->
        <div class="article-header">
          <h1 class="article-title">{{ article.title }}</h1>

          <div class="article-meta">
            <div class="author-info">
              <el-avatar :size="32" :src="article.author?.avatar">
                {{ article.author?.nickname?.charAt(0) }}
              </el-avatar>
              <span class="author-name">{{ article.author?.nickname || '管理员' }}</span>
            </div>
            <div class="meta-items">
              <span class="meta-item">
                <el-icon><Calendar /></el-icon>
                {{ formatTime(article.publishTime) }}
              </span>

              <span class="meta-item">
                <el-icon><View /></el-icon>
                {{ article.viewCount }}
              </span>
              <span class="meta-item">
                <el-icon><Star /></el-icon>
                {{ article.likeCount }}
              </span>
            </div>
          </div>
        </div>

        <!-- 封面图 -->
        <div v-if="article.coverImage" class="article-cover">
          <img :src="article.coverImage" alt="封面图" />
        </div>

        <!-- 文章内容 -->
        <div :class="markdownClass" v-html="renderedContent"></div>

        <!-- 标签 -->
        <div v-if="article.tags && article.tags.length > 0" class="article-tags">
          <el-tag
            v-for="tag in article.tags"
            :key="tag.id"
            size="small"
            style="margin-right: 10px; margin-bottom: 10px;"
          >
            {{ tag.name }}
          </el-tag>
        </div>

        <!-- 操作按钮 -->
        <div class="article-actions">
          <el-button type="primary" @click="handleLike">
            <el-icon><Star /></el-icon>
            点赞 ({{ article.likeCount }})
          </el-button>
        </div>
      </el-card>
    </article>

    <el-empty v-else description="文章不存在" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useMarkdown } from '@/composables/useMarkdown'
import { getArticleDetail, likeArticle } from '@/api/article'

const route = useRoute()
const { render, getThemeClass } = useMarkdown()

const article = ref(null)

// 计算属性：渲染后的内容
const renderedContent = computed(() => {
  return render(article.value?.content)
})

// 计算属性：Markdown 样式类
const markdownClass = computed(() => getThemeClass())

const fetchArticle = async () => {
  try {
    const res = await getArticleDetail(route.params.id)
    article.value = res.data
  } catch (error) {
    console.error('获取文章失败:', error)
    ElMessage.error('获取文章失败')
  }
}

const handleLike = async () => {
  try {
    await likeArticle(article.value.id)
    article.value.likeCount++
    ElMessage.success('点赞成功')
  } catch (error) {
    console.error('点赞失败:', error)
  }
}

// 格式化时间为 yyyy-MM-dd HH:mm:ss
const formatTime = (time) => {
  if (!time) return ''
  const date = new Date(time)
  const year = date.getFullYear()
  const month = (date.getMonth() + 1).toString().padStart(2, '0')
  const day = date.getDate().toString().padStart(2, '0')
  const hours = date.getHours().toString().padStart(2, '0')
  const minutes = date.getMinutes().toString().padStart(2, '0')
  const seconds = date.getSeconds().toString().padStart(2, '0')
  return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`
}

onMounted(() => {
  fetchArticle()
})
</script>

<style lang="scss" scoped>
.article-detail-page {
  max-width: 900px;
  margin: 0 auto;
  padding: 20px;
}

.article-content {
  margin-bottom: 20px;
}

.article-header {
  margin-bottom: 30px;

  .article-title {
    font-size: 28px;
    font-weight: bold;
    color: var(--text-primary);
    margin-bottom: 20px;
    line-height: 1.4;
  }

  .article-meta {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    justify-content: space-between;
    gap: 15px;
    color: var(--text-secondary);
    font-size: 14px;

    .author-info {
      display: flex;
      align-items: center;
      gap: 10px;

      .author-name {
        font-weight: 500;
      }
    }

    .meta-items {
      display: flex;
      gap: 15px;
      flex-wrap: wrap;

      .meta-item {
        display: flex;
        align-items: center;
        gap: 4px;
      }
    }
  }
}

.article-cover {
  margin-bottom: 30px;
  border-radius: 8px;
  overflow: hidden;

  img {
    width: 100%;
    max-height: 400px;
    object-fit: cover;
  }
}

.article-tags {
  margin-top: 30px;
  padding-top: 20px;
  border-top: 1px solid var(--border-color);
}

.article-actions {
  margin-top: 30px;
  padding-top: 20px;
  border-top: 1px solid var(--border-color);
  display: flex;
  gap: 10px;
}
</style>