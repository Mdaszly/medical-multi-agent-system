<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { listAppointmentPage } from '@/services/medical/yuyueguanli'

const loading = ref(false)
const activeTab = ref('all')

const appointments = ref<any[]>([])

const pagination = ref({
  page: 1,
  pageSize: 10,
  total: 0
})

const statusMap: Record<number, { label: string; type: string }> = {
  0: { label: '待就诊', type: 'primary' },
  1: { label: '已就诊', type: 'success' },
  2: { label: '已取消', type: 'danger' },
  3: { label: '已过期', type: 'info' }
}

const filteredAppointments = computed(() => {
  if (activeTab.value === 'all') {
    return appointments.value
  }
  const statusMap: Record<string, number> = {
    'pending': 0,
    'completed': 1,
    'cancelled': 2
  }
  return appointments.value.filter(a => a.status === statusMap[activeTab.value])
})

const loadAppointments = async () => {
  loading.value = true
  try {
    const statusMap: Record<string, number | undefined> = {
      'all': undefined,
      'pending': 0,
      'completed': 1,
      'cancelled': 2
    }
    
    const res = await listAppointmentPage({
      current: pagination.value.page,
      pageSize: pagination.value.pageSize,
      status: statusMap[activeTab.value]
    })
    if (res.data?.records) {
      appointments.value = res.data.records.map((a: any) => ({
        id: a.id,
        appointmentNo: a.appointmentNo,
        patientName: a.userName,
        phone: a.phone,
        doctorName: a.doctorName,
        department: a.department,
        appointmentDate: a.appointmentDate,
        timeSlot: a.timeSlot,
        status: a.status,
        createTime: a.createTime
      }))
      pagination.value.total = res.data.total || 0
    }
  } catch (error) {
    console.error('加载预约列表失败', error)
    ElMessage.error('加载预约列表失败')
  } finally {
    loading.value = false
  }
}

const handleViewDetail = (appointment: any) => {
  ElMessage.info('查看预约详情')
}

const handleTabChange = () => {
  pagination.value.page = 1
  loadAppointments()
}

onMounted(() => {
  loadAppointments()
})
</script>

<template>
  <div class="appointment-management">
    <el-card>
      <template #header>
        <div class="header-actions">
          <span>预约管理</span>
        </div>
      </template>

      <el-tabs v-model="activeTab" @change="handleTabChange">
        <el-tab-pane label="全部" name="all" />
        <el-tab-pane label="待就诊" name="pending" />
        <el-tab-pane label="已就诊" name="completed" />
        <el-tab-pane label="已取消" name="cancelled" />
      </el-tabs>

      <el-table :data="filteredAppointments" style="width: 100%;" v-loading="loading">
        <el-table-column prop="appointmentNo" label="预约号" />
        <el-table-column prop="patientName" label="患者" />
        <el-table-column prop="phone" label="电话" />
        <el-table-column prop="doctorName" label="医生" />
        <el-table-column prop="department" label="科室" />
        <el-table-column prop="appointmentDate" label="日期" />
        <el-table-column prop="timeSlot" label="时段" />
        <el-table-column prop="status" label="状态">
          <template #default="{ row }">
            <el-tag :type="statusMap[row.status]?.type">
              {{ statusMap[row.status]?.label }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" />
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button type="primary" size="small" link @click="handleViewDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>
      
      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.pageSize"
        :total="pagination.total"
        layout="total, prev, pager, next"
        style="margin-top: 20px; text-align: center;"
        @current-change="loadAppointments"
        @size-change="loadAppointments"
      />
      
      <el-empty v-if="!loading && filteredAppointments.length === 0" description="暂无预约" />
    </el-card>
  </div>
</template>

<style scoped>
.appointment-management {
  max-width: 1200px;
}

.header-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>