<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { listPrescriptionPage } from '@/services/medical/chufangguanli'

const router = useRouter()

const loading = ref(false)
const prescriptions = ref<any[]>([])
const searchKeyword = ref('')
const statusFilter = ref<number | null>(null)

const pagination = ref({
  page: 1,
  pageSize: 10,
  total: 0
})

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
    const res = await listPrescriptionPage({
      current: pagination.value.page,
      pageSize: pagination.value.pageSize,
      prescriptionNo: searchKeyword.value || undefined,
      status: statusFilter.value !== null ? statusFilter.value : undefined
    })
    if (res.data?.records) {
      prescriptions.value = res.data.records.map((p: any) => ({
        id: p.id,
        prescriptionNo: p.prescriptionNo,
        patientName: p.patientName,
        doctorName: p.doctorName,
        department: p.department,
        status: p.status,
        totalAmount: p.totalAmount,
        createTime: p.createTime?.split('T')[0] || ''
      }))
      pagination.value.total = res.data.total || 0
    }
  } catch (error) {
    console.error('加载处方列表失败', error)
    ElMessage.error('加载处方列表失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.value.page = 1
  loadPrescriptions()
}

const handleStatusChange = () => {
  pagination.value.page = 1
  loadPrescriptions()
}

const handleViewDetail = (prescription: any) => {
  router.push(`/admin/prescription/${prescription.id}`)
}

const handlePageChange = (page: number) => {
  pagination.value.page = page
  loadPrescriptions()
}

onMounted(() => {
  loadPrescriptions()
})
</script>

<template>
  <div class="prescription-management">
    <el-card>
      <template #header>
        <div class="header-actions">
          <span>处方管理</span>
          <div class="header-right">
            <el-input 
              v-model="searchKeyword" 
              placeholder="搜索处方编号" 
              clearable 
              style="width: 200px; margin-right: 12px;" 
              @keyup.enter="handleSearch"
            >
              <template #append>
                <el-button :icon="Search" @click="handleSearch" />
              </template>
            </el-input>
          </div>
        </div>
      </template>
      
      <div class="status-filters">
        <el-radio-group v-model="statusFilter" @change="handleStatusChange">
          <el-radio-button :value="null">全部</el-radio-button>
          <el-radio-button :value="0">待审核</el-radio-button>
          <el-radio-button :value="1">已审核</el-radio-button>
          <el-radio-button :value="2">已发药</el-radio-button>
          <el-radio-button :value="3">已完成</el-radio-button>
        </el-radio-group>
      </div>
      
      <el-table :data="prescriptions" style="width: 100%" v-loading="loading">
        <el-table-column prop="prescriptionNo" label="处方编号" />
        <el-table-column prop="patientName" label="患者姓名" />
        <el-table-column prop="doctorName" label="开方医生" />
        <el-table-column prop="department" label="科室" />
        <el-table-column prop="createTime" label="日期" />
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
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="handleViewDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>
      
      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.pageSize"
        :total="pagination.total"
        layout="total, prev, pager, next"
        style="margin-top: 20px; text-align: center;"
        @current-change="handlePageChange"
      />
      
      <el-empty v-if="!loading && prescriptions.length === 0" description="暂无处方记录" />
    </el-card>
  </div>
</template>

<style scoped>
.prescription-management {
  max-width: 1200px;
}

.header-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.status-filters {
  margin-bottom: 16px;
}
</style>
