<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { Document, Calendar, User } from '@element-plus/icons-vue'

const router = useRouter()

const stats = ref([
  { title: '今日接诊', value: 5, icon: Document, color: '#06b6d4' },
  { title: '本周预约', value: 32, icon: Calendar, color: '#14b8a6' },
  { title: '待处理', value: 3, icon: User, color: '#f59e0b' }
])

const quickActions = [
  { title: '查看排班', path: '/doctor/schedule', icon: Calendar },
  { title: '接诊列表', path: '/doctor/appointments', icon: Document }
]
</script>

<template>
  <div class="dashboard">
    <div class="stats-cards">
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
      <el-empty description="暂无待接诊患者" />
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
