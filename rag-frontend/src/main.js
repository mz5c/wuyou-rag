import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import './styles/global.css'
import * as UI from './components/ui'

const app = createApp(App)
app.use(router)
for (const [key, component] of Object.entries(UI)) {
  app.component(key, component)
}
app.mount('#app')
