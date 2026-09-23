<script setup lang="ts">
import { onMounted, ref } from 'vue'
import {
  disableKnowledgeDocument,
  getKnowledgeDocuments,
  getLabs,
  getSubmittedRepairTickets,
  processRepairTicket,
  publishKnowledgeDocument,
  updateLabStatus,
  uploadKnowledgeDocument,
} from '../api/admin'
import type { KnowledgeDocument, Lab, RepairTicket } from '../types/api'

const labs = ref<Lab[]>([])
const tickets = ref<RepairTicket[]>([])
const documents = ref<KnowledgeDocument[]>([])
const resolutionNote = ref('')
const uploadFile = ref<File>()
const uploadForm = ref({ logicalDocumentCode: '', title: '', version: 'v1.0', effectiveAt: localDateTime() })
const loading = ref(true)
const errorMessage = ref('')

function localDateTime() {
  const now = new Date()
  now.setMinutes(now.getMinutes() - now.getTimezoneOffset())
  return now.toISOString().slice(0, 16)
}

async function load() {
  loading.value = true
  try {
    ;[labs.value, tickets.value, documents.value] = await Promise.all([
      getLabs(),
      getSubmittedRepairTickets(),
      getKnowledgeDocuments(),
    ])
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '无法加载管理数据'
  } finally {
    loading.value = false
  }
}

async function toggleLabStatus(lab: Lab) {
  try {
    errorMessage.value = ''
    const status = lab.status === 'ACTIVE' ? 'MAINTENANCE' : 'ACTIVE'
    const updated = await updateLabStatus(lab.id, status)
    labs.value = labs.value.map((item) => item.id === updated.id ? updated : item)
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '无法更新实验室状态'
  }
}

async function processTicket(ticket: RepairTicket) {
  try {
    errorMessage.value = ''
    await processRepairTicket(ticket.id, resolutionNote.value)
    resolutionNote.value = ''
    tickets.value = await getSubmittedRepairTickets()
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '无法更新工单'
  }
}

function selectUploadFile(event: Event) {
  uploadFile.value = (event.target as HTMLInputElement).files?.[0]
}

async function uploadDocument() {
  if (!uploadFile.value) {
    errorMessage.value = '请选择 Markdown 或 TXT 文件'
    return
  }
  try {
    errorMessage.value = ''
    await uploadKnowledgeDocument({ file: uploadFile.value, ...uploadForm.value })
    uploadFile.value = undefined
    uploadForm.value = { logicalDocumentCode: '', title: '', version: 'v1.0', effectiveAt: localDateTime() }
    documents.value = await getKnowledgeDocuments()
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '无法上传知识文档'
  }
}

async function publishDocument(id: number) {
  try {
    errorMessage.value = ''
    const updated = await publishKnowledgeDocument(id)
    documents.value = documents.value.map((item) => item.id === updated.id ? updated : item)
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '无法发布文档'
  }
}

async function disableDocument(id: number) {
  try {
    errorMessage.value = ''
    const updated = await disableKnowledgeDocument(id)
    documents.value = documents.value.map((item) => item.id === updated.id ? updated : item)
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '无法停用文档'
  }
}

onMounted(load)
</script>

<template>
  <header class="page-header">
    <div><span class="eyebrow">ADMINISTRATION</span><h1>管理中心</h1><p>管理实验室状态、工单流转与知识文档版本。</p></div>
  </header>

  <p v-if="errorMessage" class="form-error">{{ errorMessage }}</p>
  <div v-if="loading" class="empty-state">正在加载管理数据…</div>

  <div v-else class="admin-grid">
    <section class="card">
      <h2>实验室状态</h2>
      <template v-for="lab in labs" :key="lab.id">
        <div class="setting-row"><span><strong>{{ lab.id }}</strong><small>{{ lab.name }}</small></span><span class="status-badge" :class="lab.status === 'ACTIVE' ? 'online' : 'cancelled'">{{ lab.status === 'ACTIVE' ? '正常开放' : lab.status === 'MAINTENANCE' ? '维护中' : '已停用' }}</span></div>
        <button class="secondary-button" :disabled="lab.status === 'DISABLED'" @click="toggleLabStatus(lab)">切换为{{ lab.status === 'ACTIVE' ? '维护中' : '正常开放' }}</button>
      </template>
    </section>

    <section class="card">
      <h2>待处理工单</h2>
      <template v-if="tickets.length">
        <article v-for="ticket in tickets" :key="ticket.id" class="ticket">
          <div><strong>{{ ticket.ticketNo }}</strong><span>{{ ticket.labId }} · {{ ticket.equipmentInfo }}</span></div>
          <p>{{ ticket.description }}</p>
          <textarea v-model="resolutionNote" rows="3" placeholder="填写处理说明后更新状态" />
          <button class="primary-button" @click="processTicket(ticket)">更新为处理中</button>
        </article>
      </template>
      <p v-else class="empty-state">暂无待处理工单</p>
    </section>
  </div>

  <section v-if="!loading" class="card form-card document-card">
    <h2>上传知识文档</h2>
    <form @submit.prevent="uploadDocument">
      <label>源文件<input type="file" accept=".md,.txt,text/markdown,text/plain" @change="selectUploadFile" /></label>
      <label>逻辑文档编号<input v-model="uploadForm.logicalDocumentCode" required /></label>
      <label>文档标题<input v-model="uploadForm.title" required /></label>
      <label>版本<input v-model="uploadForm.version" required /></label>
      <label>生效时间<input v-model="uploadForm.effectiveAt" type="datetime-local" required /></label>
      <button class="primary-button">上传 Markdown / TXT</button>
    </form>
  </section>

  <section v-if="!loading" class="card table-card document-card">
    <div class="section-title"><div><h2>知识文档</h2><p>上传后的文档需要索引成功才可发布。</p></div></div>
    <table>
      <thead><tr><th>文档</th><th>版本</th><th>分块</th><th>索引状态</th><th>发布状态</th><th>操作</th></tr></thead>
      <tbody>
        <tr v-for="document in documents" :key="document.id">
          <td><strong>{{ document.title }}</strong><small>{{ document.logicalDocumentCode }}</small></td>
          <td>{{ document.version }}</td><td>{{ document.chunkCount }}</td>
          <td><span class="status-badge" :class="document.indexStatus === 'SUCCEEDED' ? 'online' : 'processing'">{{ document.indexStatus }}</span></td>
          <td>{{ document.publishStatus }}</td>
          <td><button v-if="document.publishStatus !== 'PUBLISHED'" class="text-button" :disabled="document.indexStatus !== 'SUCCEEDED'" @click="publishDocument(document.id)">发布</button><button v-else class="text-button" @click="disableDocument(document.id)">停用</button></td>
        </tr>
      </tbody>
    </table>
  </section>
</template>
