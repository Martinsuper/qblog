<template>
  <aside class="sidebar space-y-5">
    <!-- 分类 -->
    <div class="card">
      <h4 class="font-medium mb-3 flex items-center gap-2" style="color: var(--text-primary)">
        <span>📁</span>
        <span>分类</span>
      </h4>
      <ul class="space-y-2">
        <li v-for="cat in categories" :key="cat.id" class="category-list-item flex items-center justify-between text-sm">
          <router-link
            :to="`/category/${cat.id}`"
            class="flex-1 truncate"
            style="color: var(--text-secondary)"
          >
            {{ cat.name }}
          </router-link>
          <span v-if="cat.articleCount !== undefined" class="text-xs ml-2" style="color: var(--text-tertiary)">
            {{ cat.articleCount }}
          </span>
        </li>
      </ul>
    </div>

    <!-- 热门文章 -->
    <div class="card">
      <h4 class="font-medium mb-3 flex items-center gap-2" style="color: var(--text-primary)">
        <span>🔥</span>
        <span>热门文章</span>
      </h4>
      <ul class="space-y-3">
        <li
          v-for="(article, index) in hotArticles"
          :key="article.id"
          class="hot-article-item flex items-start gap-2">
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

    <!-- 标签 -->
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
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getHotArticles } from '@/api/article'
import { getCategoryList } from '@/api/category'
import { getTagList } from '@/api/tag'

const hotArticles = ref([])
const categories = ref([])
const tags = ref([])

const fetchData = async () => {
  try {
    const [hotRes, catRes, tagRes] = await Promise.all([
      getHotArticles({ limit: 10 }),
      getCategoryList(),
      getTagList()
    ])
    hotArticles.value = hotRes.data || []
    categories.value = catRes.data || []
    tags.value = tagRes.data || []
  } catch (error) {
    console.error('侧边栏数据加载失败:', error)
  }
}

onMounted(fetchData)
</script>

<style scoped>
.card {
  background: var(--bg-secondary);
  border-radius: var(--border-radius-xl);
  padding: var(--spacing-lg);
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--border-color-light);
  transition: all var(--transition-normal);
}
.card:hover {
  box-shadow: var(--shadow-md);
  border-color: var(--border-color);
}

.sidebar a:hover {
  color: var(--color-primary) !important;
}
.category-list-item {
  padding: var(--spacing-xs) var(--spacing-sm);
  border-radius: var(--border-radius-sm);
  transition: all var(--transition-fast);
}
.category-list-item:hover {
  background: var(--bg-tertiary);
}

.hot-article-item {
  transition: all var(--transition-fast);
}
.hot-article-item:hover {
  background: var(--bg-tertiary);
  border-radius: var(--border-radius-sm);
  padding: var(--spacing-xs) var(--spacing-sm);
}

.sidebar {
  gap: var(--spacing-lg);
}
</style>

