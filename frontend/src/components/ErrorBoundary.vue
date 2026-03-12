<template>
  <slot v-if="!hasError" />
  <div v-else class="error-boundary">
    <el-result icon="warning" title="出错了" :sub-title="errorMessage">
      <template #extra>
        <el-button type="primary" @click="retry">重试</el-button>
      </template>
    </el-result>
  </div>
</template>

<script setup>
import { ref, onErrorCaptured } from 'vue'

const hasError = ref(false)
const errorMessage = ref('')

const emit = defineEmits(['error'])

onErrorCaptured((err, instance, info) => {
  hasError.value = true
  errorMessage.value = err.message || '组件渲染出错'
  emit('error', err)
  // 阻止错误继续向上传播
  return false
})

const retry = () => {
  hasError.value = false
  errorMessage.value = ''
}
</script>

<style scoped>
.error-boundary {
  padding: 40px 20px;
  text-align: center;
}
</style>
