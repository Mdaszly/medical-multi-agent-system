<script setup lang="ts">
import { computed } from 'vue'
import { User, ChatLineRound, DataAnalysis } from '@element-plus/icons-vue'

export type ConsultStep = 'context' | 'chat' | 'analysis'

interface Props {
  currentStep?: ConsultStep
}

const props = withDefaults(defineProps<Props>(), {
  currentStep: 'context',
})

const steps = [
  { id: 'context' as const, label: '患者信息', icon: User },
  { id: 'chat' as const, label: '症状对话', icon: ChatLineRound },
  { id: 'analysis' as const, label: '智能分析', icon: DataAnalysis },
]

const activeIndex = computed(() => {
  const idx = steps.findIndex((s) => s.id === props.currentStep)
  return idx >= 0 ? idx : 0
})
</script>

<template>
  <nav class="consult-step-bar" role="navigation" aria-label="问诊进度">
    <ol class="step-list" role="list">
      <li
        v-for="(step, index) in steps"
        :key="step.id"
        class="step-item"
        :class="{
          'is-active': index === activeIndex,
          'is-done': index < activeIndex,
        }"
        :aria-current="index === activeIndex ? 'step' : undefined"
      >
        <div class="step-marker">
          <el-icon aria-hidden="true"><component :is="step.icon" /></el-icon>
        </div>
        <span class="step-label">{{ step.label }}</span>
        <span v-if="index < steps.length - 1" class="step-connector" aria-hidden="true" />
      </li>
    </ol>
  </nav>
</template>

<style scoped>
.consult-step-bar {
  background: var(--consult-surface);
  border-bottom: 1px solid var(--consult-border);
  padding: var(--consult-spacing-sm) var(--consult-spacing-lg);
}

.step-list {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0;
  margin: 0;
  padding: 0;
  list-style: none;
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;
}

.step-item {
  display: flex;
  align-items: center;
  gap: var(--consult-spacing-sm);
  position: relative;
  flex-shrink: 0;
  min-height: 44px;
  padding: 0 var(--consult-spacing-md);
}

.step-marker {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: var(--consult-surface-muted);
  color: var(--consult-text-muted);
  transition: all var(--consult-transition);
}

.step-item.is-active .step-marker {
  background: var(--el-color-primary);
  color: #fff;
  box-shadow: 0 2px 8px rgba(13, 148, 136, 0.35);
}

.step-item.is-done .step-marker {
  background: var(--consult-success-bg);
  color: var(--consult-success);
}

.step-label {
  font-size: 14px;
  font-weight: 500;
  color: var(--consult-text-muted);
  white-space: nowrap;
}

.step-item.is-active .step-label {
  color: var(--consult-info);
  font-weight: 600;
}

.step-item.is-done .step-label {
  color: var(--consult-text-secondary);
}

.step-connector {
  width: 32px;
  height: 2px;
  background: var(--consult-border);
  margin-left: var(--consult-spacing-sm);
}

.step-item.is-done .step-connector {
  background: var(--consult-success);
}

@media (max-width: 767px) {
  .step-list {
    justify-content: flex-start;
  }

  .step-connector {
    width: 20px;
  }
}
</style>
