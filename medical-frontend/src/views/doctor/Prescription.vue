<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { listDrugs } from '@/services/medical/yaopinguanli'
import { createPrescription } from '@/services/medical/chufangguanli'
import { getAppointmentById, listAppointmentByDoctor } from '@/services/medical/yuyueguanli'

const router = useRouter()
const route = useRoute()

interface DrugItem {
  drugCode: string
  drugName: string
  spec: string
  price: number
  quantity: number
  usage: string
  frequency: string
  days: number
  subtotal: number
}

interface PatientInfo {
  id: number
  appointmentId: number
  patientName: string
  gender: string
  age: number | string
  appointmentNo: string
  scheduleDate: string
  department: string
  doctorName: string
  timeSlot?: string
}

const prescriptionForm = reactive({
  appointmentId: Number(route.params.appointmentId) || 0,
  diagnosis: '',
  drugs: [] as DrugItem[]
})

const submitting = ref(false)
const searchKeyword = ref('')
const drugList = ref<DrugItem[]>([])
const patientInfo = ref<PatientInfo | null>(null)
const patientList = ref<PatientInfo[]>([])
const selectedPatientId = ref(0)

const loadDrugs = async () => {
  try {
    const res = await listDrugs({})
    if (res.data) {
      drugList.value = res.data.map((drug: any) => ({
        drugCode: drug.drugCode,
        drugName: drug.drugName,
        spec: drug.specification,
        price: drug.retailPrice || 0,
        quantity: 1,
        usage: '口服',
        frequency: '每日3次',
        days: 7,
        subtotal: drug.retailPrice || 0
      }))
    }
  } catch (error) {
    console.error('加载药品列表失败', error)
  }
}

const loadPatientList = async () => {
  try {
    const res = await listAppointmentByDoctor({})
    if (res.data) {
      patientList.value = res.data
        .filter((a: any) => a.status === 0 || a.status === 1)
        .map((a: any) => ({
          id: a.id,
          appointmentId: a.id,
          patientName: a.userName,
          gender: a.genderText || '未知',
          age: a.age || '-',
          appointmentNo: a.appointmentNo,
          scheduleDate: a.scheduleDate,
          department: a.department,
          doctorName: a.doctorName,
          timeSlot: a.timeSlot || ''
        }))
    }
  } catch (error) {
    console.error('加载患者列表失败', error)
  }
}

const loadPatientInfo = async (appointmentId: number) => {
  try {
    const res = await getAppointmentById({ id: appointmentId })
    if (res.data) {
      const data = res.data
      patientInfo.value = {
        id: data.id,
        appointmentId: data.id,
        patientName: data.userName || '-',
        gender: data.genderText || '未知',
        age: data.age || '-',
        appointmentNo: data.appointmentNo,
        scheduleDate: data.scheduleDate || '-',
        department: data.department || '-',
        doctorName: data.doctorName || '-'
      }
      prescriptionForm.appointmentId = appointmentId
    }
  } catch (error) {
    console.error('加载患者信息失败', error)
    ElMessage.error('加载患者信息失败')
  }
}

const handlePatientChange = async (appointmentId: number) => {
  if (prescriptionForm.drugs.length > 0) {
    const confirmed = window.confirm('当前处方有药品未提交，切换患者将清空当前处方内容，确定继续吗？')
    if (!confirmed) return
  }
  
  prescriptionForm.drugs = []
  prescriptionForm.diagnosis = ''
  await loadPatientInfo(appointmentId)
}

const filteredDrugs = computed(() => {
  if (!searchKeyword.value) return drugList.value
  const keyword = searchKeyword.value.toLowerCase()
  return drugList.value.filter(d => 
    d.drugName.toLowerCase().includes(keyword) || 
    d.spec.toLowerCase().includes(keyword)
  )
})

const totalAmount = computed(() => {
  return prescriptionForm.drugs.reduce((sum, item) => sum + item.subtotal, 0)
})

const calculateSubtotal = (item: DrugItem) => {
  item.subtotal = item.price * item.quantity
}

const addDrug = (drug: DrugItem) => {
  const existing = prescriptionForm.drugs.find(d => d.drugCode === drug.drugCode)
  if (!existing) {
    prescriptionForm.drugs.push({
      ...drug,
      quantity: 1,
      subtotal: drug.price
    })
    ElMessage.success('已添加药品')
  } else {
    ElMessage.warning('该药品已在处方中')
  }
}

const removeDrug = (index: number) => {
  prescriptionForm.drugs.splice(index, 1)
}

const handleSubmit = async () => {
  if (!prescriptionForm.diagnosis) {
    ElMessage.warning('请填写诊断')
    return
  }
  if (prescriptionForm.drugs.length === 0) {
    ElMessage.warning('请至少添加一个药品')
    return
  }

  submitting.value = true
  try {
    await createPrescription({
      appointmentId: prescriptionForm.appointmentId,
      diagnosis: prescriptionForm.diagnosis,
      drugs: prescriptionForm.drugs.map(d => ({
        drugCode: d.drugCode,
        drugName: d.drugName,
        specification: d.spec,
        quantity: d.quantity,
        usage: d.usage,
        frequency: d.frequency,
        duration: String(d.days)
      }))
    })
    ElMessage.success('处方提交成功')
    prescriptionForm.drugs = []
    prescriptionForm.diagnosis = ''
    
    const nextPatient = patientList.value.find(p => p.appointmentId !== prescriptionForm.appointmentId)
    if (nextPatient) {
      await loadPatientInfo(nextPatient.appointmentId)
    } else {
      router.back()
    }
  } catch (error: any) {
    ElMessage.error(error.message || '提交失败')
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  await loadDrugs()
  await loadPatientList()
  
  const initialAppointmentId = Number(route.params.appointmentId)
  if (initialAppointmentId) {
    selectedPatientId.value = initialAppointmentId
    await loadPatientInfo(initialAppointmentId)
  } else if (patientList.value && patientList.value.length > 0) {
    const firstPatient = patientList.value[0]
    if (firstPatient) {
      selectedPatientId.value = firstPatient.appointmentId
      await loadPatientInfo(firstPatient.appointmentId)
    }
  }
})

watch(selectedPatientId, (newVal) => {
  if (newVal) {
    handlePatientChange(newVal)
  }
})
</script>

<template>
  <div class="prescription-page">
    <el-card class="patient-select-card">
      <template #header>
        <span>选择患者</span>
      </template>
      <div v-if="patientList.length > 0" class="patient-select">
        <el-select 
          v-model="selectedPatientId" 
          placeholder="请选择患者" 
          style="width: 100%; max-width: 400px;"
        >
          <el-option 
            v-for="patient in patientList" 
            :key="patient.appointmentId" 
            :label="`${patient.patientName} - ${patient.scheduleDate} ${patient.timeSlot || ''}`"
            :value="patient.appointmentId"
          />
        </el-select>
        <div class="patient-count">
          待就诊患者：{{ patientList.length }} 人
        </div>
      </div>
      <el-empty v-else description="暂无待就诊患者" />
    </el-card>

    <el-card v-if="patientInfo" class="patient-info-card">
      <template #header>
        <div class="card-header">
          <span>患者信息</span>
          <span class="appointment-no">预约编号：{{ patientInfo.appointmentNo }}</span>
        </div>
      </template>
      <el-descriptions :column="3" border>
        <el-descriptions-item label="患者姓名">{{ patientInfo.patientName }}</el-descriptions-item>
        <el-descriptions-item label="性别">{{ patientInfo.gender }}</el-descriptions-item>
        <el-descriptions-item label="年龄">{{ patientInfo.age }}岁</el-descriptions-item>
        <el-descriptions-item label="就诊日期">{{ patientInfo.scheduleDate }}</el-descriptions-item>
        <el-descriptions-item label="挂号科室">{{ patientInfo.department }}</el-descriptions-item>
        <el-descriptions-item label="就诊医生">{{ patientInfo.doctorName }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-card class="diagnosis-card">
      <template #header>
        <span>诊断</span>
      </template>
      <el-input
        v-model="prescriptionForm.diagnosis"
        type="textarea"
        :rows="3"
        placeholder="请输入诊断"
      />
    </el-card>

    <el-card class="drug-search-card">
      <template #header>
        <span>添加药品</span>
      </template>
      <div class="search-section">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索药品"
          style="width: 300px; margin-right: 12px;"
        />
      </div>
      <el-table :data="filteredDrugs" style="width: 100%; margin-top: 16px;">
        <el-table-column prop="drugName" label="药品名称" />
        <el-table-column prop="spec" label="规格" />
        <el-table-column prop="price" label="单价">
          <template #default="{ row }">¥{{ row.price.toFixed(2) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="addDrug(row)">添加</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card class="prescription-drugs-card">
      <template #header>
        <span>处方药品</span>
      </template>
      <el-table :data="prescriptionForm.drugs" style="width: 100%;">
        <el-table-column prop="drugName" label="药品名称" />
        <el-table-column prop="spec" label="规格" />
        <el-table-column label="数量" width="120">
          <template #default="{ row }">
            <el-input-number
              v-model="row.quantity"
              :min="1"
              :max="10"
              size="small"
              @change="calculateSubtotal(row)"
            />
          </template>
        </el-table-column>
        <el-table-column label="用法" width="150">
          <template #default="{ row }">
            <el-select v-model="row.usage" size="small" style="width: 100%;">
              <el-option label="口服" value="口服" />
              <el-option label="冲服" value="冲服" />
              <el-option label="外用" value="外用" />
              <el-option label="注射" value="注射" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="频次" width="150">
          <template #default="{ row }">
            <el-select v-model="row.frequency" size="small" style="width: 100%;">
              <el-option label="每日1次" value="每日1次" />
              <el-option label="每日2次" value="每日2次" />
              <el-option label="每日3次" value="每日3次" />
              <el-option label="隔日1次" value="隔日1次" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="天数" width="100">
          <template #default="{ row }">
            <el-input-number
              v-model="row.days"
              :min="1"
              :max="30"
              size="small"
            />
          </template>
        </el-table-column>
        <el-table-column label="小计" width="100">
          <template #default="{ row }">¥{{ row.subtotal.toFixed(2) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="80">
          <template #default="{ $index }">
            <el-button type="danger" size="small" link @click="removeDrug($index)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="prescriptionForm.drugs.length === 0" description="暂无药品" />
    </el-card>

    <el-card class="total-card">
      <div class="total-section">
        <span class="total-label">总费用：</span>
        <span class="total-amount">¥{{ totalAmount.toFixed(2) }}</span>
      </div>
    </el-card>

    <div class="action-buttons">
      <el-button @click="router.back()">返回接诊列表</el-button>
      <el-button type="primary" :loading="submitting" @click="handleSubmit">提交处方</el-button>
    </div>
  </div>
</template>

<style scoped>
.prescription-page {
  max-width: 1200px;
}

.patient-select-card,
.patient-info-card,
.diagnosis-card,
.drug-search-card,
.prescription-drugs-card,
.total-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.appointment-no {
  font-size: 14px;
  color: #6b7280;
  font-weight: normal;
}

.patient-select {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}

.patient-count {
  font-size: 14px;
  color: #6b7280;
}

.total-section {
  text-align: right;
  font-size: 18px;
}

.total-label {
  color: #6b7280;
  margin-right: 12px;
}

.total-amount {
  color: #ef4444;
  font-weight: bold;
  font-size: 24px;
}

.action-buttons {
  text-align: center;
  margin-top: 24px;
}
</style>
