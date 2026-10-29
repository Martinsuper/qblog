import request from './request'

/**
 * 认证相关 API
 */
export function login(data) {
  return request({
    url: '/auth/login',
    method: 'post',
    data
  })
}

export function getCurrentUser() {
  return request({
    url: '/auth/me',
    method: 'get'
  })
}

export function logout() {
  return request({
    url: '/auth/logout',
    method: 'post'
  })
}

export function changePassword(data) {
  return request({
    url: '/auth/password',
    method: 'put',
    data
  })
}
