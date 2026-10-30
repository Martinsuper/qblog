import test from 'node:test'
import assert from 'node:assert/strict'
import { createVisualModel } from './visual-seven/model.js'

const fixture = {
  folders: [{ id: 1, name: '工程', parentId: null, children: [{ id: 2, name: '前端', parentId: 1, children: [] }] }],
  notes: [
    { id: 1, title: 'Beta', content: 'Vue 搜索内容', summary: '', folderId: 2, tags: ['Vue'], favorite: true, source: 'mcp', updateTime: '2026-01-01' },
    { id: 2, title: 'Alpha', content: '其他', summary: '', folderId: null, tags: ['Vue'], favorite: false, source: 'manual', updateTime: '2026-02-01' }
  ]
}

test('search, nested folder, tag and source filters compose and reset pagination', () => {
  const m = createVisualModel(fixture)
  m.state.page = 2
  m.setFilter('scope', 1)
  m.setFilter('search', ' vue ')
  m.setFilter('listSearch', '搜索内容')
  m.setFilter('tag', 'Vue')
  m.setFilter('source', 'mcp')
  assert.equal(m.state.page, 1)
  assert.deepEqual(m.filtered.value.map(n => n.id), [1])
  m.setFilter('source', 'manual')
  assert.equal(m.filtered.value.length, 0)
  m.setFilter('scope', 'inbox')
  m.setFilter('search', '')
  m.setFilter('listSearch', '')
  assert.deepEqual(m.filtered.value.map(n => n.id), [2])
})

test('draft edits, selection and favorites stay isolated in memory', () => {
  const m = createVisualModel(fixture)
  m.state.draft.title = 'Edited'
  m.state.draft.tags.push('新标签')
  m.dirty()
  m.openNote(2)
  m.openNote(1)
  assert.equal(m.state.draft.title, 'Edited')
  assert.deepEqual(m.state.draft.tags, ['Vue', '新标签'])
  m.favorite()
  assert.equal(m.counts.value.favorites, 0)
  assert.equal(m.state.mobileView, 'reader')
  assert.equal(fixture.notes[0].title, 'Beta')
  assert.deepEqual(fixture.notes[0].tags, ['Vue'])
  assert.equal(createVisualModel(fixture).state.draft.title, 'Beta')
})

test('collapse, sorting, creation and deletion update fixture state', () => {
  const m = createVisualModel(fixture)
  m.collapseAll()
  assert.deepEqual(m.visibleWithCounts.value.map(f => f.id), [1])
  m.collapseAll()
  assert.equal(m.visibleWithCounts.value.length, 2)
  m.state.sort = 'title'
  assert.deepEqual(m.filtered.value.map(n => n.id), [2, 1])
  m.newNote()
  assert.equal(m.counts.value.all, 3)
  assert.equal(m.counts.value.inbox, 2)
  m.removeNote()
  assert.equal(m.counts.value.all, 2)
  assert.equal(m.active.value, null)
})
