import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'

const workspace = readFileSync(new URL('../src/components/KnowledgeSeven.vue', import.meta.url), 'utf8')

test('workspace removes placeholder features and duplicated explanatory chrome', () => {
  assert.doesNotMatch(workspace, /索引未配置|AI 服务尚未配置|assistantOpen|让知识有序可寻|暂无摘要|个人工作区/)
  assert.match(workspace, /const infoOpen = ref\(false\)/)
  assert.match(workspace, /v-if="headings.length && \(infoOpen \|\| narrowExpanded\)"/)
})

test('active filters remain removable independently of the filter panel', () => {
  const panel = workspace.slice(workspace.indexOf('v-if="filtersOpen"'), workspace.indexOf('class="note-list"'))
  assert.match(panel, /class="active-filters"/)
  for (const event of ['search', 'list-search', 'tag', 'source']) {
    assert.ok(panel.includes(`$emit('${event}', '')`))
  }
})

test('critical save feedback and retry actions are retained', () => {
  assert.match(workspace, /v-if="conflicted"[\s\S]*?role="alert"[\s\S]*?\$emit\('download-draft'\)[\s\S]*?\$emit\('reload-conflict'\)/)
  assert.match(workspace, /v-else-if="saveFailed"[\s\S]*?role="alert"[\s\S]*?\$emit\('save'\)/)
  assert.match(workspace, /role="status"[\s\S]*?aria-live="polite"/)
})
