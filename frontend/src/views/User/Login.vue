<template>
  <div class="min-h-[calc(100vh-140px)] flex items-center justify-center p-4 bg-gradient-custom">
    <div class="login-card animate-fade-in-up w-full max-w-md p-8 md:p-10 bg-glass backdrop-blur-md rounded-xl shadow-xl border border-glass-stroke">
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
          <el-button type="primary" :loading="loading" class="login-btn" @click="handleLogin">
            登录
          </el-button>
        </el-form-item>
        <div class="text-center mt-4">
          <router-link to="/register" class="text-sm register-link hover-underline">
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
.bg-gradient-custom {
  background:
    linear-gradient(135deg, #667eea 0%, #764ba2 100%),
    radial-gradient(circle at 20% 30%, rgba(255, 255, 255, 0.15) 0%, transparent 40%),
    radial-gradient(circle at 80% 70%, rgba(255, 255, 255, 0.1) 0%, transparent 40%);
  min-height: calc(100vh - 140px);
  position: relative;
  overflow: hidden;
}
.border-glass-stroke {
  border: 1px solid rgba(255, 255, 255, 0.2);
}

.bg-glass {
  background: rgba(255, 255, 255, 0.95);
}

.login-card {
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-radius: var(--border-radius-lg);
  box-shadow: var(--shadow-xl);
  transition: all var(--transition-normal);
}

.login-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-xl);
  border-color: rgba(255, 255, 255, 0.3);
}

.login-btn {
  width: 100%;
  background: var(--color-primary-gradient);
  border: none;
  border-radius: var(--border-radius);
  color: white;
  padding: var(--spacing-md) var(--spacing-lg);
  font-weight: 500;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.login-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
}

.register-link {
  color: var(--color-primary);
  text-decoration: none;
  transition: color var(--transition-normal);
  display: inline-block;
}

.register-link:hover {
  color: var(--color-primary-light);
  transform: translateX(2px);
}

@keyframes slideInUp {
  0% {
    transform: translateY(30px);
    opacity: 0;
  }
  100% {
    transform: translateY(0);
    opacity: 1;
  }
}

.animate-fade-in-up {
  animation: slideInUp 0.6s cubic-bezier(0.25, 0.46, 0.45, 0.94);
}
</style>
