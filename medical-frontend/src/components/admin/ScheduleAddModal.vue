<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listDoctorPage, listDepartments } from '@/services/medical/yishengguanli'
import { addSchedule, checkScheduleConflict } from '@/services/medical/paibanguanli'

const props = defineProps<{
  visible: boolean
}>()

const emit = defineEmits<{
  (e: 'close'): void
  (e: 'success'): void
}>()

const loading = ref(false)
const departments = ref<string[]>([])
const doctors = ref<API.DoctorVO[]>([])

const form = ref({
  doctorId: '',
  doctorName: '',
  department: '',
  scheduleDate: '',
  shiftType: '',
  description: ''
})

interface TimeSlot {
  timeSlot: string
  timeStart: string
  timeEnd: string
  maxSlots: number
}

const morningSlots = ref<TimeSlot[]>([
  { timeSlot: '08:00-08:30', timeStart: '08:00', timeEnd: '08:30', maxSlots: 3 },
  { timeSlot: '08:30-09:00', timeStart: '08:30', timeEnd: '09:00', maxSlots: 3 },
  { timeSlot: '09:00-09:30', timeStart: '09:00', timeEnd: '09:30', maxSlots: 3 },
  { timeSlot: '09:30-10:00', timeStart: '09:30', timeEnd: '10:00', maxSlots: 3 },
  { timeSlot: '10:00-10:30', timeStart: '10:00', timeEnd: '10:30', maxSlots: 3 },
  { timeSlot: '10:30-11:00', timeStart: '10:30', timeEnd: '11:00', maxSlots: 3 },
  { timeSlot: '11:00-11:30', timeStart: '11:00', timeEnd: '11:30', maxSlots: 3 },
  { timeSlot: '11:30-12:00', timeStart: '11:30', timeEnd: '12:00', maxSlots: 3 }
])

const afternoonSlots = ref<TimeSlot[]>([
  { timeSlot: '14:00-14:30', timeStart: '14:00', timeEnd: '14:30', maxSlots: 3 },
  { timeSlot: '14:30-15:00', timeStart: '14:30', timeEnd: '15:00', maxSlots: 3 },
  { timeSlot: '15:00-15:30', timeStart: '15:00', timeEnd: '15:30', maxSlots: 3 },
  { timeSlot: '15:30-16:00', timeStart: '15:30', timeEnd: '16:00', maxSlots: 3 },
  { timeSlot: '16:00-16:30', timeStart: '16:00', timeEnd: '16:30', maxSlots: 3 },
  { timeSlot: '16:30-17:00', timeStart: '16:30', timeEnd: '17:00', maxSlots: 3 },
  { timeSlot: '17:00-17:30', timeStart: '17:00', timeEnd: '17:30', maxSlots: 3 },
  { timeSlot: '17:30-18:00', timeStart: '17:30', timeEnd: '18:00', maxSlots: 3 }
])

const eveningSlots = ref<TimeSlot[]>([
  { timeSlot: '18:00-18:30', timeStart: '18:00', timeEnd: '18:30', maxSlots: 3 },
  { timeSlot: '18:30-19:00', timeStart: '18:30', timeEnd: '19:00', maxSlots: 3 },
  { timeSlot: '19:00-19:30', timeStart: '19:00', timeEnd: '19:30', maxSlots: 3 },
  { timeSlot: '19:30-20:00', timeStart: '19:30', timeEnd: '20:00', maxSlots: 3 },
  { timeSlot: '20:00-20:30', timeStart: '20:00', timeEnd: '20:30', maxSlots: 3 },
  { timeSlot: '20:30-21:00', timeStart: '20:30', timeEnd: '21:00', maxSlots: 3 }
])

const currentSlots = computed(() => {
  switch (form.value.shiftType) {
    case 'MORNING':
      return morningSlots.value
    case 'AFTERNOON':
      return afternoonSlots.value
    case 'EVENING':
      return eveningSlots.value
    default:
      return []
  }
})

const totalMaxSlots = computed(() => {
  return currentSlots.value.reduce((sum, slot) => sum + slot.maxSlots, 0)
})

const shiftTypes = [
  { value: 'MORNING', label: '上午', timeRange: '08:00 - 12:00' },
  { value: 'AFTERNOON', label: '下午', timeRange: '14:00 - 18:00' },
  { value: 'EVENING', label: '晚间', timeRange: '18:00 - 21:00' }
]

const rules = {
  doctorId: [{ required: true, message: '请选择医生', trigger: 'change' }],
  department: [{ required: true, message: '请选择科室', trigger: 'change' }],
  scheduleDate: [{ required: true, message: '请选择排班日期', trigger: 'change' }],
  shiftType: [{ required: true, message: '请选择班次类型', trigger: 'change' }]
}

const formRef = ref()

const loadDepartments = async () => {
  try {
    const res = await listDepartments()
    if (res.data) departments.value = res.data
  } catch (error) {
    console.error('加载科室失败', error)
    ElMessage.error('加载科室失败')
  }
}

const loadDoctors = async () => {
  loading.value = true
  try {
    const res = await listDoctorPage({ current: 1, pageSize: 100, department: form.value.department, workStatus: 1 })
    if (res.data?.records) doctors.value = res.data.records
  } catch (error) {
    console.error('加载医生失败', error)
    ElMessage.error('加载医生失败')
  } finally {
    loading.value = false
  }
}

watch(() => form.value.department, async (val) => {
  if (val) {
    await loadDoctors()
    form.value.doctorId = ''
    form.value.doctorName = ''
  } else {
    doctors.value = []
  }
})

const handleDoctorChange = (val: string) => {
  const doctor = doctors.value.find(d => d.id === Number(val))
  if (doctor) form.value.doctorName = doctor.doctorName || ''
}

const checkConflict = async () => {
  if (!form.value.doctorId || !form.value.scheduleDate || !form.value.shiftType) return false
  try {
    const res = await checkScheduleConflict({
      doctorId: Number(form.value.doctorId),
      scheduleDate: form.value.scheduleDate,
      shiftType: form.value.shiftType
    })
    return res.data || false
  } catch (error) {
    console.error('检查冲突失败', error)
    return false
  }
}

const handleSubmit = () => {
  console.log('Submit button clicked')
  if (!formRef.value) {
    console.error('formRef is null')
    ElMessage.error('表单初始化失败')
    return
  }

  formRef.value.validate((valid: boolean) => {
    if (!valid) {
      console.log('Form validation failed')
      return
    }

    console.log('Form validation passed')
    checkConflict().then((hasConflict) => {
      if (hasConflict) {
        ElMessageBox.confirm(
          '该医生在所选日期和班次已有排班，确定继续添加吗？',
          '排班冲突提示',
          { confirmButtonText: '确定添加', cancelButtonText: '取消', type: 'warning' }
        ).then(() => {
          submitSchedule()
        }).catch(() => {
          ElMessage.info('已取消')
        })
      } else {
        submitSchedule()
      }
    })
  })
}

const submitSchedule = async () => {
  loading.value = true
  try {
    const res = await addSchedule({
      doctorId: Number(form.value.doctorId),
      doctorName: form.value.doctorName,
      department: form.value.department,
      scheduleDate: form.value.scheduleDate,
      shiftType: form.value.shiftType,
      maxAppointments: totalMaxSlots.value,
      description: form.value.description
    })

    if (res.code === 0) {
      ElMessage.success('排班添加成功')
      emit('success')
      resetForm()
    } else {
      ElMessage.error(res.message || '添加失败')
    }
  } catch (error: any) {
    console.error('添加失败', error)
    ElMessage.error(error.message || '添加失败')
  } finally {
    loading.value = false
  }
}

const resetForm = () => {
  form.value = { doctorId: '', doctorName: '', department: '', scheduleDate: '', shiftType: '', description: '' }
  if (formRef.value) formRef.value.resetFields()
}

const handleClose = () => {
  resetForm()
  emit('close')
}

onMounted(() => {
  loadDepartments()
})
</script>

<template>
  <el-dialog title="添加排班" :visible="visible" width="700px" @close="handleClose">
    <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
      <el-form-item label="科室" prop="department">
        <el-select v-model="form.department" placeholder="请选择科室" filterable style="width:100%">
          <el-option v-for="dept in departments" :key="dept" :label="dept" :value="dept" />
        </el-select>
      </el-form-item>

      <el-form-item label="医生" prop="doctorId">
        <el-select v-model="form.doctorId" placeholder="请选择医生" :disabled="!form.department" style="width:100%" @change="handleDoctorChange">
          <el-option v-for="doctor in doctors" :key="doctor.id" :label="`${doctor.doctorName} - ${doctor.title || '无职称'}`" :value="doctor.id" />
        </el-select>
      </el-form-item>

      <el-form-item label="排班日期" prop="scheduleDate">
        <el-date-picker v-model="form.scheduleDate" type="date" placeholder="请选择日期" style="width:100%" :disabled-date="(time: Date) => time.getTime() < Date.now() - 8.64e7" />
      </el-form-item>

      <el-form-item label="班次类型" prop="shiftType">
        <el-radio-group v-model="form.shiftType">
          <el-radio v-for="shift in shiftTypes" :key="shift.value" :label="shift.value" class="mr-4">
            <span>{{ shift.label }}</span>
            <span class="ml-2 text-sm text-gray-500">{{ shift.timeRange }}</span>
          </el-radio>
        </el-radio-group>
      </el-form-item>

      <el-form-item v-if="form.shiftType" label="号源设置">
        <div class="bg-gray-50 rounded-lg p-4 mb-3">
          <div class="flex justify-between items-center mb-2">
            <span class="font-medium">时间段</span>
            <span class="font-medium">最大号源数</span>
            <span class="text-teal-600 font-bold">总计：{{ totalMaxSlots }}</span>
          </div>
          <div class="space-y-2">
            <div v-for="(slot, index) in currentSlots" :key="index" class="flex items-center gap-4 py-2 border-b border-gray-100 last:border-0">
              <span class="w-28 text-sm">{{ slot.timeSlot }}</span>
              <el-input-number v-model="slot.maxSlots" :min="0" :max="10" :step="1" style="width:100px" />
            </div>
          </div>
        </div>
      </el-form-item>

      <el-form-item label="备注">
        <el-input v-model="form.description" type="textarea" :rows="3" placeholder="请输入备注（可选）" />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="handleClose">取消</el-button>
      <el-button type="primary" @click="handleSubmit" :loading="loading">确认添加</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.mr-4 {
  margin-right: 24px;
}
</style>
