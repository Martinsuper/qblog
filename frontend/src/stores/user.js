import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login, register, getCurrentUser } from '@/api/auth'
import { getToken, setToken, clearToken } from '@/utils/auth'

const USER_INFO_KEY = 'userInfo'

export const useUserStore = defineStore('user', () => {
  const token = ref(getToken() || '')
  const userInfo = ref(JSON.parse(localStorage.getItem(USER_INFO_KEY) || 'null'))

  const isLoggedIn = computed(() => !!token.value)
  const isAdmin = computed(() => userInfo.value?.role === 1)

  // 登录（remember=true 时持久化到 localStorage，否则仅 sessionStorage）
  async function userLogin(loginForm) {
    const res = await login(loginForm)
    token.value = res.data
    setToken(res.data, !!loginForm.remember)

    // 获取用户信息（失败不阻断登录流程）
    await fetchUserInfo().catch((error) => console.error('获取用户信息失败:', error))

    return res
  }

  // 注册
  async function userRegister(registerForm) {
    return await register(registerForm)
  }

  // 获取用户信息
  async function fetchUserInfo() {
    const res = await getCurrentUser()
    userInfo.value = res.data
    localStorage.setItem(USER_INFO_KEY, JSON.stringify(res.data))
  }

  // 退出登录
  function userLogout() {
    token.value = ''
    userInfo.value = null
    clearToken()
    localStorage.removeItem(USER_INFO_KEY)
    localStorage.removeItem('isLoggedIn')
  }

  return {
    token,
    userInfo,
    isLoggedIn,
    isAdmin,
    userLogin,
    userRegister,
    fetchUserInfo,
    userLogout
  }
})
