import type { Component } from 'vue'
import {
  FirstAidKit,
  Document,
  Warning,
  Goods,
  User,
  Scissor,
  Medal,
  DataAnalysis,
  Odometer,
} from '@element-plus/icons-vue'

export type HealthProfileField = keyof API.HealthProfileVO

export interface HealthNavSection {
  id: string
  title: string
  subtitle: string
  icon: Component
  fields: HealthProfileField[]
}

export const HEALTH_NAV_SECTIONS: HealthNavSection[] = [
  {
    id: 'vitals',
    title: '基本体征',
    subtitle: '身高、体重、血压等',
    icon: Odometer,
    fields: ['height', 'weight', 'bloodType', 'bloodPressure'],
  },
  {
    id: 'chronic',
    title: '慢性病史',
    subtitle: '长期疾病与管理',
    icon: FirstAidKit,
    fields: ['chronicDiseases'],
  },
  {
    id: 'allergy',
    title: '过敏史',
    subtitle: '药物与食物过敏',
    icon: Warning,
    fields: ['allergyHistory'],
  },
  {
    id: 'medication',
    title: '用药史',
    subtitle: '长期或近期用药',
    icon: Goods,
    fields: ['medicationHistory'],
  },
  {
    id: 'family',
    title: '家族病史',
    subtitle: '遗传与家族风险',
    icon: User,
    fields: ['familyHistory'],
  },
  {
    id: 'surgical',
    title: '手术史',
    subtitle: '既往手术记录',
    icon: Scissor,
    fields: ['surgicalHistory'],
  },
  {
    id: 'vaccination',
    title: '疫苗接种',
    subtitle: '接种记录',
    icon: Medal,
    fields: ['vaccinationHistory'],
  },
  {
    id: 'physical',
    title: '体检记录',
    subtitle: '近期体检摘要',
    icon: DataAnalysis,
    fields: ['physicalExam'],
  },
  {
    id: 'remark',
    title: '备注',
    subtitle: '其他补充说明',
    icon: Document,
    fields: ['remark'],
  },
]

export const USER_HEALTH_FIELDS: HealthProfileField[] = [
  'chronicDiseases',
  'allergyHistory',
  'height',
  'weight',
  'bloodType',
  'bloodPressure',
  'medicationHistory',
  'remark',
]

export const ROLE_VISIBLE_FIELDS: Record<string, HealthProfileField[]> = {
  user: USER_HEALTH_FIELDS,
  doctor: [
    'chronicDiseases',
    'allergyHistory',
    'medicationHistory',
    'familyHistory',
    'surgicalHistory',
    'height',
    'weight',
    'bloodType',
    'bloodPressure',
    'physicalExam',
    'vaccinationHistory',
    'remark',
  ],
  pharmacist: ['allergyHistory', 'medicationHistory', 'chronicDiseases'],
  admin: [
    'chronicDiseases',
    'allergyHistory',
    'medicationHistory',
    'familyHistory',
    'surgicalHistory',
    'vaccinationHistory',
    'physicalExam',
    'height',
    'weight',
    'bloodType',
    'bloodPressure',
    'remark',
  ],
}

export const FIELD_LABELS: Partial<Record<HealthProfileField, string>> = {
  chronicDiseases: '慢性病史',
  allergyHistory: '过敏史',
  medicationHistory: '用药史',
  familyHistory: '家族病史',
  surgicalHistory: '手术史',
  vaccinationHistory: '疫苗接种史',
  physicalExam: '体检记录',
  height: '身高 (cm)',
  weight: '体重 (kg)',
  bloodType: '血型',
  bloodPressure: '血压',
  remark: '备注',
}

export function getVisibleFields(role: string): HealthProfileField[] {
  return ROLE_VISIBLE_FIELDS[role] ?? USER_HEALTH_FIELDS
}

export function isFieldVisible(role: string, field: HealthProfileField): boolean {
  return getVisibleFields(role).includes(field)
}

export function calcBmi(heightCm?: number | null, weightKg?: number | null): number | null {
  if (!heightCm || !weightKg || heightCm <= 0 || weightKg <= 0) return null
  const h = heightCm / 100
  return Math.round((weightKg / (h * h)) * 10) / 10
}

export function bmiLabel(bmi: number | null): string {
  if (bmi == null) return '暂无数据'
  if (bmi < 18.5) return '偏瘦'
  if (bmi < 24) return '正常'
  if (bmi < 28) return '超重'
  return '肥胖'
}

export function bmiLevel(bmi: number | null): 'unknown' | 'thin' | 'normal' | 'overweight' | 'obese' {
  if (bmi == null) return 'unknown'
  if (bmi < 18.5) return 'thin'
  if (bmi < 24) return 'normal'
  if (bmi < 28) return 'overweight'
  return 'obese'
}

export function buildHealthSummary(profile: API.HealthProfileVO | null): string[] {
  if (!profile) return []
  const lines: string[] = []
  if (profile.chronicDiseases?.trim()) {
    lines.push(`慢性病史：${profile.chronicDiseases.trim()}`)
  }
  if (profile.allergyHistory?.trim()) {
    lines.push(`过敏史：${profile.allergyHistory.trim()}`)
  }
  if (profile.medicationHistory?.trim()) {
    lines.push(`当前/既往用药：${profile.medicationHistory.trim()}`)
  }
  const bmi = calcBmi(profile.height, profile.weight)
  if (bmi != null) {
    lines.push(`BMI ${bmi}（${bmiLabel(bmi)}）`)
  }
  if (profile.bloodPressure?.trim()) {
    lines.push(`血压：${profile.bloodPressure.trim()}`)
  }
  return lines
}
