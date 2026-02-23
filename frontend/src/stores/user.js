import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login, register, getCurrentUser, logout } from '@/api/auth'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const userInfo = ref(JSON.parse(localStorage.getItem('userInfo') || 'null'))

  const isLoggedIn = computed(() => !!token.value)
  const isAdmin = computed(() => userInfo.value?.role === 1)

  // 登录
  async function userLogin(loginForm) {
    const res = await login(loginForm)
    token.value = res.data
    localStorage.setItem('token', res.data)
    localStorage.setItem('isLoggedIn', 'true')

    // 获取用户信息
    try {
      await fetchUserInfo()
    } catch (error) {
      console.error('获取用户信息失败:', error)
    }

    return res
  }

  // 注册
  async function userRegister(registerForm) {
    return await register(registerForm)
  }

  // 获取用户信息
  async function fetchUserInfo() {
    try {
      const res = await getCurrentUser()
      userInfo.value = res.data
      localStorage.setItem('userInfo', JSON.stringify(res.data))
    } catch (error) {
      // 如果获取失败，使用 token 中的信息
      console.error('获取用户信息失败:', error)
      // 可以从 token 中解析基本信息
      if (token.value) {
        const payload = JSON.parse(atob(token.value.split('.')[1]))
        userInfo.value = {
          username: payload.username,
          nickname: payload.username
        }
        localStorage.setItem('userInfo', JSON.stringify(userInfo.value))
      }
    }
  }

  // 退出登录
  function userLogout() {
    token.value = ''
    userInfo.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
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
