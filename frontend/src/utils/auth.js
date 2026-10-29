/**
 * Token 存取工具
 * "记住我"：localStorage（关闭浏览器仍有效）；未勾选：sessionStorage（关闭浏览器即失效）
 */
const TOKEN_KEY = 'token'
export const USER_INFO_KEY = 'userInfo'

/** Corrupt or obsolete cached data must not prevent public navigation. */
export function getCachedUserInfo(storage = localStorage) {
  try {
    const raw = storage.getItem(USER_INFO_KEY)
    if (!raw) return null
    const value = JSON.parse(raw)
    if (value && typeof value === 'object' && !Array.isArray(value) &&
        (value.role === 0 || value.role === 1)) {
      return value
    }
  } catch {
    // Invalid JSON or unavailable storage: fall back to anonymous UI.
  }
  try {
    storage.removeItem(USER_INFO_KEY)
  } catch {
    // Storage can be unavailable in privacy-restricted browsers.
  }
  return null
}

export function getToken() {
  return localStorage.getItem(TOKEN_KEY) || sessionStorage.getItem(TOKEN_KEY)
}

export function setToken(token, remember = true) {
  clearToken()
  ;(remember ? localStorage : sessionStorage).setItem(TOKEN_KEY, token)
}

export function clearToken() {
  localStorage.removeItem(TOKEN_KEY)
  sessionStorage.removeItem(TOKEN_KEY)
}
