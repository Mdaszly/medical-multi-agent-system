<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { getDoctorById } from '@/services/medical/yishengguanli'
import { listScheduleByDoctor } from '@/services/medical/paibanguanli'

const router = useRouter()
const route = useRoute()

const loading = ref(false)
const doctor = ref<any>(null)
const schedules = ref<any[]>([])

const loadData = async () => {
  loading.value = true
  const doctorId = route.params.id as string
  try {
    const [doctorRes, scheduleRes] = await Promise.all([
      getDoctorById({ id: Number(doctorId) }),
      listScheduleByDoctor({ doctorId: Number(doctorId) })
    ])
    if (doctorRes.data) {
      doctor.value = doctorRes.data
    }
    if (scheduleRes.data) {
      schedules.value = scheduleRes.data.map((s: any) => ({
        date: s.scheduleDate,
        periods: [
          { period: '上午', available: s.morningSlots?.length || 0, total: 10 },
          { period: '下午', available: s.afternoonSlots?.length || 0, total: 10 },
          { period: '晚间', available: s.eveningSlots?.length || 0, total: 5 }
        ]
      }))
    }
  } catch (error) {
    console.error('加载数据失败', error)
  } finally {
    loading.value = false
  }
}

const handleBooking = () => {
  router.push(`/patient/booking/${doctor.value.id}`)
}

onMounted(() => {
  loadData()
})
</script>

<template>
  <div class="doctor-detail" v-loading="loading">
    <el-card v-if="doctor" class="info-card">
      <div class="info-content">
        <el-avatar :size="120" :src="doctor.avatarUrl" style="background: #14b8a6;" />
        <div class="info">
          <h2>{{ doctor.doctorName }}</h2>
          <div class="tags" style="margin: 12px 0;">
            <el-tag type="info">{{ doctor.department }}</el-tag>
            <el-tag type="success">{{ doctor.doctorTitle }}</el-tag>
          </div>
          <p class="specialty">擅长：{{ doctor.specialty }}</p>
          <p class="intro">{{ doctor.introduction }}</p>
          <p class="price">挂号费：<strong>¥{{ doctor.registrationFee }}</strong></p>
          <el-button type="primary" size="large" @click="handleBooking">立即预约</el-button>
        </div>
      </div>
    </el-card>
    
    <el-card class="schedule-card">
      <template #header>
        <span>排班信息</span>
      </template>
      <div v-for="schedule in schedules" :key="schedule.date" class="schedule-item">
        <h4>{{ schedule.date }}</h4>
        <div class="periods">
          <div v-for="period in schedule.periods" :key="period.period" class="period" :class="{ full: period.available === 0 }">
            <span>{{ period.period }}</span>
            <span class="status">{{ period.available > 0 ? `可预约(${period.available}/${period.total})` : '已约满' }}</span>
          </div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.doctor-detail {
  max-width: 900px;
}

.info-card {
  margin-bottom: 24px;
}

.info-content {
  display: flex;
  gap: 30px;
}

.info h2 {
  margin: 0 0 8px 0;
}

.specialty {
  color: #06b6d4;
  margin: 0 0 12px 0;
}

.intro {
  color: #6b7280;
  margin: 0 0 16px 0;
  line-height: 1.6;
}

.price {
  margin: 0 0 16px 0;
}

.price strong {
  color: #14b8a6;
  font-size: 20px;
}

.schedule-item {
  margin-bottom: 24px;
  padding-bottom: 24px;
  border-bottom: 1px solid #e5e7eb;
}

.schedule-item:last-child {
  border-bottom: none;
  margin-bottom: 0;
  padding-bottom: 0;
}

.schedule-item h4 {
  margin: 0 0 12px 0;
  color: #1f2937;
}

.periods {
  display: flex;
  gap: 16px;
}

.period {
  padding: 12px 24px;
  border: 2px solid #06b6d4;
  border-radius: 8px;
  text-align: center;
  background: rgba(6, 182, 212, 0.05);
}

.period.full {
  border-color: #d1d5db;
  background: #f9fafb;
  color: #9ca3af;
}

.status {
  display: block;
  margin-top: 4px;
  font-size: 12px;
}
</style>
