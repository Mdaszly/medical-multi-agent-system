<script setup lang="ts">
import { computed, ref, watch, onMounted, onUnmounted, nextTick } from 'vue'
import { Warning, Check, Clock, Document, ChatDotRound, ArrowDown } from '@element-plus/icons-vue'
import GraphHitStatusBanner from '@/components/consult/GraphHitStatusBanner.vue'
import type { ConsultResult } from '@/services/medical/types'

interface Props {
  result?: ConsultResult | null
}

const props = defineProps<Props>()

const scrollRef = ref<HTMLElement | null>(null)
const canScroll = ref(false)
const showScrollHint = ref(false)

const riskLevelConfig = computed(() => {
  const level = props.result?.riskLevel ?? ''
  if (level.includes('高')) {
    return { label: level || '高风险', type: 'danger' as const, icon: Warning }
  }
  if (level.includes('中')) {
    return { label: level || '中风险', type: 'warning' as const, icon: Warning }
  }
  if (level.includes('低')) {
    return { label: level || '低风险', type: 'success' as const, icon: Check }
  }
  const lower = level.toLowerCase()
  const configs: Record<string, { label: string; type: 'success' | 'warning' | 'danger'; icon: typeof Check }> = {
    high: { label: '高风险', type: 'danger', icon: Warning },
    medium: { label: '中风险', type: 'warning', icon: Warning },
    low: { label: '低风险', type: 'success', icon: Check },
  }
  return configs[lower] || { label: level || '未知', type: 'success' as const, icon: Check }
})

const showCard = computed(
  () =>
    !!props.result?.answer ||
    !!props.result?.graphHitMessage ||
    props.result?.graphHit !== undefined
)

function updateScrollState() {
  const el = scrollRef.value
  if (!el) {
    canScroll.value = false
    showScrollHint.value = false
    return
  }
  const overflow = el.scrollHeight > el.clientHeight + 4
  canScroll.value = overflow
  const nearBottom = el.scrollHeight - el.scrollTop - el.clientHeight < 24
  showScrollHint.value = overflow && !nearBottom
}

function onScroll() {
  updateScrollState()
}

function scrollDown() {
  scrollRef.value?.scrollBy({ top: 120, behavior: 'smooth' })
}

let resizeObserver: ResizeObserver | null = null

watch(
  () => props.result?.answer,
  () => nextTick(updateScrollState)
)

onMounted(() => {
  nextTick(updateScrollState)
  if (scrollRef.value && typeof ResizeObserver !== 'undefined') {
    resizeObserver = new ResizeObserver(() => updateScrollState())
    resizeObserver.observe(scrollRef.value)
  }
})

onUnmounted(() => {
  resizeObserver?.disconnect()
})
</script>

<template>
  <div v-if="showCard" class="consult-result-card">
    <GraphHitStatusBanner
      :graph-hit="result?.graphHit ?? false"
      :message="result?.graphHitMessage ?? ''"
      :grounding-status="result?.groundingStatus"
      :evidence="result?.graphEvidence"
    />

    <div v-if="result?.answer" class="result-body">
      <div class="result-summary" role="group" aria-label="问诊结果摘要">
        <el-tag
          v-if="result.riskLevel"
          :type="riskLevelConfig.type"
          size="default"
          class="summary-tag"
        >
          <el-icon :size="14" aria-hidden="true">
            <component :is="riskLevelConfig.icon" />
          </el-icon>
          {{ riskLevelConfig.label }}
        </el-tag>
        <el-tag v-if="result.department" type="primary" effect="plain" class="summary-tag">
          {{ result.department }}
        </el-tag>
        <el-tag
          :type="result.graphHit ? 'success' : 'info'"
          effect="plain"
          class="summary-tag"
        >
          {{ result.graphHit ? '图谱已命中' : '图谱未命中' }}
        </el-tag>
      </div>

      <div class="result-header">
        <h3 class="result-title">
          <el-icon aria-hidden="true"><ChatDotRound /></el-icon>
          AI 问诊建议
        </h3>
      </div>

      <div class="scroll-panel">
        <div
          ref="scrollRef"
          class="result-content-scroll"
          tabindex="0"
          role="region"
          aria-label="AI 问诊建议完整内容，可向下滚动查看"
          @scroll="onScroll"
        >
          <div class="result-content">
            <div class="answer-section">
              <div class="section-label">
                <el-icon aria-hidden="true"><Document /></el-icon>
                <span>诊断建议</span>
              </div>
              <div class="answer-text">{{ result.answer }}</div>
            </div>

            <div v-if="result.department" class="department-section">
              <div class="section-label">
                <el-icon aria-hidden="true"><Clock /></el-icon>
                <span>推荐科室</span>
              </div>
              <div class="department-tag">
                <el-tag type="primary" size="large">{{ result.department }}</el-tag>
              </div>
            </div>

            <div v-if="result.nextQuestions?.length" class="followup-section">
              <div class="section-label">
                <span>建议补充信息</span>
              </div>
              <ul class="followup-list">
                <li v-for="(q, index) in result.nextQuestions" :key="index">{{ q }}</li>
              </ul>
            </div>

            <div v-if="result.suggestions?.length" class="suggestions-section">
              <div class="section-label">
                <el-icon aria-hidden="true"><Check /></el-icon>
                <span>健康建议</span>
              </div>
              <ul class="suggestions-list">
                <li v-for="(suggestion, index) in result.suggestions" :key="index">
                  {{ suggestion }}
                </li>
              </ul>
            </div>

            <div v-if="result.disclaimer" class="disclaimer-section">
              <el-alert type="warning" :closable="false" show-icon>
                <template #title>
                  <strong>免责声明</strong>
                </template>
                <template #default>
                  {{ result.disclaimer }}
                </template>
              </el-alert>
            </div>
          </div>
        </div>

        <div
          v-if="showScrollHint"
          class="scroll-hint"
          aria-hidden="true"
        >
          <button
            type="button"
            class="scroll-hint-btn"
            aria-label="向下滚动查看完整 AI 问诊建议"
            @click="scrollDown"
          >
            <el-icon class="hint-icon" aria-hidden="true"><ArrowDown /></el-icon>
            <span>向下滚动查看完整回复</span>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.consult-result-card {
  background: var(--consult-surface);
  border-radius: var(--consult-radius-lg);
  box-shadow: var(--consult-shadow-md);
  margin-top: var(--consult-spacing-md);
  padding: var(--consult-spacing-lg);
  display: flex;
  flex-direction: column;
  min-height: 0;
  border: 1px solid var(--consult-border);
}

.result-summary {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding-bottom: 12px;
  margin-bottom: 4px;
  border-bottom: 1px solid var(--consult-border-light);
}

.summary-tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  min-height: 32px;
}

.result-body {
  border-top: 1px solid #f3f4f6;
  padding-top: 4px;
  display: flex;
  flex-direction: column;
  min-height: 0;
  flex: 1 1 auto;
}

.result-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 0 12px;
  margin: 0 -4px;
  flex-shrink: 0;
}

.result-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: var(--consult-info);
}

.scroll-panel {
  position: relative;
  flex: 1 1 auto;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.result-content-scroll {
  max-height: min(420px, 42vh);
  overflow-y: auto;
  overflow-x: hidden;
  padding-right: 4px;
  padding-bottom: 8px;
  scrollbar-gutter: stable;
  scroll-behavior: smooth;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  background: #fafafa;
}

.result-content-scroll:focus-visible {
  outline: 2px solid #0d9488;
  outline-offset: 2px;
}

/* 可见滚动条 - 医疗风 Teal 点缀 */
.result-content-scroll::-webkit-scrollbar {
  width: 10px;
}

.result-content-scroll::-webkit-scrollbar-track {
  background: #f1f5f9;
  border-radius: 0 10px 10px 0;
}

.result-content-scroll::-webkit-scrollbar-thumb {
  background: linear-gradient(180deg, #5eead4 0%, #0d9488 100%);
  border-radius: 6px;
  border: 2px solid #f1f5f9;
  min-height: 40px;
}

.result-content-scroll::-webkit-scrollbar-thumb:hover {
  background: linear-gradient(180deg, #2dd4bf 0%, #0f766e 100%);
}

@supports (scrollbar-color: auto) {
  .result-content-scroll {
    scrollbar-width: thin;
    scrollbar-color: #0d9488 #f1f5f9;
  }
}

.result-content {
  padding: 16px 16px 8px;
}

.section-label {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 600;
  color: #0d9488;
  margin-bottom: 12px;
}

.section-label .el-icon {
  font-size: 18px;
}

.answer-section {
  margin-bottom: 24px;
}

.answer-text {
  font-size: 15px;
  line-height: 1.75;
  color: #374151;
  white-space: pre-wrap;
  word-break: break-word;
}

.department-section,
.followup-section,
.suggestions-section {
  margin-bottom: 24px;
}

.department-tag .el-tag {
  padding: 8px 16px;
  font-size: 14px;
  border-radius: 8px;
}

.followup-list,
.suggestions-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.followup-list li,
.suggestions-list li {
  position: relative;
  padding: 12px 16px 12px 32px;
  background: #f0fdf4;
  border-left: 3px solid #10b981;
  margin-bottom: 8px;
  border-radius: 4px;
  font-size: 14px;
  color: #065f46;
  line-height: 1.5;
}

.followup-list li::before,
.suggestions-list li::before {
  content: '•';
  position: absolute;
  left: 14px;
  color: #10b981;
  font-weight: bold;
}

.disclaimer-section {
  margin-top: 8px;
  margin-bottom: 8px;
}

.disclaimer-section :deep(.el-alert) {
  border-radius: 8px;
}

.scroll-hint {
  position: absolute;
  left: 0;
  right: 12px;
  bottom: 0;
  height: 72px;
  display: flex;
  align-items: flex-end;
  justify-content: center;
  padding-bottom: 8px;
  background: linear-gradient(
    to bottom,
    rgba(250, 250, 250, 0) 0%,
    rgba(250, 250, 250, 0.85) 40%,
    rgba(250, 250, 250, 1) 100%
  );
  pointer-events: none;
  border-radius: 0 0 10px 10px;
}

.scroll-hint-btn {
  pointer-events: auto;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-height: 44px;
  padding: 10px 18px;
  font-size: 14px;
  font-weight: 500;
  color: #0f766e;
  background: #ffffff;
  border: 1px solid #99f6e4;
  border-radius: 22px;
  box-shadow: 0 2px 8px rgba(13, 148, 136, 0.15);
  cursor: pointer;
  transition: background 0.2s ease, transform 0.2s ease, box-shadow 0.2s ease;
}

.scroll-hint-btn:hover {
  background: #ecfdf5;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(13, 148, 136, 0.2);
}

.scroll-hint-btn:focus-visible {
  outline: 2px solid #0d9488;
  outline-offset: 2px;
}

.hint-icon {
  animation: hint-bounce 1.5s ease-in-out infinite;
}

@keyframes hint-bounce {
  0%,
  100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(4px);
  }
}

@media (max-width: 768px) {
  .consult-result-card {
    padding: 16px;
  }

  .result-header {
    flex-direction: column;
    gap: 12px;
    align-items: flex-start;
  }

  .result-content-scroll {
    max-height: min(360px, 50vh);
  }

  .answer-text {
    font-size: 14px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .result-content-scroll {
    scroll-behavior: auto;
  }

  .scroll-hint-btn {
    transition: none;
  }

  .scroll-hint-btn:hover {
    transform: none;
  }

  .hint-icon {
    animation: none;
  }
}
</style>
