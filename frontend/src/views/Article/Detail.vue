<template>
  <div class="article-detail-page">
    <!-- 加载骨架屏 -->
    <el-card v-if="loading">
      <el-skeleton :rows="10" animated />
    </el-card>

    <article v-else-if="article" class="article-content">
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
              <span class="meta-item" :title="formatDateTime(article.publishTime)">
                <el-icon><Calendar /></el-icon>
                {{ formatRelativeTime(article.publishTime) }}
              </span>

              <span class="meta-item">
                <el-icon><View /></el-icon>
                {{ article.viewCount }}
              </span>

              <span class="meta-item">
                <el-icon><Timer /></el-icon>
                约 {{ readingMinutes }} 分钟
              </span>

              <span v-if="article.status === 0" class="draft-badge">草稿</span>
            </div>
          </div>
        </div>

        <!-- 目录 -->
        <nav v-if="toc.length >= 3" class="article-toc" aria-label="文章目录">
          <div class="toc-title">目录</div>
          <ul class="toc-list">
            <li
              v-for="item in toc"
              :key="item.anchor"
              :class="['toc-item', `toc-level-${item.level}`]"
            >
              <a :href="`#${item.anchor}`">{{ item.title }}</a>
            </li>
          </ul>
        </nav>

        <!-- 封面图 -->
        <div v-if="article.coverImage" class="article-cover">
          <img :src="article.coverImage" alt="封面图" loading="lazy" decoding="async" />
        </div>

        <!-- 文章内容 -->
        <div :class="markdownClass" v-html="renderedContent"></div>

        <!-- 标签 -->
        <div v-if="article.tags && article.tags.length > 0" class="article-tags">
          <router-link
            v-for="tag in article.tags"
            :key="tag.id"
            :to="`/articles?tagId=${tag.id}`"
          >
            <el-tag size="small" style="margin-right: 10px; margin-bottom: 10px; cursor: pointer;">
              {{ tag.name }}
            </el-tag>
          </router-link>
        </div>
      </el-card>
    </article>

    <el-empty v-else description="文章不存在" />

    <!-- 相关文章 -->
    <el-card v-if="relatedArticles.length > 0" class="related-articles-card">
      <template #header>
        <span class="related-title">相关文章</span>
      </template>
      <div class="related-list">
        <router-link
          v-for="related in relatedArticles"
          :key="related.id"
          :to="`/article/${related.id}`"
          class="related-item"
        >
          <div class="related-item-title">{{ related.title }}</div>
          <div class="related-item-meta">
            <span><el-icon><View /></el-icon> {{ related.viewCount }}</span>
            <span>{{ formatRelativeTime(related.publishTime) }}</span>
          </div>
        </router-link>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { createLatestRequest } from '@/utils/latestRequest'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Calendar, View, Timer } from '@element-plus/icons-vue'
import { useMarkdown } from '@/composables/useMarkdown'
import { formatRelativeTime, formatDateTime, estimateReadingMinutes } from '@/composables/useTime'
import { generateSlug } from '@/utils/markdownPlugins/headingAnchor'
import { getArticleDetail, getRelatedArticles } from '@/api/article'

const route = useRoute()
const { render, getThemeClass } = useMarkdown()

const article = ref(null)
const relatedArticles = ref([])
const loading = ref(true)

// 计算属性：渲染后的内容
const renderedContent = computed(() => {
  return render(article.value?.content)
})

// 计算属性：Markdown 样式类
const markdownClass = computed(() => getThemeClass())

// 阅读时长估算
const readingMinutes = computed(() => estimateReadingMinutes(article.value?.content))

// 从 Markdown 中提取 h2/h3 生成目录（锚点与 headingAnchor 插件使用同一 slug 算法）
const toc = computed(() => {
  const content = article.value?.content
  if (!content) return []
  const result = []
  let inCodeBlock = false
  for (const line of content.split('\n')) {
    const trimmed = line.trim()
    if (trimmed.startsWith('```')) {
      inCodeBlock = !inCodeBlock
      continue
    }
    if (inCodeBlock) continue
    const match = trimmed.match(/^(#{2,3})\s+(.+?)\s*#*$/)
    if (match) {
      result.push({ level: match[1].length, title: match[2], anchor: generateSlug(match[2]) })
    }
  }
  return result.slice(0, 30)
})

const requests = createLatestRequest()
const fetchArticle = async () => {
  const isCurrent = requests.start()
  const id = route.params.id
  loading.value = true
  article.value = null
  relatedArticles.value = []
  try {
    const res = await getArticleDetail(id)
    if (!isCurrent()) return
    article.value = res.data
    fetchRelatedArticles(id, isCurrent)
  } catch (error) {
    if (!isCurrent()) return
    console.error('获取文章失败:', error)
    ElMessage.error('获取文章失败')
  } finally {
    if (isCurrent()) loading.value = false
  }
}

const fetchRelatedArticles = async (id, isCurrent) => {
  try {
    const res = await getRelatedArticles(id, { limit: 5 })
    if (isCurrent()) relatedArticles.value = res.data
  } catch (error) {
    if (isCurrent()) console.error('获取相关文章失败:', error)
  }
}

// 同一路由组件复用时（文章间跳转）重新拉取
watch(() => route.params.id, (newId) => {
  if (newId && route.name === 'article-detail') {
    article.value = null
    relatedArticles.value = []
    fetchArticle()
  }
})

onMounted(() => {
  fetchArticle()
})
onUnmounted(() => requests.invalidate())
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
      align-items: center;

      .meta-item {
        display: flex;
        align-items: center;
        gap: 4px;
      }
    }
  }
}

.draft-badge {
  padding: 2px 10px;
  border-radius: 999px;
  font-size: 12px;
  background: var(--bg-tertiary);
  color: var(--text-secondary);
}

.article-toc {
  margin-bottom: 30px;
  padding: 16px 20px;
  border-radius: var(--border-radius);
  background: var(--bg-tertiary);
  border-left: 3px solid var(--color-primary);

  .toc-title {
    font-weight: 600;
    font-size: 14px;
    color: var(--text-primary);
    margin-bottom: 10px;
  }

  .toc-list {
    list-style: none;
    margin: 0;
    padding: 0;
  }

  .toc-item {
    a {
      display: block;
      padding: 4px 0;
      color: var(--text-secondary);
      text-decoration: none;
      font-size: 13.5px;
      line-height: 1.6;
      transition: color var(--transition-fast);

      &:hover {
        color: var(--color-primary);
      }
    }

    &.toc-level-3 a {
      padding-left: 16px;
      font-size: 13px;
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

.related-articles-card {
  margin-top: 20px;

  .related-title {
    font-size: 16px;
    font-weight: 600;
  }

  .related-list {
    display: flex;
    flex-direction: column;
    gap: 12px;
  }

  .related-item {
    display: block;
    padding: 12px;
    border-radius: 8px;
    background: var(--bg-secondary);
    text-decoration: none;
    transition: background var(--transition-fast), transform var(--transition-fast);

    &:hover {
      background: var(--bg-hover);
      transform: translateX(4px);
    }

    .related-item-title {
      color: var(--text-primary);
      font-weight: 500;
      margin-bottom: 8px;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .related-item-meta {
      display: flex;
      gap: 16px;
      color: var(--text-secondary);
      font-size: 12px;

      span {
        display: flex;
        align-items: center;
        gap: 4px;
      }
    }
  }
}
</style>
