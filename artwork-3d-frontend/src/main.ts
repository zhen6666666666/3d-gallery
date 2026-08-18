import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'

import App from './App.vue'
import router from './router'

const app = createApp(App)

// 1. 创建并注册 Pinia
const pinia = createPinia()
app.use(pinia)

// 2. 注册 Vue Router 和 UI 组件库
app.use(router)
app.use(ElementPlus)

// 3. 最后挂载应用（必须在 app.use 之后）
app.mount('#app')