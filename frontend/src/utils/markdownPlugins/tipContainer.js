/**
 * Tip 容器插件
 * 支持 ::: tip / ::: warning / ::: danger / ::: info 语法
 * 样式参考 VuePress 默认主题
 */

// SVG 图标
const icons = {
  tip: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="12" y1="16" x2="12" y2="12"/><line x1="12" y1="8" x2="12.01" y2="8"/></svg>`,
  warning: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/><line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg>`,
  danger: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/></svg>`,
  info: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="12" y1="16" x2="12" y2="12"/><line x1="12" y1="8" x2="12.01" y2="8"/></svg>`
}

export function tipContainerPlugin(md) {
  const containerTypes = [
    { type: 'tip', defaultTitle: 'TIP', icon: icons.tip, color: '#42b983' },
    { type: 'warning', defaultTitle: 'WARNING', icon: icons.warning, color: '#e7c000' },
    { type: 'danger', defaultTitle: 'DANGER', icon: icons.danger, color: '#cc0000' },
    { type: 'info', defaultTitle: 'INFO', icon: icons.info, color: '#3b82f6' }
  ]

  containerTypes.forEach(({ type, defaultTitle, icon, color }) => {
    md.block.ruler.before(
      'paragraph',
      `container_${type}`,
      (state, startLine, endLine, silent) => {
        const pos = state.bMarks[startLine] + state.tShift[startLine]
        const max = state.eMarks[startLine]

        // 检查是否匹配 ::: type 或 ::: type 标题
        const regex = new RegExp(`^:::\\s*${type}(?:\\s+(.+))?$`, 'i')
        const line = state.src.slice(pos, max)

        if (!regex.test(line)) {
          return false
        }

        if (silent) {
          return true
        }

        const match = line.match(regex)
        const title = match[1] || defaultTitle

        // 查找结束标记
        let nextLine = startLine + 1
        let foundEnd = false

        while (nextLine < endLine) {
          const endPos = state.bMarks[nextLine] + state.tShift[nextLine]
          const endMax = state.eMarks[nextLine]
          const endLineContent = state.src.slice(endPos, endMax).trim()

          if (endLineContent === ':::') {
            foundEnd = true
            break
          }
          nextLine++
        }

        if (!foundEnd) {
          return false
        }

        // 创建 open token
        const openToken = state.push(`container_${type}_open`, 'div', 1)
        openToken.markup = ':::'
        openToken.block = true
        openToken.meta = { title, icon, color, type }

        // 获取容器内容
        const contentStart = startLine + 1
        const contentEnd = nextLine

        // 将内容作为一个整体处理
        if (contentStart < contentEnd) {
          // 获取内容文本
          const contentLines = []
          for (let i = contentStart; i < contentEnd; i++) {
            const linePos = state.bMarks[i] + state.tShift[i]
            const lineMax = state.eMarks[i]
            contentLines.push(state.src.slice(linePos, lineMax))
          }
          const content = contentLines.join('\n')

          // 创建内容 token，使用 inline 解析
          const contentToken = state.push('inline', '', 0)
          contentToken.content = content
          contentToken.children = []
          // 解析内联内容
          md.inline.parse(content, md, state.env, contentToken.children)
        }

        // 创建 close token
        const closeToken = state.push(`container_${type}_close`, 'div', -1)
        closeToken.markup = ':::'
        closeToken.block = true

        state.line = nextLine + 1
        return true
      },
      { alt: ['paragraph', 'reference', 'blockquote', 'list'] }
    )

    // 渲染容器 open
    md.renderer.rules[`container_${type}_open`] = (tokens, idx) => {
      const token = tokens[idx]
      const meta = token.meta || { title: defaultTitle, icon, color }
      return `<div class="custom-container ${meta.type || type}">
  <p class="custom-container-title">${meta.icon} ${meta.title}</p>
`
    }

    // 渲染容器 close
    md.renderer.rules[`container_${type}_close`] = () => {
      return `</div>
`
    }
  })
}