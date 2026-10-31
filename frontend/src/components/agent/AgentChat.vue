<template>
  <div class="agent-chat">
    <!-- 方案A：欢迎区 + 建议（仅全屏模式，且会话为空） -->
    <div
      ref="scrollEl"
      class="ac-scroll"
      :class="{ 'is-empty': isEmpty }"
    >
      <div
        v-if="isEmpty"
        class="ac-welcome"
      >
        <template v-if="showHero">
          <div class="ac-glyph-lg">
            <svg
              fill="none"
              stroke="#fff"
              stroke-width="2.2"
              stroke-linecap="round"
              stroke-linejoin="round"
              viewBox="0 0 24 24"
            >
              <path d="M12 3l1.8 5.2L19 10l-5.2 1.8L12 17l-1.8-5.2L5 10l5.2-1.8z" />
              <path d="M19 15l.9 2.1L22 18l-2.1.9L19 21l-.9-2.1L16 18l2.1-.9z" />
            </svg>
          </div>
          <h2>有什么可以帮你？</h2>
          <p class="ac-hero-sub">检索全站文章 · 整理知识库笔记 · 话题成稿 · 生成复盘</p>
          <div class="ac-badges">
            <span>文章 · 笔记读写</span><span>工具调用全程可见</span><span>历史仅存本机</span>
          </div>
          <div class="ac-sugs">
            <button
              v-for="s in suggestions"
              :key="s.t"
              class="ac-sug"
              @click="input = s.q; submit()"
            >
              <span
                class="ac-sug-ic"
                :style="s.bg"
              >{{ s.icon }}</span>
              <span><b>{{ s.t }}</b><small>{{ s.d }}</small></span>
            </button>
          </div>
        </template>
        <template v-else>
          <div class="ac-mini-welcome">
            <span class="ac-glyph-sm" />站点智能体
          </div>
        </template>
      </div>

      <template v-else>
        <div
          v-for="(message, index) in store.current.messages"
          :key="index"
          class="ac-row"
          :class="message.role"
        >
          <div
            v-if="message.role === 'user'"
            class="msg-user"
          >{{ message.content }}</div>

          <div
            v-else
            class="msg-ai"
          >
            <div class="ai-head">
              <span class="ai-glyph">
                <svg
                  fill="none"
                  stroke="#fff"
                  stroke-width="2.4"
                  stroke-linecap="round"
                  viewBox="0 0 24 24"
                >
                  <path d="M12 3l1.8 5.2L19 10l-5.2 1.8L12 17l-1.8-5.2L5 10l5.2-1.8z" />
                </svg>
              </span>
              <span>{{ headLabel(message, index) }}</span>
            </div>
            <div
              v-if="message.tools && message.tools.length"
              class="tools"
            >
              <span
                v-for="(step, stepIndex) in message.tools"
                :key="stepIndex + step.tool"
                class="tool"
                :class="step.state"
              >
                <span class="st">
                  <svg
                    v-if="step.state === 'done'"
                    fill="none"
                    stroke="#fff"
                    stroke-width="3"
                    viewBox="0 0 24 24"
                  ><path d="m5 13 4 4L19 7" /></svg>
                  <svg
                    v-else-if="step.state === 'failed'"
                    fill="none"
                    stroke="#fff"
                    stroke-width="2.6"
                    viewBox="0 0 24 24"
                  ><path d="m7 7 10 10M17 7 7 17" /></svg>
                  <svg
                    v-else
                    fill="none"
                    stroke="#fff"
                    stroke-width="3"
                    viewBox="0 0 24 24"
                  ><circle
                    cx="12"
                    cy="12"
                    r="8"
                    class="spin-ring"
                  /></svg>
                </span>
                <code>{{ step.tool }}</code>
                <span class="summary">{{ step.summary }}</span>
              </span>
            </div>
            <div
              v-if="message.content"
              class="body"
              :class="md.getThemeClass()"
              v-html="renderAnswer(message.content)"
            />
            <div
              v-else-if="store.sending && index === store.current.messages.length - 1"
              class="body ac-thinking"
              style="color:var(--ac-sub)"
            >正在思考 <span class="caret" /></div>
          </div>
        </div>
      </template>
    </div>

    <div class="composer">
      <div class="box">
        <textarea
          v-model="input"
          :disabled="store.sending"
          :placeholder="placeholder"
          @keydown="onKeydown"
        />
        <div
          v-if="store.error"
          class="ac-error"
        >{{ store.error }}</div>
        <div class="row2">
          <span class="pill on">文章读写</span>
          <span class="pill on">知识库读写</span>
          <span class="row-sp" />
          <button
            v-if="store.sending"
            class="send stop"
            aria-label="停止生成"
            @click="store.stop()"
          >
            <svg
              fill="currentColor"
              stroke="none"
              viewBox="0 0 24 24"
            ><rect
              x="7"
              y="7"
              width="10"
              height="10"
              rx="2"
            /></svg>
          </button>
          <button
            v-else
            class="send"
            aria-label="发送"
            :disabled="!input.trim()"
            @click="submit"
          >
            <svg
              fill="none"
              stroke="#fff"
              stroke-width="2.4"
              stroke-linecap="round"
              stroke-linejoin="round"
              viewBox="0 0 24 24"
            ><path d="M12 19V5M5 12l7-7 7 7" /></svg>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, nextTick, watch } from 'vue'
import { useAgentStore } from '@/stores/agent'
import { useMarkdown } from '@/composables/useMarkdown'

const props = defineProps({
  showHero: { type: Boolean, default: false },
  placeholder: { type: String, default: '询问站点内容、整理笔记、生成话题草稿…（Enter 发送，Shift+Enter 换行）' }
})

const store = useAgentStore()
const md = useMarkdown()
const input = ref('')
const scrollEl = ref(null)

const suggestions = [
  { icon: '🗂', bg: 'background:rgba(99,102,241,.12)', t: '整理待归档队列', d: '把待归档笔记归并到对应目录，汇总清单', q: '查看待归档队列，把能归并的同主题笔记整理掉' },
  { icon: '✍️', bg: 'background:rgba(34,211,238,.13)', t: '话题成稿', d: '把一个话题整理成结构化知识库笔记', q: '把「上下文工程」这个话题整理成一篇知识库笔记' },
  { icon: '🔍', bg: 'background:rgba(62,207,142,.13)', t: '站点问答', d: '基于全部文章与笔记回答提问', q: '搜索笔记里关于 RAG 的内容，总结出要点' },
  { icon: '🗓', bg: 'background:rgba(217,161,60,.14)', t: '生成复盘', d: '汇总本周新增与更新的笔记和文章', q: '汇总本周新增与更新的笔记和文章，做成复盘清单' }
]

const isEmpty = computed(() => !store.current || !store.current.messages.length)
const lastStable = computed(() => store.current?.messages
  .map(m => `${(m.content || '').length}|${(m.tools || []).length}|${(m.tools || []).map(t => t.state).join(',')}`)
  .join('#'))
watch([lastStable, () => store.currentId], () => nextTick(scrollBottom))

function scrollBottom() {
  if (scrollEl.value) scrollEl.value.scrollTop = scrollEl.value.scrollHeight
}

function headLabel(message, index) {
  const count = (message.tools || []).length
  if (store.sending && index === store.current.messages.length - 1 && !message.content) return '思考中'
  return count ? `Q·Agent · 已执行 ${count} 个工具` : 'Q·Agent'
}

function renderAnswer(content) {
  return md.render(content)
}

function onKeydown(event) {
  if (event.key === 'Enter' && !event.shiftKey) {
    event.preventDefault()
    submit()
  }
}

function submit() {
  const text = input.value.trim()
  if (!text || store.sending) return
  input.value = ''
  store.send(text)
}
</script>

<style scoped>
.agent-chat{
  --ac-line:rgba(0,0,0,.08); --ac-sub:#667085; --ac-faint:#98a2b3; --ac-card:#ffffff;
  --ac-userbg:linear-gradient(120deg,rgba(99,102,241,.10),rgba(34,211,238,.09));
  --ac-userline:rgba(99,102,241,.24);
  --ac-grad:linear-gradient(120deg,#6366f1,#22d3ee);
  display:flex;flex-direction:column;height:100%;min-height:0;background:transparent;color:#101828
}
html.dark .agent-chat{
  --ac-line:rgba(255,255,255,.09); --ac-sub:#94a3b8; --ac-faint:#64748b; --ac-card:#14181f;
}

/* ---- 滚动区 ---- */
.ac-scroll{flex:1;min-height:0;overflow-y:auto;padding:22px;display:flex;flex-direction:column;gap:18px;scrollbar-width:thin}
.ac-scroll.is-empty{justify-content:center}
.msg-user{align-self:flex-end;max-width:68%;background:var(--ac-userbg);border:1px solid var(--ac-userline);
  color:#101828;padding:10px 15px;border-radius:16px 16px 5px 16px;font-size:13px;white-space:pre-wrap}
html.dark .msg-user{color:#e8ecf2}
.msg-ai{max-width:94%;display:flex;flex-direction:column;gap:9px}
.ai-head{display:flex;align-items:center;gap:8px;font-size:11px;color:var(--ac-faint)}
.ai-glyph{width:20px;height:20px;border-radius:7px;background:var(--ac-grad);flex:none;display:inline-flex;align-items:center;justify-content:center}
.tools{display:inline-flex;flex-wrap:wrap;gap:6px}
.tool{display:inline-flex;align-items:center;gap:7px;background:var(--ac-card);border:1px solid var(--ac-line);
  border-radius:999px;padding:5px 12px 5px 9px;font-size:11.5px;color:var(--ac-sub)}
.tool code{font-family:ui-monospace,Menlo,monospace;font-size:11px;color:#101828}
html.dark .tool code{color:#e8ecf2}
.tool .st{width:14px;height:14px;border-radius:50%;display:inline-flex;align-items:center;justify-content:center;flex:none}
.tool .st svg{width:9px;height:9px}
.tool.ok .st{background:#3ecf8e}
.tool.run .st{background:#f5a623;animation:ag-pulse 1.1s infinite}
.tool.run .spin-ring{animation:ag-spin 1.4s linear infinite}
.tool.failed .st{background:#f05252}
@keyframes ag-pulse{50%{opacity:.35}}
@keyframes ag-spin{to{transform:rotate(360deg)}}
.spin-ring{stroke-dasharray:14 8}
.msg-ai .body{font-size:13px;line-height:1.75;overflow-wrap:anywhere}
.caret{display:inline-block;width:7px;height:14px;background:var(--ac-grad);border-radius:2px;vertical-align:-2px;animation:ag-caret 1s steps(2) infinite}
@keyframes ag-caret{50%{opacity:0}}
.ac-error{margin:0 3px 8px;padding:8px 12px;border-radius:9px;background:rgba(240,82,82,.08);
  border:1px solid rgba(240,82,82,.25);color:#d92d20;font-size:12px}

/* ---- 欢迎区（方案A） ---- */
.ac-welcome{width:100%;max-width:760px;margin:0 auto;text-align:center}
.ac-glyph-lg{width:52px;height:52px;border-radius:16px;background:var(--ac-grad);margin:0 auto 14px;
  display:flex;align-items:center;justify-content:center;box-shadow:0 10px 30px -8px rgba(99,102,241,.55)}
.ac-glyph-lg svg{width:24px;height:24px}
.ac-welcome h2{font-size:24px;font-weight:800;letter-spacing:-.4px;
  background:var(--ac-grad);-webkit-background-clip:text;background-clip:text;color:transparent}
.ac-hero-sub{color:var(--ac-sub);margin-top:6px;font-size:13px}
.ac-badges{margin-top:12px;display:flex;gap:8px;justify-content:center;flex-wrap:wrap}
.ac-badges span{font-size:10.5px;color:var(--ac-faint);border:1px solid var(--ac-line);border-radius:999px;padding:3px 10px}
.ac-sugs{display:grid;grid-template-columns:1fr 1fr;gap:10px;padding:22px 0 2px;text-align:left}
.ac-sug{display:flex;gap:10px;align-items:flex-start;background:var(--ac-card);border:1px solid var(--ac-line);
  border-radius:12px;padding:12px;cursor:pointer;box-shadow:0 1px 2px rgba(16,24,40,.05),0 6px 20px -6px rgba(16,24,40,.09);transition:.15s}
.ac-sug:hover{transform:translateY(-1.5px);border-color:rgba(99,102,241,.35)}
.ac-sug-ic{width:30px;height:30px;border-radius:9px;flex:none;display:flex;align-items:center;justify-content:center;font-size:14px}
.ac-sug b{font-size:12.5px;color:#101828;display:block}
html.dark .ac-sug b{color:#e8ecf2}
.ac-sug small{color:var(--ac-faint);font-size:11px;line-height:1.55;display:block;margin-top:1px}
.ac-mini-welcome{display:flex;align-items:center;justify-content:center;gap:9px;color:var(--ac-sub);font-size:13px}
.ac-glyph-sm{width:22px;height:22px;border-radius:8px;background:var(--ac-grad);display:inline-block}

/* ---- 输入卡 ---- */
.composer{flex:none;padding:12px 22px 18px}
.composer .box{max-width:860px;margin:0 auto;background:var(--ac-card);border:1px solid var(--ac-line);
  border-radius:16px;display:flex;flex-direction:column;padding:12px 12px 10px;
  box-shadow:0 8px 40px -8px rgba(16,24,40,.18);transition:border-color .2s}
.composer .box:focus-within{border-color:rgba(99,102,241,.45)}
.composer textarea{border:0;outline:0;resize:none;background:transparent;color:#101828;
  font:500 13.5px/1.6 inherit;height:40px}
html.dark .composer textarea{color:#e8ecf2}
.composer textarea::placeholder{color:#98a2b3}
.row2{display:flex;align-items:center;gap:8px;padding-top:8px;border-top:1px dashed var(--ac-line);margin-top:6px}
.pill{border:1px solid var(--ac-line);background:transparent;color:var(--ac-faint);border-radius:999px;padding:3px 10px;font:600 11px inherit}
.pill.on{background:rgba(99,102,241,.10);border-color:rgba(99,102,241,.35);color:#6366f1}
.row-sp{flex:1}
.send{width:34px;height:34px;border:0;border-radius:10px;background:var(--ac-grad);cursor:pointer;
  display:flex;align-items:center;justify-content:center;box-shadow:0 4px 12px -3px rgba(99,102,241,.5)}
.send:disabled{opacity:.45;cursor:not-allowed}
.send svg{width:15px;height:15px}
.send.stop{background:#f5a623;box-shadow:0 4px 12px -3px rgba(245,166,35,.5)}
.send.stop svg{width:14px;height:14px}

@media(max-width:600px){.ac-sugs{grid-template-columns:1fr}}
</style>
