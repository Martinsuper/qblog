import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useSettingsStore = defineStore('settings', () => {
  // Markdown 渲染主题：'vuepress' | 'github'
  const markdownTheme = ref('vuepress')

  // 切换 Markdown 主题
  function setMarkdownTheme(theme) {
    if (['vuepress', 'github'].includes(theme)) {
      markdownTheme.value = theme
    }
  }

  return {
    markdownTheme,
    setMarkdownTheme
  }
})