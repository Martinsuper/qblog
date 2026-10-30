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
          @click="editor.chain().focus().toggleBold().run()"
        >
          <KnowledgeIcon name="bold" />
        </button>
        <button
          type="button"
          class="format-btn"
          title="二级标题"
          @click="editor.chain().focus().toggleHeading({ level: 2 }).run()"
        >
          <KnowledgeIcon name="heading" />
        </button>
        <span class="format-divider" />
        <button
          type="button"
          class="format-btn"
          title="无序列表"
          @click="editor.chain().focus().toggleBulletList().run()"
        >
          <KnowledgeIcon name="list" />
        </button>
        <button
          type="button"
          class="format-btn"
          title="代码块"
          @click="editor.chain().focus().toggleCodeBlock().run()"
        >
          <KnowledgeIcon name="code" />
        </button>
        <button
          type="button"
          class="format-btn"
          title="插入表格"
          @click="editor.chain().focus().insertTable({ rows: 3, cols: 3, withHeaderRow: true }).run()"
        >
          <KnowledgeIcon name="table" />
        </button>
        <span class="format-divider" />
        <button
          type="button"
          class="format-btn"
          title="撤销"
          :disabled="!editor?.can().undo()"
          @click="editor.chain().focus().undo().run()"
        >
          <KnowledgeIcon name="undo" />
        </button>
        <button
          type="button"
          class="format-btn"
          title="重做"
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
import { TableKit } from '@tiptap/extension-table'
import { TaskItem, TaskList } from '@tiptap/extension-list'
import KnowledgeIcon from './KnowledgeIcon.vue'
import { SlashCommand } from '../utils/slashCommands'
import { EditorEmptyHint } from '../utils/editorEmptyHint'
const props = defineProps({ modelValue: { type: String, default: '' }, readonly: Boolean, quietToolbar: Boolean, toolbarTarget: { type: Object, default: null } })
const emit = defineEmits(['update:modelValue', 'headings'])
let lastEmitted = props.modelValue
function reportHeadings(instance) {
  const result = []
  instance.state.doc.descendants(node => { if (node.type.name === 'heading') result.push({ level: node.attrs.level, text: node.textContent }) })
  emit('headings', result)
}
const editor = useEditor({
  extensions: [StarterKit.configure({ link: { openOnClick: false }, heading: { levels: [1, 2, 3] } }), TaskList, TaskItem.configure({ nested: true }), TableKit, Markdown, SlashCommand, EditorEmptyHint],
  content: props.modelValue,
  contentType: 'markdown',
  editable: !props.readonly,
  editorProps: { attributes: { 'aria-label': '笔记正文', spellcheck: 'false' } },
  onCreate: ({ editor: instance }) => reportHeadings(instance),
  onUpdate: ({ editor: instance }) => { reportHeadings(instance); lastEmitted = instance.getMarkdown(); emit('update:modelValue', lastEmitted) }
})
watch(() => props.modelValue, value => {
  if (editor.value && value !== lastEmitted) {
    lastEmitted = value
    editor.value.commands.setContent(value, { contentType: 'markdown', emitUpdate: false }); reportHeadings(editor.value)
  }
})
watch(() => props.readonly, value => editor.value?.setEditable(!value))
onBeforeUnmount(() => editor.value?.destroy())
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
</style>
