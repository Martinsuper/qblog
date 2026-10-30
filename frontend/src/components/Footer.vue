<template>
  <footer class="footer">
    <div class="container">
      <div class="footer-content">
        <div class="footer-info">
          <span class="brand">QBlog</span>
          <span class="divider">|</span>
          <span class="copyright">© {{ currentYear }} All Rights Reserved</span>
        </div>
        <div class="footer-tagline">
          私人知识库 · 让知识有序可寻
        </div>
      </div>
    </div>
  </footer>
</template>

<script setup>
import { computed } from 'vue'

const currentYear = computed(() => new Date().getFullYear())
</script>

<style scoped>
.footer {
  margin-top: auto;
  padding: var(--spacing-xl) 0;
  background: var(--bg-secondary);
  border-top: 2px solid var(--border-color);
}

.footer-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: var(--spacing-lg);
}

.footer-info {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  color: var(--text-secondary);
  font-size: 0.875rem;
}

.brand {
  font-weight: 600;
  background: var(--color-primary-gradient);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  color: transparent;
  display: inline-block;
}
.divider {
  color: var(--text-tertiary);
}

.footer-tagline {
  color: var(--text-tertiary);
  font-size: 0.8125rem;
}

@media (max-width: 640px) {
  .footer-content {
    flex-direction: column;
    text-align: center;
  }
}
</style>
