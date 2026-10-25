import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'
import { getToken, clearToken } from '@/utils/auth'

// 创建 axios 实例
const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 10000
})

// 请求拦截器
request.interceptors.request.use(
  config => {
    const token = getToken()
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

// 登出并跳转登录页（异常路径整页跳转，确保所有组件状态重置）
const forceLogout = () => {
  clearToken()
  localStorage.removeItem('userInfo')
  window.location.href = '/login'
}

// 响应拦截器
request.interceptors.response.use(
  response => {
    // 如果是文件下载，直接返回 blob 数据
    if (response.config.responseType === 'blob') {
      return response.data
    }

    const res = response.data

    // 业务码非 200 视为失败
    if (res.code !== 200) {
      ElMessage.error(res.message || '请求失败')
      return Promise.reject(new Error(res.message || '请求失败'))
    }

    return res
  },
  error => {
    console.error('请求错误:', error)

    // 后端已返回结构化错误体（真实 HTTP 状态码 + Result JSON）
    const result = error.response?.data
    const message = result?.message || error.message || '网络错误'

    // 401: 未授权/Token 失效；403: 无权限
    if (error.response?.status === 401) {
      ElMessage.error(message)
      forceLogout()
      return Promise.reject(error)
    }

    ElMessage.error(message)
    return Promise.reject(error)
  }
)

export default request
