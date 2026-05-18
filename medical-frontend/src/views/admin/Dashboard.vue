<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Document, Calendar, UserFilled } from '@element-plus/icons-vue'
import { listUserPage } from '@/services/medical/yonghuguanli'
import { listDoctorPage } from '@/services/medical/yishengguanli'
import { listAppointmentPage } from '@/services/medical/yuyueguanli'

const router = useRouter()

const loading = ref(false)

const stats = ref([
  { title: '总用户', value: 0, icon: User, color: '#06b6d4' },
  { title: '今日预约', value: 0, icon: Document, color: '#14b8a6' },
  { title: '活跃医生', value: 0, icon: UserFilled, color: '#0f766e' }
])

const quickActions = [
  { title: '用户管理', path: '/admin/users', icon: User },
  { title: '医生管理', path: '/admin/doctors', icon: UserFilled },
  { title: '预约管理', path: '/admin/appointments', icon: Document },
  { title: '排班管理', path: '/admin/schedules', icon: Calendar }
]

const loadStats = async () => {
  loading.value = true
  try {
    const today = new Date().toISOString().split('T')[0]
    
    const [userRes, doctorRes, appointmentRes] = await Promise.all([
      listUserPage({ current: 1, pageSize: 1 }),
      listDoctorPage({ current: 1, pageSize: 1 }),
      listAppointmentPage({ current: 1, pageSize: 1, scheduleDate: today })
    ])
    
    const userCount = userRes.data?.total || 0
    const doctorCount = doctorRes.data?.total || 0
    const todayAppointmentCount = appointmentRes.data?.total || 0
    
    stats.value = [
      { title: '总用户', value: userCount, icon: User, color: '#06b6d4' },
      { title: '今日预约', value: todayAppointmentCount, icon: Document, color: '#14b8a6' },
      { title: '活跃医生', value: doctorCount, icon: UserFilled, color: '#0f766e' }
    ]
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
  <div class="admin-dashboard">
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
  </div>
</template>

<style scoped>
.admin-dashboard {
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
  flex-wrap: wrap;
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