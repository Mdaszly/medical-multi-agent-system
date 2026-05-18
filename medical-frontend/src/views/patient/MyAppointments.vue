<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listAppointmentByUser, cancelAppointment } from '@/services/medical/yuyueguanli'

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
    const res = await listAppointmentByUser({})
    if (res.data) {
      appointments.value = res.data
    }
  } catch (error: any) {
    console.error('加载预约列表失败', error)
    ElMessage.error(error.message || '加载预约列表失败')
  } finally {
    loading.value = false
  }
}

const handleCancel = async (appointment: any) => {
  try {
    await ElMessageBox.confirm('确认取消该预约吗？', '提示', {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    loading.value = true
    const result = await cancelAppointment({ appointmentId: appointment.id })
    
    if (result.code === 0) {
      ElMessage.success('预约已成功取消')
      await loadAppointments()
    } else {
      ElMessage.error(result.message || '取消预约失败')
    }
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('取消预约失败', error)
      ElMessage.error(error.message || '取消预约失败，请稍后重试')
    }
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadAppointments()
})
</script>

<template>
  <div class="my-appointments" v-loading="loading">
    <el-card>
      <template #header>
        <span>我的预约</span>
      </template>
      
      <el-table :data="appointments" style="width: 100%;">
        <el-table-column prop="appointmentNo" label="预约编号" />
        <el-table-column prop="doctorName" label="医生" />
        <el-table-column prop="department" label="科室" />
        <el-table-column prop="scheduleDate" label="就诊日期" />
        <el-table-column prop="timeSlot" label="时段" />
        <el-table-column prop="status" label="状态">
          <template #default="{ row }">
            <el-tag :type="statusMap[row.status]?.type">{{ statusMap[row.status]?.label }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" />
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button 
              v-if="row.status === 0" 
              size="small" 
              type="danger" 
              link 
              @click="handleCancel(row)"
              :disabled="loading"
            >
              取消预约
            </el-button>
            <span v-else-if="row.status === 4" class="status-text">已取消</span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<style scoped>
.my-appointments {
  max-width: 1200px;
}

.status-text {
  color: #f56c6c;
  font-size: 14px;
}
</style>
