/**
 * TipTap PlantUML 预览插件（折叠式）
 * plantuml 代码块（language === 'plantuml' 或内容以 @startuml 开头）默认收起：
 * 用 node 装饰隐藏源码块，改为展示渲染 SVG 的 widget 卡片。
 * 点击卡片在下方展开/收起源码；卡片右上角「复制」按钮复制 PlantUML 源码。
 * 装饰不改动文档，markdown 序列化与自动保存链路不受影响。
 * 编辑源码时暂停 400ms 防抖后才重新渲染图表。
 */
import { Extension } from '@tiptap/core'
import { Plugin, PluginKey } from '@tiptap/pm/state'
import { Decoration, DecorationSet } from '@tiptap/pm/view'
import { plantumlSvgUrl } from './markdownPlugins/plantuml'

export const plantumlPreviewKey = new PluginKey('plantumlPreview')

const REBUILD_DEBOUNCE_MS = 400
/** 空占位 @startuml/@enduml 模板至少这个长度才有渲染意义 */
const MIN_SOURCE_LENGTH = 16

const COPY_ICON =
  '<svg viewBox="0 0 24 24"><rect x="9" y="9" width="13" height="13" rx="2" ry="2"/><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"/></svg>'

/** 收集文档中的 plantuml 代码块（起止位置 + 源码） */
function collectBlocks(doc) {
  const blocks = []
  doc.descendants((node, pos) => {
    if (node.type.name !== 'codeBlock') return
    const text = node.textContent
    // language 双条件：fence info 为 plantuml，或裸写 @startuml
    if (node.attrs.language !== 'plantuml' && !text.trim().startsWith('@startuml')) return
    if (text.trim().length < MIN_SOURCE_LENGTH) return
    blocks.push({ from: pos, to: pos + node.nodeSize, text: text.trim() })
  })
  return blocks
}

function sigOf(doc) {
  return collectBlocks(doc).map((b) => b.text).join('\u0000')
}

function copyToClipboard(text, btn) {
  const done = () => {
    btn.classList.add('copied')
    btn.querySelector('span').textContent = '已复制'
    setTimeout(() => {
      btn.classList.remove('copied')
      if (btn.isConnected) btn.querySelector('span').textContent = '复制'
    }, 1400)
  }
  const fallback = () => {
    const ta = document.createElement('textarea')
    ta.value = text
    ta.style.cssText = 'position:fixed;opacity:0;pointer-events:none'
    document.body.appendChild(ta)
    ta.select()
    try { document.execCommand('copy'); done() } catch { /* 忽略 */ }
    ta.remove()
  }
  if (navigator.clipboard?.writeText) {
    navigator.clipboard.writeText(text).then(done).catch(fallback)
  } else fallback()
}

/** 预览卡片：点击整卡切换展开/收起；右上角工具（源码提示 + 复制按钮） */
function widgetEl(view, getPos, idx, text, expanded) {
  const wrap = document.createElement('div')
  wrap.className = 'plantuml-preview'
  wrap.setAttribute('contenteditable', 'false')
  wrap.setAttribute('draggable', 'false')

  const img = document.createElement('img')
  img.src = plantumlSvgUrl(text)
  img.alt = expanded ? 'PlantUML 图表（点击收起源码）' : 'PlantUML 图表（点击查看源码）'
  img.loading = 'lazy'
  img.decoding = 'async'
  wrap.appendChild(img)

  const tools = document.createElement('div')
  tools.className = 'plantuml-tools'
  const hint = document.createElement('span')
  hint.className = 'plantuml-hint'
  hint.textContent = expanded ? '点击收起源码' : '点击查看源码'
  const copy = document.createElement('button')
  copy.type = 'button'
  copy.className = 'plantuml-copy'
  copy.title = '复制 PlantUML 源码'
  copy.innerHTML = COPY_ICON + '<span>复制</span>'
  copy.addEventListener('click', (ev) => {
    ev.stopPropagation()
    copyToClipboard(text, copy)
  })
  tools.appendChild(hint)
  tools.appendChild(copy)
  wrap.appendChild(tools)

  wrap.addEventListener('click', () => {
    const pos = typeof getPos === 'function' ? getPos() : null
    if (pos == null || pos === false) return
    const tr = view.state.tr.setMeta(plantumlPreviewKey, { toggle: idx })
    tr.setMeta('addToHistory', false)
    view.dispatch(tr)
  })
  return wrap
}

function decosFor(doc, expanded) {
  const blocks = collectBlocks(doc)
  if (!blocks.length) return DecorationSet.empty
  const decos = blocks.map((b, idx) => {
    // 卡片放在代码块之前：展开时渲染图在上、源码在下
    const list = [Decoration.widget(
      b.from,
      (view, getPos) => widgetEl(view, getPos, idx, b.text, expanded.has(idx)),
      { side: -1, ignoreSelection: true, marks: [] }
    )]
    // 收起时用 node 装饰隐藏源码块（display:none 由样式实现）
    if (!expanded.has(idx)) list.push(Decoration.node(b.from, b.to, { class: 'plantuml-src-hidden' }))
    return list
  })
  return DecorationSet.create(doc, decos.flat())
}

export function PlantUmlPreview() {
  let timer = null
  return Extension.create({
    name: 'plantUmlPreview',
    addProseMirrorPlugins() {
      const key = plantumlPreviewKey
      return [
        new Plugin({
          key,
          state: {
            init: (_, state) => ({ sig: sigOf(state.doc), expanded: new Set(), decos: decosFor(state.doc, new Set()) }),
            apply: (tr, prev, _old, next) => {
              const meta = tr.getMeta(key)
              // 点击卡片切换展开/收起：立即重建该块的装饰
              if (meta && typeof meta.toggle === 'number') {
                const expanded = new Set(prev.expanded)
                if (expanded.has(meta.toggle)) expanded.delete(meta.toggle)
                else expanded.add(meta.toggle)
                return { sig: sigOf(next.doc), expanded, decos: decosFor(next.doc, expanded) }
              }
              // 防抖定时器触发的整体重建（保留展开状态）
              if (meta?.rebuild) {
                const sig = sigOf(next.doc)
                return prev.sig === sig
                  ? prev
                  : { sig, expanded: prev.expanded, decos: decosFor(next.doc, prev.expanded) }
              }
              if (!tr.docChanged && !tr.selectionSet) return prev
              const sig = sigOf(next.doc)
              if (sig === prev.sig) {
                // 源码未变、只是位置变了：映射旧装饰到新位置
                return { sig, expanded: prev.expanded, decos: prev.decos.map(tr.mapping, next.doc) }
              }
              // 输入过程中：保持已渲染的装饰，交给 view() 的防抖定时器重建
              return { sig: prev.sig, expanded: prev.expanded, decos: prev.decos.map(tr.mapping, next.doc) }
            },
          },
          props: {
            decorations: (state) => key.getState(state)?.decos || DecorationSet.empty,
          },
          view: () => ({
            update(view) {
              const s = key.getState(view.state)
              if (!s || s.sig === undefined) return
              const live = sigOf(view.state.doc)
              if (live === s.sig) {
                if (timer) { clearTimeout(timer); timer = null }
                return
              }
              if (timer) clearTimeout(timer)
              timer = setTimeout(() => {
                timer = null
                if (view.destroyed) return
                const cur = key.getState(view.state)
                if (!cur) return
                if (sigOf(view.state.doc) === cur.sig) return
                const tr = view.state.tr.setMeta(key, { rebuild: true })
                tr.setMeta('addToHistory', false)
                tr.setMeta('preventUpdate', true)
                view.dispatch(tr)
              }, REBUILD_DEBOUNCE_MS)
            },
            destroy() {
              if (timer) clearTimeout(timer)
            },
          }),
        }),
      ]
    },
    onDestroy() {
      if (timer) clearTimeout(timer)
    },
  })
}
