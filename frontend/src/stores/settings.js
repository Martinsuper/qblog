import { defineStore } from 'pinia'
import { ref } from 'vue'

const STORAGE_KEY = 'qblog.markdown-theme'
const THEMES = ['vuepress', 'github', 'astro']

function readInitialTheme() {
  try {
    const stored = localStorage.getItem(STORAGE_KEY)
    return THEMES.includes(stored) ? stored : 'vuepress'
  } catch {
    return 'vuepress'
  }
}

export const useSettingsStore = defineStore('settings', () => {
  // Markdown 渲染主题：'vuepress' | 'github' | 'astro'
  const markdownTheme = ref(readInitialTheme())

  // 切换 Markdown 主题，并持久化到 localStorage（刷新后不丢失）
  function setMarkdownTheme(theme) {
    if (THEMES.includes(theme)) {
      markdownTheme.value = theme
      try {
        localStorage.setItem(STORAGE_KEY, theme)
      } catch {
        /* 私隐模式下忽略持久化失败 */
      }
    }
  }

  return {
    markdownTheme,
    setMarkdownTheme
  }
})
