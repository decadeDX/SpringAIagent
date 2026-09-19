<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { confirmCancellation, getMyReservations, prepareCancellation } from '../api/reservation'
import type { ActionDraft, Reservation } from '../types/api'

const reservations = ref<Reservation[]>([])
const draft = ref<ActionDraft>()
const selectedReservation = ref<Reservation>()
const loading = ref(true)

const activeReservations = computed(() => reservations.value.filter((item) => item.status === 'CONFIRMED'))

onMounted(async () => {
  reservations.value = await getMyReservations()
  loading.value = false
})

async function createCancellationDraft(reservation: Reservation) {
  selectedReservation.value = reservation
  draft.value = await prepareCancellation(reservation)
}

async function confirm() {
  if (!selectedReservation.value) return
  await confirmCancellation(selectedReservation.value.id)
  draft.value = undefined
}
</script>

<template>
  <header class="page-header">
    <div>
      <span class="eyebrow">MY BOOKINGS</span>
      <h1>我的预约</h1>
      <p>当前有效预约 {{ activeReservations.length }} / 2。取消操作将先生成草案。</p>
    </div>
  </header>

  <section class="card table-card">
    <div v-if="loading" class="empty-state">正在加载预约记录…</div>
    <table v-else>
      <thead><tr><th>预约编号</th><th>实验室</th><th>使用时间</th><th>人数</th><th>状态</th><th>操作</th></tr></thead>
      <tbody>
        <tr v-for="reservation in reservations" :key="reservation.id">
          <td>{{ reservation.reservationNo }}</td>
          <td><strong>{{ reservation.labName }}</strong><small>{{ reservation.labId }}</small></td>
          <td>{{ reservation.startTime.slice(0, 16).replace('T', ' ') }}<br />至 {{ reservation.endTime.slice(11, 16) }}</td>
          <td>{{ reservation.participantCount }} 人</td>
          <td><span class="status-badge" :class="reservation.status.toLowerCase()">{{ reservation.status === 'CONFIRMED' ? '已确认' : '已取消' }}</span></td>
          <td><button v-if="reservation.status === 'CONFIRMED'" class="text-button" @click="createCancellationDraft(reservation)">申请取消</button></td>
        </tr>
      </tbody>
    </table>
  </section>

  <div v-if="draft && selectedReservation" class="modal-backdrop">
    <section class="modal-card">
      <span class="eyebrow">CANCEL RESERVATION</span>
      <h2>确认取消预约？</h2>
      <p>你即将取消 <strong>{{ selectedReservation.labName }}</strong> 于 {{ selectedReservation.startTime.slice(0, 16).replace('T', ' ') }} 的预约。</p>
      <p class="notice">确认时后端会再次校验预约归属和距离开始时间是否至少 30 分钟。</p>
      <div class="modal-actions"><button class="secondary-button" @click="draft = undefined">返回</button><button class="danger-button" @click="confirm">确认取消</button></div>
    </section>
  </div>
</template>
