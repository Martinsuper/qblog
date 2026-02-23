<template>
  <div class="py-6 md:py-8">
    <div class="max-w-2xl mx-auto">
      <div class="card p-6 md:p-8">
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
    </div>
  </div>
</template>

<script setup>
import { reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

const userForm = reactive({
  username: '',
  nickname: '',
  email: '',
  avatar: ''
})

const handleUpdate = () => {
  ElMessage.info('修改功能开发中...')
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
</style>
