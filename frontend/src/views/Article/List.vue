<template>
  <div class="py-6 md:py-8">
    <div class="flex flex-col lg:flex-row gap-6">
      <div class="flex-1 min-w-0">
        <!-- 加载骨架屏 -->
        <template v-if="loading">
          <div v-for="n in 5" :key="n" class="skeleton-item card mb-4">
            <el-skeleton :rows="2" animated />
          </div>
        </template>

        <template v-else>
          <div class="space-y-4">
            <ArticleCard
              v-for="item in articleList"
              :key="item.id"
              :article="item"
              :horizontal="true"
            />
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
            @current-change="onPageChange"
            @size-change="fetchArticles"
          />
        </template>
      </div>

      <aside v-if="showSidebar" class="w-full lg:w-70 flex-shrink-0">
        <Sidebar />
      </aside>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import ArticleCard from '@/components/ArticleCard.vue'
import Sidebar from '@/components/Sidebar.vue'
import { getArticleList } from '@/api/article'

const props = defineProps({
  categoryId: {
    type: [String, Number],
    default: null
  },
  tagId: {
    type: [String, Number],
    default: null
  },
  showSidebar: {
    type: Boolean,
    default: true
  }
})

const route = useRoute()
const articleList = ref([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const loading = ref(true)

const fetchArticles = async () => {
  loading.value = true
  try {
    const catId = props.categoryId || (route.name === 'category' ? route.params.id : null)
    const tId = props.tagId || (route.name === 'tag' ? route.params.id : null)

    const res = await getArticleList({
      page: currentPage.value,
      size: pageSize.value,
      categoryId: catId,
      tagId: tId,
      keyword: route.query.keyword || ''
    })
    articleList.value = res.data.records || []
    total.value = res.data.total || 0
  } catch (error) {
    console.error('获取文章列表失败:', error)
  } finally {
    loading.value = false
  }
}

// 翻页后回到列表顶部
const onPageChange = () => {
  fetchArticles()
  nextTick(() => {
    window.scrollTo({ top: 0, behavior: 'smooth' })
  })
}

watch(() => [props.categoryId, props.tagId, route.params.id, route.query.keyword], () => {
  currentPage.value = 1
  fetchArticles()
})

onMounted(() => {
  fetchArticles()
})
</script>

<style scoped>
.skeleton-item {
  padding: var(--spacing-lg);
}

.w-70 {
  width: 280px;
}

@media (max-width: 1024px) {
  .lg\:w-70 {
    width: 100%;
  }
}
</style>
