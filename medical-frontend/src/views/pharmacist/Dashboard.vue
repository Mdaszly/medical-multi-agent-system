<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Document, Check, Clock } from '@element-plus/icons-vue'
import { listPendingDispensePrescriptions, listPrescriptionPage } from '@/services/medical/chufangguanli'

const router = useRouter()

const loading = ref(false)
const stats = ref([
  { title: '今日待发药', value: 0, icon: Clock, color: '#f59e0b' },
  { title: '本周完成', value: 0, icon: Check, color: '#10b981' },
  { title: '总处方数', value: 0, icon: Document, color: '#8b5cf6' }
])

const recentPrescriptions = ref<any[]>([])

const quickActions = [
  { title: '待发药列表', path: '/pharmacist/pending', icon: Clock },
  { title: '已完成记录', path: '/pharmacist/pending', icon: Check }
]

const loadStats = async () => {
  loading.value = true
  try {
    const today = new Date().toISOString().split('T')[0]
    
    const [pendingRes, allRes] = await Promise.all([
      listPendingDispensePrescriptions({}),
      listPrescriptionPage({ current: 1, pageSize: 100 })
    ])
    
    const pendingList = pendingRes.data || []
    const allList = allRes.data?.records || []
    
    const todayPending = pendingList.filter((p: any) => {
      const createDate = p.createTime?.split('T')[0]
      return createDate === today
    }).length
    
    const weekAgo = new Date()
    weekAgo.setDate(weekAgo.getDate() - 7)
    const weekAgoStr = weekAgo.toISOString().split('T')[0] || ''
    const weekCompleted = allList.filter((p: any) => {
      const createDate = p.createTime?.split('T')[0]
      return createDate && createDate >= weekAgoStr && p.status === 2
    }).length
    
    stats.value = [
      { title: '今日待发药', value: todayPending, icon: Clock, color: '#f59e0b' },
      { title: '本周完成', value: weekCompleted, icon: Check, color: '#10b981' },
      { title: '总处方数', value: allList.length, icon: Document, color: '#8b5cf6' }
    ]
    
    recentPrescriptions.value = pendingList.slice(0, 5).map((p: any) => ({
      id: p.id,
      prescriptionNo: p.prescriptionNo,
      patientName: p.patientName,
      doctorName: p.doctorName,
      department: p.department,
      status: p.status,
      createTime: p.createTime
    }))
  } catch (error) {
    console.error('加载统计数据失败', error)
    ElMessage.error('加载统计数据失败')
  } finally {
    loading.value = false
  }
}

const handleViewDetail = (prescription: any) => {
  router.push(`/pharmacist/prescription/${prescription.id}`)
}

onMounted(() => {
  loadStats()
})
</script>

<template>
  <div class="dashboard">
    <div class="stats-cards" v-loading="loading">
      <el-card v-for="stat in stats" :key="stat.title" class="stat-card">
        <div class="stat-content">
          <div class="stat-icon" :style="{ background: stat.color + '20' }">
            <el-icon :color="stat.color" :size="32">
              <component :is="stat.icon" />
            </el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stat.value }}</div>
            <div class="stat-title">{{ stat.title }}</div>
          </div>
        </div>
      </el-card>
    </div>
    
    <el-card class="quick-actions-card">
      <template #header>
        <span>快捷操作</span>
      </template>
      <div class="quick-actions">
        <div 
          v-for="action in quickActions" 
          :key="action.path"
          class="action-item"
          @click="router.push(action.path)"
        >
          <el-icon><component :is="action.icon" /></el-icon>
          <span>{{ action.title }}</span>
        </div>
      </div>
    </el-card>
    
    <el-card class="recent-prescriptions">
      <template #header>
        <span>近期待发药处方</span>
      </template>
      <el-table v-if="recentPrescriptions.length > 0" :data="recentPrescriptions" style="width: 100%">
        <el-table-column prop="prescriptionNo" label="处方编号" />
        <el-table-column prop="patientName" label="患者姓名" />
        <el-table-column prop="doctorName" label="医生" />
        <el-table-column prop="department" label="科室" />
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="handleViewDetail(row)">处理</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-else description="暂无待发药处方" />
    </el-card>
  </div>
</template>

<style scoped>
.dashboard {
  max-width: 1200px;
}

.stats-cards {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
  gap: 20px;
  margin-bottom: 24px;
}

.stat-card {
  cursor: pointer;
  transition: transform 0.2s;
}

.stat-card:hover {
  transform: translateY(-4px);
}

.stat-content {
  display: flex;
  align-items: center;
  gap: 20px;
}

.stat-icon {
  width: 60px;
  height: 60px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: 32px;
  font-weight: bold;
  color: #1f2937;
  line-height: 1.2;
}

.stat-title {
  font-size: 14px;
  color: #6b7280;
}

.quick-actions-card {
  margin-bottom: 24px;
}

.quick-actions {
  display: flex;
  gap: 16px;
}

.action-item {
  padding: 20px 40px;
  background: #f3f4f6;
  border-radius: 8px;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 8px;
  transition: all 0.2s;
}

.action-item:hover {
  background: #e5e7eb;
}

.action-item span {
  font-size: 16px;
  color: #1f2937;
}
</style>
