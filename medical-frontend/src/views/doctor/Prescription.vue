<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { listDrugs } from '@/services/medical/yaopinguanli'
import { createPrescription } from '@/services/medical/chufangguanli'

const router = useRouter()
const route = useRoute()

interface DrugItem {
  drugId: number
  drugName: string
  spec: string
  price: number
  quantity: number
  usage: string
  frequency: string
  days: number
  subtotal: number
}

const prescriptionForm = reactive({
  appointmentId: Number(route.params.appointmentId) || 0,
  diagnosis: '',
  drugs: [] as DrugItem[]
})

const submitting = ref(false)
const searchKeyword = ref('')

const drugList = ref<DrugItem[]>([])

const loadDrugs = async () => {
  try {
    const res = await listDrugs({})
    if (res.data) {
      drugList.value = res.data.map((drug: any) => ({
        drugId: drug.id,
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

onMounted(() => {
  loadDrugs()
})

// 计算总费用
const totalAmount = computed(() => {
  return prescriptionForm.drugs.reduce((sum, item) => sum + item.subtotal, 0)
})

// 计算小计
const calculateSubtotal = (item: DrugItem) => {
  item.subtotal = item.price * item.quantity
}

// 添加药品到处方
const addDrug = (drug: DrugItem) => {
  const existing = prescriptionForm.drugs.find(d => d.drugId === drug.drugId)
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

// 移除药品
const removeDrug = (index: number) => {
  prescriptionForm.drugs.splice(index, 1)
}

// 提交处方
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
      drugs: JSON.stringify(prescriptionForm.drugs.map(d => ({
        drugId: d.drugId,
        quantity: d.quantity,
        usage: d.usage,
        frequency: d.frequency,
        days: d.days
      })))
    })
    ElMessage.success('处方提交成功')
    router.back()
  } catch (error: any) {
    ElMessage.error(error.message || '提交失败')
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="prescription-page">
    <el-card class="patient-info-card">
      <template #header>
        <div class="card-header">
          <span>患者信息</span>
        </div>
      </template>
      <el-descriptions :column="3" border>
        <el-descriptions-item label="患者姓名">张三</el-descriptions-item>
        <el-descriptions-item label="性别">男</el-descriptions-item>
        <el-descriptions-item label="年龄">35岁</el-descriptions-item>
        <el-descriptions-item label="就诊日期">2026-05-18</el-descriptions-item>
        <el-descriptions-item label="挂号科室">心内科</el-descriptions-item>
        <el-descriptions-item label="就诊医生">李医生</el-descriptions-item>
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
      <el-table :data="drugList" style="width: 100%; margin-top: 16px;">
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
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="频次" width="150">
          <template #default="{ row }">
            <el-select v-model="row.frequency" size="small" style="width: 100%;">
              <el-option label="每日1次" value="每日1次" />
              <el-option label="每日2次" value="每日2次" />
              <el-option label="每日3次" value="每日3次" />
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
    </el-card>

    <el-card class="total-card">
      <div class="total-section">
        <span class="total-label">总费用：</span>
        <span class="total-amount">¥{{ totalAmount.toFixed(2) }}</span>
      </div>
    </el-card>

    <div class="action-buttons">
      <el-button @click="router.back()">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="handleSubmit">提交处方</el-button>
    </div>
  </div>
</template>

<style scoped>
.prescription-page {
  max-width: 1200px;
}

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
