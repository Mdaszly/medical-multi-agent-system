<script setup lang="ts">
import { ref, onMounted, nextTick, computed, watch, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowDown, ArrowUp, User } from '@element-plus/icons-vue'
import ConsultLayout from '@/components/layout/ConsultLayout.vue'
import SessionSidebar from '@/components/consult/SessionSidebar.vue'
import ChatPanel from '@/components/consult/ChatPanel.vue'
import PatientContextForm from '@/components/consult/PatientContextForm.vue'
import ConsultResultCard from '@/components/consult/ConsultResultCard.vue'
import ConsultStepBar from '@/components/consult/ConsultStepBar.vue'
import type { ConsultStep } from '@/components/consult/ConsultStepBar.vue'
import ConsultPipelineStrip from '@/components/consult/ConsultPipelineStrip.vue'
import type { PipelineStage } from '@/components/consult/ConsultPipelineStrip.vue'
import SmartInsightPanel from '@/components/consult/SmartInsightPanel.vue'
import AgentTraceDrawer from '@/components/consult/AgentTraceDrawer.vue'
import { consultStream, createSession, listMessages } from '@/services/medical/consult'
import type { ConsultResult } from '@/services/medical/types'

interface ChatMessageVO {
  id: string | number
  role: 'user' | 'assistant' | 'system'
  content: string
  createdAt?: string | Date
}

const route = useRoute()
const router = useRouter()

const currentSessionId = ref<string>('')
const messages = ref<ChatMessageVO[]>([])
const isStreaming = ref(false)
const showContextForm = ref(true)
const isFormCollapsed = ref(false)
const contextFormRef = ref<InstanceType<typeof PatientContextForm> | null>(null)
const chatPanelRef = ref<InstanceType<typeof ChatPanel> | null>(null)
const streamingContent = ref('')
const consultResult = ref<ConsultResult | null>(null)
const showAgentTrace = ref(false)
const demoMode = ref(false)
const pipelineStage = ref<PipelineStage>('input')

let pipelineTimer: ReturnType<typeof setInterval> | null = null

const hasMessages = computed(() => messages.value.length > 0)

const currentStep = computed<ConsultStep>(() => {
  if (consultResult.value && !isStreaming.value) return 'analysis'
  if (hasMessages.value || isStreaming.value) return 'chat'
  return 'context'
})

const showPipeline = computed(() => isStreaming.value || !!consultResult.value)

const loadSessionMessages = async (sessionId: string) => {
  if (!sessionId) return
  try {
    const res = await listMessages({ sessionId, limit: 50 })
    if (res.code === 0 && res.data) {
      messages.value = res.data.map((msg: Record<string, unknown>) => ({
        id: (msg.id as string | number) || Date.now(),
        role: msg.role as 'user' | 'assistant' | 'system',
        content: (msg.content as string) || '',
        createdAt: msg.createTime as string | Date,
      }))
    }
  } catch (error) {
    console.error('加载历史消息失败:', error)
    ElMessage.error('加载历史消息失败')
  }
}

const initSession = async () => {
  const sessionId = route.params.sessionId as string | undefined
  if (sessionId) {
    currentSessionId.value = sessionId
    showContextForm.value = false
    isFormCollapsed.value = true
    await loadSessionMessages(sessionId)
  } else {
    currentSessionId.value = ''
    showContextForm.value = true
    isFormCollapsed.value = false
    messages.value = []
    consultResult.value = null
  }
}

const startPipelineTimer = () => {
  stopPipelineTimer()
  pipelineStage.value = 'terms'
  const stages: PipelineStage[] = ['terms', 'kg', 'agents', 'suggest']
  let idx = 0
  pipelineTimer = setInterval(() => {
    if (idx < stages.length) {
      pipelineStage.value = stages[idx]!
      idx++
    }
  }, 1200)
}

const stopPipelineTimer = () => {
  if (pipelineTimer) {
    clearInterval(pipelineTimer)
    pipelineTimer = null
  }
  pipelineStage.value = 'suggest'
}

const handleCreateSession = async () => {
  try {
    const res = await createSession({})
    if (res.code === 0 && res.data?.sessionId) {
      currentSessionId.value = res.data.sessionId
      router.push({ name: 'Consult', params: { sessionId: res.data.sessionId } })
      ElMessage.success('会话创建成功')
    }
  } catch (error) {
    console.error('创建会话失败:', error)
    ElMessage.error('创建会话失败')
  }
}

const handleSessionSelect = async (sessionId: string) => {
  if (sessionId) {
    currentSessionId.value = sessionId
    router.push({ name: 'Consult', params: { sessionId } })
    messages.value = []
    consultResult.value = null
    showContextForm.value = false
    isFormCollapsed.value = true
    await loadSessionMessages(sessionId)
  } else {
    currentSessionId.value = ''
    router.push({ name: 'Consult' })
    messages.value = []
    consultResult.value = null
    showContextForm.value = true
    isFormCollapsed.value = false
  }
}

const toggleContextForm = () => {
  isFormCollapsed.value = !isFormCollapsed.value
}

const handleFormSubmit = async () => {
  const context = contextFormRef.value?.getContext()
  if (!context) {
    ElMessage.warning('请填写患者信息')
    return
  }
  if (!context.symptom?.trim()) {
    ElMessage.warning('请输入症状描述')
    return
  }
  if (!currentSessionId.value) {
    await handleCreateSession()
  }
  if (currentSessionId.value && context.symptom) {
    isFormCollapsed.value = true
    showContextForm.value = false
    await sendMessage(context.symptom)
  }
}

const sendMessage = async (content: string) => {
  if (!content.trim() || isStreaming.value) return

  const userMessage: ChatMessageVO = {
    id: `user-${Date.now()}`,
    role: 'user',
    content: content.trim(),
    createdAt: new Date().toISOString(),
  }
  messages.value.push(userMessage)

  const assistantMessage: ChatMessageVO = {
    id: `assistant-${Date.now()}`,
    role: 'assistant',
    content: '',
    createdAt: new Date().toISOString(),
  }
  messages.value.push(assistantMessage)

  isStreaming.value = true
  streamingContent.value = ''
  consultResult.value = null
  pipelineStage.value = 'input'
  startPipelineTimer()

  await nextTick()
  chatPanelRef.value?.scrollToBottom()

  try {
    const context = contextFormRef.value?.getContext()
    const patientContext = context
      ? {
          age: context.age || undefined,
          gender: context.gender,
          medicalHistory: context.medicalHistory,
          allergies: context.allergies,
          currentMedications: context.currentMedications,
          symptom: context.symptom,
        }
      : undefined

    await consultStream(
      {
        question: content.trim(),
        sessionId: currentSessionId.value,
        patientContext,
      },
      (data) => {
        if (data.type === 'content' && data.content) {
          streamingContent.value += data.content
          const lastMsg = messages.value[messages.value.length - 1]
          if (lastMsg && lastMsg.role === 'assistant') {
            lastMsg.content = streamingContent.value
          }
          chatPanelRef.value?.scrollToBottom()
        } else if (data.type === 'done' && data.data) {
          consultResult.value = data.data
          const lastMsg = messages.value[messages.value.length - 1]
          if (lastMsg?.role === 'assistant' && data.data.graphHitMessage) {
            lastMsg.content = `${streamingContent.value}\n\n---\n${data.data.graphHitMessage}`
          }
        } else if (data.type === 'error') {
          ElMessage.error(data.error || '问诊失败')
        }
      },
      (error) => {
        console.error('SSE error:', error)
        ElMessage.error('连接失败，请重试')
      }
    )
  } catch (error) {
    console.error('问诊失败:', error)
    ElMessage.error('问诊失败，请重试')
  } finally {
    isStreaming.value = false
    stopPipelineTimer()
  }
}

const handleSend = async (content: string) => {
  if (!currentSessionId.value) {
    await handleCreateSession()
  }
  if (currentSessionId.value) {
    await sendMessage(content)
  }
}

const handleContextUpdate = () => {}

watch(
  () => route.params.sessionId,
  () => {
    initSession()
  }
)

onMounted(() => {
  initSession()
})

onUnmounted(() => {
  stopPipelineTimer()
})
</script>

<template>
  <ConsultLayout>
    <template #toolbar>
      <ConsultStepBar :current-step="currentStep" />
      <div class="toolbar-actions">
        <el-switch
          v-model="demoMode"
          inline-prompt
          active-text="技术说明"
          inactive-text="技术说明"
          aria-label="切换技术说明展示"
        />
      </div>
    </template>

    <template #sidebar>
      <SessionSidebar
        :current-session-id="currentSessionId"
        @select="handleSessionSelect"
        @new="handleCreateSession"
      />
    </template>

    <template #main>
      <div class="consult-page">
        <ConsultPipelineStrip
          v-if="showPipeline"
          :streaming="isStreaming"
          :active-stage="pipelineStage"
          :result="consultResult"
          :show-tech-notes="demoMode"
        />

        <div v-if="showContextForm && !isFormCollapsed" class="context-form-section">
          <div class="form-header" @click="toggleContextForm">
            <h3 class="form-title">
              <span class="title-icon"><el-icon><User /></el-icon></span>
              填写患者信息
            </h3>
            <el-button
              type="primary"
              text
              :icon="isFormCollapsed ? ArrowDown : ArrowUp"
              class="collapse-btn"
            >
              {{ isFormCollapsed ? '展开' : '收起' }}
            </el-button>
          </div>

          <el-collapse-transition>
            <div v-show="!isFormCollapsed" class="form-content">
              <PatientContextForm
                ref="contextFormRef"
                :disabled="isStreaming"
                @update:context="handleContextUpdate"
              />
              <div class="form-actions">
                <el-button
                  type="primary"
                  size="large"
                  class="submit-btn"
                  :disabled="isStreaming"
                  @click="handleFormSubmit"
                >
                  开始问诊
                </el-button>
              </div>
            </div>
          </el-collapse-transition>
        </div>

        <div v-if="showContextForm && isFormCollapsed" class="form-collapsed-banner">
          <span class="banner-text">患者信息已填写</span>
          <el-button type="primary" text size="small" @click="toggleContextForm">
            编辑信息
          </el-button>
        </div>

        <div class="chat-section">
          <ChatPanel
            ref="chatPanelRef"
            :messages="messages"
            :loading="isStreaming"
            :streaming="isStreaming"
            :pipeline-stage="pipelineStage"
            @send="handleSend"
          />
        </div>

        <div v-if="consultResult && !isStreaming" class="result-section">
          <ConsultResultCard :result="consultResult" />
          <SmartInsightPanel
            :result="consultResult"
            @open-agent-trace="showAgentTrace = true"
          />
        </div>

        <AgentTraceDrawer
          v-model:visible="showAgentTrace"
          :traces="consultResult?.agentTrace"
        />
      </div>
    </template>
  </ConsultLayout>
</template>

<style scoped>
.consult-page {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  overflow: hidden;
  background: var(--consult-bg);
}

.toolbar-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  padding: 0 var(--consult-spacing-lg) var(--consult-spacing-sm);
  background: var(--consult-surface);
  border-bottom: 1px solid var(--consult-border-light);
}

.context-form-section {
  background: var(--consult-surface);
  border-bottom: 1px solid var(--consult-border);
  box-shadow: var(--consult-shadow-sm);
}

.form-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--consult-spacing-md) var(--consult-spacing-lg);
  cursor: pointer;
  user-select: none;
  transition: background var(--consult-transition);
}

.form-header:hover {
  background: var(--consult-bg);
}

.form-title {
  display: flex;
  align-items: center;
  gap: var(--consult-spacing-sm);
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: var(--consult-text-primary);
}

.title-icon {
  font-size: 20px;
  color: var(--consult-info);
}

.collapse-btn {
  color: var(--el-color-primary);
}

.form-content {
  padding: 0 var(--consult-spacing-lg) var(--consult-spacing-lg);
}

.form-actions {
  margin-top: var(--consult-spacing-lg);
  display: flex;
  justify-content: center;
}

.submit-btn {
  min-width: 200px;
  min-height: 44px;
  font-size: 16px;
}

.form-collapsed-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px var(--consult-spacing-lg);
  background: var(--consult-success-bg);
  border-bottom: 1px solid #d1fae5;
}

.banner-text {
  font-size: 14px;
  color: #065f46;
  font-weight: 500;
}

.chat-section {
  flex: 1 1 0;
  min-height: 0;
  overflow: hidden;
}

.chat-section > :deep(.chat-panel) {
  border-radius: 0;
  height: 100%;
}

.result-section {
  flex: 0 1 auto;
  min-height: 0;
  max-height: min(560px, 52vh);
  overflow-y: auto;
  overflow-x: hidden;
  padding: 0 var(--consult-spacing-lg) var(--consult-spacing-lg);
  max-width: 900px;
  margin: 0 auto;
  width: 100%;
  scrollbar-gutter: stable;
}

.result-section::-webkit-scrollbar {
  width: 8px;
}

.result-section::-webkit-scrollbar-thumb {
  background: #cbd5e1;
  border-radius: 4px;
}

@media (max-width: 768px) {
  .form-header {
    padding: 12px var(--consult-spacing-md);
  }

  .form-content {
    padding: 0 var(--consult-spacing-md) var(--consult-spacing-md);
  }

  .submit-btn {
    width: 100%;
  }

  .result-section {
    padding: 0 var(--consult-spacing-md) var(--consult-spacing-md);
  }
}
</style>
