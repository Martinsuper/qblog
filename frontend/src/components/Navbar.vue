<template>
  <header class="navbar">
    <div class="container">
      <div class="navbar-content">
        <router-link to="/" class="navbar-brand">
          <span class="brand-text">QBlog</span>
        </router-link>

        <nav class="navbar-menu">
          <router-link to="/" class="nav-link" exact-active-class="active">首页</router-link>
          <router-link to="/archives" class="nav-link" active-class="active">时间轴</router-link>
          <router-link to="/categories" class="nav-link" active-class="active">分类</router-link>
          <router-link to="/tags" class="nav-link" active-class="active">标签</router-link>
        </nav>

        <div class="navbar-right">
          <button class="theme-toggle" @click="toggleTheme" title="切换主题">
            <el-icon v-if="isDark"><Sunny /></el-icon>
            <el-icon v-else><Moon /></el-icon>
          </button>

          <template v-if="isLoggedIn">
            <el-dropdown trigger="click" @command="handleCommand">
              <span class="user-dropdown">
                <el-avatar :size="32" :src="userInfo?.avatar">
                  {{ userInfo?.nickname?.charAt(0) || 'U' }}
                </el-avatar>
                <span class="user-name">{{ userInfo?.nickname || '用户' }}</span>
                <el-icon><ArrowDown /></el-icon>
              </span>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="profile">
                    <el-icon><User /></el-icon>
                    个人中心
                  </el-dropdown-item>
                  <el-dropdown-item v-if="isAdmin" command="admin" divided>
                    <el-icon><Setting /></el-icon>
                    管理后台
                  </el-dropdown-item>
                  <el-dropdown-item command="logout" divided>
                    <el-icon><SwitchButton /></el-icon>
                    退出登录
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
          <template v-else>
            <router-link to="/login" class="nav-link">登录</router-link>
            <router-link to="/register" class="btn-register">注册</router-link>
          </template>
        </div>
      </div>
    </div>
  </header>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'

const router = useRouter()

const isLoggedIn = ref(false)
const userInfo = ref(null)
const isDark = ref(false)

const isAdmin = computed(() => userInfo.value?.role === 1)

const checkAuth = () => {
  const token = localStorage.getItem('token')
  const info = localStorage.getItem('userInfo')
  isLoggedIn.value = !!token
  if (info) {
    try {
      userInfo.value = JSON.parse(info)
    } catch (e) {
      userInfo.value = null
    }
  } else if (token) {
    try {
      const payload = JSON.parse(atob(token.split('.')[1]))
      userInfo.value = {
        username: payload.username,
        nickname: payload.username
      }
      localStorage.setItem('userInfo', JSON.stringify(userInfo.value))
    } catch (e) {
      console.error('解析 token 失败:', e)
    }
  }
}

const handleStorageChange = (e) => {
  if (e.key === 'token' || e.key === 'userInfo') {
    checkAuth()
  }
}

const toggleTheme = () => {
  isDark.value = !isDark.value
  document.documentElement.setAttribute('data-theme', isDark.value ? 'dark' : 'light')
  localStorage.setItem('theme', isDark.value ? 'dark' : 'light')
}

const handleCommand = (command) => {
  switch (command) {
    case 'profile':
      router.push('/user/profile')
      break
    case 'admin':
      router.push('/admin/dashboard')
      break
    case 'logout':
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
      isLoggedIn.value = false
      userInfo.value = null
      ElMessage.success('已退出登录')
      router.push('/')
      break
  }
}

onMounted(() => {
  checkAuth()
  window.addEventListener('storage', handleStorageChange)
  
  const savedTheme = localStorage.getItem('theme')
  if (savedTheme === 'dark') {
    isDark.value = true
    document.documentElement.setAttribute('data-theme', 'dark')
  }
})
</script>

<style scoped>
/* Navbar Container with Glassmorphism */
.navbar {
  position: sticky;
  top: 0;
  z-index: 100;
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-bottom: 1px solid var(--border-color);
  box-shadow: var(--shadow-sm);
  transition: all var(--transition-normal);
}

.navbar[data-theme="dark"] {
  background: rgba(31, 41, 55, 0.8);
}

.navbar-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 60px;
}

.navbar-brand {
  display: flex;
  align-items: center;
  text-decoration: none;
  font-size: 1.25rem;
  font-weight: 600;
  color: var(--color-primary);
}

.brand-text {
  background: linear-gradient(135deg, var(--color-primary) 0%, var(--color-primary-light) 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.navbar-menu {
  display: flex;
  align-items: center;
  gap: var(--spacing-lg);
}

.nav-link {
  text-decoration: none;
  color: var(--text-secondary);
  font-size: 0.9375rem;
  transition: color var(--transition-fast);
  padding: var(--spacing-xs) var(--spacing-sm);
  border-radius: var(--border-radius-sm);
  position: relative;
}

.nav-link::after {
  content: '';
  position: absolute;
  bottom: 2px;
  left: 0;
  width: 0;
  height: 2px;
  background: var(--color-primary);
  transition: width var(--transition-fast);
}

.nav-link:hover {
  color: var(--color-primary);
}

.nav-link:hover::after {
  width: 100%;
}

.nav-link.active {
  color: var(--color-primary);
  font-weight: 500;
}

.navbar-right {
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
}

.theme-toggle {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border: none;
  background: transparent;
  border-radius: var(--border-radius);
  cursor: pointer;
  color: var(--text-secondary);
  transition: all var(--transition-fast);
  transform: rotate(0deg);
  transition: all var(--transition-fast), transform var(--transition-fast);
}

.theme-toggle:hover {
  background: var(--bg-tertiary);
  color: var(--color-primary);
  transform: rotate(360deg);
  transition: all var(--transition-fast);
}

.user-dropdown {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  cursor: pointer;
  padding: var(--spacing-xs);
  border-radius: var(--border-radius);
  transition: background var(--transition-fast);
}

.user-dropdown:hover {
  background: var(--bg-tertiary);
}

.user-name {
  font-size: 0.875rem;
  color: var(--text-primary);
  max-width: 100px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.btn-register {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: var(--spacing-sm) var(--spacing-md);
  background: var(--color-primary-gradient);
  color: white;
  text-decoration: none;
  font-size: 0.875rem;
  font-weight: 500;
  border-radius: var(--border-radius);
  transition: all var(--transition-fast);
  border: none;
}

.btn-register:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
}

@media (max-width: 640px) {
  .navbar-menu {
    display: none;
  }

  .user-name {
    display: none;
  }
}
</style>
