<script setup lang="ts">
defineOptions({ name: 'AssistantPage' })

import { onMounted, ref } from 'vue'
import { confirmAction } from '../api/action'
import { createChatSession, sendChatMessage } from '../api/chat'
import { chatSessionId, confirmed, draft, messages } from '../assistantState'

const input = ref('我们 3 个人下周二 14 点到 16 点需要 GPU 实验室，帮我安排一下。')
const sending = ref(false)
const errorMessage = ref('')

onMounted(async () => {
  if (chatSessionId.value) return
  try {
    chatSessionId.value = (await createChatSession()).sessionId
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '无法创建聊天会话'
  }
})

function formatTime(value: unknown) {
  return typeof value === 'string' ? value.replace('T', ' ').replace('+08:00', '') : String(value)
}

async function send() {
  const content = input.value.trim()
  if (!content || sending.value || !chatSessionId.value) return
  messages.value.push({ role: 'user', content })
  input.value = ''
  sending.value = true
  errorMessage.value = ''
  try {
    const response = await sendChatMessage(chatSessionId.value, content)
    messages.value.push({ role: 'assistant', content: response.answer, citations: response.citations })
    draft.value = response.draft
    confirmed.value = false
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '助手请求失败'
  } finally {
    sending.value = false
  }
}

async function confirmDraft() {
  if (!draft.value) return
  errorMessage.value = ''
  try {
    const result = await confirmAction(draft.value)
    confirmed.value = true
    messages.value.push({ role: 'assistant', content: `操作已确认：${JSON.stringify(result.result)}` })
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '确认失败'
  }
}
</script>

<template>
  <header class="page-header">
    <div>
      <span class="eyebrow">AI WORKSPACE</span>
      <h1>智能助手</h1>
      <p>用自然语言查询制度与实时业务；所有修改操作均需你明确确认。</p>
    </div>
    <span class="status-badge online">服务可用</span>
  </header>

  <div class="assistant-layout">
    <section class="chat-panel card">
      <div class="chat-log">
        <article v-for="(message, index) in messages" :key="index" class="message" :class="message.role">
          <span class="message-role">{{ message.role === 'user' ? '我' : '助手' }}</span>
          <div>
            <p>{{ message.content }}</p>
            <div v-if="message.citations?.length" class="citations">
              <div v-for="citation in message.citations" :key="citation.chunkId" class="citation">
                <strong>[{{ citation.chunkId }}] {{ citation.documentTitle }} · {{ citation.version }}</strong>
                <span>{{ citation.excerpt }}</span>
              </div>
            </div>
          </div>
        </article>
      </div>
      <form class="chat-input" @submit.prevent="send">
        <textarea v-model="input" rows="3" placeholder="例如：帮我预约下周二下午的 GPU 实验室" />
        <button class="primary-button" :disabled="sending">{{ sending ? '处理中…' : '发送' }}</button>
      </form>
      <p v-if="errorMessage" class="form-error">{{ errorMessage }}</p>
    </section>

    <aside class="draft-panel">
      <section class="card draft-card">
        <span class="eyebrow">PENDING ACTION</span>
        <h2>待确认操作</h2>
        <template v-if="draft">
          <dl class="detail-list">
            <template v-for="(value, key) in draft.payload" :key="String(key)">
              <dt>{{ key === 'labName' ? '实验室' : key === 'startTime' ? '开始时间' : key === 'endTime' ? '结束时间' : key === 'participantCount' ? '预约人数' : String(key) }}</dt>
              <dd>{{ key === 'startTime' || key === 'endTime' ? formatTime(value) : value }}</dd>
            </template>
            <dt>草案有效期</dt><dd>{{ formatTime(draft.expiresAt) }}</dd>
          </dl>
          <p v-for="notice in draft.notices" :key="notice" class="notice">{{ notice }}</p>
          <button v-if="!confirmed" class="primary-button full-width" @click="confirmDraft">确认提交</button>
          <p v-else class="success-message">已确认。接入后端后将返回真实预约编号。</p>
        </template>
        <p v-else class="empty-state">对话生成草案后，完整的操作参数会显示在这里。</p>
      </section>
    </aside>
  </div>
</template>
