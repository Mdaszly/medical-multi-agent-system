<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import {
  listDepartmentWeekStatus,
  listDepartmentDoctorBooking,
} from '@/services/medical/yuyueguanli'
import { BOOKING_THEME } from '@/constants/departments'

const router = useRouter()
const route = useRoute()

const department = computed(() => decodeURIComponent(route.params.department as string))

const loading = ref(false)
const weekDays = ref<API.DepartmentDateStatusVO[]>([])
const doctors = ref<API.DepartmentDoctorBookingVO[]>([])
const selectedDate = ref('')

const formatDateParam = (d: string | undefined): string => {
  if (!d) return ''
  return d.split('T')[0] || ''
}

const loadWeek = async () => {
  const res = await listDepartmentWeekStatus({
    department: department.value,
    days: 7,
  })
  weekDays.value = res.data || []
  if (!selectedDate.value && weekDays.value.length > 0) {
    const first =
      weekDays.value.find((d) => d.hasAvailable) || weekDays.value[0]
    if (first) {
      selectedDate.value = formatDateParam(first.scheduleDate || '')
    }
  }
}

const loadDoctors = async () => {
  if (!selectedDate.value) return
  loading.value = true
  try {
    const res = await listDepartmentDoctorBooking({
      department: department.value,
      scheduleDate: selectedDate.value,
    })
    doctors.value = res.data || []
  } catch (e: any) {
    ElMessage.error(e?.message || '加载医生列表失败')
  } finally {
    loading.value = false
  }
}

const selectDate = (day: API.DepartmentDateStatusVO) => {
  if (day.allFull && !day.hasAvailable) return
  selectedDate.value = formatDateParam(day.scheduleDate ?? '')
}

const remainText = (doc: API.DepartmentDoctorBookingVO) => {
  const parts: string[] = []
  if (doc.morningRemaining != null && doc.morningRemaining > 0) {
    parts.push(`上午:${doc.morningRemaining}`)
  }
  if (doc.afternoonRemaining != null && doc.afternoonRemaining > 0) {
    parts.push(`下午:${doc.afternoonRemaining}`)
  }
  if (doc.eveningRemaining != null && doc.eveningRemaining > 0) {
    parts.push(`晚上:${doc.eveningRemaining}`)
  }
  if (!parts.length) return '余号: 0'
  return `余号: ${parts.join(' ')}`
}

const goDoctor = (doc: API.DepartmentDoctorBookingVO) => {
  if (!doc.bookable || !doc.doctorId) {
    ElMessage.info('该医生当日号源已满')
    return
  }
  router.push({
    name: 'DoctorDetail',
    params: { id: String(doc.doctorId) },
    query: { department: department.value, date: selectedDate.value },
  })
}

const goBack = () => router.push({ name: 'DepartmentSelect' })

onMounted(async () => {
  try {
    await loadWeek()
    await loadDoctors()
  } catch {
    ElMessage.error('加载出诊信息失败')
  }
})

watch(selectedDate, loadDoctors)
</script>

<template>
  <div class="dept-doctors-page">
    <header class="page-header">
      <el-button text class="back-btn" @click="goBack">
        <el-icon><ArrowLeft /></el-icon>
        返回科室
      </el-button>
      <h2>{{ department }}</h2>
    </header>

    <section class="week-strip">
      <span class="week-label">本周出诊</span>
      <div class="week-scroll">
        <button
          v-for="day in weekDays"
          :key="day.scheduleDate"
          type="button"
          class="day-item"
          :class="{
            active: formatDateParam(day.scheduleDate || '') === selectedDate,
            full: day.allFull && !day.hasAvailable,
          }"
          @click="selectDate(day)"
        >
          <span class="dow">{{ day.weekDayLabel }}</span>
          <span class="dom">{{ day.dayOfMonth }}</span>
          <span class="status-tag">{{ day.hasAvailable ? '有' : day.allFull ? '满' : '—' }}</span>
        </button>
      </div>
    </section>

    <section class="doctor-list" v-loading="loading">
      <article
        v-for="doc in doctors"
        :key="doc.doctorId"
        class="doctor-row"
        @click="goDoctor(doc)"
      >
        <div class="avatar">{{ doc.doctorName?.charAt(0) }}</div>
        <div class="body">
          <div class="title-line">
            <strong>{{ doc.doctorName }}</strong>
            <span class="title">{{ doc.title }}</span>
          </div>
          <p class="remain">{{ remainText(doc) }}</p>
          <p class="specialty" v-if="doc.specialty">擅长：{{ doc.specialty }}</p>
        </div>
        <button
          type="button"
          class="price-btn"
          :class="{ disabled: !doc.bookable }"
          @click.stop="goDoctor(doc)"
        >
          ¥{{ doc.consultationFee?.toFixed(0) ?? '—' }}
        </button>
      </article>
      <el-empty v-if="!loading && !doctors.length" description="该日暂无出诊医生" />
    </section>
  </div>
</template>

<style scoped>
.dept-doctors-page {
  max-width: 1100px;
  margin: 0 auto;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
  overflow: hidden;
}

.page-header {
  padding: 16px 24px;
  border-bottom: 1px solid #f3f4f6;
}

.page-header h2 {
  margin: 8px 0 0;
  font-size: 20px;
  color: #111827;
}

.back-btn {
  padding-left: 0;
  color: #14b8a6;
}

.week-strip {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 20px;
  border-bottom: 1px solid #f3f4f6;
  background: #fafafa;
}

.week-label {
  flex-shrink: 0;
  font-size: 14px;
  color: #6b7280;
  writing-mode: horizontal-tb;
}

.week-scroll {
  display: flex;
  gap: 10px;
  overflow-x: auto;
  flex: 1;
  padding-bottom: 4px;
}

.day-item {
  flex-shrink: 0;
  width: 56px;
  padding: 8px 4px;
  border: none;
  background: transparent;
  cursor: pointer;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  transition: background 0.2s;
}

.day-item .dow {
  font-size: 12px;
  color: #6b7280;
}

.day-item .dom {
  width: 36px;
  height: 36px;
  line-height: 36px;
  border-radius: 50%;
  font-size: 15px;
  font-weight: 600;
  color: #374151;
}

.day-item.active .dom {
  background: #14b8a6;
  color: #fff;
}

.day-item.full .status-tag {
  color: #9ca3af;
}

.status-tag {
  font-size: 11px;
  color: #14b8a6;
}

.day-item.active .status-tag {
  color: #0d9488;
}

.doctor-list {
  min-height: 200px;
}

.doctor-row {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  padding: 20px 24px;
  border-bottom: 1px solid #f3f4f6;
  cursor: pointer;
  transition: background 0.15s;
}

.doctor-row:hover {
  background: #f9fafb;
}

.avatar {
  width: 72px;
  height: 72px;
  border-radius: 8px;
  background: linear-gradient(135deg, #ccfbf1, #99f6e4);
  color: #0f766e;
  font-size: 28px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.body {
  flex: 1;
  min-width: 0;
}

.title-line {
  display: flex;
  align-items: baseline;
  gap: 8px;
  flex-wrap: wrap;
}

.title-line strong {
  font-size: 17px;
  color: #111827;
}

.title {
  font-size: 14px;
  color: #6b7280;
}

.remain {
  margin: 6px 0 0;
  font-size: 13px;
  color: #14b8a6;
}

.specialty {
  margin: 6px 0 0;
  font-size: 13px;
  color: #9ca3af;
  line-height: 1.45;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.price-btn {
  flex-shrink: 0;
  min-width: 72px;
  height: 36px;
  padding: 0 16px;
  border: none;
  border-radius: 18px;
  background: #f97316;
  color: #fff;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: opacity 0.2s;
}

.price-btn:hover:not(.disabled) {
  opacity: 0.9;
}

.price-btn.disabled {
  background: #d1d5db;
  cursor: not-allowed;
}
</style>
