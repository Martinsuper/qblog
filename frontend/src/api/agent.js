import request from './request'
import { getToken } from '@/utils/auth'

/**
 * 站内智能体：大模型配置 + SSE 流式对话。
 * 对话走原生 fetch（需要边收边渲染的 SSE 流，axios 不便处理）。
 */

export const getAiConfig = () => request.get('/ai/config')
export const updateAiConfig = data => request.put('/ai/config', data)
export const testAiConnection = () => request.post('/ai/config/test', {})

const apiBase = () => (import.meta.env.VITE_API_BASE_URL || '/api').replace(/\/$/, '')

/**
 * 发起一次智能体对话（SSE 流式）。
 * @param {object} options
 * @param {Array<{role,content}>} options.history 会话历史（不含本次提问）
 * @param {string} options.question 本次提问
 * @param {(text: string) => void} options.onToken 增量文本回调
 * @param {(event: {tool, summary, phase, ok}) => void} [options.onTool] 工具调用回调
 * @param {AbortSignal} [options.signal] 取消信号
 * @returns {Promise<void>} 流结束（done 或 error）后 resolve
 */
export function streamAgentChat({ history, question, onToken, onTool, signal }) {
  const url = `${apiBase()}/ai/agent?q=${encodeURIComponent(question)}`
  const headers = { 'Content-Type': 'application/json', Accept: 'text/event-stream' }
  const token = getToken()
  if (token) headers.Authorization = `Bearer ${token}`

  return fetch(url, {
    method: 'POST',
    headers,
    credentials: 'include',
    signal,
    body: JSON.stringify({ history })
  }).then(response => {
    if (!response.ok) {
      return response.json().catch(() => ({})).then(body => {
        throw new Error(body.message || `请求失败（${response.status}）`)
      })
    }
    return readSseStream(response, onToken, onTool)
  })
}

async function readSseStream(response, onToken, onTool) {
  const reader = response.body.getReader()
  const decoder = new TextDecoder()
  let buffer = ''

  const handleBlock = block => {
    const lines = block.split('\n')
    let event = 'message'
    const payloads = []
    for (const line of lines) {
      if (line.startsWith('event:')) event = line.slice(6).trim()
      else if (line.startsWith('data:')) payloads.push(line.slice(5).trim())
    }
    if (!payloads.length) return
    let data = {}
    try { data = JSON.parse(payloads.join('\n')) } catch { return }

    if (event === 'token') { onToken?.(data.t || ''); return }
    if (event === 'tool') { onTool?.(data); return }
    if (event === 'error') { throw new Error(data.message || '会话出错') }
    // done → 正常结束
    if (event === 'done') throw { normalized: true }
  }

  // SSE 帧：data/json 恰好不含 \n\n，按空行分帧足够安全
  while (true) {
    const { done, value } = await reader.read()
    if (done) break
    buffer += decoder.decode(value, { stream: true })
    const frames = buffer.split('\n\n')
    buffer = frames.pop()
    for (const frame of frames) {
      try { handleBlock(frame) } catch (e) {
        if (e?.normalized) return
        throw e
      }
    }
  }
}
