<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { listPrescriptionByUser, getPrescriptionById } from '@/services/medical/chufangguanli'

const loading = ref(false)
const prescriptions = ref<any[]>([])

const statusMap: Record<number, { label: string; type: string }> = {
  0: { label: '待审核', type: 'warning' },
  1: { label: '已审核', type: 'info' },
  2: { label: '已发药', type: 'success' },
  3: { label: '已完成', type: 'success' },
  4: { label: '已取消', type: 'danger' }
}

const loadPrescriptions = async () => {
  loading.value = true
  try {
    const res = await listPrescriptionByUser({})
    if (res.data) {
      prescriptions.value = res.data.map((p: any) => ({
        id: p.id,
        prescriptionNo: p.prescriptionNo,
        doctorName: p.doctorName,
        department: p.department,
        date: p.createTime?.split('T')[0] || '',
        status: p.status,
        totalAmount: p.totalAmount
      }))
    }
  } catch (error) {
    console.error('加载处方列表失败', error)
    ElMessage.error('加载处方列表失败')
  } finally {
    loading.value = false
  }
}

const handleViewDetail = async (prescription: any) => {
  try {
    const res = await getPrescriptionById({ id: prescription.id })
    if (res.data) {
      console.log('处方详情:', res.data)
    }
  } catch (error) {
    ElMessage.error('获取处方详情失败')
  }
}

onMounted(() => {
  loadPrescriptions()
})
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
        <el-table-column prop="totalAmount" label="金额">
          <template #default="{ row }">
            <span style="color: #14b8a6; font-weight: bold;">¥{{ row.totalAmount?.toFixed(2) || '0.00' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态">
          <template #default="{ row }">
            <el-tag :type="statusMap[row.status]?.type">{{ statusMap[row.status]?.label }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="handleViewDetail(row)">查看详情</el-button>
          </template>
        </el-table-column>
      </el-table>
      
      <el-empty v-if="!loading && prescriptions.length === 0" description="暂无处方记录" />
    </el-card>
  </div>
</template>

<style scoped>
.prescriptions-page {
  max-width: 1200px;
}
</style>