<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'

const schedules = ref([
  { date: '2026-05-19', period: '上午', slots: 10, available: 8 },
  { date: '2026-05-19', period: '下午', slots: 10, available: 5 },
  { date: '2026-05-20', period: '上午', slots: 10, available: 10 }
])

const handleApplyLeave = () => {
  ElMessage.success('请假申请已提交')
}
</script>

<template>
  <div class="schedule-page">
    <el-card>
      <template #header>
        <div class="header-actions">
          <span>排班管理</span>
          <el-button type="primary" @click="handleApplyLeave">申请调班</el-button>
        </div>
      </template>
      
      <el-table :data="schedules" style="width: 100%">
        <el-table-column prop="date" label="日期" />
        <el-table-column prop="period" label="时段" />
        <el-table-column label="预约状态" width="200">
          <template #default="{ row }">
            <div class="progress-bar">
              <div class="progress" :style="{ width: ((row.slots - row.available) / row.slots * 100) + '%' }"></div>
            </div>
            <span>{{ row.slots - row.available }}/{{ row.slots }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="available" label="剩余号源" />
      </el-table>
    </el-card>
  </div>
</template>

<style scoped>
.schedule-page {
  max-width: 1000px;
}

.header-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.progress-bar {
  width: 100px;
  height: 8px;
  background: #e5e7eb;
  border-radius: 4px;
  margin-bottom: 4px;
  overflow: hidden;
}

.progress {
  height: 100%;
  background: #06b6d4;
  transition: width 0.3s;
}
</style>
