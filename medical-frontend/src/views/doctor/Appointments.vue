<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'

const appointments = ref([
  { id: 1, patientName: '张三', time: '09:00-09:30', status: '待接诊' },
  { id: 2, patientName: '李四', time: '10:00-10:30', status: '待接诊' },
  { id: 3, patientName: '王五', time: '11:00-11:30', status: '已完成' }
])

const statusMap: Record<string, string> = {
  '待接诊': 'warning',
  '已完成': 'success',
  '已取消': 'info'
}

const handleStartConsult = (appointment: any) => {
  ElMessage.success(`开始接诊: ${appointment.patientName}`)
}
</script>

<template>
  <div class="appointments-page">
    <el-card>
      <template #header>
        <span>今日接诊列表</span>
      </template>
      
      <el-table :data="appointments" style="width: 100%">
        <el-table-column prop="patientName" label="患者姓名" />
        <el-table-column prop="time" label="预约时间" />
        <el-table-column prop="status" label="状态">
          <template #default="{ row }">
            <el-tag :type="statusMap[row.status]">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150">
          <template #default="{ row }">
            <el-button 
              v-if="row.status === '待接诊'" 
              type="primary" 
              size="small" 
              @click="handleStartConsult(row)"
            >
              开始接诊
            </el-button>
            <el-button type="primary" size="small" link>查看详情</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<style scoped>
.appointments-page {
  max-width: 1000px;
}
</style>
