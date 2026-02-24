<template>
  <div class="py-6 md:py-8">
    <div class="card p-6 md:p-10">
      <div class="archive-header mb-10">
        <h2 class="text-2xl font-bold mb-2" style="color: var(--text-primary)">时间轴</h2>
        <p class="text-sm text-gray-500">目前共计 {{ total }} 篇文章。继续努力。</p>
      </div>

      <div class="timeline">
        <div v-for="yearGroup in groupedArticles" :key="yearGroup.year" class="year-group">
          <div class="year-header">
            <span class="year-text">{{ yearGroup.year }}</span>
          </div>
          <div class="articles-list">
            <div
              v-for="article in yearGroup.articles"
              :key="article.id"
              class="article-item"
              @click="$router.push(`/article/${article.id}`)"
            >
              <span class="article-date">{{ formatDate(article.publishTime || article.createTime) }}</span>
              <span class="article-title">{{ article.title }}</span>
            </div>
          </div>
        </div>
      </div>

      <div v-if="loading" class="flex justify-center py-10">
        <el-loading />
      </div>
      
      <el-empty v-if="!loading && groupedArticles.length === 0" description="暂无文章" />

      <!-- 如果文章很多，可能需要分页或加载更多，NexT 通常是一次性加载或按年加载 -->
      <div v-if="hasMore" class="mt-8 text-center">
        <el-button :loading="loading" @click="fetchMore">加载更多</el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getArticleList } from '@/api/article'

const articles = ref([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(100) // 一次加载较多用于时间轴
const loading = ref(false)
const hasMore = ref(false)

const groupedArticles = computed(() => {
  const groups = {}
  articles.value.forEach(article => {
    const date = new Date(article.publishTime || article.createTime)
    const year = date.getFullYear()
    if (!groups[year]) {
      groups[year] = []
    }
    groups[year].push(article)
  })
  
  return Object.keys(groups)
    .sort((a, b) => b - a)
    .map(year => ({
      year,
      articles: groups[year]
    }))
})

const formatDate = (dateStr) => {
  const date = new Date(dateStr)
  const month = (date.getMonth() + 1).toString().padStart(2, '0')
  const day = date.getDate().toString().padStart(2, '0')
  return `${month}-${day}`
}

const fetchArticles = async () => {
  loading.value = true
  try {
    const res = await getArticleList({
      page: currentPage.value,
      size: pageSize.value,
      sortBy: 'publishTime',
      sortOrder: 'desc'
    })
    const records = res.data.records || []
    articles.value = [...articles.value, ...records]
    total.value = res.data.total || 0
    hasMore.value = articles.value.length < total.value
  } catch (error) {
    console.error('获取文章列表失败:', error)
  } finally {
    loading.value = false
  }
}

const fetchMore = () => {
  currentPage.value++
  fetchArticles()
}

onMounted(fetchArticles)
</script>

<style scoped>
.card {
  background: var(--bg-secondary);
  border-radius: var(--border-radius);
  box-shadow: var(--shadow-sm);
  position: relative;
}

.timeline {
  position: relative;
  padding-left: 30px;
}

.timeline::before {
  content: "";
  position: absolute;
  top: 10px;
  bottom: 0;
  left: 0;
  width: 4px;
  background: var(--bg-tertiary);
  border-radius: 2px;
}

.year-group {
  position: relative;
  margin-bottom: 40px;
}

.year-header {
  position: relative;
  margin-bottom: 20px;
  left: -38px;
  display: flex;
  align-items: center;
}

.year-header::before {
  content: "";
  position: absolute;
  left: 36px;
  width: 10px;
  height: 10px;
  background: var(--color-primary);
  border-radius: 50%;
  border: 2px solid white;
  box-shadow: 0 0 0 3px var(--color-primary-light);
  z-index: 1;
}

.year-text {
  font-size: 24px;
  font-weight: bold;
  color: var(--text-primary);
  padding-left: 60px;
}

.article-item {
  position: relative;
  display: flex;
  align-items: center;
  padding: 12px 0;
  cursor: pointer;
  border-bottom: 1px dashed var(--border-color);
  transition: all 0.2s;
}

.article-item:hover {
  border-bottom-color: var(--color-primary);
  transform: translateX(5px);
}

.article-item::before {
  content: "";
  position: absolute;
  left: -33px;
  width: 6px;
  height: 6px;
  background: #bbb;
  border-radius: 50%;
  transition: all 0.2s;
}

.article-item:hover::before {
  background: var(--color-primary);
  transform: scale(1.5);
}

.article-date {
  font-size: 13px;
  color: var(--text-tertiary);
  margin-right: 20px;
  width: 50px;
  flex-shrink: 0;
}

.article-title {
  font-size: 16px;
  color: var(--text-secondary);
  transition: color 0.2s;
}

.article-item:hover .article-title {
  color: var(--color-primary);
}

@media (max-width: 640px) {
  .year-text {
    font-size: 20px;
  }
  .article-title {
    font-size: 14px;
  }
}
</style>
