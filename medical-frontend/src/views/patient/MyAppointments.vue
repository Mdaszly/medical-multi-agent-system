<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listAppointmentByUser, cancelAppointment } from '@/services/medical/yuyueguanli'

const loading = ref(false)
const appointments = ref<any[]>([])

const statusMap: Record<number, { label: string, type: string }> = {
  0: { label: '待就诊', type: 'warning' },
  1: { label: '已完成', type: 'success' },
  2: { label: '已取消', type: 'info' },
  3: { label: '已过期', type: 'danger' }
}

const loadAppointments = async () => {
  loading.value = true
  try {
    const res = await listAppointmentByUser({})
    if (res.data) {
      appointments.value = res.data
    }
  } catch (error) {
    console.error('加载预约列表失败', error)
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
    await cancelAppointment({ appointmentId: appointment.id })
    ElMessage.success('已取消预约')
    loadAppointments()
  } catch {}
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
        <el-table-column prop="appointmentDate" label="就诊日期" />
        <el-table-column prop="period" label="时段" />
        <el-table-column prop="status" label="状态">
          <template #default="{ row }">
            <el-tag :type="statusMap[row.status]?.type">{{ statusMap[row.status]?.label }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" />
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button v-if="row.status === 0" size="small" type="danger" link @click="handleCancel(row)">取消预约</el-button>
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
</style>
