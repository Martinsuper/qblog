<template>
  <div class="admin-layout">
    <!-- 侧边栏 -->
    <aside class="admin-sidebar" :class="{ collapsed: sidebarCollapsed }">
      <div class="sidebar-header">
        <h1 v-show="!sidebarCollapsed" class="logo">博客管理后台</h1>
        <h1 v-show="sidebarCollapsed" class="logo-short">管理</h1>
      </div>
      
      <el-menu
        :default-active="activeMenu"
        class="sidebar-menu"
        :collapse="sidebarCollapsed"
        router
        @select="handleMenuSelect"
      >
        <el-menu-item index="/admin/dashboard" route="/admin/dashboard">
          <el-icon><Document /></el-icon>
          <template #title>控制台</template>
        </el-menu-item>

        <el-menu-item index="/admin/articles" route="/admin/articles">
          <el-icon><Document /></el-icon>
          <template #title>文章管理</template>
        </el-menu-item>

        <el-menu-item index="/admin/create" route="/admin/create">
          <el-icon><Edit /></el-icon>
          <template #title>写文章</template>
        </el-menu-item>
      </el-menu>
      
      <div class="sidebar-footer">
        <el-button link @click="goHome" class="footer-btn">
          <el-icon><HomeFilled /></el-icon>
          <span v-show="!sidebarCollapsed">返回首页</span>
        </el-button>
      </div>
    </aside>

    <!-- 主内容区 -->
    <div class="admin-main" :class="{ collapsed: sidebarCollapsed }">
      <!-- 顶部导航栏 -->
      <header class="admin-header">
        <div class="header-left">
          <el-button link class="collapse-btn" @click="toggleSidebar">
            <el-icon>
              <ArrowLeft v-if="!sidebarCollapsed" />
              <ArrowRight v-else />
            </el-icon>
          </el-button>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item>首页</el-breadcrumb-item>
            <el-breadcrumb-item v-if="breadcrumb">{{ breadcrumb }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        
        <div class="header-right">
          <el-dropdown @command="handleCommand">
            <span class="user-info">
              <el-avatar :size="32" :src="userAvatar" />
              <span class="username">{{ userName }}</span>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <!-- 内容区域 -->
      <main class="admin-content">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ArrowLeft,
  ArrowRight,
  Document,
  Edit,
  HomeFilled
} from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()

const sidebarCollapsed = ref(false)
const userAvatar = ref('')
const userName = ref('管理员')

const activeMenu = computed(() => route.path)

const breadcrumb = computed(() => {
  const menuMap = {
    '/admin/dashboard': '控制台',
    '/admin/articles': '文章管理',
    '/admin/create': '写文章'
  }
  return menuMap[route.path] || ''
})

const toggleSidebar = () => {
  sidebarCollapsed.value = !sidebarCollapsed.value
}

const handleMenuSelect = (index) => {
  router.push(index)
}

const goHome = () => {
  router.push('/')
}

const handleCommand = async (command) => {
  if (command === 'logout') {
    try {
      await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      })
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
      ElMessage.success('已退出登录')
      router.push('/login')
    } catch {
      // 取消退出
    }
  } else if (command === 'profile') {
    router.push('/user/profile')
  }
}
</script>

<style lang="scss" scoped>
.admin-layout {
  display: flex;
  height: 100vh;
  overflow: hidden;
  background: var(--bg-primary);
}

// 侧边栏
.admin-sidebar {
  width: 220px;
  background: var(--bg-secondary);
  display: flex;
  flex-direction: column;
  transition: width 0.3s ease;
  border-right: 1px solid var(--border-color);

  &.collapsed {
    width: 64px;
  }

  .sidebar-header {
    height: 60px;
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 0 16px;
    border-bottom: 1px solid var(--border-color);

    .logo,
    .logo-short {
      color: var(--color-primary);
      font-size: 16px;
      font-weight: 600;
      margin: 0;
      white-space: nowrap;
    }

    .logo-short {
      font-size: 14px;
    }
  }

  .sidebar-menu {
    flex: 1;
    overflow-y: auto;
    border-right: none;
    padding: 8px;
    background: transparent;

    &::-webkit-scrollbar {
      width: 6px;
    }

    &::-webkit-scrollbar-track {
      background: transparent;
    }

    &::-webkit-scrollbar-thumb {
      background: var(--border-color);
      border-radius: 3px;
    }
  }

  .sidebar-footer {
    padding: 12px;
    border-top: 1px solid var(--border-color);

    .footer-btn {
      width: 100%;
      color: var(--text-secondary);
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 8px;
      padding: 8px 12px;
      border-radius: var(--border-radius);
      transition: all var(--transition-fast);

      &:hover {
        color: var(--color-primary);
        background: var(--bg-tertiary);
      }

      .el-icon {
        font-size: 16px;
      }
    }
  }
}

// 主内容区
.admin-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: var(--bg-primary);
}

// 顶部导航栏
.admin-header {
  height: 60px;
  background: var(--bg-secondary);
  box-shadow: var(--shadow-sm);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  z-index: 10;
  border-bottom: 1px solid var(--border-color);

  .header-left {
    display: flex;
    align-items: center;
    gap: 16px;

    .collapse-btn {
      width: 36px;
      height: 36px;
      display: flex;
      align-items: center;
      justify-content: center;
      border-radius: var(--border-radius);
      font-size: 18px;
      color: var(--text-secondary);
      transition: all var(--transition-fast);

      &:hover {
        background: var(--bg-tertiary);
        color: var(--color-primary);
      }
    }

    :deep(.el-breadcrumb) {
      .el-breadcrumb__item {
        font-size: 14px;

        .el-breadcrumb__inner {
          color: var(--text-secondary);
          font-weight: 500;
        }

        &:last-child .el-breadcrumb__inner {
          color: var(--text-primary);
          font-weight: 600;
        }

        .el-breadcrumb__separator {
          color: var(--text-tertiary);
        }
      }
    }
  }

  .header-right {
    .user-info {
      display: flex;
      align-items: center;
      gap: 8px;
      cursor: pointer;
      padding: 6px 10px;
      border-radius: var(--border-radius-lg);
      transition: all var(--transition-fast);

      &:hover {
        background: var(--bg-tertiary);
      }

      .username {
        font-size: 14px;
        color: var(--text-primary);
        font-weight: 500;
      }

      :deep(.el-avatar) {
        border: 2px solid var(--bg-tertiary);
      }
    }
  }
}

// 内容区域
.admin-content {
  flex: 1;
  overflow-y: auto;
  padding: var(--spacing-lg);
  background: var(--bg-primary);

  &::-webkit-scrollbar {
    width: 8px;
  }

  &::-webkit-scrollbar-track {
    background: transparent;
  }

  &::-webkit-scrollbar-thumb {
    background: var(--border-color);
    border-radius: 4px;

    &:hover {
      background: var(--text-tertiary);
    }
  }
}

// 菜单样式优化
:deep(.el-menu) {
  border-right: none;
  background: transparent;

  .el-menu-item {
    height: 44px;
    margin: 4px 0;
    border-radius: var(--border-radius);
    transition: all var(--transition-fast);

    &:hover {
      background: var(--bg-tertiary) !important;
    }

    &.is-active {
      background: var(--color-primary) !important;
      color: #fff !important;

      .el-icon {
        color: #fff;
      }
    }

    .el-icon {
      margin-right: 10px;
      font-size: 16px;
      color: var(--text-secondary);
      transition: all var(--transition-fast);
    }

    span {
      font-size: 14px;
      font-weight: 500;
      color: var(--text-primary);
    }
  }
}

// 响应式
@media (max-width: 768px) {
  .admin-sidebar {
    position: fixed;
    left: 0;
    top: 0;
    bottom: 0;
    z-index: 100;
    transform: translateX(-100%);

    &.collapsed {
      transform: translateX(0);
    }
  }

  .admin-header {
    padding: 0 16px;
  }

  .admin-content {
    padding: 16px;
  }
}
</style>
