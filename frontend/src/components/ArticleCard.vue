<template>
  <div
    class="article-card card cursor-pointer hover:shadow-md transition-shadow duration-200"
    :class="{ 'flex flex-col md:flex-row overflow-hidden': horizontal }"
    @click="$router.push(`/article/${article.id}`)"
  >
    <div v-if="article.coverImage && horizontal" class="w-full md:w-60 flex-shrink-0">
      <img :src="article.coverImage" alt="封面" class="w-full h-40 md:h-full object-cover" />
    </div>
    
    <div class="flex-1" :class="horizontal ? 'p-4 md:p-5' : ''">
      <h3 :class="titleClass" style="color: var(--text-primary)">
        {{ article.title }}
      </h3>
      <p class="text-sm mb-3 line-clamp-2" style="color: var(--text-secondary)">
        {{ article.summary }}
      </p>
      <div class="flex flex-wrap items-center gap-3 text-xs" style="color: var(--text-tertiary)">
        <span class="flex items-center gap-1" v-if="showAuthor">
          <el-avatar :size="20" :src="article.author?.avatar">
            {{ article.author?.nickname?.charAt(0) }}
          </el-avatar>
          {{ article.author?.nickname || '管理员' }}
        </span>
        <span class="flex items-center gap-1">
          <el-icon><Calendar /></el-icon>
          {{ article.createTime || article.publishTime }}
        </span>
        <span class="flex items-center gap-1">
          <el-icon><View /></el-icon>
          {{ article.viewCount }}
        </span>
        <span class="flex items-center gap-1">
          <el-icon><Star /></el-icon>
          {{ article.likeCount }}
        </span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  article: {
    type: Object,
    required: true
  },
  horizontal: {
    type: Boolean,
    default: false
  },
  showAuthor: {
    type: Boolean,
    default: true
  },
  isTop: {
    type: Boolean,
    default: false
  }
})

const titleClass = computed(() => {
  if (props.isTop) {
    return 'text-xl md:text-2xl font-semibold mb-3'
  }
  return 'text-base font-medium mb-2'
})
</script>

<style scoped>
.card {
  background: var(--bg-secondary);
  border-radius: var(--border-radius);
  padding: var(--spacing-md);
  box-shadow: var(--shadow-sm);
}

.article-card.overflow-hidden {
  padding: 0;
}

.line-clamp-2 {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>
