<template>
  <div class="agent-chat">
    <!-- 工作区欢迎与快捷任务；抽屉使用紧凑欢迎。 -->
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
          <div class="ac-eyebrow">
            知识库 · 智能协作
          </div>
          <h2>让知识有序，让思路成文</h2>
          <p class="ac-hero-sub">
            围绕已有文章与笔记，查找资料、梳理内容，或从一个话题开始。
          </p>
          <div class="ac-badges">
            <span>文章 · 笔记读写</span><span>网页抓取</span><span>工具调用全程可见</span><span>历史仅存本机</span>
          </div>
          <div class="ac-sugs">
            <button
              v-for="s in suggestions"
              :key="s.t"
              class="ac-sug"
              :disabled="store.sending"
              @click="input = s.q; submit()"
            >
              <span class="ac-sug-ic"><KnowledgeIcon :name="s.icon" /></span>
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
          >
            {{ message.content }}
          </div>

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
            >
              正在思考 <span class="caret" />
            </div>
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
          aria-label="消息内容"
          @keydown="onKeydown"
        />
        <div
          v-if="store.error"
          class="ac-error"
          role="alert"
        >
          {{ store.error }}
        </div>
        <div class="row2">
          <span class="pill on">文章读写</span>
          <span class="pill on">知识库读写</span>
          <span class="row-sp" />
          <span class="composer-shortcut">Enter 发送 · Shift+Enter 换行</span>
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
import KnowledgeIcon from '@/components/KnowledgeIcon.vue'
import '@/styles/agent-theme.css'

defineProps({
  showHero: { type: Boolean, default: false },
  placeholder: { type: String, default: '询问站点内容、整理笔记、生成话题草稿…（Enter 发送，Shift+Enter 换行）' }
})

const store = useAgentStore()
const md = useMarkdown()
const input = ref('')
const scrollEl = ref(null)

const suggestions = [
  { icon: 'file', t: '网页转笔记', d: '抓取外部网页，整理为结构化笔记', q: '抓取这篇文章并整理成知识库笔记：https://developer.mozilla.org/zh-CN/docs/Web/API/Fetch_API' },
  { icon: 'folder', t: '整理待归档队列', d: '归并同主题笔记，梳理目录与清单', q: '查看待归档队列，把能归并的同主题笔记整理掉' },
  { icon: 'edit', t: '话题成稿', d: '从一个话题出发，沉淀结构化笔记', q: '把「上下文工程」这个话题整理成一篇知识库笔记' },
  { icon: 'search', t: '站点问答', d: '查找已有资料，提炼相关内容要点', q: '搜索笔记里关于 RAG 的内容，总结出要点' }
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
.agent-chat{display:flex;flex-direction:column;height:100%;min-height:0;min-width:0;background:var(--agent-panel)}

/* ---- 滚动区 ---- */
.ac-scroll{flex:1;min-height:0;overflow-y:auto;padding:28px max(24px,calc((100% - 800px)/2));display:flex;flex-direction:column;gap:22px;scrollbar-width:thin;overscroll-behavior:contain}
.ac-scroll.is-empty{justify-content:safe center}
.ac-row{display:flex;flex-direction:column;flex-shrink:0;min-width:0}
.msg-user{align-self:flex-end;max-width:80%;background:var(--agent-tint);border:1px solid var(--agent-line);color:var(--agent-ink);padding:10px 14px;border-radius:8px 8px 2px 8px;font-size:13px;white-space:pre-wrap;overflow-wrap:anywhere}
.msg-ai{width:100%;min-width:0;display:flex;flex-direction:column;gap:10px}
.ai-head{display:flex;align-items:center;gap:8px;font-size:11px;color:var(--agent-muted)}
.ai-glyph{width:22px;height:22px;border-radius:5px;background:var(--agent-accent);flex:none;display:inline-flex;align-items:center;justify-content:center}
.ai-glyph svg{width:14px;height:14px}
.tools{display:inline-flex;flex-wrap:wrap;gap:6px}
.tool{display:inline-flex;flex-wrap:wrap;max-width:100%;overflow-wrap:anywhere;align-items:center;gap:7px;background:var(--agent-soft);border:1px solid var(--agent-line);border-radius:5px;padding:5px 9px;font-size:11px;color:var(--agent-muted)}
.tool code{font-family:ui-monospace,Menlo,monospace;font-size:11px;color:var(--agent-ink)}
.tool .st{width:14px;height:14px;border-radius:50%;display:inline-flex;align-items:center;justify-content:center;flex:none}
.tool .st svg{width:9px;height:9px}
.tool.done .st{background:#47816c}
.tool.run .st{background:#f5a623;animation:ag-pulse 1.1s infinite}
.tool.run .spin-ring{animation:ag-spin 1.4s linear infinite}
.tool.failed .st{background:#f05252}
@keyframes ag-pulse{50%{opacity:.35}}
@keyframes ag-spin{to{transform:rotate(360deg)}}
.spin-ring{stroke-dasharray:14 8}
.msg-ai .body{font-size:13px;line-height:1.75;overflow-wrap:anywhere}
.caret{display:inline-block;width:5px;height:14px;background:var(--agent-accent);border-radius:1px;vertical-align:-2px;animation:ag-caret 1s steps(2) infinite}
@keyframes ag-caret{50%{opacity:0}}
.ac-error{margin:0 3px 8px;padding:8px 12px;border-radius:5px;background:var(--agent-soft);border:1px solid var(--agent-danger);color:var(--agent-danger);font-size:12px}

/* ---- 欢迎区 ---- */
.ac-welcome{width:100%;max-width:720px;margin:auto;text-align:left;flex-shrink:0}
.ac-glyph-lg{width:40px;height:40px;border:1px solid var(--agent-line);border-radius:8px;background:var(--agent-tint);color:var(--agent-accent);margin-bottom:20px;display:flex;align-items:center;justify-content:center}
.ac-glyph-lg svg{stroke:currentColor}
.ac-glyph-lg svg{width:24px;height:24px}
.ac-eyebrow{font-size:11px;color:var(--agent-muted);letter-spacing:1px;margin-bottom:6px}
.ac-welcome h2{font-size:23px;font-weight:650;line-height:1.5;letter-spacing:.2px;color:var(--agent-ink);margin:0}
.ac-hero-sub{color:var(--agent-muted);margin:10px 0 0;font-size:13px;line-height:1.8}
.ac-badges{margin-top:16px;display:flex;gap:8px 16px;flex-wrap:wrap}
.ac-badges span{font-size:11px;color:var(--agent-muted);display:inline-flex;align-items:center;gap:6px}
.ac-badges span::before{content:'';width:4px;height:4px;border-radius:50%;background:var(--agent-accent)}
.ac-sugs{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:10px;padding:26px 0 2px;text-align:left}
.ac-sug{display:flex;gap:12px;align-items:flex-start;text-align:left;background:var(--agent-panel);border:1px solid var(--agent-line);border-radius:6px;padding:16px;cursor:pointer;transition:background .15s,border-color .15s}
.ac-sug:hover:not(:disabled){background:var(--agent-soft);border-color:var(--agent-accent)}
.ac-sug-ic{width:28px;height:28px;border-radius:5px;background:var(--agent-soft);color:var(--agent-accent);flex:none;display:flex;align-items:center;justify-content:center}
.ac-sug-ic svg{width:16px;height:16px;fill:none;stroke:currentColor;stroke-width:1.6;stroke-linecap:round;stroke-linejoin:round}
.ac-sug b{font-size:13px;font-weight:600;color:var(--agent-ink);display:block}
.ac-sug small{color:var(--agent-muted);font-size:11px;line-height:1.7;display:block;margin-top:4px}
.ac-mini-welcome{display:flex;align-items:center;justify-content:center;gap:9px;color:var(--agent-muted);font-size:13px}
.ac-glyph-sm{width:8px;height:8px;border-radius:2px;background:var(--agent-accent);display:inline-block}

/* ---- 输入卡 ---- */
.composer{flex:none;padding:12px 24px 22px}
.composer .box{max-width:800px;margin:0 auto;background:var(--agent-panel);border:1px solid var(--agent-line);border-radius:8px;display:flex;flex-direction:column;padding:14px 14px 10px;transition:border-color .2s}
.composer .box:focus-within{border-color:var(--agent-accent)}
.composer textarea{border:0;outline:0;resize:vertical;min-height:52px;max-height:180px;background:transparent;color:var(--agent-ink);font:inherit;line-height:1.7;width:100%}
.composer textarea::placeholder{color:var(--agent-muted)}
.row2{display:flex;align-items:center;gap:8px;padding-top:10px;border-top:1px solid var(--agent-line);margin-top:8px}
.pill{border:1px solid var(--agent-line);background:var(--agent-soft);color:var(--agent-muted);border-radius:4px;padding:2px 6px;font-size:10px;white-space:nowrap}
.composer-shortcut{font-size:10px;color:var(--agent-muted)}
.row-sp{flex:1}
.send{width:32px;height:32px;flex:none;border:0;border-radius:5px;background:var(--agent-accent);color:var(--agent-on-accent);cursor:pointer;display:flex;align-items:center;justify-content:center}
.send:hover:not(:disabled){background:var(--agent-accent);filter:brightness(.95)}
.send:disabled{opacity:.45;cursor:not-allowed}
.send svg{width:15px;height:15px;stroke:currentColor}
.send.stop{background:var(--agent-soft);color:var(--agent-ink);border:1px solid var(--agent-line)}
.send.stop svg{stroke:none;fill:currentColor}
.send.stop svg{width:14px;height:14px}

.ac-sug:focus-visible,.send:focus-visible{outline:2px solid var(--agent-accent);outline-offset:3px}
@media(max-width:600px){.ac-sugs{grid-template-columns:1fr;padding-top:20px}.ac-scroll{padding:22px 16px}.composer{padding:10px 16px 16px}.ac-welcome h2{font-size:20px}.composer-shortcut{display:none}}
@media(prefers-reduced-motion:reduce){.ac-sug,.composer .box{transition:none}.caret,.tool.run .st,.tool.run .spin-ring{animation:none}}
</style>
