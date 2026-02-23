<template>
  <footer class="footer">
    <div class="container">
      <div class="footer-content">
        <div class="footer-info">
          <span class="brand">QBlog</span>
          <span class="divider">|</span>
          <span class="copyright">© {{ currentYear }} All Rights Reserved</span>
        </div>
        <div class="footer-links">
          <a href="https://github.com" target="_blank" rel="noopener noreferrer">GitHub</a>
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
  padding: var(--spacing-lg) 0;
  background: var(--bg-secondary);
  border-top: 1px solid var(--border-color);
}

.footer-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: var(--spacing-md);
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
  color: var(--color-primary);
}

.divider {
  color: var(--text-tertiary);
}

.footer-links {
  display: flex;
  align-items: center;
  gap: var(--spacing-lg);
}

.footer-links a {
  color: var(--text-secondary);
  text-decoration: none;
  font-size: 0.875rem;
  transition: color var(--transition-fast);
}

.footer-links a:hover {
  color: var(--color-primary);
}

@media (max-width: 640px) {
  .footer-content {
    flex-direction: column;
    text-align: center;
  }
}
</style>
