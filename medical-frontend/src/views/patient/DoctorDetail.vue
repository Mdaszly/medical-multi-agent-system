<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, Star, Share } from '@element-plus/icons-vue'
import { getDoctorById } from '@/services/medical/yishengguanli'
import { listScheduleByDoctor } from '@/services/medical/paibanguanli'
import { getAppointmentSlots, createAppointment } from '@/services/medical/yuyueguanli'

const router = useRouter()
const route = useRoute()

const activeTab = ref('register')
const loading = ref(false)
const submitting = ref(false)
const doctor = ref<any>(null)
const schedules = ref<any[]>([])
const slotRows = ref<any[]>([])

const department = computed(() => (route.query.department as string) || doctor.value?.department || '')
const preferredDate = computed(() => route.query.date as string | undefined)

const loadDoctor = async () => {
  loading.value = true
  const doctorId = Number(route.params.id)
  try {
    const [doctorRes, scheduleRes] = await Promise.all([
      getDoctorById({ id: doctorId }),
      listScheduleByDoctor({ doctorId }),
    ])
    if (doctorRes.data) doctor.value = doctorRes.data
    if (scheduleRes.data) {
      schedules.value = scheduleRes.data
      await buildSlotRows()
    }
  } catch {
    ElMessage.error('加载医生信息失败')
  } finally {
    loading.value = false
  }
}

const buildSlotRows = async () => {
  const rows: any[] = []
  let list = schedules.value
  if (preferredDate.value) {
    list = list.filter((s) => s.scheduleDate === preferredDate.value)
  }
  for (const schedule of list) {
    try {
      const res = await getAppointmentSlots({ scheduleId: schedule.id! })
      const slots = res.data || []
      for (const slot of slots) {
        rows.push({
          scheduleId: schedule.id,
          scheduleDate: schedule.scheduleDate,
          shiftType: schedule.shiftType,
          shiftName: schedule.shiftName,
          timeSlot: slot.timeSlot,
          timeStart: slot.timeStart,
          timeEnd: slot.timeEnd,
          available: (slot.availableSlots ?? 0) > 0,
          availableSlots: slot.availableSlots,
          fee: doctor.value?.consultationFee,
        })
      }
    } catch {
      /* skip schedule without slots */
    }
  }
  rows.sort((a, b) =>
    `${a.scheduleDate}${a.timeStart}`.localeCompare(`${b.scheduleDate}${b.timeStart}`)
  )
  slotRows.value = rows
}

const groupedByDate = computed(() => {
  const map = new Map<string, any[]>()
  for (const row of slotRows.value) {
    const key = row.scheduleDate || ''
    if (!map.has(key)) map.set(key, [])
    map.get(key)!.push(row)
  }
  return [...map.entries()].sort((a, b) => a[0].localeCompare(b[0]))
})

const shiftLabel = (type: string) => {
  const m: Record<string, string> = {
    morning: '上午',
    afternoon: '下午',
    evening: '晚上',
  }
  return m[type] || type
}

const handleBookSlot = async (row: any) => {
  if (!row.available) {
    ElMessage.warning('该时段已约满')
    return
  }
  submitting.value = true
  try {
    await createAppointment({
      scheduleId: row.scheduleId,
      timeSlot: row.timeSlot,
    })
    ElMessage.success('预约成功')
    router.push('/patient/my-appointments')
  } catch (e: any) {
    ElMessage.error(e?.message || '预约失败')
  } finally {
    submitting.value = false
  }
}

const goBack = () => {
  if (department.value) {
    router.push({
      name: 'DepartmentDoctors',
      params: { department: encodeURIComponent(department.value) },
    })
  } else {
    router.push({ name: 'DepartmentSelect' })
  }
}

onMounted(loadDoctor)
</script>

<template>
  <div class="doctor-home-page" v-loading="loading">
    <template v-if="doctor">
      <header class="hero">
        <el-button text class="hero-back" @click="goBack">
          <el-icon><ArrowLeft /></el-icon>
        </el-button>
        <div class="hero-body">
          <div class="hero-avatar">{{ doctor.doctorName?.charAt(0) }}</div>
          <div class="hero-text">
            <h1>{{ doctor.doctorName }}</h1>
            <p class="sub">{{ doctor.title }} · {{ doctor.department }}</p>
            <p class="skill" v-if="doctor.specialty">擅长：{{ doctor.specialty }}</p>
          </div>
          <div class="hero-actions">
            <el-button circle :icon="Star" aria-label="收藏" />
            <el-button circle :icon="Share" aria-label="分享" />
          </div>
        </div>
      </header>

      <el-tabs v-model="activeTab" class="detail-tabs">
        <el-tab-pane label="挂号" name="register">
          <div v-if="groupedByDate.length" class="slot-list">
            <div v-for="[date, rows] in groupedByDate" :key="date" class="date-group">
              <h4 class="date-title">{{ date }}</h4>
              <div
                v-for="row in rows"
                :key="`${row.scheduleId}-${row.timeSlot}`"
                class="slot-row"
                :class="{ disabled: !row.available }"
                @click="handleBookSlot(row)"
              >
                <span class="time">
                  {{ shiftLabel(row.shiftType) }}
                  {{ row.timeStart }}~{{ row.timeEnd }}
                </span>
                <span class="fee">¥ {{ Number(row.fee || 0).toFixed(2) }}</span>
                <span class="chevron">›</span>
              </div>
            </div>
          </div>
          <el-empty v-else description="暂无可预约号源，请返回选择其他日期或医生" />
        </el-tab-pane>

        <el-tab-pane label="医生简介" name="profile">
          <div class="profile-panel">
            <h4>擅长</h4>
            <p>{{ doctor.specialty || '暂无' }}</p>
            <h4>介绍</h4>
            <p>{{ doctor.description || '暂无详细介绍' }}</p>
          </div>
        </el-tab-pane>

        <el-tab-pane label="所有号源" name="all-slots">
          <el-table :data="slotRows" style="width: 100%">
            <el-table-column prop="scheduleDate" label="日期" width="120" />
            <el-table-column label="班次" width="80">
              <template #default="{ row }">{{ shiftLabel(row.shiftType) }}</template>
            </el-table-column>
            <el-table-column prop="timeSlot" label="时段" />
            <el-table-column label="余号" width="80">
              <template #default="{ row }">{{ row.availableSlots ?? 0 }}</template>
            </el-table-column>
            <el-table-column label="操作" width="100">
              <template #default="{ row }">
                <el-button
                  type="primary"
                  size="small"
                  :disabled="!row.available"
                  :loading="submitting"
                  @click="handleBookSlot(row)"
                >
                  预约
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </template>
  </div>
</template>

<style scoped>
.doctor-home-page {
  max-width: 900px;
  margin: 0 auto;
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
}

.hero {
  position: relative;
  background: linear-gradient(135deg, #14b8a6 0%, #0d9488 100%);
  color: #fff;
  padding: 12px 24px 24px;
}

.hero-back {
  color: #fff !important;
  margin-bottom: 8px;
}

.hero-body {
  display: flex;
  gap: 16px;
  align-items: flex-start;
}

.hero-avatar {
  width: 80px;
  height: 80px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.25);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32px;
  font-weight: 700;
  flex-shrink: 0;
}

.hero-text {
  flex: 1;
}

.hero-text h1 {
  margin: 0;
  font-size: 22px;
}

.hero-text .sub {
  margin: 4px 0 0;
  opacity: 0.9;
  font-size: 14px;
}

.hero-text .skill {
  margin: 8px 0 0;
  font-size: 13px;
  opacity: 0.85;
  line-height: 1.4;
}

.hero-actions {
  display: flex;
  gap: 8px;
}

.detail-tabs {
  padding: 0 16px 24px;
}

.detail-tabs :deep(.el-tabs__item.is-active) {
  color: #14b8a6;
}

.detail-tabs :deep(.el-tabs__active-bar) {
  background-color: #14b8a6;
}

.date-title {
  margin: 16px 0 8px;
  color: #6b7280;
  font-size: 14px;
  font-weight: 500;
}

.slot-row {
  display: flex;
  align-items: center;
  padding: 14px 8px;
  border-bottom: 1px solid #f3f4f6;
  cursor: pointer;
  transition: background 0.15s;
}

.slot-row:hover:not(.disabled) {
  background: #f0fdfa;
}

.slot-row.disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.slot-row .time {
  flex: 1;
  color: #111827;
}

.slot-row .fee {
  color: #f97316;
  font-weight: 600;
  margin-right: 8px;
}

.slot-row .chevron {
  color: #d1d5db;
  font-size: 18px;
}

.profile-panel {
  padding: 16px 8px;
  line-height: 1.7;
  color: #374151;
}

.profile-panel h4 {
  margin: 16px 0 8px;
  color: #111827;
}

.profile-panel h4:first-child {
  margin-top: 0;
}
</style>
