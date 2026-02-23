<template>
  <div class="min-h-[calc(100vh-140px)] flex items-center justify-center p-4" style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%)">
    <div class="w-full max-w-md p-8 md:p-10 rounded-xl shadow-2xl" style="background: var(--bg-secondary)">
      <h2 class="text-2xl font-bold text-center mb-8" style="color: var(--text-primary)">
        用户登录
      </h2>
      <el-form ref="formRef" :model="loginForm" :rules="rules" label-width="80px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="loginForm.username" placeholder="请输入用户名" clearable />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input
            v-model="loginForm.password"
            type="password"
            placeholder="请输入密码"
            show-password
            @keyup.enter="handleLogin"
          />
        </el-form-item>
        <el-form-item>
          <el-checkbox v-model="loginForm.remember">记住我</el-checkbox>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" class="w-full" @click="handleLogin">
            登录
          </el-button>
        </el-form-item>
        <div class="text-center mt-4">
          <router-link to="/register" class="text-sm" style="color: var(--color-primary)">
            还没有账号？立即注册
          </router-link>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref()
const loading = ref(false)

const loginForm = reactive({
  username: '',
  password: '',
  remember: false
})

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度至少 6 位', trigger: 'blur' }
  ]
}

const handleLogin = async () => {
  await formRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        await userStore.userLogin(loginForm)
        ElMessage.success('登录成功')
        // 强制刷新页面以确保状态更新
        window.location.href = '/'
      } catch (error) {
        console.error('登录失败:', error)
      } finally {
        loading.value = false
      }
    }
  })
}
</script>

<style scoped>
.w-full {
  width: 100%;
}
</style>
