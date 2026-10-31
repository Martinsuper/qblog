import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import UnoCSS from 'unocss/vite'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'

export default defineConfig({
  base: '/',
  plugins: [
    vue(),
    UnoCSS(),
    // Element Plus 按需引入：组件与 API 自动导入，只打包实际用到的部分
    AutoImport({
      resolvers: [ElementPlusResolver()]
    }),
    Components({
      resolvers: [ElementPlusResolver()]
    })
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  build: {
    rollupOptions: {
      output: {
        // 按依赖族分包：博客页不必下载知识库编辑器（tiptap），博客页缓存不受发版影响
        // 注意：不要把 element-plus 整体归入单一 chunk——入口仅引用 ElMessage/ElLoading，
        // el-table 等重组件只有管理后台使用，跟随路由 chunk 按需加载（未归组的共享模块
        // 由 Rollup 自动提升为公共 chunk）
        manualChunks(id) {
          if (!id.includes('node_modules')) return undefined
          if (/[\\/]node_modules[\\/](vue|vue-router|pinia|@vue)[\\/]/.test(id)) return 'vue-vendor'
          if (id.includes('@tiptap') || id.includes('prosemirror')) return 'editor'
          if (id.includes('highlight.js')) return 'highlight'
          if (id.includes('markdown-it')) return 'markdown'
          return undefined
        }
      }
    },
    cssCodeSplit: true,
    chunkSizeWarningLimit: 500
  },
  server: {
    port: 3001,
    // 允许通过 frp + Caddy 的公网域名访问 dev server（https://ali.younote.top/blog）
    allowedHosts: ['ali.younote.top'],
    proxy: {
      '/api': {
        target: 'http://localhost:8081',
        changeOrigin: true,
        secure: false,
        configure: (proxy) => {
          proxy.on('proxyReq', (proxyReq) => {
            // 仅对非 multipart 请求设置 Content-Type，避免破坏文件上传
            const contentType = proxyReq.getHeader('Content-Type')
            if (!contentType || !contentType.includes('multipart')) {
              // 不覆盖已有的 Content-Type（如 multipart/form-data）
              if (!contentType) {
                proxyReq.setHeader('Content-Type', 'application/json; charset=utf-8')
              }
            }
          })
          proxy.on('proxyRes', (proxyRes) => {
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
