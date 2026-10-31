<template>
  <div class="agent-page">
    <header class="agent-topbar">
      <router-link
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
        placement="bottom-end"
        :width="280"
        trigger="click"
      >
        <template #reference>
          <button
            class="tb-ghost"
            :disabled="store.sending"
          >
            ⊞ 历史会话
          </button>
        </template>
        <div class="history-panel">
          <template v-if="store.ordered.length">
            <button
              v-for="conv in store.ordered"
              :key="conv.id"
              class="history-item"
              :class="{ active: conv.id === store.currentId }"
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
        ＋ 新会话
      </button>
      <button
        class="tb-ghost"
        @click="router.push('/settings/tokens')"
      >
        ⚙ 模型配置
      </button>
    </header>

    <div class="agent-page-body">
      <AgentChat
        :show-hero="true"
        placeholder="询问站点内容、整理笔记、生成话题草稿…（Enter 发送，Shift+Enter 换行）"
      />
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAgentStore } from '@/stores/agent'
import AgentChat from '@/components/agent/AgentChat.vue'

const store = useAgentStore()
const router = useRouter()

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
.agent-page{height:100dvh;display:flex;flex-direction:column;background:#fafafa;color:#101828}
html.dark .agent-page{background:#0c0f14;color:#e8ecf2}

/* ---- 玻璃拟态顶栏 ---- */
.agent-topbar{flex:none;display:flex;align-items:center;gap:12px;padding:0 18px;height:52px;
  border-bottom:1px solid rgba(0,0,0,.07);
  background:rgba(255,255,255,.72);backdrop-filter:blur(14px);-webkit-backdrop-filter:blur(14px);z-index:10}
html.dark .agent-topbar{border-bottom-color:rgba(255,255,255,.08);background:rgba(20,24,31,.72)}
.tb-back{color:#667085;font-size:12.5px;text-decoration:none;white-space:nowrap}
.tb-back:hover{color:#101828}
html.dark .tb-back{color:#94a3b8}
html.dark .tb-back:hover{color:#e8ecf2}
.tb-title{font-weight:700;font-size:13.5px;letter-spacing:.2px;max-width:40vw;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}
.tb-sub{color:#98a2b3;font-size:11.5px;white-space:nowrap}
.tb-sp{flex:1}
.tb-ghost{border:1px solid rgba(0,0,0,.11);background:transparent;color:#101828;border-radius:999px;
  padding:6px 14px;font:600 12px inherit;cursor:pointer;white-space:nowrap}
.tb-ghost:hover{border-color:#98a2b3}
.tb-ghost:disabled{opacity:.5;cursor:not-allowed}
.tb-ghost.primary{background:#101828;border-color:#101828;color:#fafafa}
html.dark .tb-ghost{border-color:rgba(255,255,255,.14);color:#e8ecf2}
html.dark .tb-ghost.primary{background:#e8ecf2;border-color:#e8ecf2;color:#0c0f14}

.agent-page-body{flex:1;min-height:0;display:flex}
.agent-page-body > *{flex:1;min-width:0}

/* ---- 历史会话 popover ---- */
.history-panel{display:flex;flex-direction:column;max-height:320px;overflow:auto;scrollbar-width:thin}
.history-item{display:flex;align-items:center;gap:10px;justify-content:space-between;width:100%;
  text-align:left;border:0;background:transparent;border-radius:8px;padding:8px 10px;cursor:pointer;font:inherit}
.history-item:hover{background:rgba(99,102,241,.07)}
.history-item.active{background:rgba(99,102,241,.12);font-weight:700}
.history-title{font-size:12.5px;color:#101828;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}
.history-item time{font-size:10.5px;color:#98a2b3;flex:none}
.history-empty{font-size:12px;color:#98a2b3;text-align:center;padding:14px 0}
</style>
