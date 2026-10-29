<template>
  <div class="py-6 md:py-8">
    <div class="mb-6">
      <el-breadcrumb separator="/">
        <el-breadcrumb-item :to="{ path: '/' }">
          首页
        </el-breadcrumb-item>
        <el-breadcrumb-item :to="{ path: '/tags' }">
          标签
        </el-breadcrumb-item>
        <el-breadcrumb-item>{{ tagName }}</el-breadcrumb-item>
      </el-breadcrumb>
    </div>

    <div class="card p-6 mb-6">
      <div class="flex items-center gap-3">
        <span class="text-xl">🏷️</span>
        <h3
          class="text-xl font-semibold"
          style="color: var(--text-primary)"
        >
          标签：{{ tagName }}
        </h3>
      </div>
    </div>

    <ArticleList
      :tag-id="tagId"
      :show-sidebar="true"
    />
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import ArticleList from '@/views/Article/List.vue'
import { getTagDetail } from '@/api/tag'

const route = useRoute()
// 同一路由组件复用时（/tag/1 → /tag/2）需响应参数变化
const tagId = computed(() => route.params.id)
const tagName = ref('加载中...')

const loadTag = async () => {
  tagName.value = '加载中...'
  try {
    const res = await getTagDetail(tagId.value)
    tagName.value = res.data?.name || '未知标签'
  } catch (error) {
    // 失败提示由 axios 拦截器统一弹出
    console.error('获取标签失败:', error)
    tagName.value = '加载失败'
  }
}

watch(tagId, loadTag)
onMounted(loadTag)
</script>

<style scoped>
.card {
  background: var(--bg-secondary);
  border-radius: var(--border-radius);
  box-shadow: var(--shadow-sm);
}
</style>
