<template>
  <div class="min-h-[calc(100vh-140px)] flex items-center justify-center p-4 register-container">
    <div class="w-full max-w-md p-8 md:p-10 rounded-xl shadow-2xl register-card" ref="cardRef">
      <h2 class="text-2xl font-bold text-center mb-8 text-white">
        用户注册
      </h2>
      <el-form ref="formRef" :model="registerForm" :rules="rules" label-width="80px" class="relative z-10">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="registerForm.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="registerForm.password" type="password" placeholder="请输入密码" />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="registerForm.confirmPassword" type="password" placeholder="请确认密码" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="registerForm.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" class="w-full register-btn" @click="handleRegister">
            注册
          </el-button>
        </el-form-item>
        <div class="text-center mt-4 login-link-container">
          <router-link to="/login" class="text-sm register-login-link">
            已有账号？立即登录
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
const cardRef = ref()
const loading = ref(false)

const registerForm = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  email: ''
})

const validateConfirmPassword = (rule, value, callback) => {
  if (value !== registerForm.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度在 3-20 之间', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度至少 6 位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ],
  email: [
    { type: 'email', message: '请输入正确的邮箱', trigger: 'blur' }
  ]
}

const handleRegister = async () => {
  await formRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        await userStore.userRegister(registerForm)
        ElMessage.success('注册成功，请登录')
        router.push('/login')
      } catch (error) {
        console.error('注册失败:', error)
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

.register-container {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%),
            radial-gradient(circle at top right, rgba(255, 255, 255, 0.2) 0%, transparent 50%),
            radial-gradient(circle at bottom left, rgba(255, 255, 255, 0.2) 0%, transparent 50%);
  min-height: calc(100vh - 140px);
  position: relative;
  overflow: hidden;
}

.register-card {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-radius: var(--border-radius-lg, 1rem);
  box-shadow: var(--shadow-xl, 0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04));
  transition: all var(--transition-normal, 0.3s ease);
  z-index: 1;
}

.register-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-xl, 0 25px 50px -12px rgba(0, 0, 0, 0.25));
}

.register-btn {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  border-radius: var(--border-radius-md, 0.5rem);
  transition: all var(--transition-fast, 0.2s ease);
  padding: 12px 20px;
}

.register-btn:hover:not(.is-loading) {
  transform: translateY(-2px);
  box-shadow: 0 10px 20px rgba(102, 126, 234, 0.3);
}

.register-login-link {
  color: var(--color-primary, #667eea);
  transition: all var(--transition-fast, 0.2s ease);
}

.register-login-link:hover {
  color: var(--color-primary-light, #764ba2);
}

.el-form-item {
  margin-bottom: var(--spacing-md, 1.25rem);
}
</style>
