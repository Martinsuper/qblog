<template>
  <!-- 全局悬浮入口：任意页面右下角，点击展开抽屉 -->
  <teleport to="body">
    <button
      v-if="userStore.isAdmin && route.path !== '/assistant'"
      class="agent-fab"
      :aria-label="store.drawerOpen ? '关闭智能体' : '打开智能体'"
      :class="{ active: store.drawerOpen }"
      @click="store.drawerOpen ? store.closeDrawer() : store.openDrawer()"
    >
      <svg
        viewBox="0 0 24 24"
        width="22"
        height="22"
        fill="none"
        stroke="currentColor"
        stroke-width="1.8"
        stroke-linecap="round"
        stroke-linejoin="round"
      >
        <path d="M12 3a8 8 0 0 1 8 8c0 2.5-1.1 4.7-2.9 6.2L18 21l-3.3-1.4A8 8 0 1 1 12 3z" />
        <circle cx="9" cy="11" r="0.9" fill="currentColor" stroke="none" />
        <circle cx="12" cy="11" r="0.9" fill="currentColor" stroke="none" />
        <circle cx="15" cy="11" r="0.9" fill="currentColor" stroke="none" />
      </svg>
      <span
        v-if="store.sending"
        class="fab-dot"
      />
    </button>

    <transition name="agent-drawer">
      <div
        v-if="store.drawerOpen"
        class="agent-drawer"
        role="dialog"
        aria-label="站点智能体"
      >
        <header class="agent-drawer-header">
          <strong>站点智能体</strong>
          <span class="header-actions">
            <el-button
              size="small"
              link
              @click="startNew"
            >
              新会话
            </el-button>
            <el-button
              size="small"
              link
              @click="router.push('/assistant')"
            >
              独立页面
            </el-button>
            <el-button
              size="small"
              link
              @click="store.closeDrawer()"
            >
              关闭
            </el-button>
          </span>
        </header>

        <div class="agent-drawer-convos">
          <button
            v-for="conv in store.ordered.slice(0, 6)"
            :key="conv.id"
            :class="{ active: conv.id === store.currentId, sendingable: store.sending }"
            :disabled="store.sending"
            @click="store.select(conv.id)"
          >
            {{ conv.title }}
          </button>
        </div>

        <div class="agent-drawer-body">
          <AgentChat placeholder="继续对话…（Enter 发送，Shift+Enter 换行）" />
        </div>
      </div>
    </transition>
    <div
      v-if="store.drawerOpen"
      class="agent-drawer-mask"
      @click="store.closeDrawer()"
    />
  </teleport>
</template>

<script setup>
import { useRoute, useRouter } from 'vue-router'
import { useAgentStore } from '@/stores/agent'
import { useUserStore } from '@/stores/user'
import AgentChat from './AgentChat.vue'

const store = useAgentStore()
const userStore = useUserStore()
const route = useRoute()
const router = useRouter()

function startNew() {
  if (store.sending) return
  store.newConversation()
}
</script>

<style scoped>
.agent-fab{position:fixed;right:22px;bottom:84px;z-index:2400;width:46px;height:46px;border-radius:50%;border:1px solid var(--border-color);background:var(--bg-secondary);color:var(--color-primary);cursor:pointer;box-shadow:0 6px 20px rgba(0,0,0,.14);display:flex;align-items:center;justify-content:center}
.agent-fab:hover,.agent-fab.active{background:var(--color-primary);color:#fff;border-color:var(--color-primary)}
.fab-dot{position:absolute;top:4px;right:4px;width:9px;height:9px;border-radius:50%;background:#67c23a;animation:pulse 1.2s infinite}
@keyframes pulse{50%{opacity:.3}}
.agent-fab{bottom:84px}
.agent-drawer-mask{position:fixed;inset:0;z-index:2450;background:rgba(0,0,0,.18)}
.agent-drawer{position:fixed;top:0;right:0;bottom:0;z-index:2500;width:min(480px,100vw);background:var(--bg-primary);color:var(--text-primary);border-left:1px solid var(--border-color);display:flex;flex-direction:column}
.agent-drawer-header{display:flex;align-items:center;justify-content:space-between;padding:12px 16px;border-bottom:1px solid var(--border-color);background:var(--bg-secondary)}
.header-actions{display:inline-flex;gap:2px}
.agent-drawer-convos{display:flex;gap:6px;overflow-x:auto;padding:8px 12px;border-bottom:1px solid var(--border-color)}
.agent-drawer-convos button{flex:none;max-width:150px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;padding:6px 10px;border:1px solid var(--border-color);border-radius:999px;background:var(--bg-secondary);color:var(--text-secondary);cursor:pointer;font-size:12px}
.agent-drawer-convos button.active{color:var(--color-primary);border-color:var(--color-primary)}
.agent-drawer-convos button.sendingable{cursor:not-allowed;opacity:.6}
.agent-drawer-body{flex:1;min-height:0}
.agent-drawer-enter-active,.agent-drawer-leave-active{transition:transform .25s ease}
.agent-drawer-enter-from,.agent-drawer-leave-to{transform:translateX(100%)}
</style>
