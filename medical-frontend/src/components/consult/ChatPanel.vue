<script setup lang="ts">
import { ref, nextTick, watch, computed } from 'vue'
import { ChatLineRound, Promotion, Edit, Connection, Search, Cpu } from '@element-plus/icons-vue'
import type { PipelineStage } from './ConsultPipelineStrip.vue'

export interface ChatMessageVO {
  id: string | number
  role: 'user' | 'assistant' | 'system'
  content: string
  createdAt?: string | Date
}

interface Props {
  messages?: ChatMessageVO[]
  loading?: boolean
  streaming?: boolean
  pipelineStage?: PipelineStage
}

const props = withDefaults(defineProps<Props>(), {
  messages: () => [],
  loading: false,
  streaming: false,
  pipelineStage: 'input',
})

const emit = defineEmits<{
  send: [content: string]
  loadMore: []
}>()

const messageListRef = ref<HTMLElement | null>(null)
const inputText = ref('')
const streamingContent = ref('')
const streamingMessageId = ref<string | number | null>(null)

const canSend = computed(() => {
  return inputText.value.trim().length > 0 && !props.loading && !props.streaming
})

const pipelineHint = computed(() => {
  if (!props.streaming) return ''
  const map: Record<PipelineStage, string> = {
    input: '正在理解您的问题…',
    terms: '正在进行术语标准化…',
    kg: '正在查询知识图谱…',
    agents: '多 Agent 协作分析中…',
    suggest: '正在生成问诊建议…',
  }
  return map[props.pipelineStage] || map.agents
})

const pipelineIcon = computed(() => {
  const map: Record<PipelineStage, typeof Edit> = {
    input: Edit,
    terms: Connection,
    kg: Search,
    agents: Cpu,
    suggest: ChatLineRound,
  }
  return map[props.pipelineStage] || Cpu
})

const handleKeyDown = (event: KeyboardEvent) => {
  if (event.key === 'Enter' && !event.shiftKey) {
    event.preventDefault()
    handleSend()
  }
}

const handleSend = () => {
  const content = inputText.value.trim()
  if (!content || props.loading || props.streaming) return
  emit('send', content)
  inputText.value = ''
}

const scrollToBottom = async () => {
  await nextTick()
  if (messageListRef.value) {
    messageListRef.value.scrollTop = messageListRef.value.scrollHeight
  }
}

const handleScroll = (event: Event) => {
  const target = event.target as HTMLElement
  if (target.scrollTop === 0 && !props.loading && props.messages.length > 0) {
    emit('loadMore')
  }
}

watch(() => props.messages.length, () => {
  scrollToBottom()
})

watch(() => props.streaming, (newVal) => {
  if (newVal) {
    const lastMessage = props.messages[props.messages.length - 1]
    if (lastMessage && lastMessage.role === 'assistant') {
      streamingMessageId.value = lastMessage.id
      streamingContent.value = lastMessage.content
    }
  } else {
    streamingContent.value = ''
    streamingMessageId.value = null
  }
})

defineExpose({
  scrollToBottom,
})
</script>

<template>
  <div class="chat-panel" role="region" aria-label="问诊聊天面板">
    <div v-if="streaming" class="streaming-bar" role="status" aria-live="polite">
      <el-progress :percentage="100" :indeterminate="true" :show-text="false" :stroke-width="3" />
      <div class="streaming-hint">
        <el-icon class="hint-icon is-loading" aria-hidden="true">
          <component :is="pipelineIcon" />
        </el-icon>
        <span>{{ pipelineHint }}</span>
      </div>
    </div>

    <div ref="messageListRef" class="message-list" @scroll="handleScroll">
      <div v-if="loading && messages.length === 0" class="loading-container">
        <el-skeleton :rows="5" animated />
      </div>

      <div v-else-if="messages.length === 0" class="empty-state">
        <el-icon :size="64" class="empty-icon" aria-hidden="true">
          <ChatLineRound />
        </el-icon>
        <h3 class="empty-title">开始问诊</h3>
        <ol class="empty-steps">
          <li>描述您的症状与健康问题</li>
          <li>AI 结合知识图谱与多 Agent 进行分析</li>
          <li>查看问诊建议与智能化分析依据</li>
        </ol>
      </div>

      <div v-else class="messages-container">
        <div
          v-for="message in messages"
          :key="message.id"
          class="message-wrapper"
          :class="[`message-${message.role}`]"
        >
          <div class="message-bubble">
            <div
              v-if="streaming && message.id === streamingMessageId"
              class="streaming-content"
            >
              {{ message.content || streamingContent }}
              <span class="cursor-blink" aria-hidden="true">|</span>
            </div>
            <div v-else class="message-text">{{ message.content }}</div>
          </div>
          <div v-if="message.createdAt" class="message-time">
            {{ new Date(message.createdAt).toLocaleTimeString() }}
          </div>
        </div>
      </div>
    </div>

    <div class="input-area">
      <div class="input-container">
        <el-input
          v-model="inputText"
          type="textarea"
          :rows="3"
          resize="none"
          placeholder="请输入您的症状或问题（Enter 发送，Shift+Enter 换行）"
          :disabled="loading || streaming"
          @keydown="handleKeyDown"
          aria-label="输入问诊内容"
        />
        <el-button
          type="primary"
          :loading="streaming"
          :disabled="!canSend"
          class="send-button"
          aria-label="发送消息"
          @click="handleSend"
        >
          <el-icon v-if="!streaming" aria-hidden="true"><Promotion /></el-icon>
          <span v-if="!streaming">发送</span>
          <span v-else>生成中...</span>
        </el-button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.chat-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: var(--consult-surface);
  border-radius: var(--consult-radius-lg);
  overflow: hidden;
}

.streaming-bar {
  flex-shrink: 0;
  background: var(--consult-info-bg);
  border-bottom: 1px solid var(--consult-border);
}

.streaming-hint {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 20px;
  font-size: 13px;
  color: var(--consult-info);
  font-weight: 500;
}

.hint-icon {
  font-size: 16px;
}

.message-list {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  background: var(--consult-bg);
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  text-align: center;
  padding: 40px 20px;
}

.empty-icon {
  color: var(--consult-info);
  margin-bottom: 16px;
}

.empty-title {
  margin: 0 0 16px;
  font-size: 22px;
  font-weight: 600;
  color: var(--consult-text-primary);
}

.empty-steps {
  margin: 0;
  padding: 0;
  list-style: none;
  text-align: left;
  max-width: 320px;
}

.empty-steps li {
  position: relative;
  padding: 10px 0 10px 28px;
  font-size: 15px;
  color: var(--consult-text-secondary);
  line-height: 1.5;
}

.empty-steps li::before {
  content: counter(step);
  counter-increment: step;
  position: absolute;
  left: 0;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: var(--consult-info-bg);
  color: var(--consult-info);
  font-size: 12px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
}

.empty-steps {
  counter-reset: step;
}

.messages-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.message-wrapper {
  display: flex;
  flex-direction: column;
  max-width: 70%;
}

.message-user {
  align-self: flex-end;
  align-items: flex-end;
}

.message-assistant {
  align-self: flex-start;
  align-items: flex-start;
}

.message-bubble {
  padding: 12px 16px;
  border-radius: 12px;
  word-wrap: break-word;
  line-height: 1.6;
  font-size: 16px;
}

.message-user .message-bubble {
  background: var(--el-color-primary);
  color: #fff;
  border-bottom-right-radius: 4px;
}

.message-assistant .message-bubble {
  background: var(--consult-surface-muted);
  color: var(--consult-text-primary);
  border-bottom-left-radius: 4px;
}

.message-text,
.streaming-content {
  white-space: pre-wrap;
}

.cursor-blink {
  animation: blink 1s infinite;
  color: var(--consult-info);
}

@keyframes blink {
  0%, 50% { opacity: 1; }
  51%, 100% { opacity: 0; }
}

.message-time {
  margin-top: 4px;
  font-size: 12px;
  color: var(--consult-text-muted);
}

.input-area {
  padding: 16px 20px;
  background: var(--consult-surface);
  border-top: 1px solid var(--consult-border);
}

.input-container {
  display: flex;
  gap: 12px;
  align-items: flex-end;
}

.input-container :deep(.el-textarea__inner) {
  border-radius: 12px;
  font-size: 16px;
  line-height: 1.6;
}

.send-button {
  min-height: 44px;
  min-width: 88px;
  border-radius: 12px;
}

@media (prefers-reduced-motion: reduce) {
  .cursor-blink {
    animation: none;
  }
}
</style>
