import MarkdownIt from 'markdown-it'
import hljs from 'highlight.js/lib/core'
import javascript from 'highlight.js/lib/languages/javascript'
import typescript from 'highlight.js/lib/languages/typescript'
import xml from 'highlight.js/lib/languages/xml'
import css from 'highlight.js/lib/languages/css'
import json from 'highlight.js/lib/languages/json'
import bash from 'highlight.js/lib/languages/bash'
import java from 'highlight.js/lib/languages/java'
import python from 'highlight.js/lib/languages/python'
import sql from 'highlight.js/lib/languages/sql'
import yaml from 'highlight.js/lib/languages/yaml'
import { computed } from 'vue'
import { useSettingsStore } from '@/stores/settings'
import {
  tipContainerPlugin,
  headingAnchorPlugin,
  codeCopyPlugin
} from '@/utils/markdownPlugins'

// 未注册的语言安全回退到纯文本，避免加载全部语言。
Object.entries({ javascript, typescript, xml, css, json, bash, java, python, sql, yaml })
  .forEach(([name, language]) => hljs.registerLanguage(name, language))

// 缓存渲染器实例
const rendererCache = new Map()

/**
 * 创建 Markdown 渲染器
 * @param {string} theme - 'vuepress' | 'github'
 * @returns {MarkdownIt}
 */
function createRenderer(theme) {
  const md = new MarkdownIt({
    html: false,  // 禁用原始HTML，防止XSS
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

  // 添加安全链接校验（只允许 http/https/mailto 协议）
  md.validateLink = (url) => {
    const allowed = /^(?:https?:\/\/|mailto:)/i
    return allowed.test(url)
  }

  // 应用插件
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