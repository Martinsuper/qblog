import { createApp } from 'vue'
import { createPinia } from 'pinia'

import App from './App.vue'
import router from './router'
import 'virtual:uno.css'
import './styles/index.css'
import { initCodeCopyHandler } from './composables/useMarkdown'

// 初始化代码复制功能
initCodeCopyHandler()

const app = createApp(App)
const pinia = createPinia()

app.use(pinia)
app.use(router)

app.mount('#app')
