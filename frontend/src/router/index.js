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
    {
      path: '/articles',
      name: 'article-list',
      component: () => import('@/views/Article/List.vue')
    },
    {
      path: '/article/:id',
      name: 'article-detail',
      component: () => import('@/views/Article/Detail.vue')
    },
    {
      path: '/archives',
      name: 'archive',
      component: () => import('@/views/Archive/Index.vue')
    },
    {
      path: '/categories',
      name: 'category-list',
      component: () => import('@/views/Category/List.vue')
    },
    {
      path: '/category/:id',
      name: 'category',
      component: () => import('@/views/Category/Index.vue')
    },
    {
      path: '/tags',
      name: 'tag-list',
      component: () => import('@/views/Tag/List.vue')
    },
    {
      path: '/tag/:id',
      name: 'tag',
      component: () => import('@/views/Tag/Index.vue')
    },
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
