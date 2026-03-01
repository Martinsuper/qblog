import MarkdownIt from 'markdown-it'
import hljs from 'highlight.js'
import { computed, ref, watch } from 'vue'
import { useSettingsStore } from '@/stores/settings'
import {
  plantumlPlugin,
  tipContainerPlugin,
  headingAnchorPlugin,
  codeCopyPlugin
} from '@/utils/markdownPlugins'

// 缓存渲染器实例
const rendererCache = new Map()

/**
 * 创建 Markdown 渲染器
 * @param {string} theme - 'vuepress' | 'github'
 * @returns {MarkdownIt}
 */
function createRenderer(theme) {
  const md = new MarkdownIt({
    html: true,
    linkify: true,
    typographer: true,
    breaks: true,
    highlight: function (str, lang) {
      // 使用 highlight.js 进行语法高亮
      if (lang && hljs.getLanguage(lang)) {
        try {
          return `<pre class="hljs"><code>${hljs.highlight(str, { language: lang, ignoreIllegals: true }).value}</code></pre>`
        } catch (error) {
          console.error('代码高亮失败:', error)
        }
      }
      // 默认转义
      return `<pre class="hljs"><code>${md.utils.escapeHtml(str)}</code></pre>`
    }
  })

  // 应用插件
  plantumlPlugin(md)
  tipContainerPlugin(md)
  headingAnchorPlugin(md)

  // VuePress 风格启用代码复制按钮
  if (theme === 'vuepress') {
    codeCopyPlugin(md)
  }

  return md
}

/**
 * 获取或创建渲染器实例
 * @param {string} theme
 * @returns {MarkdownIt}
 */
function getRenderer(theme) {
  if (!rendererCache.has(theme)) {
    rendererCache.set(theme, createRenderer(theme))
  }
  return rendererCache.get(theme)
}

/**
 * Markdown 渲染 composable
 */
export function useMarkdown() {
  const settings = useSettingsStore()

  // 当前主题
  const theme = computed(() => settings.markdownTheme)

  // 获取当前渲染器
  const renderer = computed(() => getRenderer(theme.value))

  /**
   * 渲染 Markdown 内容
   * @param {string} content - Markdown 内容
   * @returns {string} - 渲染后的 HTML
   */
  function render(content) {
    if (!content) {
      return '<p>暂无内容</p>'
    }
    return renderer.value.render(content)
  }

  /**
   * 获取主题 CSS 类名
   * @returns {string}
   */
  function getThemeClass() {
    return `markdown-body ${theme.value}-theme`
  }

  return {
    theme,
    render,
    getThemeClass
  }
}

// 导出初始化函数
export { initCodeCopyHandler } from '@/utils/markdownPlugins'