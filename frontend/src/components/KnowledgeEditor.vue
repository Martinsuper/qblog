<template>
  <div class="knowledge-editor" :class="{ 'is-readonly': readonly }">
    <div v-if="!readonly" class="format-bar" aria-label="文档格式">
      <button type="button" :class="{ active: editor?.isActive('bold') }" title="加粗" @click="editor.chain().focus().toggleBold().run()"><b>B</b></button>
      <button type="button" title="二级标题" @click="editor.chain().focus().toggleHeading({ level: 2 }).run()">H2</button>
      <button type="button" title="无序列表" @click="editor.chain().focus().toggleBulletList().run()">☷</button>
      <button type="button" title="代码块" @click="editor.chain().focus().toggleCodeBlock().run()">&lt;/&gt;</button>
      <button type="button" title="插入表格" @click="editor.chain().focus().insertTable({ rows: 3, cols: 3, withHeaderRow: true }).run()">▦</button>
      <button type="button" title="撤销" :disabled="!editor?.can().undo()" @click="editor.chain().focus().undo().run()">↶</button>
      <button type="button" title="重做" :disabled="!editor?.can().redo()" @click="editor.chain().focus().redo().run()">↷</button>
      <span>点击正文直接编辑 · 自动保存</span>
    </div>
    <editor-content :editor="editor" />
  </div>
</template>
<script setup>
import { watch, onBeforeUnmount } from 'vue'
import { useEditor, EditorContent } from '@tiptap/vue-3'
import StarterKit from '@tiptap/starter-kit'
import { Markdown } from '@tiptap/markdown'
import { TableKit } from '@tiptap/extension-table'
const props = defineProps({ modelValue: { type: String, default: '' }, readonly: Boolean, quietToolbar: Boolean })
const emit = defineEmits(['update:modelValue', 'headings'])
let lastEmitted = props.modelValue
function reportHeadings(instance) {
  const result = []
  instance.state.doc.descendants(node => { if (node.type.name === 'heading') result.push({ level: node.attrs.level, text: node.textContent }) })
  emit('headings', result)
}
const editor = useEditor({
  extensions: [StarterKit.configure({ link: { openOnClick: false }, heading: { levels: [1, 2, 3] } }), TableKit, Markdown],
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
