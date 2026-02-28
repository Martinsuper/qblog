<template>
  <div class="login-container">
    <div class="login-card animate-fade-in-up">
      <h2 class="login-title">用户登录</h2>

      <el-form ref="formRef" :model="loginForm" :rules="rules" class="login-form">
        <el-form-item prop="username">
          <el-input
            v-model="loginForm.username"
            placeholder="用户名"
            clearable
            size="large"
          >
            <template #prefix>
              <el-icon><User /></el-icon>
            </template>
          </el-input>
        </el-form-item>

        <el-form-item prop="password">
          <el-input
            v-model="loginForm.password"
            type="password"
            placeholder="密码"
            show-password
            size="large"
            @keyup.enter="handleLogin"
          >
            <template #prefix>
              <el-icon><Lock /></el-icon>
            </template>
          </el-input>
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            :loading="loading"
            class="login-btn"
            size="large"
            @click="handleLogin"
          >
            登 录
          </el-button>
        </el-form-item>

        <div class="form-footer">
          <el-checkbox v-model="loginForm.remember" label="记住我" />
          <router-link to="/register" class="register-link">注册账号</router-link>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
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
.login-container {
  min-height: calc(100vh - 140px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: var(--spacing-lg);
  background: var(--bg-primary);
}

.login-card {
  width: 100%;
  max-width: 360px;
  padding: 40px;
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-radius: var(--border-radius-lg);
  border: 1px solid var(--border-color);
  box-shadow: var(--shadow-md);
}

:global([data-theme="dark"]) .login-card {
  background: rgba(31, 41, 55, 0.8);
}

.login-title {
  text-align: center;
  font-size: 1.5rem;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0 0 32px;
}

.login-form :deep(.el-form-item) {
  margin-bottom: 16px;
}

.login-form :deep(.el-input__wrapper) {
  border-radius: var(--border-radius);
  background: var(--bg-secondary);
}

.login-form :deep(.el-input__prefix) {
  color: var(--text-tertiary);
}

.login-btn {
  width: 100%;
  height: 44px;
  font-size: 0.95rem;
  font-weight: 500;
  border-radius: var(--border-radius);
}

.form-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 8px;
}

.form-footer :deep(.el-checkbox__label) {
  color: var(--text-secondary);
  font-size: 0.875rem;
}

.register-link {
  color: var(--color-primary);
  text-decoration: none;
  font-size: 0.875rem;
  transition: color var(--transition-fast);
}

.register-link:hover {
  color: var(--color-primary-dark);
}

@keyframes slideInUp {
  from {
    transform: translateY(20px);
    opacity: 0;
  }
  to {
    transform: translateY(0);
    opacity: 1;
  }
}

.animate-fade-in-up {
  animation: slideInUp 0.4s ease-out;
}

@media (max-width: 480px) {
  .login-card {
    padding: 32px 24px;
  }
}
</style>