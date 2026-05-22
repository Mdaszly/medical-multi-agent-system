<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { getHealthProfile } from '@/services/medical/jiankangdanganguanli'
import SymptomInput from './SymptomInput.vue'

const authStore = useAuthStore()

export interface PatientContext {
  age?: number | null
  gender?: string
  medicalHistory?: string
  allergies?: string
  currentMedications?: string
  symptom?: string
}

interface Props {
  disabled?: boolean
}

withDefaults(defineProps<Props>(), {
  disabled: false,
})

const STORAGE_KEY = 'patient_context'

const formData = ref<PatientContext>({
  age: null,
  gender: '',
  medicalHistory: '',
  allergies: '',
  currentMedications: '',
  symptom: '',
})

const emit = defineEmits<{
  (e: 'update:context', context: PatientContext): void
}>()

const loadFromStorage = () => {
  const stored = localStorage.getItem(STORAGE_KEY)
  if (stored) {
    try {
      const parsed = JSON.parse(stored)
      formData.value = { ...formData.value, ...parsed }
    } catch (error) {
      console.error('Failed to load patient context from storage:', error)
    }
  }
}

const saveToStorage = () => {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(formData.value))
    emit('update:context', { ...formData.value })
  } catch (error) {
    console.error('Failed to save patient context to storage:', error)
  }
}

const getContext = (): PatientContext => ({ ...formData.value })

const reset = () => {
  formData.value = {
    age: null,
    gender: '',
    medicalHistory: '',
    allergies: '',
    currentMedications: '',
    symptom: '',
  }
  localStorage.removeItem(STORAGE_KEY)
  emit('update:context', { ...formData.value })
}

const prefillFromHealthProfile = async () => {
  const userId = authStore.userInfo?.id
  if (!userId) return

  const hasLocal =
    formData.value.medicalHistory?.trim() ||
    formData.value.allergies?.trim() ||
    formData.value.currentMedications?.trim()
  if (hasLocal) return

  try {
    const res = await getHealthProfile({ userId }, { showError: false })
    const hp = res.data
    if (!hp) return

    if (hp.chronicDiseases?.trim()) {
      formData.value.medicalHistory = hp.chronicDiseases.trim()
    }
    if (hp.allergyHistory?.trim()) {
      formData.value.allergies = hp.allergyHistory.trim()
    }
    if (hp.medicationHistory?.trim()) {
      formData.value.currentMedications = hp.medicationHistory.trim()
    }
    saveToStorage()
  } catch {
    // 无档案或无权时静默跳过
  }
}

watch(formData, () => saveToStorage(), { deep: true })

onMounted(async () => {
  loadFromStorage()
  await prefillFromHealthProfile()
})

defineExpose({ getContext, reset, prefillFromHealthProfile })
</script>

<template>
  <div class="patient-context-form">
    <el-form :model="formData" label-position="top" class="patient-form" :disabled="disabled">
      <fieldset class="form-section">
        <legend class="section-legend">基本信息</legend>
        <div class="basic-row">
          <el-form-item label="年龄" class="age-item">
            <el-input-number
              v-model="formData.age"
              :min="0"
              :max="150"
              :step="1"
              :precision="0"
              controls-position="right"
              class="age-input"
              aria-label="患者年龄"
            />
          </el-form-item>
          <el-form-item label="性别" class="gender-item">
            <el-radio-group v-model="formData.gender" aria-label="患者性别">
              <el-radio value="male">男</el-radio>
              <el-radio value="female">女</el-radio>
            </el-radio-group>
          </el-form-item>
        </div>
      </fieldset>

      <fieldset class="form-section">
        <legend class="section-legend">病史与过敏</legend>
        <el-form-item label="既往病史">
          <el-input
            v-model="formData.medicalHistory"
            type="textarea"
            :rows="2"
            placeholder="请输入既往病史"
            aria-label="既往病史"
          />
        </el-form-item>
        <el-form-item label="过敏信息">
          <el-input
            v-model="formData.allergies"
            type="textarea"
            :rows="2"
            placeholder="请输入过敏信息"
            aria-label="过敏信息"
          />
        </el-form-item>
        <el-form-item label="当前用药">
          <el-input
            v-model="formData.currentMedications"
            type="textarea"
            :rows="2"
            placeholder="请输入当前用药情况"
            aria-label="当前用药"
          />
        </el-form-item>
      </fieldset>

      <fieldset class="form-section symptom-section">
        <legend class="section-legend">当前症状</legend>
        <el-form-item label="症状描述" class="symptom-form-item">
          <SymptomInput
            v-model="formData.symptom"
            placeholder="请描述症状，支持知识图谱联想（如：头痛、发热）"
            @submit="(v) => (formData.symptom = v)"
          />
          <el-input
            v-model="formData.symptom"
            type="textarea"
            :rows="3"
            class="symptom-textarea"
            placeholder="可补充详细描述：持续时间、诱因等"
            aria-label="症状详细描述"
          />
        </el-form-item>
      </fieldset>
    </el-form>
  </div>
</template>

<style scoped>
.patient-context-form {
  width: 100%;
  padding: 8px 0;
}

.form-section {
  border: none;
  margin: 0 0 20px;
  padding: 0 0 16px;
  border-bottom: 1px solid var(--consult-border-light);
}

.form-section:last-child {
  border-bottom: none;
  margin-bottom: 0;
}

.section-legend {
  font-size: 14px;
  font-weight: 600;
  color: var(--consult-info);
  padding: 0 0 12px;
  width: 100%;
}

.basic-row {
  display: flex;
  flex-wrap: wrap;
  gap: 20px;
}

.age-item {
  flex: 0 0 160px;
}

.gender-item {
  flex: 1;
  min-width: 160px;
}

.age-input {
  width: 100%;
}

.symptom-form-item :deep(.symptom-input-wrapper) {
  margin-bottom: 12px;
}

.symptom-textarea :deep(.el-textarea__inner) {
  border-color: var(--consult-border);
  font-size: 14px;
  line-height: 1.6;
}

.symptom-textarea :deep(.el-textarea__inner:focus) {
  border-color: var(--el-color-primary);
}

.patient-form :deep(.el-form-item__label) {
  font-weight: 500;
  color: var(--consult-text-secondary);
}
</style>
