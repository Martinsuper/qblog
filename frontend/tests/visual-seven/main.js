import {createApp} from 'vue'
import {createRouter,createMemoryHistory} from 'vue-router'
import App from './VisualApp.vue'
const router=createRouter({history:createMemoryHistory(),routes:[{path:'/:pathMatch(.*)*',component:{render:()=>null}}]})
createApp(App).use(router).mount('#app')
