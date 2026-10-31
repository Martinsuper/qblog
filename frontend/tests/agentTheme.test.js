import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'

const read = path => readFileSync(new URL(`../src/${path}`, import.meta.url), 'utf8')

test('智能体使用工作区主题变量而非独立渐变与全局深色开关', () => {
  const theme = read('styles/agent-theme.css')
  for (const token of ['--panel', '--kb-panel', '--ink', '--kb-ink', '--blue', '--kb-accent', '--line', '--kb-line']) {
    assert.ok(theme.includes(token), `缺少主题映射 ${token}`)
  }
  for (const path of ['components/agent/AgentChat.vue', 'views/Agent/Index.vue']) {
    const source = read(path)
    assert.ok(source.includes('agent-theme.css'))
    assert.doesNotMatch(source, /linear-gradient|html\.dark|#6366f1|#22d3ee/)
  }
})

test('欢迎区与工具栏使用站点图标并提供输入可访问标签', () => {
  const chat = read('components/agent/AgentChat.vue')
  assert.match(chat, /<KnowledgeIcon :name="s.icon"/)
  assert.match(chat, /aria-label="消息内容"/)
  assert.match(chat, /role="alert"/)
  assert.match(chat, /:disabled="store.sending"/)
  assert.doesNotMatch(chat, /:style="s.bg"/)
  assert.match(read('views/Agent/Index.vue'), /<KnowledgeIcon name="plus"/)
})
