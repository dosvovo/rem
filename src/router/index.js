import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const routes = [
  { path: '/login', name: 'login', component: () => import('../views/LoginView.vue') },
  { path: '/', name: 'list', component: () => import('../views/ListView.vue') },
  { path: '/new', name: 'new', component: () => import('../views/EditView.vue') },
  { path: '/edit/:id', name: 'edit', component: () => import('../views/EditView.vue') },
  { path: '/schedule', name: 'schedule', component: () => import('../views/ScheduledView.vue') },
  { path: '/:pathMatch(.*)*', redirect: '/' },
]

export const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to) => {
  const auth = useAuthStore()
  if (!auth.configured) return true
  if (!auth.user && to.name !== 'login') return { name: 'login' }
  if (auth.user && to.name === 'login') return { name: 'list' }
  return true
})
