import { createApp } from 'vue'
import { createPinia } from 'pinia'
import { ElLoading } from 'element-plus'

// 函数式调用与指令类组件的样式需手动引入（按需模式下不会自动注入）
import 'element-plus/es/components/message/style/css'
import 'element-plus/es/components/message-box/style/css'
import 'element-plus/es/components/notification/style/css'
import 'element-plus/es/components/loading/style/css'
// Element Plus 官方暗色变量（配合 html.dark 类生效）
import 'element-plus/theme-chalk/dark/css-vars.css'

import App from './App.vue'
import router from './router'
import 'virtual:uno.css'
import './styles/index.css'
import { initCodeCopyHandler } from './utils/markdownPlugins/codeCopy'

// 初始化代码复制功能
initCodeCopyHandler()

const app = createApp(App)
const pinia = createPinia()

// v-loading 指令
app.use(ElLoading)

app.use(pinia)
app.use(router)
app.mount('#app')
