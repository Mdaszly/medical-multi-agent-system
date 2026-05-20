<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { listSchedulePage } from '@/services/medical/paibanguanli'
import ScheduleAddModal from '@/components/admin/ScheduleAddModal.vue'

const loading = ref(false)
const showAddModal = ref(false)

const schedules = ref<any[]>([])

const pagination = ref({
  page: 1,
  pageSize: 10,
  total: 0
})

const statusMap: Record<number, { label: string; type: string }> = {
  0: { label: '休息', type: 'info' },
  1: { label: '可预约', type: 'success' },
  2: { label: '已满', type: 'danger' }
}

const shiftTypeMap: Record<string, { label: string; type: string }> = {
  MORNING: { label: '上午', type: 'info' },
  AFTERNOON: { label: '下午', type: 'success' },
  EVENING: { label: '晚间', type: 'warning' }
}

const loadSchedules = async () => {
  loading.value = true
  try {
    const res = await listSchedulePage({
      current: pagination.value.page,
      pageSize: pagination.value.pageSize
    })
    if (res.data?.records) {
      schedules.value = res.data.records.map((s: any) => ({
        id: s.id,
        doctorName: s.doctorName,
        department: s.department,
        scheduleDate: s.scheduleDate,
        shiftType: s.shiftType,
        maxAppointments: s.maxAppointments || 20,
        currentAppointments: s.currentAppointments || 0,
        status: s.status
      }))
      pagination.value.total = res.data.total || 0
    }
  } catch (error) {
    console.error('加载排班列表失败', error)
    ElMessage.error('加载排班列表失败')
  } finally {
    loading.value = false
  }
}

const handleEdit = (schedule: any) => {
  ElMessage.info('编辑排班')
}

const handleAdd = () => {
  showAddModal.value = true
}

const handleAddSuccess = () => {
  showAddModal.value = false
  loadSchedules()
}

const handleCloseModal = () => {
  showAddModal.value = false
}

onMounted(() => {
  loadSchedules()
})
</script>

<template>
  <div class="schedule-management">
    <el-card>
      <template #header>
        <div class="header-actions">
          <span>排班管理</span>
          <el-button type="primary" @click="handleAdd">+ 添加排班</el-button>
        </div>
      </template>

      <el-table :data="schedules" style="width: 100%;" v-loading="loading">
        <el-table-column prop="doctorName" label="医生" />
        <el-table-column prop="department" label="科室" />
        <el-table-column prop="scheduleDate" label="日期" />
        <el-table-column prop="shiftType" label="时段">
          <template #default="{ row }">
            <el-tag :type="shiftTypeMap[row.shiftType]?.type || 'info'">
              {{ shiftTypeMap[row.shiftType]?.label || row.shiftType }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="预约情况">
          <template #default="{ row }">
            {{ row.currentAppointments }} / {{ row.maxAppointments }}
            <el-progress
              :percentage="(row.currentAppointments / row.maxAppointments) * 100"
              :stroke-width="10"
              style="margin-top: 4px; width: 120px;"
            />
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态">
          <template #default="{ row }">
            <el-tag :type="statusMap[row.status]?.type">{{ statusMap[row.status]?.label }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button type="primary" size="small" link @click="handleEdit(row)">编辑</el-button>
          </template>
        </el-table-column>
      </el-table>
      
      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.pageSize"
        :total="pagination.total"
        layout="total, prev, pager, next"
        style="margin-top: 20px; text-align: center;"
        @current-change="loadSchedules"
        @size-change="loadSchedules"
      />
      
      <el-empty v-if="!loading && schedules.length === 0" description="暂无排班" />
    </el-card>

    <ScheduleAddModal
      :visible="showAddModal"
      @close="handleCloseModal"
      @success="handleAddSuccess"
    />
  </div>
</template>

<style scoped>
.schedule-management {
  max-width: 1200px;
}

.header-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>