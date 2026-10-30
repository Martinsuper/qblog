import { Extension } from '@tiptap/core'
import Suggestion from '@tiptap/suggestion'

export const COMMANDS = [
  { id: 'paragraph', glyph: '¶', label: '正文', hint: '普通段落文本', keywords: ['text', 'paragraph', 'p', 'wenben', 'wb'], run: (editor, range) => editor.chain().focus().deleteRange(range).setParagraph().run() },
  { id: 'heading1', glyph: 'H1', label: '标题 1', hint: '大节标题', keywords: ['h1', 'heading', 'biaoti', 'bt'], run: (editor, range) => editor.chain().focus().deleteRange(range).setHeading({ level: 1 }).run() },
  { id: 'heading2', glyph: 'H2', label: '标题 2', hint: '章节标题', keywords: ['h2', 'heading', 'biaoti', 'bt'], run: (editor, range) => editor.chain().focus().deleteRange(range).setHeading({ level: 2 }).run() },
  { id: 'heading3', glyph: 'H3', label: '标题 3', hint: '小节标题', keywords: ['h3', 'heading', 'biaoti', 'bt'], run: (editor, range) => editor.chain().focus().deleteRange(range).setHeading({ level: 3 }).run() },
  { id: 'bulletList', glyph: '☷', label: '无序列表', hint: '简单列表项', keywords: ['list', 'bullet', 'ul', 'liebiao', 'lb'], run: (editor, range) => editor.chain().focus().deleteRange(range).toggleBulletList().run() },
  { id: 'orderedList', glyph: '1.', label: '有序列表', hint: '带编号的列表', keywords: ['list', 'ordered', 'ol', 'bianhao', 'bh'], run: (editor, range) => editor.chain().focus().deleteRange(range).toggleOrderedList().run() },
  { id: 'taskList', glyph: '☑', label: '待办列表', hint: '可勾选的任务清单', keywords: ['task', 'todo', 'checklist', 'daiban', 'db', 'qingdan'], run: (editor, range) => editor.chain().focus().deleteRange(range).toggleTaskList().run() },
  { id: 'blockquote', glyph: '❝', label: '引用', hint: '强调的一段引文', keywords: ['quote', 'blockquote', 'yinyong', 'yy'], run: (editor, range) => editor.chain().focus().deleteRange(range).toggleBlockquote().run() },
  { id: 'codeBlock', glyph: '<>', label: '代码块', hint: '等宽字体的代码段', keywords: ['code', 'codeblock', 'daima', 'dm'], run: (editor, range) => editor.chain().focus().deleteRange(range).toggleCodeBlock().run() },
  { id: 'table', glyph: '▦', label: '表格', hint: '3×3 带表头的表格', keywords: ['table', 'biaoge', 'bg'], run: (editor, range) => editor.chain().focus().deleteRange(range).insertTable({ rows: 3, cols: 3, withHeaderRow: true }).run() },
  { id: 'horizontalRule', glyph: '—', label: '分隔线', hint: '划分内容的水平线', keywords: ['hr', 'divider', 'rule', 'fengexian', 'fgx', 'gefengxian'], run: (editor, range) => editor.chain().focus().deleteRange(range).setHorizontalRule().run() }
]

export function filterCommands(query) {
  const term = String(query || '').trim().toLowerCase()
  if (!term) return COMMANDS
  return COMMANDS.filter(({ label, keywords }) => label.toLowerCase().includes(term) || keywords.some(keyword => keyword.toLowerCase().includes(term)))
}

function createMenuRenderer() {
  let root = null, listEl = null, unmount = null, lastProps = null, items = [], activeIndex = 0

  function setActive(next) {
    activeIndex = (next + items.length) % items.length
    Array.from(listEl.children).forEach((child, index) => child.classList?.toggle('is-active', index === activeIndex))
    listEl.children[activeIndex]?.scrollIntoView({ block: 'nearest' })
  }

  function renderItems(props) {
    lastProps = props
    items = props.items
    activeIndex = 0
    listEl.replaceChildren()
    if (!items.length) {
      const empty = document.createElement('div')
      empty.className = 'slash-menu-empty'
      empty.textContent = '没有匹配的命令'
      listEl.appendChild(empty)
      return
    }
    props.items.forEach(item => {
      const button = document.createElement('button')
      button.type = 'button'
      button.className = 'slash-menu-item'
      const glyph = document.createElement('span')
      glyph.className = 'slash-menu-glyph'
      glyph.textContent = item.glyph
      const text = document.createElement('span')
      text.className = 'slash-menu-text'
      const label = document.createElement('span')
      label.className = 'slash-menu-label'
      label.textContent = item.label
      const hint = document.createElement('span')
      hint.className = 'slash-menu-hint'
      hint.textContent = item.hint
      text.append(label, hint)
      button.append(glyph, text)
      button.addEventListener('mousedown', event => event.preventDefault())
      button.addEventListener('click', () => props.command(item))
      listEl.appendChild(button)
    })
    Array.from(listEl.children).forEach((child, index) => child.classList?.toggle('is-active', index === activeIndex))
  }

  function syncTheme(props) {
    // 两套布局的根都带 data-color；html.dark 兜底覆盖组件被复用到其他场景的情况
    const dark = props.editor.view.dom.closest('[data-color="dark"]')
      || (document.documentElement.classList.contains('dark') || document.documentElement.dataset.theme === 'dark' ? document.documentElement : null)
    root.classList.toggle('is-dark', Boolean(dark))
  }

  return {
    onStart(props) {
      root = document.createElement('div')
      root.className = 'slash-menu'
      listEl = document.createElement('div')
      listEl.className = 'slash-menu-list'
      const foot = document.createElement('div')
      foot.className = 'slash-menu-foot'
      foot.textContent = '↑↓ 选择 · Enter 确认 · Esc 关闭'
      root.append(listEl, foot)
      syncTheme(props)
      renderItems(props)
      unmount = props.mount(root)
    },
    onUpdate(props) {
      syncTheme(props)
      renderItems(props)
    },
    onExit() {
      unmount?.()
      unmount = null
      root = null
      listEl = null
      lastProps = null
      items = []
    },
    onKeyDown({ event }) {
      if (!items.length) return false
      if (event.key === 'ArrowDown') { setActive(activeIndex + 1); return true }
      if (event.key === 'ArrowUp') { setActive(activeIndex - 1); return true }
      if (event.key === 'Enter') { lastProps?.command(items[activeIndex]); return true }
      return false
    }
  }
}

export const SlashCommand = Extension.create({
  name: 'slashCommand',
  addProseMirrorPlugins() {
    return [Suggestion({
      editor: this.editor,
      char: '/',
      allow: ({ editor }) => !editor.isActive('codeBlock'),
      items: ({ query }) => filterCommands(query),
      command: ({ editor, range, props }) => props.run(editor, range),
      floatingUi: { strategy: 'fixed' },
      render: createMenuRenderer
    })]
  }
})
