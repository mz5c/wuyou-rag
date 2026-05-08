import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import './styles/global.css'
import WIcon from './components/ui/WIcon.vue'

const app = createApp(App)
app.use(router)
app.component('WIcon', WIcon)
app.mount('#app')
