import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { dashboardStats, inboxQueue, recentNotes, tagCounts, folderDistribution } from '../src/utils/knowledgeDashboard.js'

const note = (id, { folderId = 1, favorite = false, source = 'manual', tags = [], updateTime = '2026-10-01T10:00:00' } = {}) =>
  ({ id, title: `笔记${id}`, folderId, favorite, source, tags, updateTime })

test('统计卡计数：总数、收藏、MCP 与待归档', () => {
  const notes = [
    note(1, { favorite: true, source: 'mcp' }),
    note(2, { folderId: null, source: 'mcp' }),
    note(3, { folderId: null }),
    note(4)
  ]
  assert.deepEqual(dashboardStats(notes), { total: 4, favorites: 1, mcp: 2, inbox: 2 })
  assert.deepEqual(dashboardStats([]), { total: 0, favorites: 0, mcp: 0, inbox: 0 })
  assert.deepEqual(dashboardStats(), { total: 0, favorites: 0, mcp: 0, inbox: 0 })
})

test('待归档队列只含未归档笔记并按更新时间倒序', () => {
  const notes = [
    note(1, { folderId: 2, updateTime: '2026-10-03T09:00:00' }),
    note(2, { folderId: null, updateTime: '2026-10-01T09:00:00' }),
    note(3, { folderId: null, updateTime: '2026-10-02T09:00:00' })
  ]
  assert.deepEqual(inboxQueue(notes).map(n => n.id), [3, 2])
  assert.equal(inboxQueue(notes, 1).length, 1)
  assert.deepEqual(inboxQueue([]), [])
})

test('最近更新按更新时间倒序并可截断', () => {
  const notes = [note(1, { updateTime: '2026-10-01T09:00:00' }), note(2, { updateTime: '2026-10-03T09:00:00' }), note(3, { updateTime: '2026-10-02T09:00:00' })]
  assert.deepEqual(recentNotes(notes).map(n => n.id), [2, 3, 1])
  assert.deepEqual(recentNotes(notes, 2).map(n => n.id), [2, 3])
  // 不修改原数组
  assert.equal(notes[0].id, 1)
})

test('标签聚合计数并按数量与名称排序截断', () => {
  const notes = [
    note(1, { tags: ['检索', '性能'] }),
    note(2, { tags: ['检索'] }),
    note(3, { tags: ['API', '性能'] }),
    note(4, { tags: ['检索'] })
  ]
  const counts = tagCounts(notes)
  assert.deepEqual(counts[0], { name: '检索', count: 3 })
  assert.deepEqual(counts[1], { name: '性能', count: 2 })
  assert.deepEqual(counts[2], { name: 'API', count: 1 })
  assert.equal(tagCounts(notes, 2).length, 2)
  assert.deepEqual(tagCounts([]), [])
  // 空标签被忽略
  assert.deepEqual(tagCounts([note(9, { tags: [''] })]), [])
})

test('目录分布按子树递归计数并固定包含收件箱一行', () => {
  const folders = [
    { id: 1, name: '知识库指南', children: [{ id: 11, name: '规范', children: [] }] },
    { id: 2, name: '工程与开发', children: [] }
  ]
  const notes = [
    note(1, { folderId: 1 }),
    note(2, { folderId: 11 }),
    note(3, { folderId: 2 }),
    note(4, { folderId: null })
  ]
  assert.deepEqual(folderDistribution(folders, notes), [
    { name: '知识库指南', count: 2 },
    { name: '工程与开发', count: 1 },
    { name: '收件箱', count: 1 }
  ])
  assert.deepEqual(folderDistribution([], []), [{ name: '收件箱', count: 0 }])
})

test('KnowledgeSeven 模板接入总览入口与仪表盘', () => {
  const source = readFileSync(new URL('../src/components/KnowledgeSeven.vue', import.meta.url), 'utf8')
  assert.ok(source.includes('nav-overview'), '侧栏缺少总览入口')
  assert.ok(source.includes('<KnowledgeDashboard'), '正文区缺少仪表盘组件')
  assert.ok(source.includes("overview: '总览'"), '移动端导航缺少总览标签')
  assert.ok(source.includes('overview-retry'), '缺少总览重试事件')
})
