<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { RouterLink, RouterView, useRoute, useRouter } from 'vue-router'
import { request, useMockApi } from './api/http'
import { accessToken, clearSession, role, SESSION_STORAGE_KEY, sessionExpired, synchronizeSession, trainingStatus, username } from './session'

const route = useRoute()
const router = useRouter()
const isLoginPage = computed(() => route.name === 'login')
const isAdminArea = computed(() => route.meta.area === 'admin')
const accountMenuOpen = ref(false)
const logoutErrorMessage = ref('')
let sessionCheckTimer: ReturnType<typeof setInterval> | undefined

const studentNavigation = [
  { name: 'assistant', label: '智能助手', icon: '✦' },
  { name: 'reservations', label: '我的预约', icon: '▣' },
  { name: 'repairs', label: '我的报修', icon: '⌁' },
]

const adminNavigation = [
  { name: 'admin', label: '管理中心', icon: '⌘' },
]

const navigation = computed(() => isAdminArea.value ? adminNavigation : studentNavigation)
const accountDescription = computed(() => role.value === 'ADMIN' ? '管理员账户' : trainingStatus.value === 'PASSED' ? '已通过安全培训' : '未通过安全培训')

function homeRoute() {
  return { name: role.value === 'ADMIN' ? 'admin' : 'assistant' }
}

function stopSessionCheck() {
  if (sessionCheckTimer) clearInterval(sessionCheckTimer)
  sessionCheckTimer = undefined
}

function startSessionCheck() {
  stopSessionCheck()
  if (useMockApi || !accessToken.value) return
  sessionCheckTimer = setInterval(() => {
    void request<void>('/auth/session').catch(() => undefined)
  }, 30_000)
}

function handleStorage(event: StorageEvent) {
  if (event.key !== SESSION_STORAGE_KEY && event.key !== null) return
  synchronizeSession()
  accountMenuOpen.value = false
  logoutErrorMessage.value = ''
  void router.replace(accessToken.value ? homeRoute() : { name: 'login' })
}

async function logout() {
  logoutErrorMessage.value = ''
  try {
    if (!useMockApi) await request<void>('/auth/logout', { method: 'POST' })
    clearSession()
    accountMenuOpen.value = false
    await router.push({ name: 'login' })
  } catch (error) {
    logoutErrorMessage.value = error instanceof Error ? error.message : '退出登录失败，请稍后重试'
  }
}

watch(accessToken, (token, previousToken) => {
  if (token) {
    startSessionCheck()
    return
  }
  stopSessionCheck()
  if (previousToken && route.name !== 'login') {
    void router.push({ name: 'login', query: sessionExpired.value ? { reason: 'session-expired' } : {} })
  }
}, { immediate: true })

onMounted(() => window.addEventListener('storage', handleStorage))
onBeforeUnmount(() => {
  window.removeEventListener('storage', handleStorage)
  stopSessionCheck()
})
</script>

<template>
  <RouterView v-if="isLoginPage" />

  <div v-else class="app-shell">
    <aside class="sidebar">
      <RouterLink class="brand" :to="{ name: isAdminArea ? 'admin' : 'assistant' }">
        <span class="brand-mark">L</span>
        <span>{{ isAdminArea ? '管理后台' : 'Lab Assistant' }}</span>
      </RouterLink>

      <nav aria-label="主导航">
        <RouterLink
          v-for="item in navigation"
          :key="item.name"
          class="nav-item"
          :to="{ name: item.name }"
        >
          <span aria-hidden="true">{{ item.icon }}</span>
          {{ item.label }}
        </RouterLink>
      </nav>

    </aside>

    <main class="page-content">
      <header class="topbar">
        <span>{{ isAdminArea ? '管理后台' : '学生服务' }}</span>
        <div class="account-menu">
          <button class="account-trigger" @click="accountMenuOpen = !accountMenuOpen">
            <span class="avatar">{{ username.slice(0, 1).toUpperCase() }}</span>
            <span>{{ username }}</span>
          </button>
          <div v-if="accountMenuOpen" class="account-dropdown">
            <strong>{{ username }}</strong>
            <small>{{ accountDescription }}</small>
            <p v-if="logoutErrorMessage" class="form-error">{{ logoutErrorMessage }}</p>
            <button class="text-button" @click="logout">退出登录</button>
          </div>
        </div>
      </header>
      <RouterView />
    </main>
  </div>
</template>
