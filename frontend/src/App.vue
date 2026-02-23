<template>
  <div class="app-wrapper">
    <Navbar />
    <main class="main-content">
      <div class="container">
        <router-view />
      </div>
    </main>
    <Footer />
    <BackToTop />
    <ReadingProgress />
  </div>
</template>

<script setup>
import Navbar from '@/components/Navbar.vue'
import Footer from '@/components/Footer.vue'
import BackToTop from '@/components/BackToTop.vue'
import ReadingProgress from '@/components/ReadingProgress.vue'
</script>

<style>
.app-wrapper {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--bg-primary);
}

.main-content {
  flex: 1;
  padding: var(--spacing-lg) 0;
}
</style>
