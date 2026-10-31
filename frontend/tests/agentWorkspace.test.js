import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'

const read = path => readFileSync(new URL(`../src/${path}`, import.meta.url), 'utf8')

test('智能体菜单与总览一样在工作区内切换', () => {
  const seven = read('components/KnowledgeSeven.vue')
  assert.match(seven, /@click="\$emit\('view', 'agent'\)"/)
  assert.doesNotMatch(seven, /\$router.push\('\/assistant'\)/)
  assert.match(seven, /:aria-current="agentVisible \? 'page' : undefined"/)
  assert.match(seven, /<AgentWorkspace[\s\S]*?v-if="agentVisible"[\s\S]*?embedded/)
  assert.match(seven, /agent: '智能体'/)
  assert.match(seven, /!overviewVisible && !agentVisible/)
  const workspace = read('views/Knowledge/Index.vue')
  assert.doesNotMatch(workspace, /router.push\('\/assistant'\)/)
  assert.match(workspace, /@click="onView\('agent'\)"/)
})

test('智能体支持嵌入且保留独立访问与会话功能', () => {
  const agent = read('views/Agent/Index.vue')
  assert.match(agent, /embedded: Boolean/)
  assert.match(agent, /v-if="!embedded"/)
  assert.match(agent, /'is-embedded': embedded/)
  for (const feature of ['store.ordered', 'store.select(conv.id)', 'store.newConversation()', 'AgentChat', '/settings/tokens']) {
    assert.ok(agent.includes(feature), `缺少功能 ${feature}`)
  }
  assert.match(read('router/index.js'), /path: '\/assistant'/)
})
