<template>
  <div
    class="dash"
    role="region"
    aria-label="知识库总览"
  >
    <header class="dash-head">
      <h1>知识库总览</h1>
      <p>管理大量笔记的入口：先看队列与分布，再进入具体目录。</p>
    </header>
    <div
      v-if="loading"
      class="dash-state"
      role="status"
    >
      正在加载总览数据…
    </div>
    <div
      v-else-if="error"
      class="dash-state"
      role="alert"
    >
      <strong>无法加载总览数据</strong>
      <p class="feedback-description">
        请检查网络连接后重试，已打开的笔记不受影响。
      </p>
      <button
        class="btn"
        @click="$emit('retry')"
      >
        重新加载
      </button>
    </div>
    <template v-else>
      <div class="stats">
        <div class="stat">
          <span class="num">{{ stats.total }}</span><span class="lbl">总笔记数</span>
        </div>
        <div class="stat">
          <span class="num">{{ stats.favorites }}</span><span class="lbl">收藏笔记</span>
        </div>
        <div class="stat">
          <span class="num">{{ stats.mcp }}</span><span class="lbl">MCP 导入</span>
        </div>
        <div
          class="stat"
          :class="{ warn: stats.inbox > 0 }"
        >
          <span class="num">{{ stats.inbox }}</span><span class="lbl">待归档</span>
        </div>
      </div>
      <div class="dash-grid">
        <div class="dash-col">
          <section class="dash-card">
            <h2>待归档队列<span class="dash-more">{{ inboxNotes.length }}/{{ stats.inbox }} 篇</span></h2>
            <button
              v-for="note in inboxNotes"
              :key="note.id"
              class="dash-row"
              @click="$emit('open-note', note.id)"
            >
              <span class="row-title">{{ note.title || '未命名笔记' }}</span>
              <span
                v-if="note.favorite"
                class="row-star"
                aria-label="已收藏"
              >★</span><span
                class="pill"
                :class="{ mcp: note.source === 'mcp' }"
              >{{ note.source === 'mcp' ? 'MCP' : '手动' }}</span>
            </button>
            <p
              v-if="!inboxNotes.length"
              class="dash-hint"
            >
              收件箱是空的，所有笔记都已归档到目录中。
            </p>
            <p
              v-else-if="stats.inbox > inboxNotes.length"
              class="dash-hint"
            >
              其余未归档笔记可在目录树「未分类」中继续处理。
            </p>
          </section>
          <section class="dash-card">
            <h2>最近更新</h2>
            <button
              v-for="note in recent"
              :key="note.id"
              class="dash-row"
              @click="$emit('open-note', note.id)"
            >
              <span class="row-title">{{ note.title || '未命名笔记' }}</span>
              <time class="row-time">{{ shortDate(note.updateTime) }}</time>
            </button>
            <p
              v-if="!recent.length"
              class="dash-hint"
            >
              还没有任何笔记，点击左侧「+」新建第一篇。
            </p>
          </section>
        </div>
        <div class="dash-col">
          <section class="dash-card">
            <h2>目录分布</h2>
            <div
              v-for="bar in bars"
              :key="bar.name"
              class="bar-row"
            >
              <span class="bar-name">{{ bar.name }}</span>
              <span class="bar-track"><i :style="{ width: barWidth(bar.count) }" /></span>
              <span class="bar-n">{{ bar.count }}</span>
            </div>
          </section>
          <section class="dash-card">
            <h2>标签</h2>
            <div
              v-if="tags.length"
              class="tagcloud"
            >
              <button
                v-for="item in tags"
                :key="item.name"
                @click="$emit('filter-tag', item.name)"
              >
                {{ item.name }}<s>{{ item.count }}</s>
              </button>
            </div>
            <p
              v-else
              class="dash-hint"
            >
              给笔记加上标签，就可以从这里快速筛选。
            </p>
          </section>
          <section class="dash-card">
            <h2>MCP 最近活动<span class="dash-more">最近 {{ calls.length }} 条</span></h2>
            <div
              v-for="call in calls"
              :key="call.id"
              class="call-row"
            >
              <span class="row-title mono">{{ call.toolName }}</span>
              <span
                class="call-state"
                :class="call.success ? 'ok' : 'bad'"
              >{{ call.success ? '成功' : '失败' }}</span>
              <span class="row-time">{{ fullTime(call.createdAt) }}</span>
            </div>
            <p
              v-if="!calls.length"
              class="dash-hint"
            >
              暂无 MCP 调用记录。可在「工作区菜单 → MCP 接入」创建 API Token。
            </p>
          </section>
        </div>
      </div>
    </template>
  </div>
</template>
<script setup>
import { computed } from 'vue'
import { dashboardStats, inboxQueue, recentNotes, tagCounts, folderDistribution } from '../utils/knowledgeDashboard'
const props = defineProps({
  loading: Boolean, error: Boolean,
  notes: { type: Array, default: () => [] },
  folders: { type: Array, default: () => [] },
  mcpCalls: { type: Array, default: () => [] }
})
defineEmits(['open-note', 'filter-tag', 'retry'])
const stats = computed(() => dashboardStats(props.notes))
const inboxNotes = computed(() => inboxQueue(props.notes))
const recent = computed(() => recentNotes(props.notes))
const tags = computed(() => tagCounts(props.notes))
const bars = computed(() => folderDistribution(props.folders, props.notes))
const calls = computed(() => (props.mcpCalls || []).slice(0, 5))
const barWidth = count => `${Math.round((count / Math.max(1, ...bars.value.map(bar => bar.count))) * 100)}%`
function shortDate(value) { return value ? String(value).slice(5, 10).replace('-', '/') : '' }
function fullTime(value) { return value ? String(value).replace('T', ' ').slice(5, 16) : '' }
</script>
<style scoped>
/* 父组件的 button 重置是 scoped 的，这里需要自带一份。 */
.dash button { font: inherit; color: inherit; border: 0; background: none; padding: 0; cursor: pointer; }
.dash button:focus-visible { outline: 2px solid var(--blue); outline-offset: 2px; }
.dash .btn { min-height: 31px; padding: 0 11px; border: 1px solid var(--line); border-radius: 5px; background: var(--panel); font-size: 12px; color: var(--ink); }
.dash .btn:hover { background: var(--soft); }
.dash { padding: 24px 30px 60px; }
.dash-head h1 { margin: 0; font-size: 20px; font-weight: 700; letter-spacing: -.2px; }
.dash-head p { margin: 4px 0 22px; color: var(--muted); font-size: 12.5px; }
.dash-state { padding: 60px 20px; text-align: center; color: var(--muted); font-size: 13px; }
.dash-state .feedback-description { margin: 8px auto 16px; max-width: 340px; font-size: 12px; line-height: 1.8; }
.stats { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; margin-bottom: 12px; }
.stat { background: var(--panel); border: 1px solid var(--line); border-radius: 9px; padding: 15px 17px; }
.stat .num { display: block; font-size: 24px; font-weight: 750; letter-spacing: -.5px; color: var(--ink); }
.stat .lbl { display: block; margin-top: 3px; font-size: 11.5px; color: var(--muted); }
.stat.warn .num { color: #a17c37; }
.dash-grid { display: grid; grid-template-columns: minmax(0, 1.35fr) minmax(0, 1fr); gap: 12px; align-items: start; }
.dash-col { min-width: 0; display: flex; flex-direction: column; gap: 12px; }
.dash-card { background: var(--panel); border: 1px solid var(--line); border-radius: 9px; padding: 15px 17px; }
.dash-card h2 { display: flex; align-items: center; justify-content: space-between; margin: 0 0 10px; font-size: 13px; font-weight: 650; color: var(--ink); }
.dash-more { font-size: 11px; font-weight: 400; color: var(--muted); }
.dash-row { display: flex; align-items: center; gap: 8px; width: 100%; min-height: 34px; padding: 6px; border-radius: 5px; text-align: left; font-size: 12.5px; color: var(--ink); }
.dash-row:hover { background: var(--soft); }
.row-title { flex: 1; min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.row-title.mono { font-family: ui-monospace, SFMono-Regular, Consolas, monospace; font-size: 11.5px; color: var(--muted); }
.row-star { flex: none; color: #b29970; font-size: 11px; }
.row-time { flex: none; font-size: 11px; color: var(--muted); }
.pill { flex: none; font-size: 10px; padding: 1px 6px; border-radius: 4px; background: #edf2f7; color: #68768a; }
.pill.mcp { color: #5474a1; background: #edf2fa; }
.dash-hint { margin: 6px 0 0; padding: 4px 2px; font-size: 12px; line-height: 1.9; color: var(--muted); }
.bar-row { display: grid; grid-template-columns: 84px minmax(0, 1fr) 28px; gap: 10px; align-items: center; margin: 8px 0; font-size: 12px; }
.bar-name { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; text-align: right; color: var(--muted); }
.bar-track { height: 8px; border-radius: 4px; background: var(--soft); overflow: hidden; }
.bar-track i { display: block; height: 100%; border-radius: 4px; background: var(--blue); }
.bar-n { font-size: 10.5px; color: var(--muted); text-align: right; }
.tagcloud { display: flex; flex-wrap: wrap; gap: 7px; }
.tagcloud button { font-size: 12px; border: 1px solid var(--line); border-radius: 99px; padding: 3px 12px; background: var(--soft); color: var(--ink); }
.tagcloud button:hover { border-color: var(--blue); color: var(--blue); }
.tagcloud s { text-decoration: none; color: var(--blue); font-weight: 600; margin-left: 4px; }
.call-row { display: flex; align-items: center; gap: 8px; min-height: 30px; padding: 4px 6px; font-size: 12px; border-bottom: 1px solid var(--line); }
.call-row:last-of-type { border-bottom: 0; }
.call-state { flex: none; font-size: 10.5px; }
.call-state.ok { color: var(--green); }
.call-state.bad { color: #ac6570; }
.seven[data-color='dark'] .stat.warn .num { color: #d9b06c; }
.seven[data-color='dark'] .pill { background: var(--soft); color: var(--muted); }
.seven[data-color='dark'] .pill.mcp { color: #9ec3f2; background: var(--blue-bg); }
.seven[data-color='dark'] .call-state.bad { color: #d98a94; }
@media (max-width: 1100px) {
  .dash { padding: 20px 24px 50px; }
  .stats { grid-template-columns: repeat(2, 1fr); }
  .dash-grid { grid-template-columns: 1fr; }
}
@media (max-width: 620px) {
  .dash { padding: 16px 16px 50px; }
}
</style>
