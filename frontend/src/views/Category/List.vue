<template>
  <div class="py-6 md:py-8">
    <div class="card p-6 md:p-8">
      <div class="flex items-center gap-3 mb-8">
        <span class="text-2xl">📁</span>
        <h2 class="text-2xl font-bold" style="color: var(--text-primary)">分类归档</h2>
        <span class="text-sm px-2 py-0.5 bg-gray-100 dark:bg-gray-800 rounded text-gray-500">
          共 {{ categories.length }} 个分类
        </span>
      </div>

      <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
        <router-link
          v-for="cat in categories"
          :key="cat.id"
          :to="`/category/${cat.id}`"
          class="category-item group flex items-center justify-between p-4 rounded-lg border border-transparent hover:border-blue-500 hover:bg-blue-50/30 dark:hover:bg-blue-900/10 transition-all duration-200"
        >
          <div class="flex items-center gap-3">
            <div class="w-10 h-10 rounded-full bg-blue-100 dark:bg-blue-900/30 flex items-center justify-center text-blue-600 dark:text-blue-400 group-hover:scale-110 transition-transform duration-200">
              <span class="text-lg font-medium">{{ cat.name.charAt(0) }}</span>
            </div>
            <span class="font-medium text-lg" style="color: var(--text-primary)">{{ cat.name }}</span>
          </div>
          <div class="text-sm px-2 py-1 rounded-full bg-gray-100 dark:bg-gray-800 text-gray-500 group-hover:bg-blue-500 group-hover:text-white transition-colors duration-200">
            {{ cat.articleCount || 0 }} 篇文章
          </div>
        </router-link>
      </div>

      <el-empty v-if="categories.length === 0" description="暂无分类" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getCategoryList } from '@/api/category'

const categories = ref([])

onMounted(async () => {
  try {
    const res = await getCategoryList()
    categories.value = res.data || []
  } catch (error) {
    console.error('获取分类列表失败:', error)
  }
})
</script>

<style scoped>
.card {
  background: var(--bg-secondary);
  border-radius: var(--border-radius);
  box-shadow: var(--shadow-sm);
}

.category-item {
  background: var(--bg-tertiary);
  text-decoration: none;
}

.category-item:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
}
</style>
