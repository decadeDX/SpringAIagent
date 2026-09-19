import { createRouter, createWebHistory } from 'vue-router'
import AdminPage from '../pages/AdminPage.vue'
import AssistantPage from '../pages/AssistantPage.vue'
import LoginPage from '../pages/LoginPage.vue'
import RepairsPage from '../pages/RepairsPage.vue'
import ReservationsPage from '../pages/ReservationsPage.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/assistant' },
    { path: '/login', name: 'login', component: LoginPage },
    { path: '/assistant', name: 'assistant', component: AssistantPage, meta: { requiresAuth: true, area: 'student' } },
    { path: '/reservations', name: 'reservations', component: ReservationsPage, meta: { requiresAuth: true, area: 'student' } },
    { path: '/repairs', name: 'repairs', component: RepairsPage, meta: { requiresAuth: true, area: 'student' } },
    { path: '/admin', name: 'admin', component: AdminPage, meta: { requiresAuth: true, requiresAdmin: true, area: 'admin' } },
  ],
})

router.beforeEach((to) => {
  const token = localStorage.getItem('accessToken')
  const role = localStorage.getItem('userRole')

  if (to.name === 'login' && token) return { name: role === 'ADMIN' ? 'admin' : 'assistant' }
  if (to.meta.requiresAuth && !token) return { name: 'login' }
  if (to.meta.requiresAdmin && role !== 'ADMIN') return { name: 'assistant' }
  return true
})

export default router
