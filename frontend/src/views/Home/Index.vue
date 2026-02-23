<template>
  <div class="py-6 md:py-8">
    <div class="flex flex-col lg:flex-row gap-6">
      <div class="flex-1 min-w-0">
        <div v-if="topArticle" class="card mb-6 cursor-pointer hover:shadow-md transition-shadow duration-200" @click="goToArticle(topArticle.id)">
          <h2 class="text-xl md:text-2xl font-semibold mb-3" style="color: var(--text-primary)">
            {{ topArticle.title }}
          </h2>
          <p class="text-sm md:text-base mb-4 line-clamp-2" style="color: var(--text-secondary)">
            {{ topArticle.summary }}
          </p>
          <div class="flex flex-wrap gap-4 text-xs md:text-sm" style="color: var(--text-tertiary)">
            <span class="flex items-center gap-1">
              <el-icon><User /></el-icon>
              {{ topArticle.author?.nickname || '管理员' }}
            </span>
            <span class="flex items-center gap-1">
              <el-icon><Calendar /></el-icon>
              {{ topArticle.createTime }}
            </span>
            <span class="flex items-center gap-1">
              <el-icon><View /></el-icon>
              {{ topArticle.viewCount }}
            </span>
            <span class="flex items-center gap-1">
              <el-icon><Star /></el-icon>
              {{ topArticle.likeCount }}
            </span>
          </div>
        </div>

        <h3 class="text-base font-medium mb-4 pl-3 border-l-3 border-blue-500" style="color: var(--text-primary)">
          最新文章
        </h3>

        <div class="space-y-4">
          <div
            v-for="article in articleList"
            :key="article.id"
            class="card cursor-pointer hover:shadow-md transition-shadow duration-200"
            @click="goToArticle(article.id)"
          >
            <h4 class="text-base font-medium mb-2" style="color: var(--text-primary)">
              {{ article.title }}
            </h4>
            <p class="text-sm mb-3 line-clamp-2" style="color: var(--text-secondary)">
              {{ article.summary }}
            </p>
            <div class="flex flex-wrap gap-3 text-xs" style="color: var(--text-tertiary)">
              <span class="flex items-center gap-1">
                <el-icon><Calendar /></el-icon>
                {{ article.createTime }}
              </span>
              <span class="flex items-center gap-1">
                <el-icon><View /></el-icon>
                {{ article.viewCount }}
              </span>
              <span class="flex items-center gap-1">
                <el-icon><Star /></el-icon>
                {{ article.likeCount }}
              </span>
            </div>
          </div>
        </div>

        <el-empty v-if="articleList.length === 0 && !topArticle" description="暂无文章" class="py-12" />
      </div>

      <aside class="w-full lg:w-70 flex-shrink-0 space-y-5">
        <div class="card">
          <h4 class="font-medium mb-3 flex items-center gap-2" style="color: var(--text-primary)">
            <span>📁</span>
            <span>分类</span>
          </h4>
          <ul class="space-y-2">
            <li v-for="cat in categories" :key="cat.id">
              <router-link
                :to="`/category/${cat.id}`"
                class="block py-1.5 px-2 rounded text-sm transition-colors duration-150"
                style="color: var(--text-secondary)"
              >
                {{ cat.name }}
              </router-link>
            </li>
          </ul>
        </div>

        <div class="card">
          <h4 class="font-medium mb-3 flex items-center gap-2" style="color: var(--text-primary)">
            <span>🔥</span>
            <span>热门文章</span>
          </h4>
          <ul class="space-y-3">
            <li
              v-for="(article, index) in hotArticles"
              :key="article.id"
              class="flex items-start gap-2"
            >
              <span
                class="flex-shrink-0 w-5 h-5 rounded text-xs flex items-center justify-center font-medium"
                :class="[
                  index === 0 ? 'bg-red-500 text-white' : '',
                  index === 1 ? 'bg-orange-400 text-white' : '',
                  index === 2 ? 'bg-yellow-400 text-white' : '',
                  index > 2 ? 'bg-gray-200 text-gray-500 dark:bg-gray-600 dark:text-gray-300' : ''
                ]"
              >
                {{ index + 1 }}
              </span>
              <router-link
                :to="`/article/${article.id}`"
                class="flex-1 text-sm truncate transition-colors duration-150"
                style="color: var(--text-secondary)"
              >
                {{ article.title }}
              </router-link>
              <span class="text-xs flex-shrink-0" style="color: var(--text-tertiary)">
                {{ article.viewCount }}
              </span>
            </li>
          </ul>
        </div>

        <div class="card">
          <h4 class="font-medium mb-3 flex items-center gap-2" style="color: var(--text-primary)">
            <span>🏷️</span>
            <span>标签</span>
          </h4>
          <div class="flex flex-wrap gap-2">
            <el-tag
              v-for="tag in tags"
              :key="tag.id"
              size="small"
              class="cursor-pointer"
              @click="$router.push(`/tag/${tag.id}`)"
            >
              {{ tag.name }}
            </el-tag>
          </div>
        </div>
      </aside>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getArticleList, getHotArticles } from '@/api/article'
import { getCategoryList } from '@/api/category'
import { getTagList } from '@/api/tag'

const router = useRouter()

const articleList = ref([])
const topArticle = ref(null)
const hotArticles = ref([])
const categories = ref([])
const tags = ref([])

const goToArticle = (id) => {
  router.push(`/article/${id}`)
}

const fetchArticles = async () => {
  try {
    const res = await getArticleList({ page: 1, size: 10 })
    const records = res.data.records || []
    if (records.length > 0) {
      topArticle.value = records[0]
      articleList.value = records.slice(1)
    }
  } catch (error) {
    console.error('获取文章失败:', error)
  }
}

const fetchHotArticles = async () => {
  try {
    const res = await getHotArticles({ limit: 5 })
    hotArticles.value = res.data || []
  } catch (error) {
    console.error('获取热门文章失败:', error)
  }
}

const fetchCategories = async () => {
  try {
    const res = await getCategoryList()
    categories.value = res.data || []
  } catch (error) {
    console.error('获取分类失败:', error)
  }
}

const fetchTags = async () => {
  try {
    const res = await getTagList()
    tags.value = res.data || []
  } catch (error) {
    console.error('获取标签失败:', error)
  }
}

onMounted(() => {
  fetchArticles()
  fetchHotArticles()
  fetchCategories()
  fetchTags()
})
</script>

<style scoped>
.card {
  background: var(--bg-secondary);
  border-radius: var(--border-radius);
  padding: var(--spacing-md);
  box-shadow: var(--shadow-sm);
}

.border-l-3 {
  border-left-width: 3px;
}

.w-70 {
  width: 280px;
}

.line-clamp-2 {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

aside a:hover {
  color: var(--color-primary);
}

@media (max-width: 1024px) {
  .lg\\:w-70 {
    width: 100%;
  }
}
</style>
