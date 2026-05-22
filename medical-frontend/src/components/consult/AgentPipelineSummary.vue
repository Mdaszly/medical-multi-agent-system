<script setup lang="ts">
import { computed } from 'vue'
import { Connection, Search, Cpu, ChatDotRound, Link } from '@element-plus/icons-vue'
import type { AgentTrace } from '@/services/medical/consultTypes'

interface Props {
  traces?: AgentTrace[]
}

const props = withDefaults(defineProps<Props>(), {
  traces: () => [],
})

const emit = defineEmits<{
  openDetail: []
}>()

const agentIcon = (name?: string) => {
  const n = (name || '').toLowerCase()
  if (n.includes('router') || n.includes('路由')) return Connection
  if (n.includes('retriev') || n.includes('检索')) return Search
  if (n.includes('reason') || n.includes('推理')) return Cpu
  if (n.includes('chat') || n.includes('对话')) return ChatDotRound
  return Link
}

const chips = computed(() => {
  const map = new Map<string, AgentTrace>()
  for (const t of props.traces) {
    const key = t.agent || 'Agent'
    map.set(key, t)
  }
  return Array.from(map.entries()).map(([agent, trace]) => ({ agent, trace }))
})
</script>

<template>
  <div v-if="chips.length" class="agent-pipeline-summary">
    <div class="summary-header">
      <span class="summary-title">多 Agent 协作</span>
      <el-button
        type="primary"
        link
        size="small"
        aria-label="查看 Agent 执行轨迹详情"
        @click="emit('openDetail')"
      >
        查看详情
      </el-button>
    </div>
    <div class="agent-chips" role="list" aria-label="参与的 Agent 列表">
      <button
        v-for="chip in chips"
        :key="chip.agent"
        type="button"
        class="agent-chip"
        role="listitem"
        :aria-label="`${chip.agent}: ${chip.trace.action || '执行'}`"
        @click="emit('openDetail')"
      >
        <el-icon aria-hidden="true"><component :is="agentIcon(chip.agent)" /></el-icon>
        <span class="chip-name">{{ chip.agent }}</span>
        <span class="chip-action">{{ chip.trace.action || '执行' }}</span>
      </button>
    </div>
  </div>
</template>

<style scoped>
.agent-pipeline-summary {
  margin-top: var(--consult-spacing-md);
}

.summary-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--consult-spacing-sm);
}

.summary-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--consult-text-secondary);
}

.agent-chips {
  display: flex;
  flex-wrap: wrap;
  gap: var(--consult-spacing-sm);
}

.agent-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-height: 44px;
  padding: 8px 14px;
  background: var(--consult-surface);
  border: 1px solid var(--consult-border);
  border-radius: 22px;
  cursor: pointer;
  transition: border-color var(--consult-transition), box-shadow var(--consult-transition);
  font-family: inherit;
}

.agent-chip:hover {
  border-color: var(--el-color-primary);
  box-shadow: var(--consult-shadow-sm);
}

.agent-chip:focus-visible {
  outline: 2px solid var(--el-color-primary);
  outline-offset: 2px;
}

.agent-chip .el-icon {
  color: var(--consult-info);
  font-size: 16px;
}

.chip-name {
  font-size: 13px;
  font-weight: 600;
  color: var(--consult-text-primary);
}

.chip-action {
  font-size: 12px;
  color: var(--consult-text-muted);
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
