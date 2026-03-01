import plantumlEncoder from 'plantuml-encoder'

/**
 * PlantUML 渲染插件
 * 将 ```plantuml 或 ```puml 代码块渲染为 SVG 图片
 */
export function plantumlPlugin(md) {
  const defaultFenceRenderer = md.renderer.rules.fence

  md.renderer.rules.fence = (tokens, idx, options, env, self) => {
    const token = tokens[idx]
    const info = token.info ? token.info.trim().toLowerCase() : ''

    // 检查是否是 PlantUML 代码块
    if (info === 'plantuml' || info === 'puml') {
      try {
        const encoded = plantumlEncoder.encode(token.content)
        return `<div class="plantuml-diagram"><img src="https://www.plantuml.com/plantuml/svg/${encoded}" alt="PlantUML Diagram" loading="lazy" /></div>`
      } catch (error) {
        console.error('PlantUML 渲染失败:', error)
        return `<div class="plantuml-error">PlantUML 渲染失败：${error.message}</div>`
      }
    }

    // 默认代码块渲染
    return defaultFenceRenderer(tokens, idx, options, env, self)
  }
}