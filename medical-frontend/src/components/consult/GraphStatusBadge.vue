<template>
  <div class="graph-status-badge" :class="badgeClass">
    <span class="status-icon">
      <component :is="statusIcon" />
    </span>
    <span class="status-text">{{ statusText }}</span>
    <span v-if="queryTimeMs" class="query-time">{{ queryTimeMs }}ms</span>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { CircleClose, Document, CircleCheck, Warning } from '@element-plus/icons-vue'

interface Props {
  graphHit?: boolean
  groundingStatus?: string
  queryTimeMs?: number
}

const props = withDefaults(defineProps<Props>(), {
  graphHit: false,
  groundingStatus: '',
  queryTimeMs: 0,
})

const statusIcon = computed(() => {
  if (!props.graphHit) return CircleClose
  if (props.groundingStatus === 'VERIFIED') return CircleCheck
  if (props.groundingStatus === 'PARTIAL') return Warning
  return Document
})

const badgeClass = computed(() => {
  if (!props.graphHit) return 'status-not-hit'
  if (props.groundingStatus === 'VERIFIED') return 'status-verified'
  if (props.groundingStatus === 'PARTIAL') return 'status-partial'
  return 'status-hit'
})

const statusText = computed(() => {
  if (!props.graphHit) return '未命中知识图谱'
  if (props.groundingStatus === 'VERIFIED') return '已命中 · ICD 已校验'
  if (props.groundingStatus === 'WARNING') return '已命中 · 校验存疑'
  if (props.groundingStatus === 'NO_HIT') return '未命中知识图谱'
  return '已命中知识图谱'
})
</script>

<style scoped>
.graph-status-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 14px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 500;
  transition: all var(--consult-transition);
}

.status-not-hit {
  background: var(--consult-surface-muted);
  color: var(--consult-text-muted);
}

.status-hit {
  background: var(--consult-info-bg);
  color: var(--consult-info);
}

.status-verified {
  background: var(--consult-success-bg);
  color: var(--consult-success);
}

.status-partial {
  background: var(--consult-warning-bg);
  color: var(--consult-warning);
}

.query-time {
  opacity: 0.75;
  font-size: 11px;
}
</style>
