<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { getMyRepairTickets, prepareRepair } from '../api/repair'
import type { ActionDraft, RepairTicket } from '../types/api'

const tickets = ref<RepairTicket[]>([])
const draft = ref<ActionDraft>()
const form = ref({ labId: 'LAB-B402', equipmentInfo: 'GPU-03', description: '开机后有焦糊味。' })

onMounted(async () => { tickets.value = await getMyRepairTickets() })

async function createDraft() {
  draft.value = await prepareRepair(form.value)
}
</script>

<template>
  <header class="page-header">
    <div><span class="eyebrow">MY REPAIR TICKETS</span><h1>我的报修</h1><p>发现冒烟、漏电或焦糊味时，请立即停止使用并联系管理员。</p></div>
  </header>

  <div class="two-column">
    <section class="card form-card">
      <h2>创建报修草案</h2>
      <form @submit.prevent="createDraft">
        <label>实验室<input v-model="form.labId" /></label>
        <label>设备名称或资产编号<input v-model="form.equipmentInfo" /></label>
        <label>故障描述<textarea v-model="form.description" rows="4" /></label>
        <button class="primary-button">生成待确认草案</button>
      </form>
      <div v-if="draft" class="draft-preview">
        <strong>草案已生成</strong>
        <p>已识别安全风险。请停止使用设备，避免自行维修，并联系管理员。</p>
        <button class="primary-button full-width">确认提交</button>
      </div>
    </section>

    <section class="card tickets-card">
      <h2>工单记录</h2>
      <article v-for="ticket in tickets" :key="ticket.id" class="ticket">
        <div><strong>{{ ticket.ticketNo }}</strong><span>{{ ticket.labId }} · {{ ticket.equipmentInfo }}</span></div>
        <span class="status-badge processing">处理中</span>
        <p>{{ ticket.description }}</p>
      </article>
    </section>
  </div>
</template>
