import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import 'element-plus/dist/index.css'
import App from './App.vue'
import router from './router'
import { pinia } from './stores'
import { useUserStore } from './stores/user'
import './styles/index.css'

const app = createApp(App)

for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.directive('perm', {
  mounted(el, binding) {
    const store = useUserStore()
    const value = binding.value
    const allowed = Array.isArray(value) ? value.some((p) => store.hasPerm(p)) : store.hasPerm(value)
    if (!allowed && el.parentNode) {
      el.parentNode.removeChild(el)
    }
  }
})

app.use(pinia)
app.use(router)
app.use(ElementPlus, { locale: zhCn, size: 'default' })
app.mount('#app')
