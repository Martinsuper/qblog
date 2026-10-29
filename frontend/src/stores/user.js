import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login, getCurrentUser } from '@/api/auth'
import { getToken, setToken, clearToken, getCachedUserInfo, USER_INFO_KEY } from '@/utils/auth'

export const useUserStore = defineStore('user', () => {
  const token = ref(getToken() || '')
  const userInfo = ref(getCachedUserInfo())

  const isLoggedIn = computed(() => !!token.value)
  const isAdmin = computed(() => userInfo.value?.role === 1)

  // 登录（remember=true 时持久化到 localStorage，否则仅 sessionStorage）
  async function userLogin(loginForm) {
    const res = await login(loginForm)
    token.value = res.data
    setToken(res.data, !!loginForm.remember)

    // 私人工作区必须成功校验实时身份，不能以旧缓存决定权限。
    try {
      await fetchUserInfo()
    } catch (error) {
      userLogout()
      throw error
    }

    return res
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
    fetchUserInfo,
    userLogout
  }
})
