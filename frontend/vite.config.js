import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import UnoCSS from 'unocss/vite'

export default defineConfig({
  plugins: [
    vue(),
    UnoCSS(),
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    port: 3001,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        secure: false,
        configure: (proxy, options) => {
          proxy.on('proxyReq', (proxyReq, req, res) => {
            // 确保请求和响应使用 UTF-8 编码
            proxyReq.setHeader('Content-Type', 'application/json; charset=utf-8')
          })
          proxy.on('proxyRes', (proxyRes, req, res) => {
            // 确保响应使用 UTF-8 编码
            proxyRes.headers['content-type'] = 'application/json; charset=utf-8'
          })
        }
      }
    }
  }
})
