<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { listAppointmentByDoctor } from '@/services/medical/yuyueguanli'

const router = useRouter()
const authStore = useAuthStore()

const loading = ref(false)
const appointments = ref<any[]>([])

const statusMap: Record<number, { label: string; type: string }> = {
  0: { label: '待就诊', type: 'warning' },
  1: { label: '已签到', type: 'primary' },
  2: { label: '诊疗中', type: 'info' },
  3: { label: '已完成', type: 'success' },
  4: { label: '已取消', type: 'danger' },
  5: { label: '已过期', type: 'info' },
  6: { label: '已结算', type: 'success' }
}

const loadAppointments = async () => {
  loading.value = true
  try {
    const res = await listAppointmentByDoctor({})
    if (res.data) {
      appointments.value = res.data.map((a: any) => ({
        id: a.id,
        patientName: a.userName,
        time: a.timeSlot,
        status: a.status,
        appointmentNo: a.appointmentNo,
        department: a.department,
        scheduleDate: a.scheduleDate
      }))
    }
  } catch (error) {
    console.error('加载接诊列表失败', error)
    ElMessage.error('加载接诊列表失败')
  } finally {
    loading.value = false
  }
}

const handleStartConsult = (appointment: any) => {
  router.push(`/doctor/prescription/${appointment.id}`)
}

const handleViewDetail = (appointment: any) => {
  router.push(`/doctor/appointment/${appointment.id}`)
}

onMounted(() => {
  loadAppointments()
})
</script>

<template>
  <div class="appointments-page">
    <el-card>
      <template #header>
        <span>今日接诊列表</span>
      </template>
      
      <el-table :data="appointments" style="width: 100%" v-loading="loading">
        <el-table-column prop="appointmentNo" label="预约编号" />
        <el-table-column prop="patientName" label="患者姓名" />
        <el-table-column prop="department" label="科室" />
        <el-table-column prop="scheduleDate" label="就诊日期" />
        <el-table-column prop="time" label="预约时间" />
        <el-table-column prop="status" label="状态">
          <template #default="{ row }">
            <el-tag :type="statusMap[row.status]?.type">{{ statusMap[row.status]?.label }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220">
          <template #default="{ row }">
            <el-button 
              v-if="row.status === 0 || row.status === 1" 
              type="primary" 
              size="small" 
              @click="handleStartConsult(row)"
            >
              开始接诊
            </el-button>
            <el-button type="primary" size="small" link @click="handleViewDetail(row)">查看详情</el-button>
          </template>
        </el-table-column>
      </el-table>
      
      <el-empty v-if="!loading && appointments.length === 0" description="暂无接诊患者" />
    </el-card>
  </div>
</template>

<style scoped>
.appointments-page {
  max-width: 1000px;
}
</style>