import test from 'node:test'
import assert from 'node:assert/strict'
import { noteListFeedback } from '../src/utils/knowledgeFeedback.js'
import { readFileSync } from 'node:fs'

test('empty states distinguish first use, inbox, favorites and folders', () => {
  assert.equal(noteListFeedback({ scope: 'all' }).title, '写下第一篇笔记')
  assert.equal(noteListFeedback({ scope: 'inbox' }).title, '收件箱已清空')
  assert.equal(noteListFeedback({ scope: 'favorites' }).action, 'all')
  assert.equal(noteListFeedback({ scope: 8 }).title, '这个目录还没有笔记')
})

test('any active filter takes priority and offers clearing instead of misleading creation', () => {
  for (const key of ['search', 'listSearch', 'tag', 'source']) {
    assert.equal(noteListFeedback({ scope: 'favorites', [key]: 'value' }).action, 'clear')
    assert.equal(noteListFeedback({ scope: 'all', [key]: '  ' }).action, 'create')
  }
})

test('reader fetch failure is handled without discarding the existing draft', () => {
  const page = readFileSync(new URL('../src/views/Knowledge/Index.vue', import.meta.url), 'utf8')
  const failure = page.slice(page.indexOf('catch { if (generation === noteGeneration)'), page.indexOf('async function newNote'))
  assert.match(failure, /noteError.value = true/)
  assert.match(failure, /failedNoteId.value = id/)
  assert.doesNotMatch(failure, /active.value = null|Object.assign\(draft/)
})
