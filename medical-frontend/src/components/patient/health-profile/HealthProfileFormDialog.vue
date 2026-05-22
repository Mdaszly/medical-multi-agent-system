<script setup lang="ts">
import { ref, watch, computed } from 'vue'
import { FIELD_LABELS, getVisibleFields, type HealthProfileField } from '@/constants/healthProfile'

interface Props {
  visible: boolean
  mode: 'create' | 'edit'
  initial?: API.HealthProfileVO | null
  userId: number
  userName: string
  role: string
  saving?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  saving: false,
})

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void
  (e: 'submit', payload: API.HealthProfile): void
}>()

const form = ref<API.HealthProfile>({
  userId: props.userId,
  userName: props.userName,
})

const editableFields = computed(() => getVisibleFields(props.role))

const textFields: HealthProfileField[] = [
  'chronicDiseases',
  'allergyHistory',
  'medicationHistory',
  'familyHistory',
  'surgicalHistory',
  'vaccinationHistory',
  'physicalExam',
  'remark',
]

const resetForm = () => {
  const base: API.HealthProfile = {
    userId: props.userId,
    userName: props.userName,
  }
  if (props.mode === 'edit' && props.initial) {
    form.value = {
      ...base,
      id: props.initial.id,
      chronicDiseases: props.initial.chronicDiseases ?? '',
      allergyHistory: props.initial.allergyHistory ?? '',
      medicationHistory: props.initial.medicationHistory ?? '',
      familyHistory: props.initial.familyHistory ?? '',
      surgicalHistory: props.initial.surgicalHistory ?? '',
      vaccinationHistory: props.initial.vaccinationHistory ?? '',
      physicalExam: props.initial.physicalExam ?? '',
      height: props.initial.height,
      weight: props.initial.weight,
      bloodType: props.initial.bloodType ?? '',
      bloodPressure: props.initial.bloodPressure ?? '',
      remark: props.initial.remark ?? '',
    }
  } else {
    form.value = base
  }
}

watch(
  () => [props.visible, props.mode, props.initial],
  () => {
    if (props.visible) resetForm()
  },
  { immediate: true },
)

const handleClose = () => {
  emit('update:visible', false)
}

const handleSubmit = () => {
  emit('submit', { ...form.value, userId: props.userId, userName: props.userName })
}

const showField = (field: HealthProfileField) => editableFields.value.includes(field)
</script>

<template>
  <el-dialog
    :model-value="visible"
    :title="mode === 'create' ? '创建健康档案' : '编辑健康档案'"
    width="640px"
    destroy-on-close
    class="health-profile-dialog"
    @update:model-value="emit('update:visible', $event)"
    @close="handleClose"
  >
    <el-form label-position="top" class="health-form">
      <div v-if="showField('height') || showField('weight')" class="form-row">
        <el-form-item v-if="showField('height')" :label="FIELD_LABELS.height">
          <el-input-number
            v-model="form.height"
            :min="50"
            :max="250"
            :precision="1"
            controls-position="right"
            class="full-width"
            aria-label="身高"
          />
        </el-form-item>
        <el-form-item v-if="showField('weight')" :label="FIELD_LABELS.weight">
          <el-input-number
            v-model="form.weight"
            :min="20"
            :max="300"
            :precision="1"
            controls-position="right"
            class="full-width"
            aria-label="体重"
          />
        </el-form-item>
      </div>

      <div v-if="showField('bloodType') || showField('bloodPressure')" class="form-row">
        <el-form-item v-if="showField('bloodType')" :label="FIELD_LABELS.bloodType">
          <el-select v-model="form.bloodType" placeholder="请选择" clearable aria-label="血型">
            <el-option label="A型" value="A" />
            <el-option label="B型" value="B" />
            <el-option label="AB型" value="AB" />
            <el-option label="O型" value="O" />
            <el-option label="未知" value="未知" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="showField('bloodPressure')" :label="FIELD_LABELS.bloodPressure">
          <el-input
            v-model="form.bloodPressure"
            placeholder="如 120/80 mmHg"
            aria-label="血压"
          />
        </el-form-item>
      </div>

      <el-form-item
        v-for="field in textFields"
        :key="field"
        v-show="showField(field)"
        :label="FIELD_LABELS[field]"
      >
        <el-input
          v-model="(form as Record<string, string | undefined>)[field]"
          type="textarea"
          :rows="field === 'remark' ? 2 : 3"
          :placeholder="`请填写${FIELD_LABELS[field]}`"
          :aria-label="FIELD_LABELS[field]"
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="handleClose">取消</el-button>
      <el-button type="primary" :loading="saving" :disabled="saving" @click="handleSubmit">
        {{ mode === 'create' ? '创建' : '保存' }}
      </el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.full-width {
  width: 100%;
}

@media (max-width: 560px) {
  .form-row {
    grid-template-columns: 1fr;
  }
}
</style>
