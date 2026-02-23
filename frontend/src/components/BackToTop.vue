<template>
  <Transition name="fade">
    <button
      v-show="visible"
      class="fixed bottom-6 right-6 w-10 h-10 rounded-full shadow-lg flex items-center justify-center transition-all duration-200 hover:shadow-xl hover:scale-110 z-40"
      :style="{
        backgroundColor: 'var(--bg-secondary)',
        color: 'var(--text-primary)',
        border: '1px solid var(--border-color)'
      }"
      @click="scrollToTop"
    >
      <el-icon :size="20">
        <ArrowUp />
      </el-icon>
    </button>
  </Transition>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'

const visible = ref(false)

const checkVisibility = () => {
  visible.value = window.scrollY > 200
}

const scrollToTop = () => {
  window.scrollTo({
    top: 0,
    behavior: 'smooth'
  })
}

onMounted(() => {
  window.addEventListener('scroll', checkVisibility, { passive: true })
  checkVisibility()
})

onUnmounted(() => {
  window.removeEventListener('scroll', checkVisibility)
})
</script>

<style scoped>
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
