<script setup lang="ts">
import { ref, watch } from 'vue'
import { DataAnalysis } from '@element-plus/icons-vue'
import ConsultSectionCard from './ConsultSectionCard.vue'
import GraphStatusBadge from './GraphStatusBadge.vue'
import GraphRelationMini from './GraphRelationMini.vue'
import GraphEvidencePanel from './GraphEvidencePanel.vue'
import AgentPipelineSummary from './AgentPipelineSummary.vue'
import type { ConsultResult } from '@/services/medical/types'

interface Props {
  result?: ConsultResult | null
  defaultCollapsedOnMobile?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  defaultCollapsedOnMobile: true,
})

const emit = defineEmits<{
  openAgentTrace: []
}>()

const expanded = ref(true)

watch(
  () => props.result?.graphHit,
  (hit) => {
    if (hit) expanded.value = true
    else if (props.defaultCollapsedOnMobile && window.innerWidth < 768) {
      expanded.value = false
    }
  },
  { immediate: true }
)
</script>

<template>
  <ConsultSectionCard
    v-if="result"
    v-model:expanded="expanded"
    title="智能化分析依据"
    :icon="DataAnalysis"
    collapsible
    class="smart-insight-panel"
  >
    <div class="insight-toolbar">
      <GraphStatusBadge
        :graph-hit="result.graphHit"
        :grounding-status="result.groundingStatus"
        :query-time-ms="result.graphEvidence?.queryTimeMs"
      />
    </div>

    <GraphRelationMini
      :evidence="result.graphEvidence"
      :graph-hit="result.graphHit"
    />

    <GraphEvidencePanel
      v-if="result.graphHit && result.graphEvidence?.rows?.length"
      :evidence="result.graphEvidence"
      default-expanded
    />

    <p
      v-else-if="!result.graphHit"
      class="graph-miss-hint"
      role="note"
    >
      本次问诊未关联到知识图谱条目，暂无症状-疾病映射表可展示。
    </p>

    <AgentPipelineSummary
      v-if="result.agentTrace?.length"
      :traces="result.agentTrace"
      @open-detail="emit('openAgentTrace')"
    />
  </ConsultSectionCard>
</template>

<style scoped>
.smart-insight-panel {
  margin-top: var(--consult-spacing-md);
}

.insight-toolbar {
  margin-bottom: var(--consult-spacing-md);
}

.graph-miss-hint {
  margin: var(--consult-spacing-md) 0 0;
  padding: 12px 16px;
  font-size: 14px;
  line-height: 1.6;
  color: var(--consult-warning);
  background: var(--consult-warning-bg);
  border: 1px solid #fde68a;
  border-radius: var(--consult-radius);
}
</style>
