<script setup lang="ts">
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { login } from '../api/auth'
import { updateSession } from '../session'

const router = useRouter()
const route = useRoute()
const username = ref('student01')
const password = ref('student01')
const errorMessage = ref(route.query.reason === 'session-expired' ? '登录已失效，请重新登录' : '')
const submitting = ref(false)

async function submit() {
  errorMessage.value = ''
  submitting.value = true
  try {
    const result = await login(username.value, password.value)
    updateSession(result)
    await router.push({ name: result.user.role === 'ADMIN' ? 'admin' : 'assistant' })
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '登录失败，请稍后重试'
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <section class="login-card">
      <span class="eyebrow">CAMPUS LABORATORY SERVICE</span>
      <h1>校园实验室<br />智能服务平台</h1>
      <p>查询制度、预约实验室、提交报修，一站完成。</p>

      <form @submit.prevent="submit">
        <label>
          账号
          <input v-model="username" autocomplete="username" placeholder="student01" />
        </label>
        <label>
          密码
          <input v-model="password" type="password" autocomplete="current-password" placeholder="请输入密码" />
        </label>
        <p v-if="errorMessage" class="form-error">{{ errorMessage }}</p>
        <button class="primary-button login-button" :disabled="submitting">
          {{ submitting ? '登录中…' : '登录' }}
        </button>
      </form>

      <div class="demo-account">演示账号：student01 / student01，admin01 / admin01</div>
    </section>
  </div>
</template>
