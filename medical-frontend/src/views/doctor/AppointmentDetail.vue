<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, Printer, Check } from '@element-plus/icons-vue'
import { getAppointmentById } from '@/services/medical/yuyueguanli'
import { getHealthProfile } from '@/services/medical/jiankangdanganguanli'

const router = useRouter()
const route = useRoute()

const loading = ref(false)
const appointment = ref<any>(null)
const healthProfile = ref<API.HealthProfileVO | null>(null)

const statusMap: Record<number, { label: string; type: string }> = {
  0: { label: '待就诊', type: 'warning' },
  1: { label: '已签到', type: 'primary' },
  2: { label: '诊疗中', type: 'info' },
  3: { label: '已完成', type: 'success' },
  4: { label: '已取消', type: 'danger' },
  5: { label: '已过期', type: 'info' },
  6: { label: '已结算', type: 'success' }
}

const loadAppointment = async () => {
  loading.value = true
  const id = route.params.id as string
  try {
    const res = await getAppointmentById({ id: Number(id) })
    if (res.data) {
      appointment.value = res.data
      
      if (res.data.userId) {
        try {
          const healthRes = await getHealthProfile(
            { userId: res.data.userId },
            { showError: false },
          )
          healthProfile.value = healthRes.data ?? null
        } catch {
          healthProfile.value = null
        }
      }
    }
  } catch (error: any) {
    console.error('加载预约详情失败', error)
    ElMessage.error(error.message || '加载预约详情失败')
  } finally {
    loading.value = false
  }
}

const handleBack = () => {
  router.back()
}

const handlePrint = () => {
  ElMessage.success('打印功能已触发')
  window.print()
}

const handleMarkRead = () => {
  ElMessage.success('已标记为已读')
}

const handleStartConsult = () => {
  const status = appointment.value?.status
  if (status !== 1 && status !== 2) {
    ElMessage.warning('患者尚未签到，无法接诊')
    return
  }
  router.push(`/doctor/prescription/${appointment.value.id}`)
}

onMounted(() => {
  loadAppointment()
})
</script>

<template>
  <div class="appointment-detail" v-loading="loading">
    <div class="header">
      <el-button icon="ArrowLeft" @click="handleBack">返回列表</el-button>
      <h2>预约详情</h2>
      <div class="actions">
        <el-button icon="Check" @click="handleMarkRead">标记已读</el-button>
        <el-button icon="Printer" @click="handlePrint">打印详情</el-button>
      </div>
    </div>

    <el-card v-if="appointment" class="main-card">
      <template #header>
        <div class="card-header">
          <span class="appointment-no">预约编号：{{ appointment.appointmentNo }}</span>
          <el-tag :type="statusMap[appointment.status]?.type" size="large">
            {{ statusMap[appointment.status]?.label }}
          </el-tag>
        </div>
      </template>

      <div class="section">
        <h3 class="section-title">患者信息</h3>
        <div class="info-grid">
          <div class="info-item">
            <span class="label">患者姓名</span>
            <span class="value">{{ appointment.userName }}</span>
          </div>
        </div>
      </div>

      <el-divider />

      <div class="section">
        <h3 class="section-title">预约信息</h3>
        <div class="info-grid">
          <div class="info-item">
            <span class="label">就诊科室</span>
            <span class="value">{{ appointment.department }}</span>
          </div>
          <div class="info-item">
            <span class="label">接诊医生</span>
            <span class="value">{{ appointment.doctorName }}</span>
          </div>
          <div class="info-item">
            <span class="label">就诊日期</span>
            <span class="value">{{ appointment.scheduleDate }}</span>
          </div>
          <div class="info-item">
            <span class="label">就诊时段</span>
            <span class="value">{{ appointment.timeSlot }}</span>
          </div>
          <div class="info-item">
            <span class="label">班次类型</span>
            <span class="value">{{ appointment.shiftType }}</span>
          </div>
          <div class="info-item">
            <span class="label">挂号费用</span>
            <span class="value price">¥{{ appointment.consultationFee }}</span>
          </div>
        </div>
      </div>

      <el-divider />

      <div v-if="healthProfile" class="section">
        <h3 class="section-title">健康档案</h3>
        <div class="info-grid">
          <div class="info-item">
            <span class="label">身高</span>
            <span class="value">{{ healthProfile.height || '-' }} cm</span>
          </div>
          <div class="info-item">
            <span class="label">体重</span>
            <span class="value">{{ healthProfile.weight || '-' }} kg</span>
          </div>
          <div class="info-item">
            <span class="label">血型</span>
            <span class="value">{{ healthProfile.bloodType || '-' }}</span>
          </div>
          <div class="info-item">
            <span class="label">血压</span>
            <span class="value">{{ healthProfile.bloodPressure || '-' }}</span>
          </div>
          <div class="info-item full-width">
            <span class="label">慢性病史</span>
            <span class="value text">{{ healthProfile.chronicDiseases || '无' }}</span>
          </div>
          <div class="info-item full-width">
            <span class="label">过敏史</span>
            <span class="value text">{{ healthProfile.allergyHistory || '无' }}</span>
          </div>
          <div class="info-item full-width">
            <span class="label">用药史</span>
            <span class="value text">{{ healthProfile.medicationHistory || '无' }}</span>
          </div>
          <div class="info-item full-width">
            <span class="label">家族病史</span>
            <span class="value text">{{ healthProfile.familyHistory || '无' }}</span>
          </div>
        </div>
      </div>

      <el-divider />

      <div class="section">
        <h3 class="section-title">备注信息</h3>
        <div class="info-grid">
          <div class="info-item full-width">
            <span class="label">患者备注</span>
            <span class="value text">{{ appointment.remark || '无' }}</span>
          </div>
        </div>
      </div>

      <el-divider />

      <div class="section">
        <h3 class="section-title">状态信息</h3>
        <div class="info-grid">
          <div class="info-item">
            <span class="label">签到状态</span>
            <span class="value">{{ appointment.checkInStatus ? '已签到' : '未签到' }}</span>
          </div>
          <div class="info-item">
            <span class="label">签到时间</span>
            <span class="value">{{ appointment.checkInTime || '-' }}</span>
          </div>
          <div class="info-item">
            <span class="label">取消时间</span>
            <span class="value">{{ appointment.cancelTime || '-' }}</span>
          </div>
          <div class="info-item">
            <span class="label">取消原因</span>
            <span class="value">{{ appointment.cancelReason || '-' }}</span>
          </div>
          <div class="info-item">
            <span class="label">创建时间</span>
            <span class="value">{{ appointment.createTime }}</span>
          </div>
          <div class="info-item">
            <span class="label">更新时间</span>
            <span class="value">{{ appointment.updateTime }}</span>
          </div>
        </div>
      </div>

      <div class="footer-actions">
        <el-button @click="handleBack">返回</el-button>
        <el-button 
          v-if="appointment.status === 1 || appointment.status === 2" 
          type="primary" 
          size="large"
          @click="handleStartConsult"
        >
          开始接诊
        </el-button>
        <el-tag v-else-if="appointment.status === 0" type="warning">患者未签到</el-tag>
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.appointment-detail {
  max-width: 900px;
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
}

.header h2 {
  margin: 0;
}

.actions {
  display: flex;
  gap: 12px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.appointment-no {
  font-weight: bold;
  color: #1f2937;
}

.section {
  margin-bottom: 20px;
}

.section-title {
  margin: 0 0 16px 0;
  font-size: 16px;
  color: #374151;
  border-left: 4px solid #14b8a6;
  padding-left: 12px;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 12px;
  background: #f9fafb;
  border-radius: 8px;
}

.info-item.full-width {
  grid-column: span 2;
}

.info-item .label {
  font-size: 14px;
  color: #6b7280;
}

.info-item .value {
  font-size: 16px;
  color: #1f2937;
  font-weight: 500;
}

.info-item .value.text {
  font-weight: normal;
  white-space: pre-wrap;
  word-break: break-all;
}

.info-item .value.price {
  color: #14b8a6;
  font-size: 18px;
}

.footer-actions {
  display: flex;
  justify-content: flex-end;
  gap: 16px;
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid #e5e7eb;
}

@media (max-width: 768px) {
  .info-grid {
    grid-template-columns: 1fr;
  }
  
  .info-item.full-width {
    grid-column: span 1;
  }
  
  .header {
    flex-direction: column;
    gap: 12px;
    align-items: flex-start;
  }
  
  .header h2 {
    font-size: 18px;
  }
  
  .actions {
    margin-top: 8px;
  }
}
</style>
