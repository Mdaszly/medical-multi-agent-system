<script setup lang="ts">
import { computed } from 'vue'
import { CircleCheck, CircleClose, Warning, Connection } from '@element-plus/icons-vue'
import type { GraphEvidence } from '@/services/medical/types'

interface Props {
  graphHit: boolean
  message: string
  groundingStatus?: string
  evidence?: GraphEvidence
}

const props = defineProps<Props>()

const bannerClass = computed(() =>
  props.graphHit ? 'banner-hit' : 'banner-miss'
)

const title = computed(() =>
  props.graphHit ? '知识图谱已命中' : '知识图谱未命中'
)

const statusIcon = computed(() => {
  if (!props.graphHit) return CircleClose
  if (props.groundingStatus === 'VERIFIED') return CircleCheck
  if (props.groundingStatus === 'WARNING') return Warning
  return Connection
})

const groundingHint = computed(() => {
  if (!props.graphHit) return null
  if (props.groundingStatus === 'VERIFIED') return 'ICD 编码已通过图谱校验'
  if (props.groundingStatus === 'WARNING') return '部分 ICD 引用未通过校验'
  if (props.groundingStatus === 'NO_HIT') return null
  return props.groundingStatus ? `校验状态：${props.groundingStatus}` : null
})
</script>

<template>
  <section
    class="graph-hit-banner"
    :class="bannerClass"
    role="status"
    :aria-label="title"
  >
    <div class="banner-main">
      <el-icon class="banner-icon" :size="22" aria-hidden="true">
        <component :is="statusIcon" />
      </el-icon>
      <div class="banner-text">
        <h4 class="banner-title">{{ title }}</h4>
        <p class="banner-message">{{ message }}</p>
        <p v-if="groundingHint" class="banner-grounding">{{ groundingHint }}</p>
      </div>
      <span
        v-if="evidence?.queryTimeMs"
        class="query-time"
        :title="`图谱查询耗时 ${evidence.queryTimeMs}ms`"
      >
        {{ evidence.queryTimeMs }}ms
      </span>
    </div>

    <div
      v-if="evidence?.symptomMatches?.length"
      class="symptom-matches"
      aria-label="症状术语标准化"
    >
      <span class="matches-label">术语标准化</span>
      <div class="match-chips">
        <span
          v-for="(match, index) in evidence.symptomMatches"
          :key="index"
          class="match-chip"
        >
          <span class="phrase">{{ match.userPhrase }}</span>
          <span class="arrow" aria-hidden="true">→</span>
          <span class="canonical">{{ match.canonicalName }}</span>
          <span v-if="match.method" class="method">{{ match.method }}</span>
        </span>
      </div>
    </div>

    <div
      v-else-if="evidence?.graphSkipReason"
      class="resolution-trace skip-reason"
    >
      <span class="matches-label">图谱门控</span>
      <span class="trace-text">{{ evidence.graphSkipReason }}</span>
    </div>

    <div
      v-else-if="evidence?.symptomResolutionTrace"
      class="resolution-trace"
    >
      <span class="matches-label">术语解析</span>
      <span class="trace-text">{{ evidence.symptomResolutionTrace }}</span>
    </div>
  </section>
</template>

<style scoped>
.graph-hit-banner {
  border-radius: 10px;
  padding: 16px 18px;
  margin-bottom: 20px;
  border: 1px solid transparent;
  transition: border-color 0.2s ease, background-color 0.2s ease;
}

.banner-hit {
  background: linear-gradient(135deg, #ecfdf5 0%, #f0fdfa 100%);
  border-color: #6ee7b7;
}

.banner-miss {
  background: linear-gradient(135deg, #fffbeb 0%, #fef3c7 100%);
  border-color: #fcd34d;
}

.banner-main {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.banner-icon {
  flex-shrink: 0;
  margin-top: 2px;
}

.banner-hit .banner-icon {
  color: #059669;
}

.banner-miss .banner-icon {
  color: #d97706;
}

.banner-text {
  flex: 1;
  min-width: 0;
}

.banner-title {
  margin: 0 0 6px;
  font-size: 16px;
  font-weight: 600;
  color: #1f2937;
  line-height: 1.4;
}

.banner-message {
  margin: 0;
  font-size: 14px;
  line-height: 1.65;
  color: #374151;
}

.banner-grounding {
  margin: 8px 0 0;
  font-size: 13px;
  color: #6b7280;
}

.query-time {
  flex-shrink: 0;
  font-size: 12px;
  color: #9ca3af;
  padding: 4px 8px;
  background: rgba(255, 255, 255, 0.7);
  border-radius: 6px;
}

.symptom-matches,
.resolution-trace {
  margin-top: 14px;
  padding-top: 14px;
  border-top: 1px dashed rgba(0, 0, 0, 0.08);
}

.matches-label {
  display: block;
  font-size: 12px;
  font-weight: 600;
  color: #6b7280;
  margin-bottom: 8px;
  text-transform: uppercase;
  letter-spacing: 0.04em;
}

.match-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.match-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  background: #fff;
  border: 1px solid #a7f3d0;
  border-radius: 8px;
  font-size: 13px;
  color: #065f46;
}

.banner-miss .match-chip {
  border-color: #fde68a;
  color: #92400e;
}

.phrase {
  color: #6b7280;
}

.arrow {
  color: #9ca3af;
  font-size: 12px;
}

.canonical {
  font-weight: 600;
}

.method {
  font-size: 11px;
  padding: 2px 6px;
  background: #d1fae5;
  border-radius: 4px;
  color: #047857;
}

.trace-text {
  font-size: 13px;
  color: #374151;
  line-height: 1.5;
}

@media (max-width: 768px) {
  .graph-hit-banner {
    padding: 14px;
  }

  .banner-title {
    font-size: 15px;
  }

  .banner-message {
    font-size: 13px;
  }

  .banner-main {
    flex-wrap: wrap;
  }

  .query-time {
    width: 100%;
    text-align: right;
  }
}

@media (prefers-reduced-motion: reduce) {
  .graph-hit-banner {
    transition: none;
  }
}
</style>
