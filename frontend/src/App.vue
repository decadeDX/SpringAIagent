<script setup lang="ts">
import { computed } from 'vue'
import { RouterLink, RouterView, useRoute } from 'vue-router'

const route = useRoute()
const isLoginPage = computed(() => route.name === 'login')

const navigation = [
  { name: 'assistant', label: '智能助手', icon: '✦' },
  { name: 'reservations', label: '我的预约', icon: '▣' },
  { name: 'repairs', label: '我的报修', icon: '⌁' },
  { name: 'admin', label: '管理中心', icon: '⌘' },
]
</script>

<template>
  <RouterView v-if="isLoginPage" />

  <div v-else class="app-shell">
    <aside class="sidebar">
      <RouterLink class="brand" :to="{ name: 'assistant' }">
        <span class="brand-mark">L</span>
        <span>Lab Assistant</span>
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
        <span class="avatar">S</span>
        <span>
          <strong>student01</strong>
          <small>已通过安全培训</small>
        </span>
      </div>
    </aside>

    <main class="page-content">
      <RouterView />
    </main>
  </div>
</template>
