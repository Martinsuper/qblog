<template>
  <div class="py-6 md:py-8">
    <div class="flex flex-col lg:flex-row gap-6">
      <div class="flex-1 min-w-0">
        <div class="space-y-4">
          <div
            v-for="item in articleList"
            :key="item.id"
            class="card flex flex-col md:flex-row overflow-hidden cursor-pointer hover:shadow-md transition-shadow duration-200"
            @click="$router.push(`/article/${item.id}`)"
          >
            <div v-if="item.coverImage" class="w-full md:w-60 flex-shrink-0">
              <img :src="item.coverImage" alt="封面" class="w-full h-40 md:h-full object-cover" />
            </div>
            <div class="flex-1 p-4 md:p-5">
              <h3 class="text-lg font-medium mb-2" style="color: var(--text-primary)">
                {{ item.title }}
              </h3>
              <p class="text-sm mb-3 line-clamp-2" style="color: var(--text-secondary)">
                {{ item.summary }}
              </p>
              <div class="flex flex-wrap items-center gap-3 text-xs" style="color: var(--text-tertiary)">
                <span class="flex items-center gap-1">
                  <el-avatar :size="20" :src="item.author?.avatar">
                    {{ item.author?.nickname?.charAt(0) }}
                  </el-avatar>
                  {{ item.author?.nickname }}
                </span>
                <span class="flex items-center gap-1">
                  <el-icon><Calendar /></el-icon>
                  {{ item.createTime }}
                </span>
                <span class="flex items-center gap-1">
                  <el-icon><View /></el-icon>
                  {{ item.viewCount }}
                </span>
                <span class="flex items-center gap-1">
                  <el-icon><Star /></el-icon>
                  {{ item.likeCount }}
                </span>
              </div>
            </div>
          </div>
        </div>

        <el-empty v-if="articleList.length === 0" description="暂无文章" class="py-12" />

        <el-pagination
          v-if="total > 0"
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          class="flex justify-center mt-8"
          @current-change="fetchArticles"
          @size-change="fetchArticles"
        />
      </div>

      <aside class="w-full lg:w-70 flex-shrink-0 space-y-5">
        <div class="card">
          <h4 class="font-medium mb-3" style="color: var(--text-primary)">分类</h4>
          <ul class="space-y-2">
            <li v-for="cat in categories" :key="cat.id" class="flex items-center justify-between py-1.5 px-2 rounded text-sm" style="color: var(--text-secondary)">
              <router-link :to="`/category/${cat.id}`" class="hover:text-blue-500 transition-colors">
                {{ cat.name }}
              </router-link>
              <span class="text-xs" style="color: var(--text-tertiary)">{{ cat.articleCount || 0 }}</span>
            </li>
          </ul>
        </div>

        <div class="card">
          <h4 class="font-medium mb-3" style="color: var(--text-primary)">热门文章</h4>
          <ul class="space-y-3">
            <li v-for="(item, index) in hotArticles" :key="item.id" class="flex items-start gap-2">
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
              <router-link :to="`/article/${item.id}`" class="flex-1 text-sm truncate" style="color: var(--text-secondary)">
                {{ item.title }}
              </router-link>
              <span class="text-xs flex items-center gap-0.5 flex-shrink-0" style="color: var(--text-tertiary)">
                <el-icon><View /></el-icon>
                {{ item.viewCount }}
              </span>
            </li>
          </ul>
        </div>

        <div class="card">
          <h4 class="font-medium mb-3" style="color: var(--text-primary)">标签</h4>
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
import { ref, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { getArticleList, getHotArticles } from '@/api/article'
import { getCategoryList } from '@/api/category'
import { getTagList } from '@/api/tag'

const props = defineProps({
  categoryId: {
    type: [String, Number],
    default: null
  }
})

const route = useRoute()
const articleList = ref([])
const categories = ref([])
const tags = ref([])
const hotArticles = ref([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)

const fetchArticles = async () => {
  try {
    // 优先使用 props 传入的 categoryId，其次使用路由参数
    const catId = props.categoryId ? Number(props.categoryId) : (route.params.id ? Number(route.params.id) : null)
    const tagId = !props.categoryId && route.params.id ? Number(route.params.id) : null
    
    const res = await getArticleList({
      page: currentPage.value,
      size: pageSize.value,
      categoryId: catId,
      tagId: tagId,
      keyword: route.query.keyword || ''
    })
    articleList.value = res.data.records || []
    total.value = res.data.total || 0
  } catch (error) {
    console.error('获取文章列表失败:', error)
  }
}

// 监听 categoryId 变化
watch(() => props.categoryId, () => {
  currentPage.value = 1
  fetchArticles()
})

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

const fetchHotArticles = async () => {
  try {
    const res = await getHotArticles({ limit: 10 })
    hotArticles.value = res.data || []
  } catch (error) {
    console.error('获取热门文章失败:', error)
  }
}

onMounted(() => {
  fetchArticles()
  fetchCategories()
  fetchTags()
  fetchHotArticles()
})
</script>

<style scoped>
.card {
  background: var(--bg-secondary);
  border-radius: var(--border-radius);
  box-shadow: var(--shadow-sm);
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
