<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Document, Calendar, User } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { listAppointmentByDoctor } from '@/services/medical/yuyueguanli'
import { listScheduleByDoctor } from '@/services/medical/paibanguanli'

const router = useRouter()
const authStore = useAuthStore()

const loading = ref(false)

const stats = ref([
  { title: '今日接诊', value: 0, icon: Document, color: '#06b6d4' },
  { title: '本周预约', value: 0, icon: Calendar, color: '#14b8a6' },
  { title: '待处理', value: 0, icon: User, color: '#f59e0b' }
])

const todayAppointments = ref<any[]>([])

const quickActions = [
  { title: '查看排班', path: '/doctor/schedule', icon: Calendar },
  { title: '接诊列表', path: '/doctor/appointments', icon: Document }
]

const loadStats = async () => {
  loading.value = true
  try {
    if (!authStore.userInfo) return
    const [appointmentRes, scheduleRes] = await Promise.all([
      listAppointmentByDoctor({}),
      listScheduleByDoctor({ doctorId: authStore.userInfo.id! })
    ])
    
    if (appointmentRes.data) {
      const appointments = appointmentRes.data
      const today = new Date().toISOString().split('T')[0]
      const todayCount = appointments.filter((a: any) => a.appointmentDate === today).length
      const pendingCount = appointments.filter((a: any) => a.status === 0).length
      const weekCount = appointments.length
      
      stats.value = [
        { title: '今日接诊', value: todayCount, icon: Document, color: '#06b6d4' },
        { title: '本周预约', value: weekCount, icon: Calendar, color: '#14b8a6' },
        { title: '待处理', value: pendingCount, icon: User, color: '#f59e0b' }
      ]
      
      todayAppointments.value = appointments.filter((a: any) => a.appointmentDate === today && a.status === 0).slice(0, 5)
    }
  } catch (error) {
    console.error('加载统计数据失败', error)
    ElMessage.error('加载统计数据失败')
  } finally {
    loading.value = false
  }
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
    
    <el-card class="today-appointments">
      <template #header>
        <span>今日待接诊</span>
      </template>
      <el-table v-if="todayAppointments.length > 0" :data="todayAppointments" style="width: 100%">
        <el-table-column prop="userName" label="患者姓名" />
        <el-table-column prop="timeSlot" label="预约时间" />
        <el-table-column prop="appointmentNo" label="预约编号" />
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button type="primary" size="small">接诊</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-else description="暂无待接诊患者" />
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