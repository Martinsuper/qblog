<template>
  <aside class="sidebar space-y-5">
    <!-- 分类 -->
    <div class="panel-card">
      <h2 class="sidebar-title flex items-center gap-2">
        <span aria-hidden="true">📁</span>
        <span>分类</span>
      </h2>
      <ul class="space-y-2">
        <li
          v-for="cat in categories"
          :key="cat.id"
          class="category-list-item flex items-center justify-between text-sm"
        >
          <router-link
            :to="`/category/${cat.id}`"
            class="flex-1 truncate"
            style="color: var(--text-secondary)"
          >
            {{ cat.name }}
          </router-link>
          <span
            v-if="cat.articleCount !== undefined"
            class="text-xs ml-2"
            style="color: var(--text-tertiary)"
          >
            {{ cat.articleCount }}
          </span>
        </li>
      </ul>
    </div>

    <!-- 热门文章 -->
    <div class="panel-card">
      <h2 class="sidebar-title flex items-center gap-2">
        <span aria-hidden="true">🔥</span>
        <span>热门文章</span>
      </h2>
      <ul
        v-if="hotArticles.length > 0"
        class="space-y-3"
      >
        <li
          v-for="(article, index) in hotArticles"
          :key="article.id"
          class="hot-article-item flex items-start gap-2"
        >
          <span
            class="rank-badge flex-shrink-0 w-5 h-5 rounded text-xs flex items-center justify-center font-medium"
            :class="`rank-${Math.min(index + 1, 3)}`"
          >
            {{ index + 1 }}
          </span>
          <router-link
            :to="`/article/${article.id}`"
            class="flex-1 text-sm truncate transition-colors duration-150"
            style="color: var(--text-secondary)"
          >
            {{ article.title }}
          </router-link>
          <span
            class="text-xs flex-shrink-0"
            style="color: var(--text-tertiary)"
          >
            {{ article.viewCount }}
          </span>
        </li>
      </ul>
      <p
        v-else
        class="hot-empty text-sm"
        style="color: var(--text-tertiary)"
      >
        暂无热门文章
      </p>
    </div>

    <!-- 标签 -->
    <div class="panel-card">
      <h2 class="sidebar-title flex items-center gap-2">
        <span aria-hidden="true">🏷️</span>
        <span>标签</span>
      </h2>
      <div class="flex flex-wrap gap-2">
        <el-tag
          v-for="tag in tags"
          :key="tag.id"
          size="small"
          class="cursor-pointer"
          @click="$router.push(`/tag/${tag.id}`)"
        >
          {{ tag.name }}
        </el-tag>
      </div>
    </div>
  </aside>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getHotArticles } from '@/api/article'
import { getCategoryList } from '@/api/category'
import { getTagList } from '@/api/tag'

// 模块级缓存：同一会话内多次导航不重复请求，5 分钟过期
const CACHE_TTL = 5 * 60 * 1000
let cache = null
let cacheAt = 0
let inflight = null

const hotArticles = ref([])
const categories = ref([])
const tags = ref([])

const fetchData = async () => {
  const now = Date.now()
  if (cache && now - cacheAt < CACHE_TTL) {
    hotArticles.value = cache.hot
    categories.value = cache.categories
    tags.value = cache.tags
    return
  }
  try {
    if (!inflight) {
      inflight = Promise.all([
        getHotArticles({ limit: 10 }),
        getCategoryList(),
        getTagList()
      ]).then(([hotRes, catRes, tagRes]) => {
        cache = {
          hot: hotRes.data || [],
          categories: catRes.data || [],
          tags: tagRes.data || []
        }
        cacheAt = Date.now()
        return cache
      }).finally(() => {
        inflight = null
      })
    }
    const data = await inflight
    hotArticles.value = data.hot
    categories.value = data.categories
    tags.value = data.tags
  } catch (error) {
    console.error('侧边栏数据加载失败:', error)
  }
}

onMounted(fetchData)
</script>

<style scoped>
/* 侧边栏卡片独立命名，避免与全局 .card / uno shortcut 两层来源混淆 */
.panel-card {
  background: var(--bg-secondary);
  border-radius: var(--border-radius-xl);
  padding: var(--spacing-lg);
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--border-color-light);
  transition: all var(--transition-normal);
}
.panel-card:hover {
  box-shadow: var(--shadow-md);
  border-color: var(--border-color);
}

/* 区块标题：语义用 h2，视觉保持原来的小标题尺寸 */
.sidebar-title {
  font-size: 0.9375rem;
  font-weight: 500;
  margin: 0 0 var(--spacing-sm);
  color: var(--text-primary);
}

.hot-empty {
  margin: 0;
}

.sidebar :deep(a):hover {
  color: var(--color-primary) !important;
}
.category-list-item {
  padding: var(--spacing-xs) var(--spacing-sm);
  border-radius: var(--border-radius-sm);
  transition: all var(--transition-fast);
}
.category-list-item:hover {
  background: var(--bg-tertiary);
}

/* padding 常驻，hover 只变色，避免内容跳动 */
.hot-article-item {
  padding: var(--spacing-xs) var(--spacing-sm);
  margin: calc(var(--spacing-xs) * -1) calc(var(--spacing-sm) * -1);
  border-radius: var(--border-radius-sm);
  transition: all var(--transition-fast);
}
.hot-article-item:hover {
  background: var(--bg-tertiary);
}

/* 热门排名徽章：用层叠强调， 不再依赖 tailwind 调色板 */
.rank-badge {
  border-radius: 999px;
}
.rank-1 { background: var(--color-primary-gradient); color: #fff; }
.rank-2 { background: var(--color-primary-light); color: #fff; }
.rank-3 { background: var(--bg-tertiary); color: var(--text-secondary); }
.rank-badge:not(.rank-1):not(.rank-2):not(.rank-3) {
  background: var(--bg-tertiary);
  color: var(--text-secondary);
}

.sidebar {
  gap: var(--spacing-lg);
}
</style>
