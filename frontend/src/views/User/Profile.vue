<template>
  <SettingsLayout
    title="账号与密码"
    description="查看账号信息，管理登录密码。"
  >
    <div class="profile-content">
      <!-- 基本信息 -->
      <div class="card profile-card">
        <h1
          class="text-xl font-semibold mb-6"
          style="color: var(--text-primary)"
        >
          账号信息
        </h1>
        <el-form
          :model="userForm"
          label-width="100px"
        >
          <el-form-item label="头像">
            <el-avatar
              :size="48"
              :src="userForm.avatar"
            >
              {{ userForm.nickname?.charAt(0) }}
            </el-avatar>
          </el-form-item>
          <el-form-item label="用户名">
            <el-input
              v-model="userForm.username"
              disabled
            />
          </el-form-item>
          <el-form-item label="昵称">
            <el-input
              v-model="userForm.nickname"
              disabled
            />
          </el-form-item>
          <el-form-item label="邮箱">
            <el-input
              v-model="userForm.email"
              disabled
            />
          </el-form-item>
        </el-form>
      </div>

      <p class="profile-note">
        账号资料暂不支持在线修改。阅读风格已移至<router-link to="/settings/appearance">
          外观与布局
        </router-link>。
      </p>

      <!-- 修改密码折叠区域 -->
      <el-collapse class="card">
        <el-collapse-item
          title="修改密码"
          name="password"
        >
          <el-form
            ref="passwordFormRef"
            :model="passwordForm"
            :rules="passwordRules"
            label-width="100px"
            class="px-4 pb-4"
          >
            <el-form-item
              label="旧密码"
              prop="oldPassword"
            >
              <el-input
                v-model="passwordForm.oldPassword"
                type="password"
                show-password
                placeholder="请输入旧密码"
              />
            </el-form-item>
            <el-form-item
              label="新密码"
              prop="newPassword"
            >
              <el-input
                v-model="passwordForm.newPassword"
                type="password"
                show-password
                placeholder="请输入新密码（8-20位，含字母和数字）"
              />
            </el-form-item>
            <el-form-item
              label="确认密码"
              prop="confirmPassword"
            >
              <el-input
                v-model="passwordForm.confirmPassword"
                type="password"
                show-password
                placeholder="请再次输入新密码"
              />
            </el-form-item>
            <el-form-item>
              <el-button
                type="primary"
                :loading="loading"
                @click="handleChangePassword"
              >
                确认修改
              </el-button>
              <el-button @click="resetPasswordForm">
                重置
              </el-button>
            </el-form-item>
          </el-form>
        </el-collapse-item>
      </el-collapse>
    </div>
  </SettingsLayout>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import SettingsLayout from '@/components/SettingsLayout.vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { changePassword } from '@/api/auth'

const userStore = useUserStore()

const userForm = reactive({
  username: '',
  nickname: '',
  email: '',
  avatar: ''
})

const passwordFormRef = ref()
const loading = ref(false)

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

// 验证新密码与确认密码一致
const validateConfirmPassword = (rule, value, callback) => {
  if (value !== passwordForm.newPassword) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

// 验证新旧密码不同
const validateNewPassword = (rule, value, callback) => {
  if (value === passwordForm.oldPassword) {
    callback(new Error('新密码不能与旧密码相同'))
  } else {
    callback()
  }
}

const passwordRules = {
  oldPassword: [
    { required: true, message: '请输入旧密码', trigger: 'blur' }
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 8, max: 20, message: '密码长度在 8-20 之间', trigger: 'blur' },
    {
      pattern: /^(?=.*[A-Za-z])(?=.*\d).+$/,
      message: '密码必须同时包含字母和数字',
      trigger: 'blur'
    },
    { validator: validateNewPassword, trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ]
}

const handleChangePassword = async () => {
  const valid = await passwordFormRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await changePassword({
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword
    })
    ElMessage.success('密码修改成功')
    resetPasswordForm()
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '密码修改失败')
  } finally {
    loading.value = false
  }
}

const resetPasswordForm = () => {
  passwordFormRef.value?.resetFields()
}

onMounted(async () => {
  if (userStore.userInfo) Object.assign(userForm, userStore.userInfo)
  try { await userStore.fetchUserInfo(); Object.assign(userForm, userStore.userInfo) } catch { /* Request layer reports errors; keep cached account details. */ }
})
</script>

<style scoped>
.card {
  background: var(--bg-secondary);
  border-radius: var(--border-radius);
  box-shadow: var(--shadow-sm);
}

.profile-content { max-width:640px; }
.profile-card { padding:20px; border:1px solid var(--border-color); box-shadow:none; }
.profile-card h1 { font-size:16px; margin-bottom:20px; }
.profile-note { margin:12px 0 20px; font-size:12px; color:var(--text-secondary); line-height:1.7; }
.profile-note a { color:var(--color-primary); }
@media(max-width:640px) { .profile-card { padding:16px; } }

:deep(.el-collapse-item__header) {
  background: transparent;
  font-size: 16px;
  font-weight: 500;
  color: var(--text-primary);
  padding: 16px 20px;
  border: none;
}

:deep(.el-collapse-item__wrap) {
  background: transparent;
  border: none;
}

:deep(.el-collapse-item__content) {
  padding: 0;
}

/* 主题单选样式 */
.theme-radio-group {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

:deep(.theme-radio-group .el-radio) {
  display: flex;
  align-items: flex-start;
  margin-right: 0;
  height: auto;
}

:deep(.theme-radio-group .el-radio__input) {
  margin-top: 3px;
}

:deep(.theme-radio-group .el-radio__label) {
  display: flex;
  flex-direction: column;
  padding-left: 8px;
}

.theme-name {
  font-weight: 500;
  color: var(--text-primary);
  font-size: 14px;
  line-height: 1.4;
}

.theme-desc {
  font-size: 12px;
  color: var(--text-tertiary);
  line-height: 1.4;
}
</style>