<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getDoctorById } from '@/services/medical/yishengguanli'
import { getAppointmentSlots, createAppointment } from '@/services/medical/yuyueguanli'
import { listScheduleByDoctor } from '@/services/medical/paibanguanli'

const router = useRouter()
const route = useRoute()

const loading = ref(false)
const submitting = ref(false)
const doctor = ref<any>(null)
const selectedDate = ref('')
const selectedPeriod = ref('')
const availableDates = ref<string[]>([])
const availablePeriods = ref<any[]>([])

const loadData = async () => {
  loading.value = true
  const doctorId = route.params.doctorId as string
  try {
    const doctorRes = await getDoctorById({ id: Number(doctorId) })
    if (doctorRes.data) {
      doctor.value = doctorRes.data
    }
    
    const scheduleRes = await listScheduleByDoctor({ doctorId: Number(doctorId) })
    if (scheduleRes.data) {
      const uniqueDates = [...new Set(scheduleRes.data.map((s: any) => s.scheduleDate))]
      availableDates.value = uniqueDates
    }
  } catch (error) {
    console.error('加载医生信息失败', error)
  } finally {
    loading.value = false
  }
}

const onDateSelect = async (date: string) => {
  selectedDate.value = date
  selectedPeriod.value = ''
  const doctorId = route.params.doctorId as string
  try {
    const res = await getAppointmentSlots({ 
      scheduleId: Number(doctorId),
      date: date 
    })
    if (res.data) {
      availablePeriods.value = res.data.map((slot: any) => ({
        period: slot.timeSlot,
        available: slot.availableCount > 0
      }))
    }
  } catch (error) {
    console.error('加载时段信息失败', error)
  }
}

const handleSubmit = async () => {
  if (!selectedDate.value || !selectedPeriod.value) {
    ElMessage.warning('请选择就诊日期和时段')
    return
  }
  
  submitting.value = true
  try {
    const doctorId = route.params.doctorId as string
    await createAppointment({
      scheduleId: Number(doctorId),
      date: selectedDate.value,
      timeSlot: selectedPeriod.value
    })
    ElMessage.success('预约成功！')
    router.push('/patient/my-appointments')
  } catch (error) {
    ElMessage.error('预约失败')
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  loadData()
})
</script>

<template>
  <div class="booking-page" v-loading="loading">
    <el-card v-if="doctor" class="booking-card">
      <template #header>
        <span>预约挂号</span>
      </template>
      
      <div class="doctor-info">
        <el-avatar :size="60" :src="doctor.avatarUrl" style="background: #14b8a6;" />
        <div>
          <h3>{{ doctor.doctorName }}</h3>
          <p>{{ doctor.department }} · {{ doctor.doctorTitle }}</p>
          <p class="price">挂号费：¥{{ doctor.registrationFee }}</p>
        </div>
      </div>
      
      <el-divider />
      
      <div class="section">
        <h4>选择就诊日期</h4>
        <div class="date-list">
          <div 
            v-for="date in availableDates" 
            :key="date"
            class="date-item"
            :class="{ active: selectedDate === date }"
            @click="onDateSelect(date)"
          >
            {{ date }}
          </div>
        </div>
      </div>
      
      <div class="section" v-if="selectedDate">
        <h4>选择就诊时段</h4>
        <div class="period-list">
          <div 
            v-for="p in availablePeriods" 
            :key="p.period"
            class="period-item"
            :class="{ active: selectedPeriod === p.period, disabled: !p.available }"
            @click="p.available && (selectedPeriod = p.period)"
          >
            {{ p.period }}
            <span class="status" v-if="!p.available">已约满</span>
          </div>
        </div>
      </div>
      
      <el-divider v-if="selectedDate && selectedPeriod" />
      
      <div class="summary" v-if="selectedDate && selectedPeriod">
        <h4>预约信息确认</h4>
        <div class="summary-row">
          <span>医生：</span>
          <span>{{ doctor.doctorName }}</span>
        </div>
        <div class="summary-row">
          <span>科室：</span>
          <span>{{ doctor.department }}</span>
        </div>
        <div class="summary-row">
          <span>就诊日期：</span>
          <span>{{ selectedDate }}</span>
        </div>
        <div class="summary-row">
          <span>就诊时段：</span>
          <span>{{ selectedPeriod }}</span>
        </div>
        <div class="summary-row total">
          <span>挂号费：</span>
          <span class="price">¥{{ doctor.registrationFee }}</span>
        </div>
      </div>
      
      <div class="actions">
        <el-button @click="router.back()">取消</el-button>
        <el-button type="primary" :loading="submitting" :disabled="!selectedDate || !selectedPeriod" @click="handleSubmit">确认预约</el-button>
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.booking-page {
  max-width: 700px;
}

.doctor-info {
  display: flex;
  gap: 20px;
  align-items: center;
}

.doctor-info h3 {
  margin: 0 0 4px 0;
}

.doctor-info p {
  margin: 4px 0;
  color: #6b7280;
}

.price {
  color: #14b8a6;
  font-weight: bold;
}

.section {
  margin: 24px 0;
}

.section h4 {
  margin: 0 0 16px 0;
  color: #1f2937;
}

.date-list, .period-list {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.date-item, .period-item {
  padding: 12px 24px;
  border: 2px solid #e5e7eb;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  position: relative;
}

.date-item:hover, .period-item:hover:not(.disabled) {
  border-color: #06b6d4;
}

.date-item.active, .period-item.active {
  border-color: #06b6d4;
  background: rgba(6, 182, 212, 0.1);
}

.period-item.disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

.status {
  display: block;
  font-size: 12px;
  color: #ef4444;
}

.summary h4 {
  margin: 0 0 16px 0;
}

.summary-row {
  display: flex;
  justify-content: space-between;
  padding: 8px 0;
}

.summary-row.total {
  border-top: 1px solid #e5e7eb;
  padding-top: 16px;
  margin-top: 8px;
  font-weight: bold;
}

.price {
  color: #14b8a6;
  font-size: 20px;
}

.actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 24px;
}
</style>
