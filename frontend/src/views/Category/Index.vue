<template>
  <div class="py-6 md:py-8">
    <div class="mb-6">
      <el-breadcrumb separator="/">
        <el-breadcrumb-item :to="{ path: '/' }">
          首页
        </el-breadcrumb-item>
        <el-breadcrumb-item :to="{ path: '/categories' }">
          分类
        </el-breadcrumb-item>
        <el-breadcrumb-item>{{ categoryName }}</el-breadcrumb-item>
      </el-breadcrumb>
    </div>
    
    <div class="card p-6 mb-6">
      <div class="flex items-center gap-3">
        <span class="text-xl">📁</span>
        <h1
          class="text-xl font-semibold"
          style="color: var(--text-primary)"
        >
          分类：{{ categoryName }}
        </h1>
      </div>
    </div>

    <ArticleList
      :category-id="categoryId"
      :show-sidebar="true"
    />
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import ArticleList from '@/views/Article/List.vue'
import { getCategoryDetail } from '@/api/category'

const route = useRoute()
// 同一路由组件复用时（/category/1 → /category/2）需响应参数变化
const categoryId = computed(() => route.params.id)
const categoryName = ref('加载中...')

const loadCategory = async () => {
  categoryName.value = '加载中...'
  try {
    const res = await getCategoryDetail(categoryId.value)
    categoryName.value = res.data?.name || '未知分类'
  } catch (error) {
    // 失败提示由 axios 拦截器统一弹出
    console.error('获取分类失败:', error)
    categoryName.value = '加载失败'
  }
}

watch(categoryId, loadCategory)
onMounted(loadCategory)
</script>

<style scoped>
.card {
  background: var(--bg-secondary);
  border-radius: var(--border-radius);
  box-shadow: var(--shadow-sm);
}
</style>
