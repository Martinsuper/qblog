<template>
  <div class="py-6 md:py-8">
    <div class="card p-6">
      <h3 class="text-lg font-semibold mb-4" style="color: var(--text-primary)">
        分类：{{ categoryName }}
      </h3>
      <ArticleList />
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import ArticleList from '@/views/Article/List.vue'

const categoryName = ref('技术')
</script>

<style scoped>
.card {
  background: var(--bg-secondary);
  border-radius: var(--border-radius);
  box-shadow: var(--shadow-sm);
}
</style>
