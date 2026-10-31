<template>
  <div
    class="agent-page"
    :class="{ 'is-embedded': embedded }"
  >
    <header class="agent-topbar">
      <router-link
        v-if="!embedded"
        to="/"
        class="tb-back"
      >
        ‹ 书山有路
      </router-link>
      <span class="tb-title">{{ pageTitle }}</span>
      <span
        v-if="pageSubtitle"
        class="tb-sub"
      >{{ pageSubtitle }}</span>
      <span class="tb-sp" />

      <el-popover
        :teleported="false"
        placement="bottom-end"
        :width="280"
        trigger="click"
      >
        <template #reference>
          <button
            class="tb-ghost"
            :disabled="store.sending"
          >
            <KnowledgeIcon name="layers" />历史会话
          </button>
        </template>
        <div class="history-panel">
          <template v-if="store.ordered.length">
            <button
              v-for="conv in store.ordered"
              :key="conv.id"
              class="history-item"
              :class="{ active: conv.id === store.currentId }"
              :disabled="store.sending"
              @click="store.select(conv.id)"
            >
              <span class="history-title">{{ conv.title || '新会话' }}</span>
              <time>{{ shortTime(conv.updatedAt) }}</time>
            </button>
          </template>
          <div
            v-else
            class="history-empty"
          >
            暂无历史会话
          </div>
        </div>
      </el-popover>

      <button
        class="tb-ghost primary"
        :disabled="store.sending"
        @click="startNew"
      >
        <KnowledgeIcon name="plus" />新会话
      </button>
      <button
        class="tb-ghost"
        @click="router.push('/settings/tokens')"
      >
        <KnowledgeIcon name="panel" />模型配置
      </button>
    </header>

    <div class="agent-page-body">
      <AgentChat
        :show-hero="true"
        placeholder="询问知识库内容，或描述你想整理的笔记…"
      />
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAgentStore } from '@/stores/agent'
import AgentChat from '@/components/agent/AgentChat.vue'
import KnowledgeIcon from '@/components/KnowledgeIcon.vue'
import '@/styles/agent-theme.css'

const store = useAgentStore()
const router = useRouter()
defineProps({ embedded: Boolean })

const pageTitle = computed(() => {
  const conv = store.current
  if (!conv || !conv.messages.length) return '站点智能体'
  return conv.title || '站点智能体'
})
const pageSubtitle = computed(() => {
  const conv = store.current
  if (!conv || !conv.messages.length) return ''
  return store.sending ? '正在回答…' : new Date(conv.updatedAt).toLocaleString('zh-CN', { month: 'numeric', day: 'numeric', hour: '2-digit', minute: '2-digit' })
})

onMounted(() => {
  if (!store.currentId) store.newConversation()
  store.drawerOpen = false
})

function startNew() {
  if (store.sending) return
  store.newConversation()
}

function shortTime(value) {
  if (!value) return ''
  const date = new Date(value)
  const now = new Date()
  const sameDay = date.toDateString() === now.toDateString()
  return sameDay
    ? date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
    : date.toLocaleDateString('zh-CN', { month: 'numeric', day: 'numeric' })
}
</script>

<style scoped>
.agent-page{height:100dvh;min-width:0;display:flex;flex-direction:column;background:var(--agent-panel)}

/* ---- 工作区工具栏 ---- */
.agent-page .agent-topbar{flex:none;display:flex;align-items:center;flex-wrap:wrap;gap:8px;padding:10px 20px;height:auto;min-height:52px;border-bottom:1px solid var(--agent-line);background:var(--agent-panel);z-index:10}
.tb-back{color:var(--agent-muted);font-size:12px;text-decoration:none;white-space:nowrap;margin-right:8px}
.tb-back:hover{color:var(--agent-accent)}
.tb-title{font-weight:600;font-size:13px;max-width:min(40vw,320px);overflow:hidden;text-overflow:ellipsis;white-space:nowrap}
.tb-sub{color:var(--agent-muted);font-size:11px;white-space:nowrap}
.tb-sp{flex:1}
.tb-ghost{display:inline-flex;align-items:center;justify-content:center;gap:6px;border:1px solid var(--agent-line);background:var(--agent-panel);color:var(--agent-ink);border-radius:5px;padding:5px 10px;min-height:31px;font:inherit;font-size:12px;cursor:pointer;white-space:nowrap}
.tb-ghost svg{width:14px;height:14px;fill:none;stroke:currentColor;stroke-width:1.6;stroke-linecap:round;stroke-linejoin:round}
.tb-ghost:hover{background:var(--agent-soft);border-color:var(--agent-accent)}
.tb-ghost:disabled{opacity:.5;cursor:not-allowed}
.tb-ghost.primary{background:var(--agent-accent);border-color:var(--agent-accent);color:var(--agent-on-accent)}
.tb-ghost:focus-visible,.history-item:focus-visible{outline:2px solid var(--agent-accent);outline-offset:2px}

.agent-page-body{flex:1;min-height:0;display:flex}
.agent-page-body > *{flex:1;min-width:0}

/* ---- 历史会话 popover ---- */
.history-panel{display:flex;flex-direction:column;max-height:320px;overflow:auto;scrollbar-width:thin;background:var(--agent-panel);color:var(--agent-ink)}
.history-item{display:flex;align-items:center;gap:10px;justify-content:space-between;width:100%;
  text-align:left;border:0;background:transparent;border-radius:8px;padding:8px 10px;cursor:pointer;font:inherit}
.history-item:hover{background:var(--agent-soft)}
.history-item.active{background:var(--agent-tint);font-weight:600}
.history-title{font-size:12px;color:var(--agent-ink);overflow:hidden;text-overflow:ellipsis;white-space:nowrap}
.history-item time{font-size:10px;color:var(--agent-muted);flex:none}
.history-empty{font-size:12px;color:var(--agent-muted);text-align:center;padding:14px 0}
.agent-page.is-embedded{height:100%;min-height:0}
.agent-page :deep(.el-popover){--el-bg-color-overlay:var(--agent-panel);--el-border-color-light:var(--agent-line);--el-text-color-regular:var(--agent-ink)}
@media(max-width:640px){.tb-sub{display:none}.agent-page .agent-topbar{padding:10px 12px}.tb-ghost{padding:5px 8px}}
</style>
