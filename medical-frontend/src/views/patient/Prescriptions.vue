<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { format } from '@/utils/format'

const loading = ref(false)
const prescriptions = ref([
  { id: 1, prescriptionNo: 'PC20260518001', doctorName: '张医生', department: '内科', date: '2026-05-18', status: '已完成' },
  { id: 2, prescriptionNo: 'PC20260515002', doctorName: '李医生', department: '外科', date: '2026-05-15', status: '待缴费' }
])

const statusMap: Record<string, string> = {
  '已完成': 'success',
  '待缴费': 'warning',
  '已取消': 'info'
}
</script>

<template>
  <div class="prescriptions-page">
    <el-card>
      <template #header>
        <span>我的处方</span>
      </template>
      
      <el-table :data="prescriptions" style="width: 100%" v-loading="loading">
        <el-table-column prop="prescriptionNo" label="处方编号" />
        <el-table-column prop="doctorName" label="医生" />
        <el-table-column prop="department" label="科室" />
        <el-table-column prop="date" label="日期" />
        <el-table-column prop="status" label="状态">
          <template #default="{ row }">
            <el-tag :type="statusMap[row.status]">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button type="primary" size="small" link>查看详情</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<style scoped>
.prescriptions-page {
  max-width: 1200px;
}
</style>
