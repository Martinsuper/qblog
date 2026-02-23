<template>
  <div
    class="fixed top-0 left-0 right-0 h-1 z-50 transition-opacity duration-300"
    :style="{ opacity: progress > 0 ? 1 : 0 }"
  >
    <div
      class="h-full transition-all duration-100"
      :style="{
        width: `${progress}%`,
        backgroundColor: 'var(--color-primary)'
      }"
    />
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'

const progress = ref(0)

const updateProgress = () => {
  const scrollTop = window.scrollY
  const docHeight = document.documentElement.scrollHeight - window.innerHeight
  progress.value = docHeight > 0 ? Math.min(100, (scrollTop / docHeight) * 100) : 0
}

onMounted(() => {
  window.addEventListener('scroll', updateProgress, { passive: true })
  updateProgress()
})

onUnmounted(() => {
  window.removeEventListener('scroll', updateProgress)
})
</script>
