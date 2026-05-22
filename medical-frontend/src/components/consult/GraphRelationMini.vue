<script setup lang="ts">
import { computed } from 'vue'
import type { GraphEvidence } from '@/services/medical/types'

interface Props {
  evidence?: GraphEvidence
  graphHit?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  graphHit: false,
})

const displayRows = computed(() => (props.evidence?.rows ?? []).slice(0, 5))
const extraCount = computed(() => Math.max(0, (props.evidence?.rows?.length ?? 0) - 5))

const nodeY = (index: number, total: number) => {
  const spacing = 280 / Math.max(total, 1)
  return 40 + index * spacing
}
</script>

<template>
  <div class="graph-relation-mini" role="img" aria-label="症状与疾病关联示意图">
    <svg
      v-if="graphHit && displayRows.length"
      viewBox="0 0 400 320"
      class="relation-svg"
      preserveAspectRatio="xMidYMid meet"
    >
      <defs>
        <marker id="arrowhead" markerWidth="8" markerHeight="6" refX="7" refY="3" orient="auto">
          <polygon points="0 0, 8 3, 0 6" fill="#94a3b8" />
        </marker>
      </defs>
      <text x="20" y="24" class="axis-label">症状</text>
      <text x="300" y="24" class="axis-label">疾病</text>
      <g v-for="(row, index) in displayRows" :key="index">
        <line
          :x1="120"
          :y1="nodeY(index, displayRows.length)"
          :x2="260"
          :y2="nodeY(index, displayRows.length)"
          stroke="#94a3b8"
          stroke-width="1.5"
          marker-end="url(#arrowhead)"
        />
        <rect
          x="20"
          :y="nodeY(index, displayRows.length) - 18"
          width="90"
          height="36"
          rx="6"
          class="node-symptom"
        />
        <text
          :x="65"
          :y="nodeY(index, displayRows.length) + 4"
          text-anchor="middle"
          class="node-text"
        >
          {{ (row.symptom || '').slice(0, 6) }}{{ (row.symptom?.length ?? 0) > 6 ? '…' : '' }}
        </text>
        <rect
          x="270"
          :y="nodeY(index, displayRows.length) - 18"
          width="110"
          height="36"
          rx="6"
          class="node-disease"
        />
        <text
          :x="325"
          :y="nodeY(index, displayRows.length) + 4"
          text-anchor="middle"
          class="node-text"
        >
          {{ (row.disease || '').slice(0, 8) }}{{ (row.disease?.length ?? 0) > 8 ? '…' : '' }}
        </text>
      </g>
    </svg>
    <div v-else class="miss-placeholder">
      <svg viewBox="0 0 200 80" class="miss-svg" aria-hidden="true">
        <rect x="10" y="20" width="60" height="40" rx="6" fill="none" stroke="#d1d5db" stroke-dasharray="4 4" />
        <rect x="130" y="20" width="60" height="40" rx="6" fill="none" stroke="#d1d5db" stroke-dasharray="4 4" />
        <line x1="70" y1="40" x2="130" y2="40" stroke="#d1d5db" stroke-dasharray="4 4" />
      </svg>
      <p>本次未命中知识图谱关联</p>
    </div>
    <p v-if="extraCount > 0" class="extra-hint">另有 {{ extraCount }} 条映射见下方表格</p>
  </div>
</template>

<style scoped>
.graph-relation-mini {
  width: 100%;
  overflow-x: auto;
  padding: var(--consult-spacing-md);
  background: var(--consult-bg);
  border-radius: var(--consult-radius);
  border: 1px solid var(--consult-border-light);
}

.relation-svg {
  width: 100%;
  max-width: 400px;
  height: auto;
  display: block;
  margin: 0 auto;
}

.axis-label {
  font-size: 12px;
  fill: var(--consult-text-muted);
  font-weight: 600;
}

.node-symptom {
  fill: var(--consult-info-bg);
  stroke: var(--consult-info);
  stroke-width: 1;
}

.node-disease {
  fill: var(--consult-success-bg);
  stroke: var(--consult-success);
  stroke-width: 1;
}

.node-text {
  font-size: 11px;
  fill: var(--consult-text-primary);
}

.miss-placeholder {
  text-align: center;
  padding: var(--consult-spacing-lg);
  color: var(--consult-text-muted);
  font-size: 13px;
}

.miss-svg {
  width: 200px;
  height: 80px;
  margin: 0 auto var(--consult-spacing-sm);
  display: block;
}

.extra-hint {
  margin: var(--consult-spacing-sm) 0 0;
  font-size: 12px;
  color: var(--consult-text-muted);
  text-align: center;
}
</style>
