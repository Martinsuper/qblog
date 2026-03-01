<template>
  <div class="py-6 md:py-8">
    <div class="max-w-2xl mx-auto">
      <!-- 基本信息卡片 -->
      <div class="card p-6 md:p-8 mb-4">
        <h3 class="text-xl font-semibold mb-6" style="color: var(--text-primary)">个人中心</h3>
        <el-form :model="userForm" label-width="100px">
          <el-form-item label="头像">
            <el-avatar :size="80" :src="userForm.avatar">
              {{ userForm.nickname?.charAt(0) }}
            </el-avatar>
          </el-form-item>
          <el-form-item label="用户名">
            <el-input v-model="userForm.username" disabled />
          </el-form-item>
          <el-form-item label="昵称">
            <el-input v-model="userForm.nickname" />
          </el-form-item>
          <el-form-item label="邮箱">
            <el-input v-model="userForm.email" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleUpdate">保存修改</el-button>
          </el-form-item>
        </el-form>
      </div>

      <!-- 阅读设置卡片 -->
      <div class="card p-6 md:p-8 mb-4">
        <h3 class="text-xl font-semibold mb-6" style="color: var(--text-primary)">阅读设置</h3>
        <el-form label-width="100px">
          <el-form-item label="渲染风格">
            <el-radio-group v-model="markdownTheme" @change="handleThemeChange">
              <el-radio-button label="vuepress">
                <div class="theme-option">
                  <span class="theme-name">VuePress</span>
                  <span class="theme-desc">文档风格，适合技术文章</span>
                </div>
              </el-radio-button>
              <el-radio-button label="github">
                <div class="theme-option">
                  <span class="theme-name">GitHub</span>
                  <span class="theme-desc">简洁风格，类似 GitHub README</span>
                </div>
              </el-radio-button>
            </el-radio-group>
          </el-form-item>
        </el-form>
      </div>

      <!-- 修改密码折叠区域 -->
      <el-collapse class="card">
        <el-collapse-item title="修改密码" name="password">
          <el-form
            ref="passwordFormRef"
            :model="passwordForm"
            :rules="passwordRules"
            label-width="100px"
            class="px-4 pb-4"
          >
            <el-form-item label="旧密码" prop="oldPassword">
              <el-input
                v-model="passwordForm.oldPassword"
                type="password"
                show-password
                placeholder="请输入旧密码"
              />
            </el-form-item>
            <el-form-item label="新密码" prop="newPassword">
              <el-input
                v-model="passwordForm.newPassword"
                type="password"
                show-password
                placeholder="请输入新密码（至少6位）"
              />
            </el-form-item>
            <el-form-item label="确认密码" prop="confirmPassword">
              <el-input
                v-model="passwordForm.confirmPassword"
                type="password"
                show-password
                placeholder="请再次输入新密码"
              />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleChangePassword" :loading="loading">
                确认修改
              </el-button>
              <el-button @click="resetPasswordForm">重置</el-button>
            </el-form-item>
          </el-form>
        </el-collapse-item>
      </el-collapse>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { useSettingsStore } from '@/stores/settings'
import { changePassword } from '@/api/auth'

const userStore = useUserStore()
const settingsStore = useSettingsStore()

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

// Markdown 主题
const markdownTheme = computed({
  get: () => settingsStore.markdownTheme,
  set: (value) => settingsStore.setMarkdownTheme(value)
})

// 处理主题变更
const handleThemeChange = (value) => {
  settingsStore.setMarkdownTheme(value)
  ElMessage.success(`已切换到 ${value === 'vuepress' ? 'VuePress' : 'GitHub'} 风格`)
}

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
    { min: 6, message: '密码长度至少为6位', trigger: 'blur' },
    { validator: validateNewPassword, trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ]
}

const handleUpdate = () => {
  ElMessage.info('修改功能开发中...')
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

onMounted(() => {
  if (userStore.userInfo) {
    Object.assign(userForm, userStore.userInfo)
  }
})
</script>

<style scoped>
.card {
  background: var(--bg-secondary);
  border-radius: var(--border-radius);
  box-shadow: var(--shadow-sm);
}

.max-w-2xl {
  max-width: 600px;
}

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

/* 主题选择样式 */
.theme-option {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  padding: 8px 0;
}

.theme-name {
  font-weight: 500;
  color: var(--text-primary);
}

.theme-desc {
  font-size: 12px;
  color: var(--text-tertiary);
  margin-top: 2px;
}

/* 选中状态下的文字颜色 */
:deep(.el-radio-button.is-active .theme-name),
:deep(.el-radio-button.is-active .theme-desc) {
  color: white;
}

:deep(.el-radio-group) {
  display: flex;
  width: 100%;
}

:deep(.el-radio-button) {
  flex: 1;
}

:deep(.el-radio-button__inner) {
  padding: 12px 24px;
  height: auto;
  width: 100%;
}
</style>