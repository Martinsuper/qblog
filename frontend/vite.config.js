import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import UnoCSS from 'unocss/vite'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'
import compression from 'vite-plugin-compression2'

export default defineConfig({
  base: '/',
  plugins: [
    vue(),
    UnoCSS(),
    // Element Plus 按需自动导入
    AutoImport({
      resolvers: [ElementPlusResolver()],
      imports: ['vue', 'vue-router', 'pinia'],
      dts: false,
    }),
    Components({
      resolvers: [ElementPlusResolver()],
      dts: false,
    }),
    // Gzip 压缩
    compression({
      algorithm: 'gzip',
      exclude: [/\.(br)$/, /\.(gz)$/],
    }),
    // Brotli 压缩
    compression({
      algorithm: 'brotliCompress',
      exclude: [/\.(br)$/, /\.(gz)$/],
    }),
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  build: {
    // 代码分割配置
    rollupOptions: {
      output: {
        manualChunks: {
          'vue-vendor': ['vue', 'vue-router', 'pinia'],
          'element-plus': ['element-plus'],
          'markdown': ['markdown-it', 'highlight.js'],
        },
        // 静态资源分类打包
        chunkFileNames: 'assets/js/[name]-[hash].js',
        entryFileNames: 'assets/js/[name]-[hash].js',
        assetFileNames: 'assets/[ext]/[name]-[hash].[ext]',
      },
    },
    // 启用 CSS 代码分割
    cssCodeSplit: true,
    // 构建目标
    target: 'es2015',
    // 压缩配置
    minify: 'terser',
    terserOptions: {
      compress: {
        drop_console: true,
        drop_debugger: true,
      },
    },
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
            const contentType = proxyReq.getHeader('Content-Type')
            if (!contentType || !contentType.includes('multipart')) {
              if (!contentType) {
                proxyReq.setHeader('Content-Type', 'application/json; charset=utf-8')
              }
            }
          })
          proxy.on('proxyRes', (proxyRes, req, res) => {
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
