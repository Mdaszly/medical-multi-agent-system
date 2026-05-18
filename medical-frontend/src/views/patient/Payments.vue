<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { listByUserId, pay, getPaymentById } from '@/services/medical/zhifuguanli'

const authStore = useAuthStore()

const loading = ref(false)
const submitting = ref(false)
const payments = ref<any[]>([])

const statusMap: Record<number, { label: string; type: string }> = {
  0: { label: '待支付', type: 'warning' },
  1: { label: '已支付', type: 'success' },
  2: { label: '已退款', type: 'info' },
  3: { label: '支付失败', type: 'danger' }
}

const loadPayments = async () => {
  loading.value = true
  try {
    if (!authStore.userInfo) return
    const res = await listByUserId({ userId: authStore.userInfo.id! })
    if (res.data) {
      payments.value = res.data.map((p: any) => ({
        id: p.id,
        paymentNo: p.paymentNo,
        amount: p.amount,
        date: p.createTime?.split('T')[0] || '',
        status: p.status,
        appointmentId: p.appointmentId,
        prescriptionId: p.prescriptionId
      }))
    }
  } catch (error) {
    console.error('加载支付记录失败', error)
    ElMessage.error('加载支付记录失败')
  } finally {
    loading.value = false
  }
}

const handlePay = async (payment: any) => {
  submitting.value = true
  try {
    await pay({ paymentId: payment.id })
    ElMessage.success('支付成功')
    loadPayments()
  } catch (error) {
    ElMessage.error('支付失败')
  } finally {
    submitting.value = false
  }
}

const handleViewDetail = async (payment: any) => {
  try {
    const res = await getPaymentById({ id: payment.id })
    if (res.data) {
      console.log('支付详情:', res.data)
    }
  } catch (error) {
    ElMessage.error('获取支付详情失败')
  }
}

onMounted(() => {
  loadPayments()
})
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
            <span style="color: #14b8a6; font-weight: bold;">¥{{ row.amount?.toFixed(2) || '0.00' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="date" label="日期" />
        <el-table-column prop="status" label="状态">
          <template #default="{ row }">
            <el-tag :type="statusMap[row.status]?.type">{{ statusMap[row.status]?.label }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200">
          <template #default="{ row }">
            <el-button 
              v-if="row.status === 0" 
              type="primary" 
              size="small" 
              :loading="submitting"
              @click="handlePay(row)"
            >
              去支付
            </el-button>
            <el-button type="primary" size="small" link @click="handleViewDetail(row)">查看详情</el-button>
          </template>
        </el-table-column>
      </el-table>
      
      <el-empty v-if="!loading && payments.length === 0" description="暂无支付记录" />
    </el-card>
  </div>
</template>

<style scoped>
.payments-page {
  max-width: 1200px;
}
</style>