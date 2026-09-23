import { createRouter, createWebHistory } from 'vue-router'
import AdminPage from '../pages/AdminPage.vue'
import AssistantPage from '../pages/AssistantPage.vue'
import LoginPage from '../pages/LoginPage.vue'
import RepairsPage from '../pages/RepairsPage.vue'
import ReservationCreatePage from '../pages/ReservationCreatePage.vue'
import ReservationsPage from '../pages/ReservationsPage.vue'
import { accessToken, role } from '../session'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/assistant' },
    { path: '/login', name: 'login', component: LoginPage },
    { path: '/assistant', name: 'assistant', component: AssistantPage, meta: { requiresAuth: true, area: 'student' } },
    { path: '/reservations', name: 'reservations', component: ReservationsPage, meta: { requiresAuth: true, area: 'student' } },
    { path: '/reservations/new', name: 'reservation-create', component: ReservationCreatePage, meta: { requiresAuth: true, area: 'student' } },
    { path: '/repairs', name: 'repairs', component: RepairsPage, meta: { requiresAuth: true, area: 'student' } },
    { path: '/admin', name: 'admin', component: AdminPage, meta: { requiresAuth: true, requiresAdmin: true, area: 'admin' } },
  ],
})

router.beforeEach((to) => {
  const token = accessToken.value
  const currentRole = role.value

  if (to.name === 'login' && token) return { name: currentRole === 'ADMIN' ? 'admin' : 'assistant' }
  if (to.meta.requiresAuth && !token) return { name: 'login' }
  if (to.meta.requiresAdmin && currentRole !== 'ADMIN') return { name: 'assistant' }
  return true
})

export default router
