import { createRouter, createWebHistory } from 'vue-router'
import { getToken, getCachedUserInfo } from '@/utils/auth'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: () => import('@/views/Knowledge/Index.vue'),
      meta: { requiresAuth: true, requiresAdmin: true, standalone: true }
    },
    // 博客阅读页已下线（阅读侧只有知识库工作区）；旧链接统一回到知识库首页。
    // 文章数据与后端 API 保留：管理后台（/admin）与智能体/MCP 仍可维护文章。
    { path: '/articles', redirect: '/' },
    { path: '/article/:id', redirect: '/' },
    { path: '/archives', redirect: '/' },
    { path: '/categories', redirect: '/' },
    { path: '/category/:id', redirect: '/' },
    { path: '/tags', redirect: '/' },
    { path: '/tag/:id', redirect: '/' },
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/User/Login.vue'),
      meta: { standalone: true }
    },
    {
      path: '/register',
      redirect: '/login'
    },
    {
      path: '/assistant',
      name: 'assistant',
      component: () => import('@/views/Agent/Index.vue'),
      meta: { requiresAuth: true, requiresAdmin: true, standalone: true }
    },
    {
      path: '/settings',
      redirect: '/settings/appearance',
      meta: { requiresAuth: true, requiresAdmin: true, standalone: true }
    },
    {
      path: '/settings/appearance',
      name: 'knowledge-appearance',
      component: () => import('@/views/Knowledge/Appearance.vue'),
      meta: { requiresAuth: true, requiresAdmin: true, standalone: true }
    },
    {
      path: '/settings/tokens',
      name: 'knowledge-tokens',
      component: () => import('@/views/Knowledge/Tokens.vue'),
      meta: { requiresAuth: true, requiresAdmin: true, standalone: true }
    },
    {
      path: '/settings/backup',
      name: 'knowledge-backup',
      component: () => import('@/views/Knowledge/Backup.vue'),
      meta: { requiresAuth: true, requiresAdmin: true, standalone: true }
    },
    {
      path: '/user',
      name: 'user-center',
      redirect: '/user/profile',
      meta: { requiresAuth: true, requiresAdmin: true, standalone: true },
      children: [
        {
          path: 'profile',
          name: 'profile',
          component: () => import('@/views/User/Profile.vue')
        }
      ]
    },
    {
      path: '/admin',
      name: 'admin',
      redirect: '/admin/articles',
      meta: { requiresAuth: true, requiresAdmin: true },
      component: () => import('@/components/AdminLayout.vue'),
      children: [
        {
          path: 'articles',
          name: 'admin-articles',
          component: () => import('@/views/Admin/ArticleManage.vue')
        },
        {
          path: 'create',
          name: 'admin-article-create',
          component: () => import('@/views/Admin/ArticleEdit.vue')
        },
        {
          path: 'categories',
          name: 'admin-categories',
          component: () => import('@/views/Admin/CategoryManage.vue')
        },
        {
          path: 'tags',
          name: 'admin-tags',
          component: () => import('@/views/Admin/TagManage.vue')
        },
        {
          path: 'backup',
          // 备份与导出已收敛到知识库工作区设置页；旧入口重定向到新家
          redirect: '/settings/backup'
        }
      ]
    }
  ]
})

// 路由守卫
router.beforeEach((to, from, next) => {
  const token = getToken()
  const userInfo = getCachedUserInfo()

  if (to.path !== '/login' && !token) {
    next({ path: '/login', query: { redirect: to.fullPath } })
  } else if (to.path !== '/login' && userInfo && userInfo.role !== 1) {
    next('/login')
  } else {
    next()
  }
})

export default router
