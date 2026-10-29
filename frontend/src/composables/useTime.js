/**
 * 时间展示与阅读时长工具
 */

/**
 * 解析后端时间字符串为 Date（兼容 ISO 的 'T' 分隔与 Safari 不支持的 '-' 分隔）
 */
export function parseDate(time) {
  if (!time) return null
  const date = new Date(String(time).replace('T', ' ').replace(/-/g, '/'))
  return Number.isNaN(date.getTime()) ? null : date
}

/**
 * 相对时间：7 天内显示"x 分钟/小时/天前"，更早显示完整日期
 */
export function formatRelativeTime(time) {
  if (!time) return ''
  const date = parseDate(time)
  if (!date) return time

  const diff = Date.now() - date.getTime()
  const minute = 60 * 1000
  const hour = 60 * minute
  const day = 24 * hour

  if (diff < minute) return '刚刚'
  if (diff < hour) return `${Math.floor(diff / minute)} 分钟前`
  if (diff < day) return `${Math.floor(diff / hour)} 小时前`
  if (diff < 7 * day) return `${Math.floor(diff / day)} 天前`
  return date.toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric' })
}

/**
 * 完整时间：yyyy-MM-dd HH:mm
 */
export function formatDateTime(time) {
  if (!time) return ''
  const date = parseDate(time)
  if (!date) return time
  const pad = (n) => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`
}

/**
 * 估算阅读时长（分钟）：中文约 300 字/分钟，代码块按行数折算
 */
export function estimateReadingMinutes(content) {
  if (!content) return 0
  let weightedLength = 0
  const text = String(content).replace(/```[\s\S]*?```/g, (block) => {
    weightedLength += block.split('\n').length * 15 // 代码行折算
    return ''
  })
  weightedLength += text.replace(/\s|#|\*|`|>|\[|\]|\(|\)|!|-/g, '').length
  return Math.max(1, Math.round(weightedLength / 300))
}
