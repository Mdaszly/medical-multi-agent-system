<script setup lang="ts">
import { computed } from 'vue'
import { calcBmi, bmiLabel, bmiLevel } from '@/constants/healthProfile'

interface Props {
  height?: number | null
  weight?: number | null
  bloodType?: string | null
  bloodPressure?: string | null
}

const props = defineProps<Props>()

const bmi = computed(() => calcBmi(props.height, props.weight))
const bmiText = computed(() => bmiLabel(bmi.value))
const level = computed(() => bmiLevel(bmi.value))

const ringPercent = computed(() => {
  if (bmi.value == null) return 0
  const clamped = Math.min(Math.max(bmi.value, 15), 35)
  return ((clamped - 15) / 20) * 100
})

const ringColor = computed(() => {
  const map: Record<string, string> = {
    thin: '#38bdf8',
    normal: '#10b981',
    overweight: '#f59e0b',
    obese: '#ef4444',
    unknown: '#94a3b8',
  }
  return map[level.value]
})
</script>

<template>
  <div class="metrics-card">
    <h3 class="metrics-title">体征概览</h3>
    <div class="metrics-body">
      <div class="bmi-ring" aria-label="BMI 指标">
        <svg viewBox="0 0 120 120" class="ring-svg" role="img" aria-hidden="true">
          <circle cx="60" cy="60" r="52" class="ring-track" />
          <circle
            cx="60"
            cy="60"
            r="52"
            class="ring-progress"
            :stroke="ringColor"
            :style="{
              strokeDasharray: `${ringPercent * 3.27} 327`,
            }"
          />
        </svg>
        <div class="bmi-center">
          <span class="bmi-value">{{ bmi ?? '—' }}</span>
          <span class="bmi-unit">BMI</span>
          <span class="bmi-status" :class="`level-${level}`">{{ bmiText }}</span>
        </div>
      </div>

      <div class="metric-grid">
        <div class="metric-item">
          <span class="metric-label">身高</span>
          <span class="metric-value">{{ height != null ? `${height} cm` : '—' }}</span>
        </div>
        <div class="metric-item">
          <span class="metric-label">体重</span>
          <span class="metric-value">{{ weight != null ? `${weight} kg` : '—' }}</span>
        </div>
        <div class="metric-item">
          <span class="metric-label">血型</span>
          <span class="metric-value">{{ bloodType || '—' }}</span>
        </div>
        <div class="metric-item">
          <span class="metric-label">血压</span>
          <span class="metric-value">{{ bloodPressure || '—' }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.metrics-card {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  padding: 20px;
  height: 100%;
}

.metrics-title {
  margin: 0 0 16px;
  font-size: 16px;
  font-weight: 600;
  color: #134e4a;
}

.metrics-body {
  display: flex;
  flex-wrap: wrap;
  gap: 20px;
  align-items: center;
}

.bmi-ring {
  position: relative;
  width: 140px;
  height: 140px;
  flex-shrink: 0;
}

.ring-svg {
  width: 100%;
  height: 100%;
  transform: rotate(-90deg);
}

.ring-track {
  fill: none;
  stroke: #e5e7eb;
  stroke-width: 10;
}

.ring-progress {
  fill: none;
  stroke-width: 10;
  stroke-linecap: round;
  transition: stroke-dasharray 0.3s ease, stroke 0.3s ease;
}

.bmi-center {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.bmi-value {
  font-size: 28px;
  font-weight: 700;
  color: #0f766e;
  line-height: 1.1;
}

.bmi-unit {
  font-size: 12px;
  color: #6b7280;
}

.bmi-status {
  margin-top: 4px;
  font-size: 12px;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 999px;
  background: #f0fdfa;
  color: #0d9488;
}

.bmi-status.level-overweight,
.bmi-status.level-obese {
  background: #fff7ed;
  color: #c2410c;
}

.metric-grid {
  flex: 1;
  min-width: 160px;
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.metric-item {
  background: #f9fafb;
  border-radius: 8px;
  padding: 12px;
}

.metric-label {
  display: block;
  font-size: 12px;
  color: #6b7280;
  margin-bottom: 4px;
}

.metric-value {
  font-size: 15px;
  font-weight: 600;
  color: #1f2937;
}

@media (max-width: 480px) {
  .metrics-body {
    flex-direction: column;
  }

  .metric-grid {
    width: 100%;
  }
}
</style>
