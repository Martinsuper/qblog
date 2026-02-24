<template>
  <div class="py-6 md:py-8">
    <div class="card p-6">
      <h3 class="text-lg font-semibold mb-4" style="color: var(--text-primary)">
        分类：{{ categoryName }}
      </h3>
      <ArticleList :category-id="categoryId" :show-sidebar="false" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import ArticleList from '@/views/Article/List.vue'
import { getCategoryDetail } from '@/api/category'

const route = useRoute()
const categoryId = ref(route.params.id)
const categoryName = ref('加载中...')

onMounted(async () => {
  try {
    const res = await getCategoryDetail(categoryId.value)
    categoryName.value = res.data?.name || '未知分类'
  } catch (error) {
    categoryName.value = '加载失败'
  }
})
</script>

<style scoped>
.card {
  background: var(--bg-secondary);
  border-radius: var(--border-radius);
  box-shadow: var(--shadow-sm);
}
</style>
