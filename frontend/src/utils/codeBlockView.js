import { CodeBlockLowlight } from '@tiptap/extension-code-block-lowlight'
import { CODE_LANGUAGES, languageLabel } from './codeLanguages'

 /**
 * 代码块右下角语言节点视图：点击弹出语言菜单（支持输入过滤与自定义语言）。
 * 内容 DOM（code 元素）仍由 ProseMirror 接管（低光高亮装饰照常生效），
 * 仅语言属性变化时由 update() 自渲染标签文本。
 *
 * 交互约定：菜单顶部输入框可过滤现有语言；Enter 时若与现有项完全匹配（值或标签）
 * 则应用该项，否则把输入原样作为自定义语言写入围栏（阅读端未注册语言退回纯文本）。
 */
export const CodeBlockLangTag = CodeBlockLowlight.extend({
  addKeyboardShortcuts() {
    return {
      ...this.parent?.(),
      // 光标在代码块内时 Mod-a 只全选代码块内容；否则放行给默认的全篇全选
      'Mod-a': () => {
        if (!this.editor.isActive(this.name)) return false
        const { state } = this.editor
        const { $from } = state.selection
        for (let d = $from.depth; d >= 0; d -= 1) {
          if ($from.node(d).type.name === this.name) {
            const from = $from.start(d)
            const to = $from.end(d)
            return this.editor.commands.setTextSelection({ from, to })
          }
        }
        return false
      }
    }
  },
  addNodeView() {
    return ({ node, editor, getPos }) => {
      const wrapper = document.createElement('div')
      wrapper.className = 'code-block-view'
      const pre = document.createElement('pre')
      const code = document.createElement('code')
      pre.appendChild(code)
      const tag = document.createElement('button')
      tag.type = 'button'
      tag.className = 'code-lang-tag'
      const menu = document.createElement('div')
      menu.className = 'lang-menu'
      menu.hidden = true
      const filter = document.createElement('input')
      filter.type = 'text'
      filter.className = 'lang-input'
      filter.placeholder = '过滤或输入语言'
      filter.setAttribute('aria-label', '代码语言过滤')
      filter.spellcheck = false
      menu.appendChild(filter)
      const items = CODE_LANGUAGES.map(lang => {
        const item = document.createElement('button')
        item.type = 'button'
        item.className = 'lang-item'
        item.textContent = lang.label
        item.dataset.value = lang.value
        menu.appendChild(item)
        return item
      })
      wrapper.append(pre, tag, menu)

      function render() {
        code.className = node.attrs.language ? `language-${node.attrs.language}` : ''
        tag.textContent = node.attrs.language ? languageLabel(node.attrs.language) : '语言'
        for (const item of items) item.classList.toggle('active', item.dataset.value === node.attrs.language)
      }
      render()

      function setLanguage(language) {
        closeMenu()
        const pos = typeof getPos === 'function' ? getPos() : null
        if (pos == null || pos === false) return
        editor.view.dispatch(editor.view.state.tr.setNodeMarkup(pos, undefined, { language }))
      }

      function applyFilter(query) {
        const term = query.trim().toLowerCase()
        for (const item of items) {
          item.hidden = Boolean(term)
            && !item.dataset.value.includes(term)
            && !item.textContent.toLowerCase().includes(term)
        }
      }

      function openMenu() {
        menu.hidden = false
        filter.value = ''
        applyFilter('')
        filter.focus()
      }
      function closeMenu() { menu.hidden = true }
      function toggleMenu() { menu.hidden ? openMenu() : closeMenu() }

      // Enter：完全匹配现有项（值或标签）用其值，否则输入原样作为自定义语言
      function applyTyped() {
        const raw = filter.value.trim()
        if (!raw) { closeMenu(); return }
        const term = raw.toLowerCase()
        const matched = items.find(item => !item.hidden
          && (item.dataset.value === term || item.textContent.toLowerCase() === term))
        setLanguage(matched ? matched.dataset.value : raw)
      }

      filter.addEventListener('input', () => applyFilter(filter.value))
      filter.addEventListener('keydown', event => {
        if (event.key === 'Enter') { event.preventDefault(); applyTyped() }
        else if (event.key === 'Escape') { event.preventDefault(); closeMenu() }
      })

      // 阻止 mousedown 抢焦点，编辑器光标保持在代码块内
      tag.addEventListener('mousedown', event => event.preventDefault())
      tag.addEventListener('click', event => { event.stopPropagation(); toggleMenu() })
      menu.addEventListener('click', event => {
        event.stopPropagation()
        const item = event.target.closest('.lang-item')
        if (item) setLanguage(item.dataset.value)
      })
      const onDocumentClick = () => { menu.hidden = true }
      document.addEventListener('click', onDocumentClick)

      return {
        dom: wrapper,
        contentDOM: code,
        update(updatedNode) {
          if (updatedNode.type.name !== node.type.name) return false
          node = updatedNode
          render()
          return true
        },
        // 文本/子节点变化交给 ProseMirror 管理；标签、菜单与 wrapper 属性（含装饰 class）由本视图自理
        ignoreMutation(mutation) {
          const inContent = mutation.target === code || code.contains(mutation.target)
          return !inContent || (mutation.target === code && mutation.type === 'attributes')
        },
        // 标签与菜单区域内的事件（含键盘输入）不进入 ProseMirror 处理流程，避免抢焦点、改选区
        stopEvent(event) {
          const target = event.target
          return target === tag || target === menu || tag.contains(target) || menu.contains(target)
        },
        destroy() {
          document.removeEventListener('click', onDocumentClick)
        }
      }
    }
  }
})
