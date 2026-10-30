<template>
  <router-link
    :to="`/article/${article.id}`"
    class="article-card card"
    :class="{ 'flex flex-col md:flex-row overflow-hidden': horizontal }"
  >
    <div
      v-if="article.coverImage && horizontal"
      class="cover-wrap w-full md:w-60 flex-shrink-0"
    >
      <img
        :src="article.coverImage"
        :alt="`${article.title} - 封面图`"
        loading="lazy"
        decoding="async"
        class="w-full h-40 md:h-full object-cover"
      >
    </div>

    <div
      class="flex-1"
      :class="horizontal ? 'p-4 md:p-5' : ''"
    >
      <h3
        :class="titleClass"
        class="title-text"
      >
        <span
          v-if="isTop"
          class="top-badge"
        >置顶</span>
        {{ article.title }}
      </h3>
      <p
        class="text-sm mb-3 line-clamp-2"
        style="color: var(--text-secondary)"
      >
        {{ article.summary }}
      </p>
      <div
        class="flex flex-wrap items-center gap-3 text-xs"
        style="color: var(--text-tertiary)"
      >
        <span
          v-if="showAuthor"
          class="flex items-center gap-1"
        >
          <el-avatar
            :size="20"
            :src="article.author?.avatar"
          >
            {{ article.author?.nickname?.charAt(0) }}
          </el-avatar>
          {{ article.author?.nickname || '管理员' }}
        </span>
        <span
          class="flex items-center gap-1"
          :title="formatDateTime(article.createTime || article.publishTime)"
        >
          <el-icon><Calendar /></el-icon>
          {{ formatRelativeTime(article.publishTime || article.createTime) }}
        </span>
        <span class="flex items-center gap-1">
          <el-icon><View /></el-icon>
          {{ article.viewCount }}
        </span>
      </div>
    </div>
  </router-link>
</template>

<script setup>
import { computed } from 'vue'
import { Calendar, View } from '@element-plus/icons-vue'
import { formatRelativeTime, formatDateTime } from '@/composables/useTime'

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
.article-card {
  display: block;
  color: inherit;
  text-decoration: none;
  cursor: pointer;
  transition: transform 180ms ease-out, box-shadow 180ms ease-out, border-color 180ms ease-out;
}

.article-card:hover,
.article-card:focus-visible {
  transform: translateY(-4px);
  box-shadow: var(--shadow-hover);
  border-color: var(--border-color);
}

/* 鍵盘焦点必须有可见指示，不能和 hover 一样只靠阴影 */
.article-card:focus-visible {
  outline: 2px solid var(--color-primary);
  outline-offset: 2px;
}

.article-card:focus {
  outline: none;
}

.article-card.overflow-hidden {
  padding: 0;
}

.cover-wrap img {
  transition: transform var(--transition-slow);
}

.article-card:hover .cover-wrap img {
  transform: scale(1.03);
}

.line-clamp-2 {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.title-text {
  color: var(--text-primary);
  transition: color var(--transition-fast);
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
}

.article-card:hover .title-text {
  color: var(--color-primary);
}

.top-badge {
  flex-shrink: 0;
  font-size: 0.6875rem;
  font-weight: 500;
  line-height: 1;
  padding: 4px 8px;
  border-radius: 999px;
  color: #fff;
  background: var(--color-primary-gradient);
}
</style>
