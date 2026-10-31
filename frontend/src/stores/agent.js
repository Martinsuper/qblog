import { defineStore } from 'pinia'
import { streamAgentChat } from '@/api/agent'

/**
 * 站内智能体会话状态。
 * 历史仅存浏览器 localStorage（不存在服务端），断流/刷新不丢会话。
 */
const STORAGE_KEY = 'qblog_agent_conversations'
const MAX_CONVERSATIONS = 30

function uid() { return `${Date.now().toString(36)}${Math.random().toString(36).slice(2, 8)}` }

function load() {
  try {
    const raw = JSON.parse(localStorage.getItem(STORAGE_KEY) || '[]')
    return Array.isArray(raw) ? raw.filter(c => c && c.id && Array.isArray(c.messages)) : []
  } catch { return [] }
}

export const useAgentStore = defineStore('agent', {
  state: () => ({
    conversations: load(),
    currentId: null,
    drawerOpen: false,
    sending: false,
    error: ''
  }),

  getters: {
    current: state => state.conversations.find(c => c.id === state.currentId) || null,
    ordered: state => [...state.conversations].sort((a, b) => (b.updatedAt || 0) - (a.updatedAt || 0))
  },

  actions: {
    /* ---------- 会话管理 ---------- */

    newConversation() {
      const conv = { id: uid(), title: '新会话', createdAt: Date.now(), updatedAt: Date.now(), messages: [] }
      this.conversations.unshift(conv)
      this.currentId = conv.id
      this.error = ''
      this.prune()
      this.persist()
      return conv
    },

    select(id) {
      if (this.sending) return
      this.currentId = id
      this.error = ''
    },

    remove(id) {
      if (this.sending) return
      this.conversations = this.conversations.filter(c => c.id !== id)
      if (this.currentId === id) {
        const first = this.ordered[0]
        if (first) this.select(first.id)
        else this.newConversation()
      } else this.persist()
    },

    openDrawer() {
      if (!this.currentId) this.newConversation()
      this.drawerOpen = true
    },

    closeDrawer() { this.drawerOpen = false },

    /* ---------- 对话 ---------- */

    async send(question) {
      const text = (question || '').trim()
      if (!text || this.sending) return
      if (!this.current) this.newConversation()
      const conv = this.current

      conv.title = conv.messages.length ? conv.title : text.slice(0, 24)
      conv.messages = [
        ...conv.messages,
        { role: 'user', content: text, time: Date.now() },
        { role: 'assistant', content: '', tools: [], time: Date.now() }
      ]
      this.touch(conv)
      this.sending = true
      this.error = ''

      const controller = new AbortController()
      this._abort = controller
      const answer = conv.messages[conv.messages.length - 1]
      let schedule = null

      try {
        const history = conv.messages.slice(0, -2).map(m => ({ role: m.role, content: m.content }))
        await streamAgentChat({
          history,
          question: text,
          signal: controller.signal,
          onToken: chunk => { answer.content += chunk; this.throttledPersist() },
          onTool: ev => {
            if (ev.phase === 'begin') answer.tools.push({ tool: ev.tool, summary: ev.summary, state: 'running' })
            else {
              const step = [...answer.tools].reverse().find(t => t.tool === ev.tool && t.state === 'running')
              if (step) { step.state = ev.ok ? 'done' : 'failed' }
            }
            this.throttledPersist()
          }
        })
      } catch (e) {
        if (e?.name === 'AbortError') {
          if (!answer.content) answer.content = '（已停止）'
        } else {
          const message = e.message || '会话失败'
          this.error = message
          if (answer.content) answer.content += `\n\n> ⚠️ ${message}`
          else answer.content = `> ⚠️ ${message}`
        }
      } finally {
        this.sending = false
        this._abort = null
        this.touch(conv)
        this.persist()
      }
    },

    stop() {
      this._abort?.abort()
    },

    throttledPersist() {
      if (this._timer) return
      this._timer = setTimeout(() => { this._timer = null; this.persist() }, 800)
    },

    touch(conv) { conv.updatedAt = Date.now() },

    prune() {
      if (this.conversations.length > MAX_CONVERSATIONS) this.conversations.length = MAX_CONVERSATIONS
    },

    persist() {
      try { localStorage.setItem(STORAGE_KEY, JSON.stringify(this.conversations)) } catch { /* 配额满则忽略 */ }
    }
  }
})
