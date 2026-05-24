import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import logger from '@/utils/logger'
import { useSiteAssets } from '@/utils/siteAssets'
// 导入 Element Plus
import ElementPlus from 'element-plus'
// 导入自定义主题色配置
import './styles/element-variables.scss'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
// 导入初始化样式
import './assets/global.css'
// 导入动画样式
import './assets/animations.css'
// 中文
import zhCn from 'element-plus/dist/locale/zh-cn.mjs'

// 添加全局错误处理器来抑制ResizeObserver警告
console.error = (...args) => {
  if (args[0] && args[0].includes && args[0].includes('ResizeObserver loop')) {
    return
  }
  logger.error(...args)
}

const app = createApp(App)
const pinia = createPinia()

app.use(pinia)
app.use(router)
app.use(ElementPlus, {
  locale: zhCn
})
useSiteAssets().loadSiteAssets()
// 注册所有图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}
app.mount('#app')
