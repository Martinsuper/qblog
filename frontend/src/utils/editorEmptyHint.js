import { Extension } from '@tiptap/core'
import { Plugin, PluginKey } from '@tiptap/pm/state'
import { Decoration, DecorationSet } from '@tiptap/pm/view'

export const EditorEmptyHint = Extension.create({
  name: 'editorEmptyHint',
  addProseMirrorPlugins() {
    const extension = this
    return [new Plugin({
      key: new PluginKey('editorEmptyHint'),
      props: {
        decorations(state) {
          const first = state.doc.firstChild
          const isEmptyDoc = state.doc.childCount === 1 && first?.type.name === 'paragraph' && first.content.size === 0
          if (!isEmptyDoc || !extension.editor.isEditable) return DecorationSet.empty
          return DecorationSet.create(state.doc, [Decoration.widget(1, () => {
            const span = document.createElement('span')
            span.className = 'editor-empty-hint'
            span.textContent = '输入 / 唤起命令，或直接开始书写'
            return span
          }, { side: 100 })])
        }
      }
    })]
  }
})
