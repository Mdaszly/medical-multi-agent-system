<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { listPendingDispensePrescriptions, listPrescriptionPage } from '@/services/medical/chufangguanli'

const router = useRouter()

const loading = ref(false)
const prescriptions = ref<any[]>([])
const searchKeyword = ref('')
const statusFilter = ref<number | null>(null)

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
    const [pendingRes, allRes] = await Promise.all([
      listPendingDispensePrescriptions({}),
      listPrescriptionPage({ current: 1, pageSize: 100 })
    ])
    
    const pendingList = ((pendingRes.data?.data) || []).map((p: any) => ({
      id: p.id,
      prescriptionNo: p.prescriptionNo,
      patientName: p.userName,
      doctorName: p.doctorName,
      department: p.department,
      status: p.status,
      createTime: p.createTime?.split('T')[0] || ''
    }))
    
    const allList = ((allRes.data?.data?.records) || []).map((p: any) => ({
      id: p.id,
      prescriptionNo: p.prescriptionNo,
      patientName: p.userName,
      doctorName: p.doctorName,
      department: p.department,
      status: p.status,
      createTime: p.createTime?.split('T')[0] || ''
    }))
    
    prescriptions.value = [...pendingList, ...allList.filter((p: any) => p.status >= 2)]
      .filter((p: any) => p.status !== 4)
      .sort((a, b) => new Date(b.createTime).getTime() - new Date(a.createTime).getTime())
  } catch (error) {
    console.error('加载处方列表失败', error)
    ElMessage.error('加载处方列表失败')
  } finally {
    loading.value = false
  }
}

const filteredPrescriptions = () => {
  let result = prescriptions.value
  
  if (statusFilter.value !== null) {
    result = result.filter(p => p.status === statusFilter.value)
  }
  
  if (searchKeyword.value) {
    const keyword = searchKeyword.value.toLowerCase()
    result = result.filter(p => 
      p.prescriptionNo?.toLowerCase().includes(keyword) ||
      p.patientName?.toLowerCase().includes(keyword)
    )
  }
  
  return result
}

const handleViewDetail = (prescription: any) => {
  router.push(`/pharmacist/prescription/${prescription.id}`)
}

const handleSearch = () => {
  // 搜索触发后会通过 computed 自动过滤
}

onMounted(() => {
  loadPrescriptions()
})
</script>

<template>
  <div class="pending-dispense-page">
    <el-card>
      <template #header>
        <div class="header-actions">
          <span>处方管理</span>
          <div class="header-right">
            <el-input 
              v-model="searchKeyword" 
              placeholder="搜索处方编号或患者姓名" 
              clearable 
              style="width: 250px; margin-right: 12px;" 
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
        <el-radio-group v-model="statusFilter">
          <el-radio-button :value="null">全部</el-radio-button>
          <el-radio-button :value="1">已审核</el-radio-button>
          <el-radio-button :value="2">已发药</el-radio-button>
        </el-radio-group>
      </div>
      
      <el-table :data="filteredPrescriptions()" style="width: 100%" v-loading="loading">
        <el-table-column prop="prescriptionNo" label="处方编号" />
        <el-table-column prop="patientName" label="患者姓名" />
        <el-table-column prop="doctorName" label="开方医生" />
        <el-table-column prop="department" label="科室" />
        <el-table-column prop="createTime" label="日期" />
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
      
      <el-empty v-if="!loading && filteredPrescriptions().length === 0" description="暂无处方记录" />
    </el-card>
  </div>
</template>

<style scoped>
.pending-dispense-page {
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
