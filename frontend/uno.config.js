import { defineConfig, presetUno, presetAttributify, presetIcons } from 'unocss'

export default defineConfig({
  presets: [
    presetUno(),
    presetAttributify(),
    presetIcons({
      scale: 1.2,
      warn: true,
    }),
  ],
  shortcuts: {
    'btn': 'px-4 py-2 rounded inline-flex items-center justify-center bg-blue-500 text-white cursor-pointer hover:bg-blue-600 transition-colors duration-200 disabled:cursor-not-allowed disabled:opacity-50',
    'btn-outline': 'px-4 py-2 rounded inline-flex items-center justify-center border border-gray-300 text-gray-700 cursor-pointer hover:bg-gray-50 dark:border-gray-600 dark:text-gray-300 dark:hover:bg-gray-800 transition-colors duration-200',
    'card': 'bg-white dark:bg-gray-800 rounded-lg shadow-sm p-4',
    'link': 'text-blue-500 hover:text-blue-600 transition-colors duration-200',
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
