<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import { useRoute, useRouter } from 'vue-router';
import { addSchedule } from '@/services/medical/paibanguanli';
import { listDoctorPage } from '@/services/medical/yishengguanli';

const route = useRoute();
const router = useRouter();

const loading = ref(false);
const showSlotManagement = ref(false);

const departments = ['内科', '外科', '儿科', '妇产科', '眼科', '口腔科', '皮肤科', '骨科'];

const shiftTypes = [
  { value: 'MORNING', label: '上午', timeRange: '08:00 - 12:00' },
  { value: 'AFTERNOON', label: '下午', timeRange: '14:00 - 18:00' },
  { value: 'EVENING', label: '晚间', timeRange: '18:00 - 22:00' }
];

const form = ref({
  doctorId: '',
  doctorName: '',
  department: '',
  scheduleDate: '',
  shiftType: '',
  description: ''
});

const formRef = ref();
const doctors = ref<any[]>([]);

const timeSlots = computed(() => {
  if (!form.value.shiftType) return [];
  
  const slots: {
    label: string;
    start: string;
    end: string;
    maxSlots: number;
  }[] = [];
  
  const shiftConfig: Record<string, { start: string; end: string; interval: number }> = {
    MORNING: { start: '08:00', end: '12:00', interval: 30 },
    AFTERNOON: { start: '14:00', end: '18:00', interval: 30 },
    EVENING: { start: '18:00', end: '22:00', interval: 30 }
  };
  
  const config = shiftConfig[form.value.shiftType];
  if (!config) return [];
  
  const [startHour, startMinute] = config.start.split(':').map(Number);
  const [endHour, endMinute] = config.end.split(':').map(Number);
  
  let currentHour = startHour;
  let currentMinute = startMinute;
  
  while (currentHour < endHour || (currentHour === endHour && currentMinute < endMinute)) {
    const nextMinute = currentMinute + config.interval;
    const nextHour = currentHour + Math.floor(nextMinute / 60);
    const finalMinute = nextMinute % 60;
    
    slots.push({
      label: `${currentHour.toString().padStart(2, '0')}:${currentMinute.toString().padStart(2, '0')} - ${nextHour.toString().padStart(2, '0')}:${finalMinute.toString().padStart(2, '0')}`,
      start: `${currentHour.toString().padStart(2, '0')}:${currentMinute.toString().padStart(2, '0')}`,
      end: `${nextHour.toString().padStart(2, '0')}:${finalMinute.toString().padStart(2, '0')}`,
      maxSlots: 5
    });
    
    currentHour = nextHour;
    currentMinute = finalMinute;
  }
  
  return slots;
});

const totalMaxSlots = computed(() => {
  return timeSlots.value.reduce((sum, slot) => sum + slot.maxSlots, 0);
});

const rules = {
  department: [{ required: true, message: '请选择科室', trigger: 'change' }],
  doctorId: [{ required: true, message: '请选择医生', trigger: 'change' }],
  scheduleDate: [{ required: true, message: '请选择排班日期', trigger: 'change' }],
  shiftType: [{ required: true, message: '请选择班次类型', trigger: 'change' }]
};

const loadDoctors = async () => {
  if (!form.value.department) {
    doctors.value = [];
    return;
  }
  try {
    const res = await listDoctorPage({
      current: 1,
      pageSize: 100,
      department: form.value.department
    });
    if (res.data?.records) {
      doctors.value = res.data.records;
    }
  } catch (error) {
    console.error('加载医生列表失败', error);
    ElMessage.error('加载医生列表失败');
  }
};

const handleDepartmentChange = () => {
  form.value.doctorId = '';
  form.value.doctorName = '';
  loadDoctors();
};

const handleDoctorChange = (value: string) => {
  const doctor = doctors.value.find(d => d.id === Number(value));
  if (doctor) {
    form.value.doctorName = doctor.doctorName;
  }
};

const handleSubmit = async () => {
  if (!formRef.value) return;
  
  formRef.value.validate(async (valid: boolean) => {
    if (valid) {
      loading.value = true;
      try {
        const res = await addSchedule({
          doctorId: Number(form.value.doctorId),
          doctorName: form.value.doctorName,
          department: form.value.department,
          scheduleDate: form.value.scheduleDate,
          shiftType: form.value.shiftType,
          maxAppointments: totalMaxSlots.value,
          description: form.value.description
        });
        
        if (res.code === 0) {
          ElMessage.success('排班添加成功');
          
          if (showSlotManagement.value && res.data?.id) {
            router.push({
              path: '/admin/slots',
              query: {
                scheduleId: res.data.id,
                doctorName: form.value.doctorName,
                department: form.value.department,
                scheduleDate: form.value.scheduleDate,
                shiftType: form.value.shiftType
              }
            });
          } else {
            router.push('/admin/schedules');
          }
        } else {
          ElMessage.error(res.message || '添加失败');
        }
      } catch (error: any) {
        console.error('添加失败', error);
        ElMessage.error(error.message || '添加失败');
      } finally {
        loading.value = false;
      }
    }
  });
};

const handleCancel = () => {
  router.push('/admin/schedules');
};

onMounted(() => {
  const doctorId = route.query.doctorId;
  const doctorName = route.query.doctorName;
  const department = route.query.department;
  
  if (doctorId) {
    form.value.doctorId = String(doctorId);
  }
  if (doctorName) {
    form.value.doctorName = String(doctorName);
  }
  if (department) {
    form.value.department = String(department);
    loadDoctors();
  }
});
</script>

<template>
  <div class="schedule-add-page">
    <el-card class="form-card">
      <template #header>
        <div class="header-title">
          <span class="title-text">新增排班</span>
          <span class="subtitle-text">为医生添加排班信息</span>
        </div>
      </template>

      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px" class="schedule-form">
        <el-row :gutter="24">
          <el-col :span="12">
            <el-form-item label="科室" prop="department">
              <el-select 
                v-model="form.department" 
                placeholder="请选择科室" 
                filterable 
                class="form-select"
                @change="handleDepartmentChange"
              >
                <el-option v-for="dept in departments" :key="dept" :label="dept" :value="dept" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="医生" prop="doctorId">
              <el-select 
                v-model="form.doctorId" 
                placeholder="请选择医生" 
                :disabled="!form.department" 
                class="form-select"
                @change="handleDoctorChange"
              >
                <el-option 
                  v-for="doctor in doctors" 
                  :key="doctor.id" 
                  :label="`${doctor.doctorName} - ${doctor.title || '无职称'}`" 
                  :value="doctor.id" 
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="24">
          <el-col :span="12">
            <el-form-item label="排班日期" prop="scheduleDate">
              <el-date-picker 
                v-model="form.scheduleDate" 
                type="date" 
                placeholder="请选择日期" 
                class="form-picker"
                :disabled-date="(time: Date) => time.getTime() < Date.now() - 8.64e7"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="班次类型" prop="shiftType">
              <el-radio-group v-model="form.shiftType" class="radio-group">
                <el-radio 
                  v-for="shift in shiftTypes" 
                  :key="shift.value" 
                  :label="shift.value" 
                  class="radio-item"
                >
                  <span class="radio-label">{{ shift.label }}</span>
                  <span class="radio-time">{{ shift.timeRange }}</span>
                </el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item v-if="form.shiftType" label="号源设置" class="slot-section">
          <div class="slot-container">
            <div class="slot-header">
              <span class="slot-title">时间段</span>
              <span class="slot-title">号源数</span>
              <span class="slot-tip">（预设配置，保存后生成实际号源）</span>
            </div>
            <div 
              v-for="(slot, index) in timeSlots" 
              :key="index" 
              class="slot-row"
            >
              <span class="slot-time">{{ slot.label }}</span>
              <span class="slot-count">{{ slot.maxSlots }} 个</span>
            </div>
            <div class="slot-summary">
              <span class="summary-label">合计号源</span>
              <span class="summary-value">{{ totalMaxSlots }} 个</span>
            </div>
          </div>
        </el-form-item>

        <el-form-item label="备注">
          <el-input 
            v-model="form.description" 
            type="textarea" 
            placeholder="请输入备注信息" 
            :rows="3" 
            class="form-textarea"
          />
        </el-form-item>

        <el-form-item label="后续操作">
          <el-checkbox v-model="showSlotManagement" class="checkbox-item">
            添加成功后跳转到号源管理
          </el-checkbox>
        </el-form-item>

        <el-form-item class="form-actions">
          <el-button type="primary" @click="handleSubmit" :loading="loading" class="btn-primary">
            确认添加
          </el-button>
          <el-button @click="handleCancel" class="btn-secondary">
            取消
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<style scoped>
.schedule-add-page {
  min-height: calc(100vh - 60px);
  padding: 24px;
  background-color: #f5f7fa;
}

.form-card {
  max-width: 900px;
  margin: 0 auto;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

.header-title {
  display: flex;
  flex-direction: column;
}

.title-text {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.subtitle-text {
  font-size: 14px;
  color: #909399;
  margin-top: 4px;
}

.schedule-form {
  padding: 24px 0;
}

.form-select,
.form-picker {
  width: 100%;
}

.form-textarea {
  width: 100%;
  min-height: 80px;
}

.radio-group {
  display: flex;
  gap: 32px;
}

.radio-item {
  display: flex;
  align-items: center;
}

.radio-label {
  font-weight: 500;
  margin-right: 8px;
}

.radio-time {
  font-size: 12px;
  color: #909399;
}

.slot-section {
  margin-top: 16px;
}

.slot-container {
  background: #fafafa;
  border-radius: 8px;
  padding: 16px;
}

.slot-header {
  display: flex;
  align-items: center;
  padding-bottom: 12px;
  border-bottom: 1px solid #e4e7ed;
  margin-bottom: 8px;
}

.slot-tip {
  font-size: 12px;
  color: #909399;
  margin-left: auto;
}

.slot-title {
  font-weight: 600;
  color: #606266;
}

.slot-row {
  display: flex;
  justify-content: space-between;
  padding: 8px 0;
  border-bottom: 1px dashed #ebeef5;
}

.slot-row:last-of-type {
  border-bottom: none;
}

.slot-time {
  color: #303133;
}

.slot-count {
  color: #67c23a;
  font-weight: 600;
}

.slot-summary {
  display: flex;
  justify-content: space-between;
  padding-top: 12px;
  margin-top: 8px;
  border-top: 1px solid #e4e7ed;
}

.summary-label {
  font-weight: 600;
  color: #303133;
}

.summary-value {
  font-size: 18px;
  font-weight: 600;
  color: #67c23a;
}

.checkbox-item {
  font-size: 14px;
  color: #606266;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding-top: 16px;
  margin-top: 16px;
  border-top: 1px solid #e4e7ed;
}

.btn-primary {
  padding: 10px 24px;
  font-size: 14px;
}

.btn-secondary {
  padding: 10px 24px;
  font-size: 14px;
}

.el-form-item {
  margin-bottom: 20px;
}

.el-col {
  margin-bottom: 8px;
}
</style>