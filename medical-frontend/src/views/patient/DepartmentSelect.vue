<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { OfficeBuilding, ArrowRight } from '@element-plus/icons-vue'
import { listDepartments } from '@/services/medical/yishengguanli'
import { FALLBACK_DEPARTMENTS } from '@/constants/departments'

const router = useRouter()
const loading = ref(false)
const departments = ref<string[]>([])

const loadDepartments = async () => {
  loading.value = true
  try {
    const res = await listDepartments()
    if (res.data?.length) {
      departments.value = res.data
    } else {
      departments.value = [...FALLBACK_DEPARTMENTS]
    }
  } catch {
    departments.value = [...FALLBACK_DEPARTMENTS]
    ElMessage.warning('科室列表加载失败，已显示默认科室')
  } finally {
    loading.value = false
  }
}

const goDepartment = (dept: string) => {
  router.push({
    name: 'DepartmentDoctors',
    params: { department: encodeURIComponent(dept) },
  })
}

onMounted(loadDepartments)
</script>

<template>
  <div class="dept-select-page">
    <div class="page-banner">
      <h1>预约挂号</h1>
      <p>请先选择门诊科室，系统将展示该科室下可预约的医生</p>
    </div>

    <div class="dept-grid" v-loading="loading">
      <button
        v-for="dept in departments"
        :key="dept"
        type="button"
        class="dept-card"
        @click="goDepartment(dept)"
      >
        <div class="dept-icon">
          <el-icon :size="28"><OfficeBuilding /></el-icon>
        </div>
        <div class="dept-info">
          <span class="dept-name">{{ dept }}</span>
          <span class="dept-hint">查看本周出诊医生</span>
        </div>
        <el-icon class="dept-arrow"><ArrowRight /></el-icon>
      </button>
    </div>

    <el-empty v-if="!loading && departments.length === 0" description="暂无可预约科室" />
  </div>
</template>

<style scoped>
.dept-select-page {
  max-width: 960px;
  margin: 0 auto;
}

.page-banner {
  background: linear-gradient(135deg, #14b8a6 0%, #0d9488 100%);
  color: #fff;
  border-radius: 12px;
  padding: 28px 32px;
  margin-bottom: 24px;
}

.page-banner h1 {
  margin: 0 0 8px;
  font-size: 26px;
  font-weight: 600;
}

.page-banner p {
  margin: 0;
  opacity: 0.92;
  font-size: 15px;
  line-height: 1.5;
}

.dept-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
}

.dept-card {
  display: flex;
  align-items: center;
  gap: 16px;
  width: 100%;
  padding: 20px 22px;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  background: #fff;
  cursor: pointer;
  transition: border-color 0.2s, box-shadow 0.2s, transform 0.2s;
  text-align: left;
}

.dept-card:hover {
  border-color: #14b8a6;
  box-shadow: 0 8px 24px rgba(20, 184, 166, 0.12);
  transform: translateY(-2px);
}

.dept-card:focus-visible {
  outline: 2px solid #14b8a6;
  outline-offset: 2px;
}

.dept-icon {
  width: 52px;
  height: 52px;
  border-radius: 12px;
  background: rgba(20, 184, 166, 0.12);
  color: #14b8a6;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.dept-info {
  flex: 1;
  min-width: 0;
}

.dept-name {
  display: block;
  font-size: 17px;
  font-weight: 600;
  color: #111827;
}

.dept-hint {
  display: block;
  margin-top: 4px;
  font-size: 13px;
  color: #6b7280;
}

.dept-arrow {
  color: #9ca3af;
  flex-shrink: 0;
}
</style>
