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

// rAF 合并连续 scroll 事件：每帧最多计算一次，避免高频滚动下重复布局读
let ticking = false
const updateProgress = () => {
  ticking = false
  const scrollTop = window.scrollY
  const docHeight = document.documentElement.scrollHeight - window.innerHeight
  progress.value = docHeight > 0 ? Math.min(100, (scrollTop / docHeight) * 100) : 0
}
const onScroll = () => {
  if (ticking) return
  ticking = true
  requestAnimationFrame(updateProgress)
}

onMounted(() => {
  window.addEventListener('scroll', onScroll, { passive: true })
  updateProgress()
})

onUnmounted(() => {
  window.removeEventListener('scroll', onScroll)
})
</script>
