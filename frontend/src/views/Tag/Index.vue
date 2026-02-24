<template>
  <div class="py-6 md:py-8">
    <div class="card p-6">
      <h3 class="text-lg font-semibold mb-4" style="color: var(--text-primary)">
        标签：{{ tagName }}
      </h3>
      <ArticleList :tag-id="tagId" :show-sidebar="false" />
    </div>
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
