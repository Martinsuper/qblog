<template>
  <header class="navbar glass">
    <div class="container">
      <div class="navbar-content">
        <router-link
          to="/"
          class="navbar-brand"
        >
          <span class="brand-text">QBlog</span>
        </router-link>

        <nav
          class="navbar-menu"
          aria-label="主导航"
        >
          <router-link
            to="/"
            class="nav-link"
            exact-active-class="active"
          >
            首页
          </router-link>
          <router-link
            to="/archives"
            class="nav-link"
            active-class="active"
          >
            时间轴
          </router-link>
          <router-link
            to="/categories"
            class="nav-link"
            active-class="active"
          >
            分类
          </router-link>
          <router-link
            to="/tags"
            class="nav-link"
            active-class="active"
          >
            标签
          </router-link>
        </nav>

        <div class="navbar-right">
          <button
            class="icon-btn"
            :aria-label="isDark ? '切换到亮色模式' : '切换到暗色模式'"
            @click="toggleTheme"
          >
            <el-icon v-if="isDark">
              <Sunny />
            </el-icon>
            <el-icon v-else>
              <Moon />
            </el-icon>
          </button>

          <template v-if="userStore.isLoggedIn">
            <el-dropdown
              trigger="click"
              @command="handleCommand"
            >
              <span class="user-dropdown">
                <el-avatar
                  :size="32"
                  :src="userStore.userInfo?.avatar"
                >
                  {{ userStore.userInfo?.nickname?.charAt(0) || 'U' }}
                </el-avatar>
                <span class="user-name">{{ userStore.userInfo?.nickname || '用户' }}</span>
                <el-icon><ArrowDown /></el-icon>
              </span>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="profile">
                    <el-icon><User /></el-icon>
                    个人中心
                  </el-dropdown-item>
                  <el-dropdown-item
                    v-if="userStore.isAdmin"
                    command="write"
                  >
                    <el-icon><EditPen /></el-icon>
                    写文章
                  </el-dropdown-item>
                  <el-dropdown-item
                    v-if="userStore.isAdmin"
                    command="admin"
                    divided
                  >
                    <el-icon><Setting /></el-icon>
                    管理后台
                  </el-dropdown-item>
                  <el-dropdown-item
                    command="logout"
                    divided
                  >
                    <el-icon><SwitchButton /></el-icon>
                    退出登录
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
          <template v-else>
            <router-link
              to="/login"
              class="nav-link"
            >
              登录
            </router-link>
          </template>

          <!-- 移动端汉堡菜单 -->
          <button
            class="icon-btn menu-toggle"
            :class="{ open: menuVisible }"
            :aria-expanded="menuVisible"
            aria-label="打开导航菜单"
            @click="menuVisible = true"
          >
            <el-icon :size="20">
              <Menu />
            </el-icon>
          </button>
        </div>
      </div>
    </div>

    <!-- 移动端抽屉导航 -->
    <el-drawer
      v-model="menuVisible"
      direction="rtl"
      size="72%"
      :with-header="false"
      class="mobile-nav-drawer"
    >
      <nav
        class="mobile-nav"
        aria-label="移动端导航"
      >
        <router-link
          to="/"
          class="mobile-link"
          @click="menuVisible = false"
        >
          首页
        </router-link>
        <router-link
          to="/archives"
          class="mobile-link"
          @click="menuVisible = false"
        >
          时间轴
        </router-link>
        <router-link
          to="/categories"
          class="mobile-link"
          @click="menuVisible = false"
        >
          分类
        </router-link>
        <router-link
          to="/tags"
          class="mobile-link"
          @click="menuVisible = false"
        >
          标签
        </router-link>

        <template v-if="!userStore.isLoggedIn">
          <div class="mobile-auth">
            <router-link
              to="/login"
              class="mobile-link"
              @click="menuVisible = false"
            >
              登录
            </router-link>
          </div>
        </template>
        <template v-else>
          <div class="mobile-auth">
            <router-link
              to="/user/profile"
              class="mobile-link"
              @click="menuVisible = false"
            >
              个人中心
            </router-link>
            <router-link
              v-if="userStore.isAdmin"
              to="/admin/create"
              class="mobile-link"
              @click="menuVisible = false"
            >
              写文章
            </router-link>
            <router-link
              v-if="userStore.isAdmin"
              to="/admin/articles"
              class="mobile-link"
              @click="menuVisible = false"
            >
              管理后台
            </router-link>
            <a
              href="javascript:void(0)"
              class="mobile-link logout"
              @click="handleLogout"
            >退出登录</a>
          </div>
        </template>
      </nav>
    </el-drawer>
  </header>
</template>

<script setup>
import { ref, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Sunny, Moon, ArrowDown, User, EditPen, Setting, SwitchButton, Menu } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { useTheme } from '@/composables/useTheme'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const { isDark, toggleTheme } = useTheme()

const menuVisible = ref(false)

// 路由变化时关闭抽屉（覆盖浏览器返回键的场景）
watch(() => route.path, () => {
  menuVisible.value = false
})

const handleCommand = (command) => {
  switch (command) {
    case 'profile':
      router.push('/user/profile')
      break
    case 'write':
      router.push('/admin/create')
      break
    case 'admin':
      router.push('/admin/articles')
      break
    case 'logout':
      handleLogout()
      break
  }
}

const handleLogout = () => {
  userStore.userLogout()
  menuVisible.value = false
  ElMessage.success('已退出登录')
  router.push('/')
}
</script>

<style scoped>
.navbar {
  position: sticky;
  top: 0;
  z-index: 100;
  border-bottom: 1px solid var(--border-color);
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.03);
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
  background: var(--color-primary-gradient);
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

.icon-btn {
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
  transition: background var(--transition-fast), color var(--transition-fast);
}

.icon-btn:hover {
  background: var(--bg-tertiary);
  color: var(--color-primary);
}

.icon-btn:focus-visible {
  outline: 2px solid var(--color-primary);
  outline-offset: 2px;
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

.menu-toggle {
  display: none;
}

/* 移动端抽屉内部 */
.mobile-nav {
  display: flex;
  flex-direction: column;
  padding: var(--spacing-md) var(--spacing-sm);
  gap: var(--spacing-xs);
}

.mobile-link {
  padding: var(--spacing-sm) var(--spacing-md);
  border-radius: var(--border-radius);
  color: var(--text-primary);
  text-decoration: none;
  font-size: 1rem;
  transition: background var(--transition-fast), color var(--transition-fast);
}

.mobile-link:active,
.mobile-link.router-link-active {
  background: var(--bg-hover);
  color: var(--color-primary);
  font-weight: 500;
}

.mobile-auth {
  margin-top: var(--spacing-md);
  padding-top: var(--spacing-md);
  border-top: 1px solid var(--border-color);
  display: flex;
  flex-direction: column;
  gap: var(--spacing-xs);
}

.mobile-link.logout {
  color: var(--text-secondary);
}

@media (max-width: 768px) {
  .navbar-menu {
    display: none;
  }

  .user-name {
    display: none;
  }

  .navbar-right .nav-link {
    display: none;
  }

  .menu-toggle {
    display: flex;
  }
}
</style>
