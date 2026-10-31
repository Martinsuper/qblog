/**
 * PlantUML 图表插件
 * ```plantuml 代码块 → <img src="/api/plantuml/svg/{code}">
 * 编码遵循 PlantUML 标准：raw deflate 压缩 + 自定义 6 位字符表（与官方编码互认）
 */
import { deflateRaw } from 'pako'

const ENCODE64 = '0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz-_'

/** PlantUML 标准 URL 安全编码 */
export function encodePlantuml(text) {
  const data = deflateRaw(new TextEncoder().encode(text), { level: 9 })
  let out = ''
  for (let i = 0; i < data.length; i += 3) {
    const b1 = data[i] & 0xFF
    const b2 = i + 1 < data.length ? data[i + 1] & 0xFF : 0
    const b3 = i + 2 < data.length ? data[i + 2] & 0xFF : 0
    out += ENCODE64[b1 >> 2]
    out += ENCODE64[((b1 & 3) << 4) | (b2 >> 4)]
    out += ENCODE64[((b2 & 15) << 2) | (b3 >> 6)]
    out += ENCODE64[b3 & 63]
  }
  return out
}

export function plantumlSvgUrl(source) {
  return `/api/plantuml/svg/${encodeURIComponent(encodePlantuml(source))}`
}

export function plantumlPlugin(md) {
  const originalFence = md.renderer.rules.fence || function (tokens, idx, options, env, self) {
    return self.renderToken(tokens, idx, options)
  }

  md.renderer.rules.fence = (tokens, idx, options, env, self) => {
    const token = tokens[idx]
    const info = (token.info || '').trim().split(/\s+/)[0].toLowerCase()
    const content = token.content || ''
    // 未标注语言的裸 @startuml 块同样按图表处理
    if (info === 'plantuml' || (!info && content.trim().startsWith('@startuml'))) {
      const url = plantumlSvgUrl(content)
      return `<div class="plantuml-diagram"><img src="${url}" alt="PlantUML 图表" loading="lazy" decoding="async"></div>\n`
    }
    return originalFence(tokens, idx, options, env, self)
  }
}
