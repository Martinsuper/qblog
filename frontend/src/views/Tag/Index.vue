<template>
  <div class="py-6 md:py-8">
    <div class="mb-6">
      <el-breadcrumb separator="/">
        <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
        <el-breadcrumb-item :to="{ path: '/tags' }">标签</el-breadcrumb-item>
        <el-breadcrumb-item>{{ tagName }}</el-breadcrumb-item>
      </el-breadcrumb>
    </div>

    <div class="card p-6 mb-6">
      <div class="flex items-center gap-3">
        <span class="text-xl">🏷️</span>
        <h3 class="text-xl font-semibold" style="color: var(--text-primary)">
          标签：{{ tagName }}
        </h3>
      </div>
    </div>

    <ArticleList :tag-id="tagId" :show-sidebar="true" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import ArticleList from '@/views/Article/List.vue'
import { getTagList } from '@/api/tag'

const route = useRoute()
const tagId = ref(route.params.id)
const tagName = ref('加载中...')

onMounted(async () => {
  try {
    const res = await getTagList()
    const tag = res.data?.find(t => String(t.id) === String(tagId.value))
    tagName.value = tag?.name || '未知标签'
  } catch (error) {
    tagName.value = '加载失败'
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
