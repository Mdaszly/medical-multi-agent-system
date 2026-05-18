<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { listScheduleByDoctor } from '@/services/medical/paibanguanli'

const authStore = useAuthStore()

const loading = ref(false)
const schedules = ref<any[]>([])

const loadSchedules = async () => {
  loading.value = true
  try {
    if (!authStore.userInfo) return
    const res = await listScheduleByDoctor({ doctorId: authStore.userInfo.id! })
    if (res.data) {
      schedules.value = res.data.map((s: any) => ({
        id: s.id,
        date: s.scheduleDate,
        period: s.morningSlots ? '上午' : (s.afternoonSlots ? '下午' : '晚间'),
        slots: (s.morningSlots?.length || 0) + (s.afternoonSlots?.length || 0) + (s.eveningSlots?.length || 0),
        available: s.status === 1 ? (s.morningSlots?.length || 0) + (s.afternoonSlots?.length || 0) + (s.eveningSlots?.length || 0) : 0
      }))
    }
  } catch (error) {
    console.error('加载排班信息失败', error)
    ElMessage.error('加载排班信息失败')
  } finally {
    loading.value = false
  }
}

const handleApplyLeave = () => {
  ElMessage.success('请假申请已提交')
}

onMounted(() => {
  loadSchedules()
})
</script>

<template>
  <div class="schedule-page">
    <el-card>
      <template #header>
        <div class="header-actions">
          <span>排班管理</span>
          <el-button type="primary" @click="handleApplyLeave">申请调班</el-button>
        </div>
      </template>
      
      <el-table :data="schedules" style="width: 100%" v-loading="loading">
        <el-table-column prop="date" label="日期" />
        <el-table-column prop="period" label="时段" />
        <el-table-column label="预约状态" width="200">
          <template #default="{ row }">
            <div class="progress-bar">
              <div class="progress" :style="{ width: ((row.slots - row.available) / row.slots * 100) + '%' }"></div>
            </div>
            <span>{{ row.slots - row.available }}/{{ row.slots }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="available" label="剩余号源" />
      </el-table>
      
      <el-empty v-if="!loading && schedules.length === 0" description="暂无排班信息" />
    </el-card>
  </div>
</template>

<style scoped>
.schedule-page {
  max-width: 1000px;
}

.header-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.progress-bar {
  width: 100px;
  height: 8px;
  background: #e5e7eb;
  border-radius: 4px;
  margin-bottom: 4px;
  overflow: hidden;
}

.progress {
  height: 100%;
  background: #06b6d4;
  transition: width 0.3s;
}
</style>