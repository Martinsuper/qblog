/**
 * 代码复制插件
 * 为代码块添加复制按钮
 */

// 用于生成唯一ID
let codeBlockId = 0

export function codeCopyPlugin(md) {
  const defaultFenceRenderer = md.renderer.rules.fence

  md.renderer.rules.fence = (tokens, idx, options, env, self) => {
    const token = tokens[idx]
    const info = token.info ? token.info.trim() : ''
    const content = token.content
    const id = `code-block-${++codeBlockId}`

    // 默认渲染
    let result = defaultFenceRenderer(tokens, idx, options, env, self)

    // 包装代码块并添加复制按钮
    const language = info || 'text'
    const languageLabel = `<span class="code-lang">${language}</span>`
    const copyButton = `<button class="code-copy-btn" data-code-id="${id}" title="复制代码">
      <svg class="copy-icon" viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2">
        <rect x="9" y="9" width="13" height="13" rx="2" ry="2"></rect>
        <path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"></path>
      </svg>
      <svg class="copy-success-icon" viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" style="display:none">
        <polyline points="20 6 9 17 4 12"></polyline>
      </svg>
    </button>`

    // 存储原始代码内容，用于复制功能
    const codeData = `<textarea class="code-data" id="${id}" style="position:absolute;left:-9999px;">${escapeHtml(content)}</textarea>`

    return `<div class="code-block-wrapper" data-lang="${language}">
      <div class="code-block-header">
        ${languageLabel}
        ${copyButton}
      </div>
      ${result}
      ${codeData}
    </div>`
  }
}

/**
 * HTML 转义
 */
function escapeHtml(text) {
  const map = {
    '&': '&amp;',
    '<': '&lt;',
    '>': '&gt;',
    '"': '&quot;',
    "'": '&#039;'
  }
  return text.replace(/[&<>"']/g, m => map[m])
}

/**
 * 注册全局复制事件监听器
 * 在应用初始化时调用一次
 */
export function initCodeCopyHandler() {
  document.addEventListener('click', async (e) => {
    const btn = e.target.closest('.code-copy-btn')
    if (!btn) return

    const codeId = btn.dataset.codeId
    const codeData = document.getElementById(codeId)
    if (!codeData) return

    try {
      await navigator.clipboard.writeText(codeData.value)

      // 显示成功状态
      const copyIcon = btn.querySelector('.copy-icon')
      const successIcon = btn.querySelector('.copy-success-icon')
      if (copyIcon && successIcon) {
        copyIcon.style.display = 'none'
        successIcon.style.display = 'block'
        btn.classList.add('copied')

        setTimeout(() => {
          copyIcon.style.display = 'block'
          successIcon.style.display = 'none'
          btn.classList.remove('copied')
        }, 2000)
      }
    } catch (err) {
      console.error('复制失败:', err)
    }
  })
}