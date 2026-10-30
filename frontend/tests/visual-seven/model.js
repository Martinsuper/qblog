// Visual QA state only. Nothing is persisted or sent to a server.
import { reactive, computed } from 'vue'

const copy = value => JSON.parse(JSON.stringify(value))

export function createVisualModel(fixture) {
  const folders = copy(fixture.folders)
  const flat = []
  function visit(items, parentPath = '', depth = 0) {
    for (const folder of items) {
      const path = parentPath ? `${parentPath} / ${folder.name}` : folder.name
      flat.push({ ...folder, path, depth, hasChildren: Boolean(folder.children?.length) })
      visit(folder.children || [], path, depth + 1)
    }
  }
  visit(folders)
  const state = reactive({
    notes: copy(fixture.notes), activeId: fixture.notes[0]?.id ?? null,
    draft: copy(fixture.notes[0] || { title: '', content: '', tags: [], favorite: false }),
    scope: 'all', search: '', listSearch: '', tag: '', source: '', sort: 'updated',
    mobileView: 'reader', page: 1, pageSize: 50, collapsed: new Set()
  })
  const active = computed(() => state.notes.find(note => note.id === state.activeId) || null)
  const visibleFolders = computed(() => flat.filter(folder => {
    let parent = folder.parentId
    while (parent != null) {
      if (state.collapsed.has(parent)) return false
      parent = flat.find(item => item.id === parent)?.parentId
    }
    return true
  }))
  const folderName = id => flat.find(folder => folder.id === id)?.path || '收件箱'
  const counts = computed(() => ({
    all: state.notes.length,
    inbox: state.notes.filter(note => note.folderId == null).length,
    favorites: state.notes.filter(note => note.favorite).length
  }))
  const folderCounts = computed(() => flat.map(folder => ({ ...folder, count: state.notes.filter(note => {
    const path = flat.find(item => item.id === note.folderId)?.path
    return path === folder.path || path?.startsWith(`${folder.path} / `)
  }).length })))
  const visibleWithCounts = computed(() => folderCounts.value.filter(folder => visibleFolders.value.some(item => item.id === folder.id)))
  const scopeTitle = computed(() => ({ all: '全部笔记', inbox: '收件箱', favorites: '我的收藏' })[state.scope] || folderName(state.scope))
  const filtered = computed(() => {
    const terms = [state.search, state.listSearch].map(value => value.trim().toLowerCase()).filter(Boolean)
    const selectedFolder = flat.find(folder => String(folder.id) === String(state.scope))
    return state.notes.filter(note => {
      if (state.scope === 'inbox' && note.folderId != null) return false
      if (state.scope === 'favorites' && !note.favorite) return false
      if (selectedFolder) {
        const path = flat.find(folder => folder.id === note.folderId)?.path
        if (path !== selectedFolder.path && !path?.startsWith(`${selectedFolder.path} / `)) return false
      }
      if (state.tag && !note.tags?.includes(state.tag)) return false
      if (state.source && note.source !== state.source) return false
      const text = [note.title, note.content, note.summary, ...(note.tags || [])].join(' ').toLowerCase()
      return terms.every(term => text.includes(term))
    }).slice().sort((a, b) => state.sort === 'title'
      ? a.title.localeCompare(b.title, 'zh-CN') || a.id - b.id
      : String(b.updateTime || '').localeCompare(String(a.updateTime || '')) || a.id - b.id)
  })
  const pageNotes = computed(() => filtered.value.slice((state.page - 1) * state.pageSize, state.page * state.pageSize))
  function setFilter(key, value) { state[key] = value; state.page = 1 }
  function openNote(id) {
    const note = state.notes.find(item => item.id === id)
    if (!note) return
    state.activeId = id
    state.draft = copy(note)
    state.mobileView = 'reader'
  }
  function dirty() {
    if (active.value) Object.assign(active.value, copy(state.draft))
  }
  function favorite() { state.draft.favorite = !state.draft.favorite; dirty() }
  function newNote() {
    const id = Math.max(0, ...state.notes.map(note => note.id)) + 1
    const now = new Date().toISOString()
    const note = { id, title: '无标题笔记', content: '', summary: '', folderId: flat.some(f => f.id === state.scope) ? state.scope : null, tags: [], favorite: false, source: 'manual', sourceTool: '', version: 0, createTime: now, updateTime: now, indexStatus: 'not_configured' }
    state.notes.unshift(note)
    openNote(id)
  }
  function removeNote() {
    const index = state.notes.findIndex(note => note.id === state.activeId)
    if (index < 0) return
    state.notes.splice(index, 1)
    state.activeId = null
    state.draft = { title: '', content: '', tags: [], favorite: false }
  }
  function collapseAll() {
    const parents = flat.filter(folder => folder.hasChildren).map(folder => folder.id)
    if (parents.every(id => state.collapsed.has(id))) state.collapsed.clear()
    else parents.forEach(id => state.collapsed.add(id))
  }
  return { state, folders, flat, active, counts, folderCounts, visibleWithCounts, folderName, scopeTitle, filtered, pageNotes, setFilter, openNote, dirty, favorite, newNote, removeNote, collapseAll }
}
