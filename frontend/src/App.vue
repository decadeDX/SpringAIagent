<script setup lang="ts">
import { computed } from 'vue'
import { RouterLink, RouterView, useRoute } from 'vue-router'

const route = useRoute()
const isLoginPage = computed(() => route.name === 'login')
const isAdminArea = computed(() => route.meta.area === 'admin')
const username = computed(() => localStorage.getItem('username') ?? '当前用户')
const role = computed(() => localStorage.getItem('userRole') ?? 'STUDENT')
const trainingStatus = computed(() => localStorage.getItem('trainingStatus') ?? 'PENDING')

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

      <div class="current-user">
        <span class="avatar">{{ username.slice(0, 1).toUpperCase() }}</span>
        <span>
          <strong>{{ username }}</strong>
          <small>{{ accountDescription }}</small>
        </span>
      </div>
    </aside>

    <main class="page-content">
      <RouterView />
    </main>
  </div>
</template>
