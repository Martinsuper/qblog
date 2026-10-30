<template>
  <div class="py-6 md:py-8">
    <div class="card p-6 md:p-8">
      <div class="flex items-center gap-3 mb-8">
        <span class="text-2xl">🏷️</span>
        <h1
          class="text-2xl font-bold"
          style="color: var(--text-primary)"
        >
          标签云
        </h1>
        <span
          class="text-sm px-2 py-0.5 rounded"
          style="background: var(--bg-tertiary); color: var(--text-secondary)"
        >
          共 {{ tags.length }} 个标签
        </span>
      </div>

      <div class="flex flex-wrap gap-4">
        <router-link
          v-for="tag in tags"
          :key="tag.id"
          :to="`/tag/${tag.id}`"
          class="tag-item transition-all duration-200"
        >
          <el-tag
            :size="getTagSize(tag)"
            :type="getTagType(tag)"
            class="cursor-pointer hover:scale-110"
            effect="plain"
          >
            {{ tag.name }}
            <span
              v-if="tag.articleCount"
              class="ml-1 text-xs opacity-70"
            >({{ tag.articleCount }})</span>
          </el-tag>
        </router-link>
      </div>

      <el-empty
        v-if="tags.length === 0"
        description="暂无标签"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getTagList } from '@/api/tag'

const tags = ref([])

const getTagSize = (tag) => {
  const count = tag.articleCount || 0
  if (count > 10) return 'large'
  if (count > 5) return 'default'
  return 'small'
}

const getTagType = (tag) => {
  const types = ['', 'success', 'info', 'warning', 'danger']
  return types[tag.id % types.length]
}

onMounted(async () => {
  try {
    const res = await getTagList()
    tags.value = res.data || []
  } catch (error) {
    console.error('获取标签列表失败:', error)
  }
})
</script>

<style scoped>
.card {
  background: var(--bg-secondary);
  border-radius: var(--border-radius);
  box-shadow: var(--shadow-sm);
}

.tag-item {
  text-decoration: none;
}

.tag-item :deep(.el-tag) {
  transition: all 0.2s;
}
</style>
