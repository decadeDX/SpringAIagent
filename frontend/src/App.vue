<script setup lang="ts">
import { computed, ref } from 'vue'
import { RouterLink, RouterView, useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()
const isLoginPage = computed(() => route.name === 'login')
const isAdminArea = computed(() => route.meta.area === 'admin')
const username = computed(() => localStorage.getItem('username') ?? '当前用户')
const role = computed(() => localStorage.getItem('userRole') ?? 'STUDENT')
const trainingStatus = computed(() => localStorage.getItem('trainingStatus') ?? 'PENDING')
const accountMenuOpen = ref(false)

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

function logout() {
  localStorage.removeItem('accessToken')
  localStorage.removeItem('username')
  localStorage.removeItem('userRole')
  localStorage.removeItem('trainingStatus')
  accountMenuOpen.value = false
  router.push({ name: 'login' })
}
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
            <button class="text-button" @click="logout">退出登录</button>
          </div>
        </div>
      </header>
      <RouterView />
    </main>
  </div>
</template>
