import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import { router } from './router'
import { useAuthStore } from './stores/auth'
import { registerSW } from 'virtual:pwa-register'
import './style.css'

registerSW({ immediate: true })

const themeMeta = document.querySelector('meta[name="theme-color"]')
function syncThemeColor(e) {
  themeMeta?.setAttribute('content', e.matches ? '#1c1917' : '#f59e0b')
}
const darkQuery = window.matchMedia('(prefers-color-scheme: dark)')
darkQuery.addEventListener('change', syncThemeColor)
syncThemeColor(darkQuery)

const app = createApp(App)
app.use(createPinia())

const auth = useAuthStore()
auth.init().finally(() => {
  app.use(router)
  app.mount('#app')
})
