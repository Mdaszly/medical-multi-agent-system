<script setup lang="ts">
import { ref, computed } from 'vue'
import {
  Link,
  Clock,
  Document,
  Setting,
  ArrowRight,
  Connection,
  Search,
  Cpu,
  ChatDotRound,
} from '@element-plus/icons-vue'
import type { AgentTrace } from '@/services/medical/consultTypes'

interface Props {
  traces?: AgentTrace[]
  visible?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  traces: () => [],
  visible: false,
})

const emit = defineEmits<{
  'update:visible': [value: boolean]
  close: []
}>()

const debugMode = ref(false)

const drawerVisible = computed({
  get: () => props.visible,
  set: (val: boolean) => emit('update:visible', val),
})

const groupedTraces = computed(() => {
  const groups = new Map<string, AgentTrace[]>()
  for (const t of props.traces) {
    const key = t.agent || '未知 Agent'
    if (!groups.has(key)) groups.set(key, [])
    groups.get(key)!.push(t)
  }
  return Array.from(groups.entries())
})

const getAgentIcon = (agent?: string) => {
  const n = (agent || '').toLowerCase()
  if (n.includes('router') || n.includes('路由')) return Connection
  if (n.includes('retriev') || n.includes('检索')) return Search
  if (n.includes('reason') || n.includes('推理')) return Cpu
  if (n.includes('chat') || n.includes('对话')) return ChatDotRound
  return Link
}

const getActionIcon = (action?: string) => {
  const actionLower = (action || '').toLowerCase()
  if (actionLower.includes('search') || actionLower.includes('查询')) return Document
  if (actionLower.includes('config') || actionLower.includes('配置')) return Setting
  if (actionLower.includes('process') || actionLower.includes('处理')) return ArrowRight
  return Link
}

const formatTimestamp = (timestamp?: string | Date) => {
  if (!timestamp) return ''
  return new Date(timestamp).toLocaleTimeString('zh-CN', {
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
  })
}
</script>

<template>
  <el-drawer
    v-model="drawerVisible"
    title="Agent 执行链路"
    direction="rtl"
    size="480px"
    aria-label="Agent 执行链路详情"
    @close="emit('close')"
  >
    <template #header>
      <div class="drawer-header">
        <el-icon :size="20" class="header-icon" aria-hidden="true"><Cpu /></el-icon>
        <span class="header-title">多 Agent 执行链路</span>
      </div>
    </template>

    <div class="trace-container">
      <div class="debug-toggle">
        <el-switch
          v-model="debugMode"
          active-text="调试模式"
          inactive-text="标准模式"
          aria-label="调试模式开关"
        />
      </div>

      <div v-if="traces.length === 0" class="empty-traces">
        <el-icon :size="48" class="empty-icon" aria-hidden="true"><Clock /></el-icon>
        <p class="empty-text">暂无执行链路</p>
      </div>

      <div v-else class="trace-groups">
        <div v-for="[agent, items] in groupedTraces" :key="agent" class="agent-group">
          <div class="group-header">
            <el-icon aria-hidden="true"><component :is="getAgentIcon(agent)" /></el-icon>
            <span class="group-name">{{ agent }}</span>
            <el-tag size="small" type="info">{{ items.length }} 步</el-tag>
          </div>
          <el-timeline class="trace-timeline">
            <el-timeline-item
              v-for="(trace, index) in items"
              :key="index"
              :timestamp="formatTimestamp(trace.timestamp)"
              placement="top"
              type="primary"
              hollow
            >
              <el-card class="trace-card" shadow="hover">
                <div class="trace-header">
                  <el-tag size="small" class="action-tag">
                    <el-icon class="action-icon" aria-hidden="true">
                      <component :is="getActionIcon(trace.action)" />
                    </el-icon>
                    {{ trace.action || '执行' }}
                  </el-tag>
                </div>
                <div class="trace-detail">
                  <div class="detail-content" :class="{ 'debug-detail': debugMode }">
                    {{ trace.detail }}
                  </div>
                </div>
              </el-card>
            </el-timeline-item>
          </el-timeline>
        </div>
      </div>
    </div>
  </el-drawer>
</template>

<style scoped>
.drawer-header {
  display: flex;
  align-items: center;
  gap: 8px;
}

.header-icon {
  color: var(--consult-info);
}

.header-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--consult-text-primary);
}

.trace-container {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.debug-toggle {
  padding: 0 0 16px;
  border-bottom: 1px solid var(--consult-border);
  margin-bottom: 16px;
}

.empty-traces {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 60px 20px;
  color: var(--consult-text-muted);
}

.agent-group {
  margin-bottom: 24px;
}

.group-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid var(--consult-border-light);
}

.group-header .el-icon {
  color: var(--consult-info);
}

.group-name {
  font-weight: 600;
  font-size: 15px;
  color: var(--consult-text-primary);
}

.trace-timeline :deep(.el-timeline-item__node--primary) {
  background-color: var(--el-color-primary);
  border-color: var(--el-color-primary);
}

.trace-card {
  border-radius: var(--consult-radius);
  border: 1px solid var(--consult-border);
  transition: border-color var(--consult-transition);
}

.trace-card:hover {
  border-color: var(--el-color-primary);
}

.action-tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  background: var(--consult-success-bg);
  border-color: var(--consult-info);
  color: var(--consult-info);
}

.detail-content {
  font-size: 14px;
  color: var(--consult-text-secondary);
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}

.debug-detail {
  background: var(--consult-surface-muted);
  padding: 8px 12px;
  border-radius: 6px;
  border-left: 3px solid var(--el-color-primary);
}

@media (prefers-reduced-motion: reduce) {
  .trace-card {
    transition: none;
  }
}

@media (max-width: 767px) {
  :deep(.el-drawer) {
    width: 100% !important;
  }
}
</style>
