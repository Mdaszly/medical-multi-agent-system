<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getPrescriptionById } from '@/services/medical/chufangguanli'

const router = useRouter()
const route = useRoute()

const loading = ref(false)
const prescription = ref<any>(null)

const statusMap: Record<number, { label: string; type: string }> = {
  0: { label: '待审核', type: 'warning' },
  1: { label: '已审核', type: 'info' },
  2: { label: '已发药', type: 'success' },
  3: { label: '已完成', type: 'success' },
  4: { label: '已取消', type: 'danger' }
}

const loadPrescription = async () => {
  loading.value = true
  try {
    const id = Number(route.params.id)
    const res = await getPrescriptionById({ id })
    if (res.data) {
      prescription.value = res.data
    }
  } catch (error) {
    console.error('加载处方详情失败', error)
    ElMessage.error('加载处方详情失败')
  } finally {
    loading.value = false
  }
}

const handleBack = () => {
  router.back()
}

onMounted(() => {
  loadPrescription()
})
</script>

<template>
  <div class="prescription-detail-page" v-loading="loading">
    <el-card v-if="prescription">
      <template #header>
        <div class="card-header">
          <span>处方详情</span>
          <el-button @click="handleBack">返回</el-button>
        </div>
      </template>
      
      <el-descriptions :column="2" border class="info-section">
        <el-descriptions-item label="处方编号">{{ prescription.prescriptionNo }}</el-descriptions-item>
        <el-descriptions-item label="处方状态">
          <el-tag :type="statusMap[prescription.status]?.type">
            {{ statusMap[prescription.status]?.label }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="开方医生">{{ prescription.doctorName }}</el-descriptions-item>
        <el-descriptions-item label="科室">{{ prescription.department }}</el-descriptions-item>
        <el-descriptions-item label="患者姓名">{{ prescription.patientName }}</el-descriptions-item>
        <el-descriptions-item label="开具日期">
          {{ prescription.createTime?.split('T')[0] || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="诊断" :span="2">
          {{ prescription.diagnosis || '-' }}
        </el-descriptions-item>
      </el-descriptions>
      
      <el-divider />
      
      <div class="drug-section">
        <h4>药品明细</h4>
        <el-table :data="prescription.items || []" style="width: 100%">
          <el-table-column prop="drugName" label="药品名称" />
          <el-table-column prop="specification" label="规格" />
          <el-table-column prop="quantity" label="数量" />
          <el-table-column prop="usage" label="用法" />
          <el-table-column prop="frequency" label="频次" />
          <el-table-column prop="duration" label="天数" />
        </el-table>
      </div>
      
      <el-divider />
      
      <div class="amount-section">
        <div class="total-amount">
          <span class="label">总费用：</span>
          <span class="value">¥{{ prescription.totalAmount?.toFixed(2) || '0.00' }}</span>
        </div>
      </div>
    </el-card>
    
    <el-empty v-else description="未找到处方信息" />
  </div>
</template>

<style scoped>
.prescription-detail-page {
  max-width: 900px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.info-section {
  margin-bottom: 16px;
}

.drug-section h4 {
  margin: 0 0 16px 0;
  color: #1f2937;
}

.amount-section {
  text-align: right;
  padding: 16px 0;
}

.total-amount {
  font-size: 18px;
}

.total-amount .label {
  color: #6b7280;
}

.total-amount .value {
  color: #14b8a6;
  font-weight: bold;
  font-size: 24px;
}
</style>
