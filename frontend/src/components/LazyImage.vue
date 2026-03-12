<template>
  <div class="lazy-image-wrapper" :style="wrapperStyle">
    <img
      v-if="loaded"
      :src="src"
      :alt="alt"
      class="lazy-image"
      :class="{ 'lazy-image--loaded': showImage }"
      @load="onImageLoad"
      @error="onImageError"
    />
    <div v-else class="lazy-image-placeholder">
      <el-icon v-if="loading" class="is-loading"><Loading /></el-icon>
      <el-icon v-else-if="error"><Picture /></el-icon>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, computed } from 'vue'

const props = defineProps({
  src: { type: String, required: true },
  alt: { type: String, default: '' },
  width: { type: [String, Number], default: '100%' },
  height: { type: [String, Number], default: 'auto' },
  threshold: { type: Number, default: 0.1 },
})

const loaded = ref(false)
const showImage = ref(false)
const loading = ref(true)
const error = ref(false)

const wrapperStyle = computed(() => ({
  width: typeof props.width === 'number' ? `${props.width}px` : props.width,
  height: typeof props.height === 'number' ? `${props.height}px` : props.height,
}))

let observer = null

const onImageLoad = () => {
  loading.value = false
  showImage.value = true
}

const onImageError = () => {
  loading.value = false
  error.value = true
}

onMounted(() => {
  const wrapper = document.querySelector('.lazy-image-wrapper')
  if (!wrapper) return

  observer = new IntersectionObserver(
    (entries) => {
      entries.forEach((entry) => {
        if (entry.isIntersecting) {
          loaded.value = true
          observer?.disconnect()
        }
      })
    },
    { threshold: props.threshold }
  )

  observer.observe(wrapper)
})

onUnmounted(() => {
  observer?.disconnect()
})
</script>

<style scoped>
.lazy-image-wrapper {
  position: relative;
  overflow: hidden;
  background: var(--bg-secondary, #f5f5f5);
  border-radius: 4px;
}

.lazy-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  opacity: 0;
  transition: opacity 0.3s ease;
}

.lazy-image--loaded {
  opacity: 1;
}

.lazy-image-placeholder {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-tertiary, #999);
  font-size: 24px;
}

.is-loading {
  animation: rotate 1s linear infinite;
}

@keyframes rotate {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}
</style>
