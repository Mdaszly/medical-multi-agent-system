<script setup lang="ts">
import { useRouter } from 'vue-router'
import { Calendar, Document, FirstAidKit } from '@element-plus/icons-vue'
import AiConsultHeroEntry from '@/components/patient/AiConsultHeroEntry.vue'

const router = useRouter()

const quickActions = [
  { title: '预约挂号', icon: Calendar, path: '/patient/doctors', description: '先选科室，再选医生与号源' },
  { title: '我的预约', icon: Document, path: '/patient/my-appointments', description: '查看和管理预约记录' },
  { title: '健康档案', icon: FirstAidKit, path: '/patient/health-profile', description: '管理病史、过敏与体征信息' },
]
</script>

<template>
  <div class="patient-home">
    <AiConsultHeroEntry />

    <el-card class="welcome-card">
      <h2>欢迎使用医疗门诊系统！</h2>
      <p>在这里您可以方便地预约挂号、查看您的预约记录。</p>
    </el-card>

    <div class="quick-actions">
      <el-card
        v-for="action in quickActions"
        :key="action.path"
        class="action-card"
        shadow="hover"
        @click="router.push(action.path)"
      >
        <div class="action-content">
          <el-icon class="action-icon" aria-hidden="true"><component :is="action.icon" /></el-icon>
          <h3>{{ action.title }}</h3>
          <p>{{ action.description }}</p>
        </div>
      </el-card>
    </div>
  </div>
</template>

<style scoped>
.patient-home {
  max-width: 1000px;
}

.welcome-card {
  margin-bottom: 24px;
  background: linear-gradient(135deg, #06b6d4 0%, #14b8a6 100%);
  border: none;
}

.welcome-card h2 {
  color: white;
  margin: 0 0 8px 0;
}

.welcome-card p {
  color: rgba(255, 255, 255, 0.9);
  margin: 0;
}

.quick-actions {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
}

.action-card {
  cursor: pointer;
  transition: transform 0.2s ease;
}

.action-card:hover {
  transform: translateY(-4px);
}

.action-content {
  text-align: center;
  padding: 20px;
}

.action-icon {
  font-size: 48px;
  color: #06b6d4;
  margin-bottom: 12px;
}

.action-content h3 {
  margin: 0 0 8px 0;
  color: #1f2937;
}

.action-content p {
  margin: 0;
  color: #6b7280;
  font-size: 14px;
}

@media (prefers-reduced-motion: reduce) {
  .action-card {
    transition: none;
  }

  .action-card:hover {
    transform: none;
  }
}
</style>
