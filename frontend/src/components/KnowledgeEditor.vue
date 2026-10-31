<template>
  <div
    class="knowledge-editor"
    :class="{ 'is-readonly': readonly }"
  >
    <Teleport
      :to="toolbarTarget || 'body'"
      :disabled="!toolbarTarget"
    >
      <div
        v-if="!readonly"
        class="format-bar"
        :class="{ 'reader-format-bar': toolbarTarget }"
        aria-label="文档格式"
        @mousedown.prevent
      >
        <button
          type="button"
          class="format-btn"
          :class="{ active: editor?.isActive('bold') }"
          title="加粗"
          aria-label="加粗"
          @click="editor.chain().focus().toggleBold().run()"
        >
          <KnowledgeIcon name="bold" />
        </button>
        <button
          type="button"
          class="format-btn"
          title="二级标题"
          aria-label="二级标题"
          @click="editor.chain().focus().toggleHeading({ level: 2 }).run()"
        >
          <KnowledgeIcon name="heading" />
        </button>
        <span class="format-divider" />
        <button
          type="button"
          class="format-btn"
          title="无序列表"
          aria-label="无序列表"
          @click="editor.chain().focus().toggleBulletList().run()"
        >
          <KnowledgeIcon name="list" />
        </button>
        <button
          type="button"
          class="format-btn"
          title="代码块"
          aria-label="代码块"
          @click="editor.chain().focus().toggleCodeBlock().run()"
        >
          <KnowledgeIcon name="code" />
        </button>
        <button
          type="button"
          class="format-btn"
          title="插入表格"
          aria-label="插入表格"
          @click="editor.chain().focus().insertTable({ rows: 3, cols: 3, withHeaderRow: true }).run()"
        >
          <KnowledgeIcon name="table" />
        </button>
        <span class="format-divider" />
        <button
          type="button"
          class="format-btn"
          title="撤销"
          aria-label="撤销"
          :disabled="!editor?.can().undo()"
          @click="editor.chain().focus().undo().run()"
        >
          <KnowledgeIcon name="undo" />
        </button>
        <button
          type="button"
          class="format-btn"
          title="重做"
          aria-label="重做"
          :disabled="!editor?.can().redo()"
          @click="editor.chain().focus().redo().run()"
        >
          <KnowledgeIcon name="redo" />
        </button>
      </div>
    </Teleport>
    <editor-content :editor="editor" />
  </div>
</template>
<script setup>
import { watch, onBeforeUnmount } from 'vue'
import { useEditor, EditorContent } from '@tiptap/vue-3'
import StarterKit from '@tiptap/starter-kit'
import { Markdown } from '@tiptap/markdown'
import { CodeBlockLangTag } from '../utils/codeBlockView'
import { TableKit } from '@tiptap/extension-table'
import { TaskItem, TaskList } from '@tiptap/extension-list'
import KnowledgeIcon from './KnowledgeIcon.vue'
import { SlashCommand } from '../utils/slashCommands'
import { EditorEmptyHint } from '../utils/editorEmptyHint'
import { PlantUmlPreview } from '../utils/plantumlPreview'
import lowlight from '../utils/codeLanguages'
const props = defineProps({ modelValue: { type: String, default: '' }, readonly: Boolean, quietToolbar: Boolean, toolbarTarget: { type: Object, default: null } })
const emit = defineEmits(['update:modelValue', 'headings'])
let lastEmitted = props.modelValue
// getMarkdown 全文序列化是键入链路最重的操作：尾随防抖合并连续击键，
// 250ms 后统一序列化并上报。离开/销毁前必须 flushPending()，避免最后一击丢失。
const EMIT_DEBOUNCE_MS = 250
let emitTimer = null
function reportHeadings(instance) {
  const result = []
  instance.state.doc.descendants(node => { if (node.type.name === 'heading') result.push({ level: node.attrs.level, text: node.textContent }) })
  emit('headings', result)
}
function emitNow() {
  if (!editor.value) return
  emitTimer = null
  lastEmitted = editor.value.getMarkdown()
  emit('update:modelValue', lastEmitted)
  reportHeadings(editor.value)
}
function flushPending() {
  if (emitTimer) cancelAndEmit()
}
function cancelAndEmit() {
  clearTimeout(emitTimer)
  emitNow()
}
const editor = useEditor({
  extensions: [StarterKit.configure({ codeBlock: false, link: { openOnClick: false }, heading: { levels: [1, 2, 3] } }), CodeBlockLangTag.configure({ lowlight }), TaskList, TaskItem.configure({ nested: true }), TableKit, Markdown, SlashCommand, EditorEmptyHint, PlantUmlPreview()],
  content: props.modelValue,
  contentType: 'markdown',
  editable: !props.readonly,
  editorProps: {
    attributes: { 'aria-label': '笔记正文', spellcheck: 'false' },
    handleClick: (view, pos, event) => {
      // 正文链接点击接管：TipTap 的 openOnClick 配置为 false（编辑态点击不跳转、不丢焦点），
      // 这些会导致阅读时链接完全无响应；此处统一接管，点击直接打开外页。
      // # 开头的站内锚点不做处理，交回默认行为。
      const anchor = event.target?.closest?.('a[href]')
      if (!anchor) return false
      const href = anchor.getAttribute('href')
      if (!href || href.startsWith('#')) return false
      window.open(anchor.href, '_blank', 'noopener,noreferrer')
      return true
    }
  },
  onCreate: ({ editor: instance }) => reportHeadings(instance),
  onUpdate: () => { clearTimeout(emitTimer); emitTimer = setTimeout(emitNow, EMIT_DEBOUNCE_MS) }
})
watch(() => props.modelValue, value => {
  if (editor.value && value !== lastEmitted) {
    // 程序化载入（打开笔记/冲突重载）：丢弃未发出的序列化结果，防止旧内容回写覆盖新载入值
    if (emitTimer) { clearTimeout(emitTimer); emitTimer = null }
    lastEmitted = value
    editor.value.commands.setContent(value, { contentType: 'markdown', emitUpdate: false }); reportHeadings(editor.value)
  }
})
watch(() => props.readonly, value => editor.value?.setEditable(!value))
defineExpose({ flushPending })
onBeforeUnmount(() => {
  // 同步冲刷未上报的修改（切笔记/切布局经 :key 重建编辑器时，保证父层草稿拿到最新内容）
  if (emitTimer) cancelAndEmit()
  editor.value?.destroy()
})
</script>
<style>
.slash-menu { position: relative; z-index: 1000; display: flex; flex-direction: column; min-width: 248px; max-height: 322px; overflow: hidden; background: var(--panel, #fff); border: 1px solid var(--line, #e5e9ef); border-radius: 8px; box-shadow: 0 10px 32px #20334b1f; color: #243044; font: 12px/1.5 Inter,-apple-system,BlinkMacSystemFont,"Segoe UI","PingFang SC","Microsoft YaHei",sans-serif; }
.slash-menu.is-dark { background: #1c2633; border-color: #334257; color: #dce5f1; box-shadow: 0 10px 32px #00000059; }
.slash-menu-list { overflow-y: auto; padding: 5px; }
.slash-menu-item { display: flex; align-items: center; gap: 9px; width: 100%; padding: 6px 8px; border: 0; border-radius: 5px; background: transparent; color: inherit; text-align: left; cursor: pointer; }
.slash-menu-item.is-active { background: var(--blue-bg, #eaf0f9); }
.slash-menu.is-dark .slash-menu-item.is-active { background: #2b405d; }
.slash-menu-glyph { display: inline-flex; align-items: center; justify-content: center; flex: none; width: 30px; height: 30px; border: 1px solid var(--line, #e5e9ef); border-radius: 5px; background: var(--soft, #f7f9fc); font-size: 11px; font-weight: 600; color: #576b88; }
.slash-menu.is-dark .slash-menu-glyph { background: #222f40; border-color: #334257; color: #9abded; }
.slash-menu-text { display: flex; flex-direction: column; min-width: 0; }
.slash-menu-label { font-size: 12px; font-weight: 600; color: inherit; }
.slash-menu-hint { font-size: 10px; color: #7a8596; }
.slash-menu.is-dark .slash-menu-hint { color: #98a8bd; }
.slash-menu-empty { padding: 12px 10px; color: #7a8596; font-size: 11px; text-align: center; }
.slash-menu-foot { flex: none; padding: 6px 10px; border-top: 1px solid var(--line, #e5e9ef); color: #7a8596; font-size: 10px; }
.slash-menu.is-dark .slash-menu-foot { border-color: #334257; color: #98a8bd; }
.tiptap .editor-empty-hint { pointer-events: none; color: var(--muted, #9aa7b8); }
/* 格式工具栏：所有知识库布局统一外观。Seven 以绝对定位悬浮于编辑器上方，
   其余布局内联；位置规则由各布局自行定义，这里只管视觉。 */
.format-bar { display: none; align-items: center; gap: 2px; padding: 4px; border: 1px solid var(--line, #e2e8f0); background: var(--panel, #fff); border-radius: 8px; box-shadow: 0 10px 24px rgba(32, 51, 75, .13); width: max-content; max-width: 100%; }
.knowledge-editor:focus-within .format-bar { display: flex; }
.format-bar > span { display: none; }
.format-bar .format-divider { display: block; width: 1px; height: 14px; margin: 0 3px; background: var(--line, #e2e8f0); }
.format-bar .format-btn { display: inline-flex; align-items: center; justify-content: center; width: 26px; height: 26px; padding: 0; border: 0; border-radius: 5px; background: transparent; color: var(--muted, #7d8b9e); cursor: pointer; }
.format-bar .format-btn svg { width: 15px; height: 15px; fill: none; stroke: currentColor; stroke-width: 2; stroke-linecap: round; stroke-linejoin: round; pointer-events: none; }
.format-bar .format-btn:hover { background: var(--blue-bg, #eef3fa); color: var(--ink, #2f4157); }
.format-bar .format-btn.active { background: var(--blue-bg, #eaf0f9); color: var(--blue, #3564a9); }
.format-bar .format-btn:disabled { opacity: .35; cursor: default; }
.format-bar .format-btn:disabled:hover { background: transparent; }
html.dark .format-bar { background: #1c2633; border-color: #334257; box-shadow: 0 10px 24px rgba(0, 0, 0, .4); }
html.dark .format-bar .format-divider { background: #334257; }
html.dark .format-bar .format-btn { color: #98a8bd; }
html.dark .format-bar .format-btn:hover { background: #2b405d; color: #9abded; }
html.dark .format-bar .format-btn.active { background: #2b405d; color: #9abded; }
/* PlantUML 预览（widget 装饰，非可编辑内容）：默认收起源码，点卡片切换展开。
   node 装饰的 class 由 PM 打在代码块节点视图最外层（.code-block-view wrapper）上 */
.tiptap .plantuml-src-hidden { display: none; }
.tiptap .plantuml-preview { position: relative; display: flex; justify-content: center; margin: 2px 0 10px; padding: 12px 10px; border: 1px solid var(--line, #e5e9ef); border-radius: 8px; background: var(--soft, #f7f9fc); user-select: none; cursor: pointer; }
.tiptap .plantuml-preview img { max-width: 100%; height: auto; display: block; }
.tiptap .plantuml-tools { position: absolute; top: 8px; right: 10px; display: flex; align-items: center; gap: 6px; }
.tiptap .plantuml-hint { font-size: 11px; line-height: 1; padding: 4px 8px; border-radius: 10px; border: 1px solid var(--line, #e5e9ef); background: #ffffffbf; color: var(--muted, #8b97a8); backdrop-filter: blur(2px); }
.tiptap .plantuml-copy { display: inline-flex; align-items: center; gap: 4px; padding: 3.5px 9px; border: 1px solid var(--line, #e5e9ef); border-radius: 6px; background: #fff; color: var(--muted, #6b7a8d); font: 11px/1 inherit; cursor: pointer; }
.tiptap .plantuml-copy:hover { color: var(--blue, #3564a9); border-color: #9dbede; background: var(--blue-bg, #eef3fa); }
.tiptap .plantuml-copy.copied { color: #1c8a4e; border-color: #9ed8b6; background: #eafaf1; }
.tiptap .plantuml-copy svg { width: 12px; height: 12px; fill: none; stroke: currentColor; stroke-width: 2; stroke-linecap: round; stroke-linejoin: round; }
html.dark .tiptap .plantuml-preview { border-color: #334257; background: #eef2f7; box-shadow: 0 0 0 6px #eef2f7; }
html.dark .tiptap .plantuml-hint { background: #f2f5f9; border-color: #d5dde6; color: #6d7c92; }
html.dark .tiptap .plantuml-copy { background: #f5f7fa; border-color: #d5dde6; color: #5d6c7e; }
html.dark .tiptap .plantuml-copy.copied { color: #1c8a4e; }
/* 代码块右下角语言标签（节点视图）+ 弹出菜单；.lang-menu/.lang-item 亦为该菜单复用。
   wrapper 不设 margin：pre 的外边距经 wrapper 折叠，视觉与原 pre 直挂完全一致，
   标签以 wrapper（即 pre 盒）右下为锚点。 */
.tiptap .code-block-view { position: relative; }
.tiptap .code-lang-tag { position: absolute; right: 10px; bottom: 8px; z-index: 2; padding: 3px 8px; border: 1px solid var(--line, #e5e9ef); border-radius: 6px; background: #fffffff2; color: #6b7a8d; font-size: 10px; line-height: 1; cursor: pointer; box-shadow: 0 1px 4px #20334b14; }
.tiptap .code-lang-tag:hover { color: var(--blue, #3564a9); border-color: #9dbede; background: var(--blue-bg, #eef3fa); }
.tiptap[contenteditable='false'] .code-lang-tag { display: none; }
html.dark .tiptap .code-lang-tag { background: #222f40; border-color: #334257; color: #9abded; }
html.dark .tiptap .code-lang-tag:hover { background: #2b405d; border-color: #3d5570; }
/* 菜单自右下角标签下方继续向下生长（top=标签底边再下 4px），不遮挡代码内容；
   会覆盖代码块下方紧邻内容，属浮层正常行为 */
.tiptap .lang-menu { top: calc(100% - 4px); bottom: auto; right: 10px; left: auto; }
.lang-menu { position: absolute; z-index: 1001; display: flex; flex-direction: column; min-width: 148px; padding: 5px; background: var(--panel, #fff); border: 1px solid var(--line, #e5e9ef); border-radius: 8px; box-shadow: 0 10px 32px #20334b1f; color: #243044; }
.lang-menu[hidden] { display: none; }
/* 注意：不依赖任何全局 button 重置（uniapp/kb scope 不一定覆盖），显式自带外观重置 */
/* 输入过滤框：可过滤现有语言，输入原文 Enter 可应用为自定义语言 */
.lang-input { appearance: none; width: 100%; box-sizing: border-box; margin: 0 0 4px; padding: 5px 8px; border: 1px solid var(--line, #e5e9ef); border-radius: 5px; background: #fff; font-size: 12px; line-height: 1.5; color: #243044; }
.lang-input::placeholder { color: #98a5b5; }
.lang-input:focus { outline: none; border-color: #9dbede; }
.lang-item[hidden] { display: none; }
.lang-item { appearance: none; -webkit-appearance: none; display: flex; align-items: center; width: 100%; padding: 6px 9px; border: 0; border-radius: 5px; background: transparent; text-align: left; font-size: 12px; line-height: 1.5; color: #243044; cursor: pointer; flex-shrink: 0 }
.lang-item + .lang-item { margin-top: 2px; }
.lang-item:hover { background: var(--blue-bg, #eef3fa); }
.lang-item.active { background: var(--blue-bg, #eaf0f9); color: var(--blue, #3564a9); font-weight: 600; }
html.dark .lang-menu { background: #1c2633; border-color: #334257; box-shadow: 0 10px 32px #00000059; color: #dce5f1; }
html.dark .lang-input { background: #1c2633; border-color: #334257; color: #dce5f1; }
html.dark .lang-input::placeholder { color: #7f8fa4; }
html.dark .lang-input:focus { border-color: #3d5570; }
html.dark .lang-item { color: #dce5f1; }
html.dark .lang-item:hover { background: #2b405d; }
html.dark .lang-item.active { background: #2b405d; color: #9abded; }
/* CodeBlockLowlight 装饰高亮：色彩与编辑器浅色调和，暗色面板分别覆盖 token 变量 */
.tiptap pre { --ck-keyword:#a2466f; --ck-string:#2f7d4b; --ck-function:#6f5bb5; --ck-constant:#2064b4; --ck-comment:#93a0b1; }
.kb[data-color='dark'] .tiptap pre, html.dark .tiptap pre, .seven[data-color='dark'] .tiptap pre { --ck-keyword:#d78fbd; --ck-string:#8fcf9d; --ck-function:#b9a6f2; --ck-constant:#82b6f6; --ck-comment:#7c8aa0; }
.tiptap pre .hljs-comment, .tiptap pre .hljs-quote, .tiptap pre .hljs-meta, .tiptap pre .hljs-variable, .tiptap pre .hljs-template-variable { color: var(--ck-comment); font-style: italic; }
.tiptap pre .hljs-keyword, .tiptap pre .hljs-selector-tag, .tiptap pre .hljs-tag, .tiptap pre .hljs-name, .tiptap pre .hljs-doctag { color: var(--ck-keyword); font-weight: 600; }
.tiptap pre .hljs-string, .tiptap pre .hljs-regexp, .tiptap pre .hljs-addition, .tiptap pre .hljs-symbol, .tiptap pre .hljs-bullet { color: var(--ck-string); }
.tiptap pre .hljs-title, .tiptap pre .hljs-section, .tiptap pre .hljs-built_in { color: var(--ck-function); }
.tiptap pre .hljs-number, .tiptap pre .hljs-literal, .tiptap pre .hljs-constant, .tiptap pre .hljs-type, .tiptap pre .hljs-attr, .tiptap pre .hljs-attribute, .tiptap pre .hljs-selector-id, .tiptap pre .hljs-selector-class, .tiptap pre .hljs-link, .tiptap pre .hljs-deletion { color: var(--ck-constant); }
.tiptap pre .hljs-strong { font-weight: 600; }
.tiptap pre .hljs-emphasis { font-style: italic; }


</style>
