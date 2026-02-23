<template>
  <div class="app-wrapper" :class="{ 'admin-mode': isAdminRoute }">
    <template v-if="!isAdminRoute">
      <Navbar />
      <main class="main-content">
        <div class="container">
          <router-view />
        </div>
      </main>
      <Footer />
      <BackToTop />
      <ReadingProgress />
    </template>
    <template v-else>
      <router-view />
    </template>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import Navbar from '@/components/Navbar.vue'
import Footer from '@/components/Footer.vue'
import BackToTop from '@/components/BackToTop.vue'
import ReadingProgress from '@/components/ReadingProgress.vue'

const route = useRoute()

const isAdminRoute = computed(() => {
  return route.path.startsWith('/admin')
})
</script>

<style>
.app-wrapper {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--bg-primary);
}

.app-wrapper.admin-mode {
  display: block;
}

.main-content {
  flex: 1;
  padding: var(--spacing-lg) 0;
}
</style>
