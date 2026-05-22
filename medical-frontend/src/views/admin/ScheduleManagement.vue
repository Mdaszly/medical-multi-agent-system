<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import {
  listSchedulePage,
  getScheduleById,
  updateSchedule
} from '@/services/medical/paibanguanli'

const router = useRouter()
const loading = ref(false)
const saving = ref(false)
const detailLoading = ref(false)

const schedules = ref<any[]>([])

const pagination = ref({
  page: 1,
  pageSize: 10,
  total: 0
})

const showEditDialog = ref(false)
const editForm = ref({
  id: 0,
  doctorId: 0,
  doctorName: '',
  department: '',
  scheduleDate: '',
  shiftType: '',
  maxAppointments: 20,
  currentAppointments: 0,
  status: 1,
  description: ''
})

const statusMap: Record<number, { label: string; type: string }> = {
  0: { label: '休息', type: 'info' },
  1: { label: '可预约', type: 'success' },
  2: { label: '已满', type: 'danger' }
}

const statusOptions = [
  { value: 0, label: '休息' },
  { value: 1, label: '可预约' },
  { value: 2, label: '已满' }
]

const shiftTypeMap: Record<string, { label: string; type: string }> = {
  MORNING: { label: '上午', type: 'info' },
  AFTERNOON: { label: '下午', type: 'success' },
  EVENING: { label: '晚间', type: 'warning' }
}

const shiftTypeOptions = [
  { value: 'MORNING', label: '上午 (08:00-12:00)' },
  { value: 'AFTERNOON', label: '下午 (14:00-18:00)' },
  { value: 'EVENING', label: '晚间 (18:00-22:00)' }
]

const loadSchedules = async () => {
  loading.value = true
  try {
    const res = await listSchedulePage({
      current: pagination.value.page,
      pageSize: pagination.value.pageSize
    })
    if (res.data?.records) {
      schedules.value = res.data.records.map((s: any) => ({
        id: s.id,
        doctorId: s.doctorId,
        doctorName: s.doctorName,
        department: s.department,
        scheduleDate: s.scheduleDate,
        shiftType: s.shiftType,
        maxAppointments: s.maxAppointments || 20,
        currentAppointments: s.currentAppointments || 0,
        status: s.status,
        description: s.description || ''
      }))
      pagination.value.total = res.data.total || 0
    }
  } catch (error) {
    console.error('加载排班列表失败', error)
    ElMessage.error('加载排班列表失败')
  } finally {
    loading.value = false
  }
}

const handleEdit = async (schedule: any) => {
  showEditDialog.value = true
  detailLoading.value = true
  try {
    const res = await getScheduleById({ id: schedule.id })
    if (res.code === 0 && res.data) {
      const d = res.data
      editForm.value = {
        id: d.id!,
        doctorId: d.doctorId!,
        doctorName: d.doctorName || '',
        department: d.department || '',
        scheduleDate: d.scheduleDate || '',
        shiftType: d.shiftType || 'MORNING',
        maxAppointments: d.maxAppointments ?? 20,
        currentAppointments: d.currentAppointments ?? 0,
        status: d.status ?? 1,
        description: d.description || ''
      }
    } else {
      ElMessage.error(res.message || '获取排班详情失败')
      showEditDialog.value = false
    }
  } catch (error: any) {
    console.error('获取排班详情失败', error)
    ElMessage.error(error.message || '获取排班详情失败')
    showEditDialog.value = false
  } finally {
    detailLoading.value = false
  }
}

const handleSaveEdit = async () => {
  const form = editForm.value
  if (!form.scheduleDate) {
    ElMessage.warning('请选择排班日期')
    return
  }
  if (!form.shiftType) {
    ElMessage.warning('请选择班次')
    return
  }
  if (form.maxAppointments < form.currentAppointments) {
    ElMessage.warning(`最大预约数不能小于已预约数（${form.currentAppointments}）`)
    return
  }

  saving.value = true
  try {
    const res = await updateSchedule({
      id: form.id,
      doctorId: form.doctorId,
      scheduleDate: form.scheduleDate,
      shiftType: form.shiftType,
      maxAppointments: form.maxAppointments,
      status: form.status,
      description: form.description
    })
    if (res.code === 0) {
      ElMessage.success('排班更新成功')
      showEditDialog.value = false
      await loadSchedules()
    } else {
      ElMessage.error(res.message || '更新失败')
    }
  } catch (error: any) {
    console.error('更新排班失败', error)
    ElMessage.error(error.message || '更新排班失败')
  } finally {
    saving.value = false
  }
}

const handleAdd = () => {
  router.push('/admin/schedule/add')
}

const handleManageSlots = (schedule: any) => {
  router.push({
    path: '/admin/slots',
    query: {
      scheduleId: schedule.id,
      doctorName: schedule.doctorName,
      department: schedule.department,
      scheduleDate: schedule.scheduleDate,
      shiftType: schedule.shiftType
    }
  })
}

onMounted(() => {
  loadSchedules()
})
</script>

<template>
  <div class="schedule-management">
    <el-card>
      <template #header>
        <div class="header-actions">
          <span>排班管理</span>
          <el-button type="primary" @click="handleAdd">+ 添加排班</el-button>
        </div>
      </template>

      <el-table :data="schedules" style="width: 100%;" v-loading="loading">
        <el-table-column prop="doctorName" label="医生" />
        <el-table-column prop="department" label="科室" />
        <el-table-column prop="scheduleDate" label="日期" />
        <el-table-column prop="shiftType" label="时段">
          <template #default="{ row }">
            <el-tag :type="shiftTypeMap[row.shiftType]?.type || 'info'">
              {{ shiftTypeMap[row.shiftType]?.label || row.shiftType }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="预约情况">
          <template #default="{ row }">
            {{ row.currentAppointments }} / {{ row.maxAppointments }}
            <el-progress
              :percentage="row.maxAppointments ? (row.currentAppointments / row.maxAppointments) * 100 : 0"
              :stroke-width="10"
              style="margin-top: 4px; width: 120px;"
            />
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态">
          <template #default="{ row }">
            <el-tag :type="statusMap[row.status]?.type">{{ statusMap[row.status]?.label }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180">
          <template #default="{ row }">
            <el-button type="primary" size="small" link @click="handleEdit(row)">编辑</el-button>
            <el-button type="info" size="small" link @click="handleManageSlots(row)">号源管理</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.pageSize"
        :total="pagination.total"
        layout="total, prev, pager, next"
        style="margin-top: 20px; text-align: center;"
        @current-change="loadSchedules"
        @size-change="loadSchedules"
      />

      <el-empty v-if="!loading && schedules.length === 0" description="暂无排班" />
    </el-card>

    <el-dialog
      v-model="showEditDialog"
      title="编辑排班"
      width="520px"
      destroy-on-close
      :close-on-click-modal="false"
      class="schedule-edit-dialog"
    >
      <div v-loading="detailLoading">
        <el-form
          v-if="!detailLoading"
          :model="editForm"
          label-width="100px"
          label-position="right"
        >
          <el-form-item label="医生">
            <span class="readonly-text">{{ editForm.doctorName }}</span>
          </el-form-item>
          <el-form-item label="科室">
            <span class="readonly-text">{{ editForm.department }}</span>
          </el-form-item>
          <el-form-item label="排班日期" required>
            <el-date-picker
              v-model="editForm.scheduleDate"
              type="date"
              value-format="YYYY-MM-DD"
              placeholder="选择日期"
              style="width: 100%;"
            />
          </el-form-item>
          <el-form-item label="班次" required>
            <el-select v-model="editForm.shiftType" placeholder="选择班次" style="width: 100%;">
              <el-option
                v-for="opt in shiftTypeOptions"
                :key="opt.value"
                :label="opt.label"
                :value="opt.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="最大预约数" required>
            <el-input-number
              v-model="editForm.maxAppointments"
              :min="editForm.currentAppointments"
              :max="500"
              style="width: 100%;"
            />
            <div class="form-hint">已预约 {{ editForm.currentAppointments }} 人，不可低于该值</div>
          </el-form-item>
          <el-form-item label="状态" required>
            <el-select v-model="editForm.status" placeholder="选择状态" style="width: 100%;">
              <el-option
                v-for="opt in statusOptions"
                :key="opt.value"
                :label="opt.label"
                :value="opt.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="备注">
            <el-input
              v-model="editForm.description"
              type="textarea"
              :rows="3"
              placeholder="排班说明（选填）"
              maxlength="200"
              show-word-limit
            />
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <el-button @click="showEditDialog = false" :disabled="saving">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSaveEdit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.schedule-management {
  max-width: 1200px;
}

.header-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.readonly-text {
  color: #374151;
  font-weight: 500;
}

.form-hint {
  margin-top: 6px;
  font-size: 12px;
  color: #6b7280;
  line-height: 1.5;
}

:deep(.schedule-edit-dialog .el-dialog__header) {
  border-bottom: 1px solid #e5e7eb;
  margin-right: 0;
  padding-bottom: 12px;
}

:deep(.schedule-edit-dialog .el-button--primary) {
  --el-button-bg-color: #0d9488;
  --el-button-border-color: #0d9488;
  --el-button-hover-bg-color: #0f766e;
  --el-button-hover-border-color: #0f766e;
}
</style>
