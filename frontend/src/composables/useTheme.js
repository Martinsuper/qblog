import { ref } from 'vue'

/**
 * 主题 composable（模块级单例，跨组件共享）
 * 同时维护 data-theme 属性（自定义令牌）与 html.dark 类（Element Plus 官方暗色变量）
 */
const isDark = ref(false)

// 初始化：index.html 的内联脚本已尽量提前设置，这里兜底处理（含跟随系统）
if (typeof document !== 'undefined') {
  const attr = document.documentElement.getAttribute('data-theme')
  if (attr === 'dark') {
    isDark.value = true
  } else if (attr === 'light') {
    isDark.value = false
  } else {
    isDark.value = window.matchMedia('(prefers-color-scheme: dark)').matches
  }
  applyTheme()
}

function applyTheme() {
  document.documentElement.setAttribute('data-theme', isDark.value ? 'dark' : 'light')
  document.documentElement.classList.toggle('dark', isDark.value)
  localStorage.setItem('theme', isDark.value ? 'dark' : 'light')
}

export function useTheme() {
  function toggleTheme() {
    isDark.value = !isDark.value
    applyTheme()
  }

  return { isDark, toggleTheme }
}
