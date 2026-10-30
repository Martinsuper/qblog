import { defineConfig, presetUno, presetAttributify, presetIcons } from 'unocss'

export default defineConfig({
  // 跟随 html.dark 类切换（与 useTheme / Element Plus 暗色变量保持一致），
  // 避免"手动切暗色但系统是亮色"时 dark: 工具类不生效
  dark: 'class',
  presets: [
    presetUno(),
    presetAttributify(),
    presetIcons({
      scale: 1.2,
      warn: true,
    }),
  ],
  shortcuts: {
    // 注意：card 的视觉样式统一由 src/styles/index.css 的 .card（基于 CSS 变量）提供，
    // 这里不再定义 btn / btn-outline / link / card，避免双源冲突导致品牌色与暗色模式漂移。
    'flex-center': 'flex items-center justify-center',
    'text-balance': {
      'text-wrap': 'balance',
    },
  },
  theme: {
    breakpoints: {
      sm: '640px',
      md: '768px',
      lg: '1024px',
      xl: '1280px',
    },
  },
  rules: [
    ['text-balance', { 'text-wrap': 'balance' }],
  ],
})
