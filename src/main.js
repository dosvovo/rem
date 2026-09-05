import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import { router } from './router'
import { useAuthStore } from './stores/auth'
import { registerSW } from 'virtual:pwa-register'
import './style.css'

registerSW({ immediate: true })

const app = createApp(App)
app.use(createPinia())

const auth = useAuthStore()
auth.init().finally(() => {
  app.use(router)
  app.mount('#app')
})
