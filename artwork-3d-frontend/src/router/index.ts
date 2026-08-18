import { createRouter, createWebHistory } from 'vue-router'
import ThreeCanvas from '@/components/ThreeCanvas.vue'
import LoginView from '@/views/LoginView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      name: 'gallery',
      component: ThreeCanvas
    },
    {
      path: '/login',
      name: 'login',
      component: LoginView
    }
  ]
})

export default router