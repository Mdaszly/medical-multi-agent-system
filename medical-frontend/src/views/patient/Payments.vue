<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { format } from '@/utils/format'

const loading = ref(false)
const payments = ref([
  { id: 1, paymentNo: 'PM20260518001', amount: 50, date: '2026-05-18', status: '已支付' },
  { id: 2, paymentNo: 'PM20260515002', amount: 200, date: '2026-05-15', status: '待支付' }
])

const statusMap: Record<string, string> = {
  '已支付': 'success',
  '待支付': 'warning',
  '已取消': 'info'
}
</script>

<template>
  <div class="payments-page">
    <el-card>
      <template #header>
        <span>我的账单</span>
      </template>
      
      <el-table :data="payments" style="width: 100%" v-loading="loading">
        <el-table-column prop="paymentNo" label="账单编号" />
        <el-table-column prop="amount" label="金额">
          <template #default="{ row }">
            <span style="color: #14b8a6; font-weight: bold;">¥{{ row.amount }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="date" label="日期" />
        <el-table-column prop="status" label="状态">
          <template #default="{ row }">
            <el-tag :type="statusMap[row.status]">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150">
          <template #default="{ row }">
            <el-button v-if="row.status === '待支付'" type="primary" size="small" link>去支付</el-button>
            <el-button type="primary" size="small" link>查看详情</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<style scoped>
.payments-page {
  max-width: 1200px;
}
</style>
