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
    { path: '/assistant', name: 'assistant', component: AssistantPage },
    { path: '/reservations', name: 'reservations', component: ReservationsPage },
    { path: '/repairs', name: 'repairs', component: RepairsPage },
    { path: '/admin', name: 'admin', component: AdminPage },
  ],
})

export default router
