<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'

const loading = ref(false)

// 模拟排班数据
const schedules = ref([
  { id: 1, doctorName: '李医生', department: '心内科', scheduleDate: '2026-05-19', timeSlot: '上午', maxAppointments: 20, currentAppointments: 15, status: '正常' },
  { id: 2, doctorName: '李医生', department: '心内科', scheduleDate: '2026-05-19', timeSlot: '下午', maxAppointments: 15, currentAppointments: 8, status: '正常' },
  { id: 3, doctorName: '王医生', department: '神经内科', scheduleDate: '2026-05-19', timeSlot: '上午', maxAppointments: 25, currentAppointments: 20, status: '正常' }
])

const handleEdit = (schedule: any) => {
  ElMessage.info('编辑排班')
}

const handleAdd = () => {
  ElMessage.info('添加排班')
}
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

      <el-table :data="schedules" style="width: 100%;">
        <el-table-column prop="doctorName" label="医生" />
        <el-table-column prop="department" label="科室" />
        <el-table-column prop="scheduleDate" label="日期" />
        <el-table-column prop="timeSlot" label="时段" />
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
            <el-tag type="success">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button type="primary" size="small" link @click="handleEdit(row)">编辑</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
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
