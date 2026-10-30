<template>
  <div class="py-6 md:py-8">
    <!-- 页面主标题（对屏幕阅读器/搜索引擎可见，视觉上以品牌与栏目标题承担层级） -->
    <h1 class="sr-only">
      QBlog · 首页
    </h1>

    <div class="flex flex-col lg:flex-row gap-6">
      <div class="flex-1 min-w-0">
        <!-- 加载骨架屏 -->
        <template v-if="loading">
          <div class="skeleton-hero card mb-6">
            <el-skeleton
              :rows="3"
              animated
            />
          </div>
          <div
            v-for="n in 4"
            :key="n"
            class="skeleton-item card mb-4"
          >
            <el-skeleton
              :rows="2"
              animated
            />
          </div>
        </template>

        <template v-else>
          <!-- 置顶/最新一篇：仅在文章确实是置顶时显示"置顶"徽章 -->
          <ArticleCard
            v-if="topArticle"
            :article="topArticle"
            :is-top="topArticle.top === 1"
            class="mb-6"
          />

          <h2 class="section-title text-base font-medium mb-4 pl-3">
            最新文章
          </h2>

          <!-- 文章列表 -->
          <div class="space-y-4">
            <ArticleCard
              v-for="article in articleList"
              :key="article.id"
              :article="article"
              :show-author="false"
            />
          </div>

          <div
            v-if="articleList.length > 0"
            class="mt-8 text-center"
          >
            <el-button
              @click="$router.push('/articles')"
            >
              查看更多文章
            </el-button>
          </div>

          <el-empty
            v-if="loadError"
            :description="loadError"
            class="py-12"
          >
            <el-button @click="fetchArticles">
              重试
            </el-button>
          </el-empty>
          <el-empty
            v-else-if="articleList.length === 0 && !topArticle"
            description="暂无文章"
            class="py-12"
          />
        </template>
      </div>

      <aside class="w-full lg:w-[280px] flex-shrink-0">
        <Sidebar />
      </aside>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import ArticleCard from '@/components/ArticleCard.vue'
import Sidebar from '@/components/Sidebar.vue'
import { getArticleList } from '@/api/article'

const articleList = ref([])
const topArticle = ref(null)
const loading = ref(true)
const loadError = ref('')

const fetchArticles = async () => {
  loading.value = true
  loadError.value = ''
  try {
    const res = await getArticleList({ page: 1, size: 6 })
    const records = res.data.records || []
    if (records.length > 0) {
      topArticle.value = records[0]
      articleList.value = records.slice(1)
    }
  } catch (error) {
    console.error('获取文章失败:', error)
    loadError.value = '文章加载失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchArticles()
})
</script>

<style scoped>
.section-title {
  color: var(--text-primary);
  border-left: 3px solid var(--color-primary);
}

.skeleton-hero,
.skeleton-item {
  padding: var(--spacing-lg);
}
</style>
