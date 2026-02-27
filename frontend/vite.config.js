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
        target: 'http://localhost:8081',
        changeOrigin: true,
        secure: false,
        configure: (proxy, options) => {
          proxy.on('proxyReq', (proxyReq, req, res) => {
            // 仅对非 multipart 请求设置 Content-Type，避免破坏文件上传
            const contentType = proxyReq.getHeader('Content-Type')
            if (!contentType || !contentType.includes('multipart')) {
              // 不覆盖已有的 Content-Type（如 multipart/form-data）
              if (!contentType) {
                proxyReq.setHeader('Content-Type', 'application/json; charset=utf-8')
              }
            }
          })
          proxy.on('proxyRes', (proxyRes, req, res) => {
            // 仅对 JSON 响应设置编码
            const resContentType = proxyRes.headers['content-type']
            if (resContentType && resContentType.includes('application/json')) {
              proxyRes.headers['content-type'] = 'application/json; charset=utf-8'
            }
          })
        }
      }
    }
  }
})
