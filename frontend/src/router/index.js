import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: () => import('@/views/Home/Index.vue')
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
      component: () => import('@/views/User/Login.vue')
    },
    {
      path: '/register',
      name: 'register',
      component: () => import('@/views/User/Register.vue')
    },
    {
      path: '/user',
      name: 'user-center',
      redirect: '/user/profile',
      meta: { requiresAuth: true },
      children: [
        {
          path: 'profile',
          name: 'profile',
          component: () => import('@/views/User/Profile.vue')
        },
        {
          path: 'favorites',
          name: 'favorites',
          component: () => import('@/views/User/Favorites.vue')
        }
      ]
    },
    {
      path: '/admin',
      name: 'admin',
      redirect: '/admin/dashboard',
      meta: { requiresAuth: true, requiresAdmin: true },
      component: () => import('@/components/AdminLayout.vue'),
      children: [
        {
          path: 'dashboard',
          name: 'admin-dashboard',
          component: () => import('@/views/Admin/Dashboard.vue')
        },
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
        }
      ]
    }
  ]
})

// 路由守卫
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}')
  
  if (to.meta.requiresAuth && !token) {
    next('/login')
  } else if (to.meta.requiresAdmin && userInfo.role !== 1) {
    next('/')
  } else {
    next()
  }
})

export default router
