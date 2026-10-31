import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { runInNewContext } from 'node:vm'

const page = readFileSync(new URL('../src/views/Knowledge/Index.vue', import.meta.url), 'utf8')
const workspace = readFileSync(new URL('../src/components/KnowledgeSeven.vue', import.meta.url), 'utf8')
function action(name, next, context) {
  const start = page.indexOf(`async function ${name}(`)
  const end = page.indexOf(`\n${next}`, start)
  assert.ok(start >= 0 && end > start)
  return runInNewContext(`${page.slice(start, end)}\n${name}`, context)
}
function deletionContext(confirm = async () => {}) {
  const calls = [], state = {
    noteError: { value: false }, active: { value: { id: 7, title: '测试笔记', version: 3 } }, loadingNote: { value: false }, deletingNote: { value: false },
    ElMessageBox: { confirm }, safeLeave: async () => { state.active.value.version = 4; return true },
    clearTimeout() {}, saveTimer: null,
    api: { removeNote: async (...args) => calls.push(args) }, autosave: { dispose() {} },
    scrollPositions: new Map([[7, 30]]), draft: { title: '测试笔记', content: '正文' }, saveStatus: { value: '已保存' },
    saveFailed: { value: false }, conflicted: { value: false }, readerOpen: { value: true }, propertiesOpen: { value: true }, mobileView: { value: 'reader' },
    route: { query: { note: 7, filter: 'keep' } }, router: { replace: async value => { state.navigation = value } },
    ElMessage: { success() {}, warning() {} }, loadNotes: async () => {}, loadFolders: async () => {}
  }
  return { state, calls, remove: action('deleteCurrent', 'function downloadDraft', state) }
}

test('workspace offers creation from directory context menus and groups note deletion in the action menu', () => {
  const sidebar = workspace.slice(workspace.indexOf('class="folder-tree"'), workspace.indexOf('id="knowledge-list-pane"'))
  assert.match(sidebar, /@contextmenu\.self\.prevent="openRootMenu"/)
  assert.doesNotMatch(sidebar, /class="new-folder"/)
  assert.match(workspace, /contextMenu\.type === 'root'[\s\S]*?新建笔记[\s\S]*?新建目录/)
  assert.match(workspace, /contextMenu\.type === 'folder'[\s\S]*?新建笔记[\s\S]*?新建子目录/)
  const toolbar = workspace.slice(workspace.indexOf('class="reader-toolbar"'), workspace.indexOf('ref="readerElement"'))
  assert.match(toolbar, /aria-label="笔记操作"/)
  assert.match(toolbar, /class="note-actions"[\s\S]*?aria-label="删除当前笔记"[\s\S]*?\$emit\('delete'\)/)
  assert.doesNotMatch(workspace, /class="demo-bar"|方案\s*G/)
  assert.doesNotMatch(page, /\d{2} · (留白|纸间|笔记|目录)/)
})

test('canceling deletion does not save, delete or clear the active note', async () => {
  const { state, calls, remove } = deletionContext(async () => { throw 'cancel' })
  state.safeLeave = async () => { assert.fail('cancellation must not trigger save') }
  await remove()
  assert.equal(calls.length, 0)
  assert.equal(state.active.value.id, 7)
  assert.equal(state.deletingNote.value, false)
})

test('confirmed deletion uses the latest saved version and clears reader while preserving other query parameters', async () => {
  const { state, calls, remove } = deletionContext()
  await remove()
  assert.deepEqual(calls, [[7, 4]])
  assert.equal(state.active.value, null)
  assert.equal(state.draft.content, '')
  assert.equal(state.scrollPositions.has(7), false)
  assert.equal(state.navigation.query.filter, 'keep')
  assert.equal('note' in state.navigation.query, false)
  assert.equal(state.mobileView.value, 'list')
  assert.equal(state.deletingNote.value, false)
})

test('failed save or delete preserves the current note and unlocks retry', async () => {
  for (const failure of ['save', 'delete']) {
    const { state, calls, remove } = deletionContext()
    if (failure === 'save') state.safeLeave = async () => false
    else state.api.removeNote = async () => { throw new Error('409') }
    await remove()
    assert.equal(calls.length, 0)
    assert.equal(state.active.value.id, 7)
    assert.equal(state.draft.content, '正文')
    assert.equal(state.deletingNote.value, false)
  }
})

test('folder creation validates blank names, trims input and prevents duplicate submissions', async () => {
  const calls = [], state = {
    folderSubmitting: { value: false }, folderNameError: { value: '' }, folderEditing: { value: null },
    folderForm: { name: '  ', parentId: 2 }, folderDialog: { value: true }, collapsed: new Set([2]),
    api: { createFolder: async value => calls.push(value) }, ElMessage: { success() {} }, loadFolders: async () => {}
  }
  const submit = action('submitFolder', 'async function deleteFolder', state)
  await submit()
  assert.equal(calls.length, 0)
  assert.equal(state.folderNameError.value, '请输入目录名称')
  state.folderForm.name = ' 新目录 '
  const first = submit()
  await submit()
  await first
  assert.equal(calls.length, 1)
  assert.equal(calls[0].name, '新目录')
  assert.equal(calls[0].parentId, 2)
  assert.equal(state.collapsed.has(2), false)
  assert.equal(state.folderDialog.value, false)
  assert.equal(state.folderSubmitting.value, false)
})

test('failed folder creation preserves form for retry', async () => {
  const state = {
    folderSubmitting: { value: false }, folderNameError: { value: '' }, folderEditing: { value: null },
    folderForm: { name: '新目录', parentId: null }, folderDialog: { value: true },
    api: { createFolder: async () => { throw new Error('network') } }
  }
  await action('submitFolder', 'async function deleteFolder', state)()
  assert.equal(state.folderDialog.value, true)
  assert.equal(state.folderForm.name, '新目录')
  assert.equal(state.folderSubmitting.value, false)
})
