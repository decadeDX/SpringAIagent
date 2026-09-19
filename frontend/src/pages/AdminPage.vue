<script setup lang="ts">
import { ref } from 'vue'
import { mockDocuments } from '../mocks/data'

const documents = ref(mockDocuments)
const labStatus = ref<'NORMAL' | 'MAINTENANCE'>('NORMAL')

function publishDocument(id: number) {
  const document = documents.value.find((item) => item.id === id)
  if (document?.indexStatus === 'SUCCEEDED') document.publishStatus = 'PUBLISHED'
}

function disableDocument(id: number) {
  const document = documents.value.find((item) => item.id === id)
  if (document) document.publishStatus = 'DISABLED'
}
</script>

<template>
  <header class="page-header">
    <div><span class="eyebrow">ADMINISTRATION</span><h1>管理中心</h1><p>管理实验室状态、工单流转与知识文档版本。</p></div>
  </header>

  <div class="admin-grid">
    <section class="card">
      <h2>实验室状态</h2>
      <div class="setting-row"><span><strong>LAB-B402</strong><small>人工智能实验室</small></span><span class="status-badge" :class="labStatus === 'NORMAL' ? 'online' : 'cancelled'">{{ labStatus === 'NORMAL' ? '正常开放' : '维护中' }}</span></div>
      <button class="secondary-button" @click="labStatus = labStatus === 'NORMAL' ? 'MAINTENANCE' : 'NORMAL'">切换为{{ labStatus === 'NORMAL' ? '维护中' : '正常开放' }}</button>
    </section>

    <section class="card">
      <h2>待处理工单</h2>
      <p><strong>TKT-20260918001</strong> · LAB-A301 工作站 A301-12</p>
      <textarea rows="3" placeholder="填写处理说明后更新状态" />
      <button class="primary-button">更新为处理中</button>
    </section>
  </div>

  <section class="card table-card document-card">
    <div class="section-title"><div><h2>知识文档</h2><p>上传后的文档需要索引成功才可发布。</p></div><button class="primary-button">上传 Markdown / TXT</button></div>
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
