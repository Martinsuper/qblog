/**
 * 标题锚点插件
 * 为标题添加锚点链接，VuePress 风格：hover 时在标题左侧显示链接图标
 */

// 链接图标 SVG
const linkIcon = `<svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M10 13a5 5 0 0 0 7.54.54l3-3a5 5 0 0 0-7.07-7.07l-1.72 1.71"/><path d="M14 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l1.71-1.71"/></svg>`

export function headingAnchorPlugin(md) {
  const headingOpen = md.renderer.rules.heading_open || function(tokens, idx, options, env, self) {
    return self.renderToken(tokens, idx, options)
  }

  md.renderer.rules.heading_open = (tokens, idx, options, env, self) => {
    const token = tokens[idx]
    const level = parseInt(token.tag.slice(1)) // h1, h2, h3...

    // 获取标题内容
    const nextToken = tokens[idx + 1]
    let title = ''
    if (nextToken && nextToken.type === 'inline') {
      title = nextToken.content
    }

    // 生成 slug
    const slug = generateSlug(title)

    // 添加属性
    token.attrs = token.attrs || []
    token.attrs.push(['id', slug])

    return headingOpen(tokens, idx, options, env, self)
  }

  // 在标题后添加锚点链接
  const headingClose = md.renderer.rules.heading_close || function(tokens, idx, options, env, self) {
    return self.renderToken(tokens, idx, options)
  }

  md.renderer.rules.heading_close = (tokens, idx, options, env, self) => {
    // 获取前一个 inline token 的内容作为标题
    const openIdx = idx - 2
    let slug = ''
    if (openIdx >= 0 && tokens[openIdx].type === 'heading_open') {
      const inlineIdx = openIdx + 1
      if (tokens[inlineIdx] && tokens[inlineIdx].type === 'inline') {
        slug = generateSlug(tokens[inlineIdx].content)
      }
    }

    // 添加锚点链接 - VuePress 风格
    const anchorLink = slug ? `<a class="header-anchor" href="#${slug}" aria-label="链接到本标题">${linkIcon}</a>` : ''

    return headingClose(tokens, idx, options, env, self) + anchorLink
  }
}

/**
 * 生成 URL 友好的 slug
 */
function generateSlug(text) {
  if (!text) return ''

  return text
    .toLowerCase()
    .replace(/\s+/g, '-')           // 空格转连字符
    .replace(/[^\w\u4e00-\u9fa5-]/g, '') // 移除非字母数字字符（保留中文）
    .replace(/-+/g, '-')            // 多个连字符合并
    .replace(/^-|-$/g, '')          // 移除首尾连字符
    .slice(0, 50)                   // 限制长度
}