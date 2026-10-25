/**
 * Token 存取工具
 * "记住我"：localStorage（关闭浏览器仍有效）；未勾选：sessionStorage（关闭浏览器即失效）
 */
const TOKEN_KEY = 'token'

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
