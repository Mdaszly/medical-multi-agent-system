<script setup lang="ts">
import { computed } from 'vue'
import { Edit, Connection, Search, Cpu, DocumentChecked, CircleCheck } from '@element-plus/icons-vue'
import type { ConsultResult } from '@/services/medical/types'

export type PipelineStage = 'input' | 'terms' | 'kg' | 'agents' | 'suggest'

interface Props {
  streaming?: boolean
  activeStage?: PipelineStage
  result?: ConsultResult | null
  showTechNotes?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  streaming: false,
  activeStage: 'input',
  showTechNotes: false,
})

const steps = [
  { id: 'input' as const, label: '用户输入', icon: Edit },
  { id: 'terms' as const, label: '术语标准化', icon: Connection },
  { id: 'kg' as const, label: '知识图谱检索', icon: Search },
  { id: 'agents' as const, label: '多 Agent 协作', icon: Cpu },
  { id: 'suggest' as const, label: '生成建议', icon: DocumentChecked },
]

const stageOrder: PipelineStage[] = ['input', 'terms', 'kg', 'agents', 'suggest']

const activeIndex = computed(() => {
  const idx = stageOrder.indexOf(props.activeStage)
  return idx >= 0 ? idx : 0
})

const isStepDone = (index: number) => {
  if (props.streaming) return index < activeIndex.value
  if (props.result) return true
  return false
}

const isStepActive = (index: number) => {
  if (props.streaming) return index === activeIndex.value
  return false
}

const stepMeta = (id: PipelineStage) => {
  if (!props.result || props.streaming) return ''
  const ev = props.result.graphEvidence
  if (id === 'terms') {
    const n = ev?.symptomMatches?.length ?? 0
    return n > 0 ? `${n} 项映射` : ''
  }
  if (id === 'kg') {
    const ms = props.result.graphEvidence?.queryTimeMs
    return props.result.graphHit
      ? `已命中${ms ? ` · ${ms}ms` : ''}`
      : '未命中'
  }
  if (id === 'agents') {
    const n = props.result.agentTrace?.length ?? 0
    return n > 0 ? `${n} 步` : ''
  }
  return ''
}
</script>

<template>
  <div class="pipeline-strip" role="list" aria-label="智能化分析流水线">
    <div class="pipeline-steps">
      <div
        v-for="(step, index) in steps"
        :key="step.id"
        class="pipeline-step"
        role="listitem"
        :class="{
          'is-active': isStepActive(index),
          'is-done': isStepDone(index),
        }"
        :aria-current="isStepActive(index) ? 'step' : undefined"
      >
        <div class="step-icon-wrap">
          <el-icon v-if="isStepDone(index) && !streaming" class="done-icon" aria-hidden="true">
            <CircleCheck />
          </el-icon>
          <el-icon v-else aria-hidden="true"><component :is="step.icon" /></el-icon>
        </div>
        <div class="step-text">
          <span class="step-name">{{ step.label }}</span>
          <span v-if="stepMeta(step.id)" class="step-meta">{{ stepMeta(step.id) }}</span>
        </div>
        <span v-if="index < steps.length - 1" class="step-arrow" aria-hidden="true">→</span>
      </div>
    </div>
    <p v-if="showTechNotes" class="tech-notes" role="note">
      多 Agent 分工：路由分诊 → 知识检索 → 推理生成；知识图谱提供症状-疾病证据支撑与同义术语标准化。
    </p>
  </div>
</template>

<style scoped>
.pipeline-strip {
  background: var(--consult-glass);
  backdrop-filter: blur(8px);
  border: 1px solid var(--consult-border);
  border-radius: var(--consult-radius-lg);
  padding: var(--consult-spacing-md);
  margin: 0 var(--consult-spacing-lg) var(--consult-spacing-md);
}

.pipeline-steps {
  display: flex;
  align-items: flex-start;
  gap: var(--consult-spacing-xs);
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;
  padding-bottom: 4px;
}

.pipeline-step {
  display: flex;
  align-items: center;
  gap: var(--consult-spacing-sm);
  flex-shrink: 0;
  min-height: 44px;
  padding: var(--consult-spacing-sm);
  border-radius: var(--consult-radius);
  transition: background var(--consult-transition);
}

.pipeline-step.is-active {
  background: var(--consult-info-bg);
}

.pipeline-step.is-active .step-icon-wrap {
  animation: pulse 1.5s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.85; transform: scale(1.05); }
}

.step-icon-wrap {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: var(--consult-surface-muted);
  color: var(--consult-text-muted);
  font-size: 16px;
}

.pipeline-step.is-done .step-icon-wrap,
.pipeline-step.is-done .done-icon {
  background: var(--consult-success-bg);
  color: var(--consult-success);
}

.pipeline-step.is-active .step-icon-wrap {
  background: var(--el-color-primary);
  color: #fff;
}

.step-text {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.step-name {
  font-size: 12px;
  font-weight: 600;
  color: var(--consult-text-primary);
  white-space: nowrap;
}

.step-meta {
  font-size: 11px;
  color: var(--consult-text-muted);
}

.step-arrow {
  color: var(--consult-border);
  font-size: 14px;
  margin: 0 2px;
}

.tech-notes {
  margin: var(--consult-spacing-md) 0 0;
  padding-top: var(--consult-spacing-md);
  border-top: 1px dashed var(--consult-border);
  font-size: 12px;
  line-height: 1.6;
  color: var(--consult-text-muted);
}

@media (max-width: 767px) {
  .pipeline-strip {
    margin: 0 var(--consult-spacing-md) var(--consult-spacing-md);
  }
}

@media (prefers-reduced-motion: reduce) {
  .pipeline-step.is-active .step-icon-wrap {
    animation: none;
  }
}
</style>
