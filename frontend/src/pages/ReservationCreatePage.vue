<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { confirmAction } from '../api/action'
import { getLabAvailability, getLabs, prepareReservation } from '../api/reservation'
import type { ActionDraft, Lab, LabAvailabilitySlot } from '../types/api'
import './reservation-create.css'

const router = useRouter()
const labs = ref<Lab[]>([])
const selectedLabId = ref('')
const selectedDate = ref('')
const slots = ref<LabAvailabilitySlot[]>([])
const selectedStartTime = ref('')
const duration = ref(1)
const participantCount = ref(1)
const draft = ref<ActionDraft>()
const loadingLabs = ref(true)
const loadingSlots = ref(false)
const errorMessage = ref('')

const selectedLab = computed(() => labs.value.find((lab) => lab.id === selectedLabId.value))
const maxDate = computed(() => addDays(shanghaiDate(new Date()), 7))
const selectedSlotIndex = computed(() => slots.value.findIndex((slot) => slot.startTime === selectedStartTime.value))
const selectedEndTime = computed(() => {
  const endSlot = slots.value[selectedSlotIndex.value + duration.value - 1]
  return endSlot?.endTime ?? ''
})
const dateError = computed(() => {
  if (!selectedDate.value) return ''
  if (selectedDate.value < shanghaiDate(new Date()) || selectedDate.value > maxDate.value) return '请选择未来 7 天内的日期。'
  if (isWeekend(selectedDate.value)) return '实验室仅在工作日开放预约。'
  return ''
})

onMounted(async () => {
  try {
    labs.value = await getLabs()
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '无法加载实验室列表'
  } finally {
    loadingLabs.value = false
  }
})

watch([selectedLabId, selectedDate], () => {
  selectedStartTime.value = ''
  duration.value = 1
  draft.value = undefined
  void loadAvailability()
})

watch(selectedLab, (lab) => {
  if (lab && participantCount.value > lab.capacity) participantCount.value = lab.capacity
})

async function loadAvailability() {
  slots.value = []
  if (!selectedLabId.value || !selectedDate.value || dateError.value) return
  loadingSlots.value = true
  try {
    errorMessage.value = ''
    slots.value = (await getLabAvailability(selectedLabId.value, selectedDate.value)).slots
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '无法加载可用时段'
  } finally {
    loadingSlots.value = false
  }
}

function canUseDuration(hours: number) {
  const startIndex = selectedSlotIndex.value
  if (startIndex < 0) return false
  const selection = slots.value.slice(startIndex, startIndex + hours)
  return selection.length === hours && selection.every((slot, index) =>
    slot.available && (index === 0 || selection[index - 1].endTime === slot.startTime))
}

function selectStart(slot: LabAvailabilitySlot) {
  if (!slot.available) return
  selectedStartTime.value = slot.startTime
  if (!canUseDuration(duration.value)) duration.value = 1
  draft.value = undefined
}

async function createDraft() {
  if (!selectedLab.value || !selectedStartTime.value || !selectedEndTime.value || !canUseDuration(duration.value)
    || participantCount.value < 1 || participantCount.value > selectedLab.value.capacity) {
    errorMessage.value = '请完整选择实验室、日期、连续时段和参与人数。'
    return
  }
  try {
    errorMessage.value = ''
    draft.value = await prepareReservation({
      labId: selectedLab.value.id,
      startTime: selectedStartTime.value,
      endTime: selectedEndTime.value,
      participantCount: participantCount.value,
    })
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '无法生成预约草案'
  }
}

async function confirm() {
  if (!draft.value) return
  try {
    errorMessage.value = ''
    await confirmAction(draft.value)
    await router.push({ name: 'reservations', query: { created: '1' } })
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '预约确认失败'
    await loadAvailability()
  }
}

function formatTime(value: string) {
  return value.slice(11, 16)
}

function formatDateTime(value: string) {
  return `${value.slice(0, 10)} ${formatTime(value)}`
}

function shanghaiDate(value: Date) {
  return new Intl.DateTimeFormat('en-CA', { timeZone: 'Asia/Shanghai' }).format(value)
}

function addDays(date: string, days: number) {
  const value = new Date(`${date}T12:00:00+08:00`)
  value.setUTCDate(value.getUTCDate() + days)
  return shanghaiDate(value)
}

function isWeekend(date: string) {
  const day = new Date(`${date}T12:00:00+08:00`).getUTCDay()
  return day === 0 || day === 6
}
</script>

<template>
  <header class="page-header">
    <div>
      <span class="eyebrow">NEW BOOKING</span>
      <h1>新建预约</h1>
      <p>预约需在确认时再次校验培训资格、可用时段和有效预约数量。</p>
    </div>
    <RouterLink class="secondary-button" :to="{ name: 'reservations' }">返回我的预约</RouterLink>
  </header>

  <div class="reservation-create-layout">
    <section class="card reservation-form-card">
      <form @submit.prevent="createDraft">
        <div class="reservation-step">
          <div class="step-heading"><span>1</span><div><h2>选择实验室</h2><p>仅开放中的实验室可预约。</p></div></div>
          <div v-if="loadingLabs" class="empty-state">正在加载实验室…</div>
          <div v-else class="lab-choice-grid">
            <button
              v-for="lab in labs"
              :key="lab.id"
              type="button"
              class="lab-choice"
              :class="{ selected: selectedLabId === lab.id, unavailable: lab.status !== 'ACTIVE' }"
              :disabled="lab.status !== 'ACTIVE'"
              :aria-pressed="selectedLabId === lab.id"
              @click="selectedLabId = lab.id"
            >
              <strong>{{ lab.name }}</strong>
              <span>{{ lab.id }} · 容量 {{ lab.capacity }} 人</span>
              <small>{{ lab.equipmentDescription }}</small>
              <em v-if="lab.status !== 'ACTIVE'">{{ lab.status === 'MAINTENANCE' ? '维护中，不可预约' : '已停用，不可预约' }}</em>
              <em v-else>{{ lab.openTime }}–{{ lab.closeTime }}</em>
            </button>
          </div>
        </div>

        <div class="reservation-step">
          <div class="step-heading"><span>2</span><div><h2>选择日期和时段</h2><p>可预约 1 至 3 个连续整点时段，仅供参考。</p></div></div>
          <label class="date-field">预约日期<input v-model="selectedDate" type="date" :min="shanghaiDate(new Date())" :max="maxDate" /></label>
          <p v-if="dateError" class="form-error">{{ dateError }}</p>
          <p v-else-if="!selectedLabId || !selectedDate" class="empty-state">请先选择实验室和日期。</p>
          <p v-else-if="loadingSlots" class="empty-state">正在加载可用时段…</p>
          <p v-else-if="slots.length === 0" class="empty-state">当天没有可查询的预约时段。</p>
          <div v-else class="slot-grid">
            <button
              v-for="slot in slots"
              :key="slot.startTime"
              type="button"
              class="slot-button"
              :class="{ selected: selectedStartTime === slot.startTime, unavailable: !slot.available }"
              :disabled="!slot.available"
              :aria-pressed="selectedStartTime === slot.startTime"
              @click="selectStart(slot)"
            >{{ formatTime(slot.startTime) }}</button>
          </div>
          <div v-if="selectedStartTime" class="duration-options">
            <span>预约时长</span>
            <button v-for="hours in [1, 2, 3]" :key="hours" type="button" :class="{ selected: duration === hours }" :disabled="!canUseDuration(hours)" @click="duration = hours">{{ hours }} 小时</button>
          </div>
        </div>

        <div class="reservation-step participant-field">
          <div class="step-heading"><span>3</span><div><h2>填写参与人数</h2><p>人数不能超过所选实验室容量。</p></div></div>
          <label>参与人数<input v-model.number="participantCount" type="number" min="1" :max="selectedLab?.capacity" required /></label>
        </div>

        <p v-if="errorMessage" class="form-error">{{ errorMessage }}</p>
        <button class="primary-button" :disabled="!selectedLab || !selectedDate || !selectedStartTime || !selectedEndTime || loadingSlots">生成待确认草案</button>
      </form>
    </section>

    <aside class="card reservation-draft-card">
      <template v-if="draft && selectedLab">
        <span class="eyebrow">CONFIRM BOOKING</span>
        <h2>确认预约单</h2>
        <dl class="detail-list">
          <dt>实验室</dt><dd>{{ selectedLab.name }}</dd>
          <dt>使用时间</dt><dd>{{ formatDateTime(selectedStartTime) }} 至 {{ formatTime(selectedEndTime) }}</dd>
          <dt>参与人数</dt><dd>{{ participantCount }} 人</dd>
          <dt>草案到期</dt><dd>{{ formatDateTime(draft.expiresAt) }}</dd>
        </dl>
        <p v-for="notice in draft.notices" :key="notice" class="notice">{{ notice }}</p>
        <p class="notice">确认时会重新校验，展示为空闲的时段仍可能因他人先确认而创建失败。</p>
        <button class="primary-button full-width" @click="confirm">确认创建预约</button>
        <button class="text-button" @click="draft = undefined">返回修改</button>
      </template>
      <template v-else>
        <span class="eyebrow">BOOKING DRAFT</span>
        <h2>待确认预约单</h2>
        <p class="empty-state">完成选择后生成草案，再确认提交预约。</p>
      </template>
    </aside>
  </div>
</template>
