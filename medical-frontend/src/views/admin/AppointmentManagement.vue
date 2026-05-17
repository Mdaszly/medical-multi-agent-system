<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'

const loading = ref(false)
const activeTab = ref('all')

// 模拟预约数据
const appointments = ref([
  { id: 1, patientName: '张三', doctorName: '李医生', department: '心内科', appointmentDate: '2026-05-19', timeSlot: '上午', status: '待就诊', phone: '13800138001' },
  { id: 2, patientName: '李四', doctorName: '王医生', department: '神经内科', appointmentDate: '2026-05-19', timeSlot: '下午', status: '已就诊', phone: '13800138005' },
  { id: 3, patientName: '王五', doctorName: '李医生', department: '心内科', appointmentDate: '2026-05-18', timeSlot: '上午', status: '已取消', phone: '13800138006' }
])

const handleViewDetail = (appointment: any) => {
  ElMessage.info('查看预约详情')
}
</script>

<template>
  <div class="appointment-management">
    <el-card>
      <template #header>
        <div class="header-actions">
          <span>预约管理</span>
        </div>
      </template>

      <el-tabs v-model="activeTab">
        <el-tab-pane label="全部" name="all" />
        <el-tab-pane label="待就诊" name="pending" />
        <el-tab-pane label="已就诊" name="completed" />
        <el-tab-pane label="已取消" name="cancelled" />
      </el-tabs>

      <el-table :data="appointments" style="width: 100%;">
        <el-table-column prop="id" label="预约号" width="100" />
        <el-table-column prop="patientName" label="患者" />
        <el-table-column prop="phone" label="电话" />
        <el-table-column prop="doctorName" label="医生" />
        <el-table-column prop="department" label="科室" />
        <el-table-column prop="appointmentDate" label="日期" />
        <el-table-column prop="timeSlot" label="时段" />
        <el-table-column prop="status" label="状态">
          <template #default="{ row }">
            <el-tag :type="row.status === '待就诊' ? 'primary' : row.status === '已就诊' ? 'success' : 'danger'">
              {{ row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button type="primary" size="small" link @click="handleViewDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>
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
