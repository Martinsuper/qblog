<template>
  <div class="py-6 md:py-8">
    <div class="max-w-4xl mx-auto">
      <div class="card p-6 md:p-8">
        <h3 class="text-xl font-semibold mb-6" style="color: var(--text-primary)">我的收藏</h3>
        <el-empty description="暂无收藏" />
      </div>
    </div>
  </div>
</template>

<script setup>
</script>

<style scoped>
.card {
  background: var(--bg-secondary);
  border-radius: var(--border-radius);
  box-shadow: var(--shadow-sm);
}

.max-w-4xl {
  max-width: 900px;
}
</style>
