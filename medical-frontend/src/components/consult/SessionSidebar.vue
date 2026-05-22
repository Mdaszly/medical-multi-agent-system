<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { Plus, Delete, ChatDotRound, Message } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { queryHistory, createSession, deleteSession } from '@/services/medical/consult'
import type { ChatSessionHistoryVO, ChatSessionVO } from '@/services/medical/types'

interface Props {
  currentSessionId?: string
}

const props = defineProps<Props>()

const emit = defineEmits<{
  select: [sessionId: string]
  new: []
}>()

const loading = ref(false)
const sessionsData = ref<ChatSessionHistoryVO>({
  today: [],
  last30Days: [],
  lastYear: [],
  olderThanYear: []
})

const hasSessions = computed(() => {
  return (
    (sessionsData.value.today?.length ?? 0) > 0 ||
    (sessionsData.value.last30Days?.length ?? 0) > 0 ||
    (sessionsData.value.lastYear?.length ?? 0) > 0 ||
    (sessionsData.value.olderThanYear?.length ?? 0) > 0
  )
})

const fetchSessions = async () => {
  loading.value = true
  try {
    console.log('正在获取会话历史...')
    const res = await queryHistory()
    console.log('会话历史API响应:', res)
    if (res.code === 0 && res.data) {
      sessionsData.value = res.data
      console.log('解析后的会话数据:', sessionsData.value)
    } else {
      console.log('API返回了非成功响应或没有数据')
    }
  } catch (error) {
    console.error('获取会话历史失败:', error)
    ElMessage.error('获取会话历史失败')
  } finally {
    loading.value = false
  }
}

const handleCreateSession = async () => {
  try {
    const res = await createSession({})
    if (res.code === 0 && res.data?.sessionId) {
      ElMessage.success('创建成功')
      await fetchSessions()
      emit('new')
      emit('select', res.data.sessionId)
    }
  } catch (error) {
    console.error('创建会话失败:', error)
    ElMessage.error('创建会话失败')
  }
}

const handleDeleteSession = async (sessionId: string) => {
  try {
    const res = await deleteSession({ sessionId })
    if (res.code === 0) {
      ElMessage.success('删除成功')
      await fetchSessions()
      if (props.currentSessionId === sessionId) {
        emit('select', '')
      }
    }
  } catch (error) {
    console.error('删除会话失败:', error)
    ElMessage.error('删除会话失败')
  }
}

const handleSelectSession = (sessionId: string) => {
  emit('select', sessionId)
}

const formatTime = (timeStr?: string) => {
  if (!timeStr) return ''
  const date = new Date(timeStr)
  const now = new Date()
  const diffMs = now.getTime() - date.getTime()
  const diffMins = Math.floor(diffMs / 60000)
  if (diffMins < 1) return '刚刚'
  if (diffMins < 60) return `${diffMins} 分钟前`
  const diffHours = Math.floor(diffMins / 60)
  if (diffHours < 24) return `${diffHours} 小时前`
  const diffDays = Math.floor(diffHours / 24)
  if (diffDays < 7) return `${diffDays} 天前`
  const hours = date.getHours().toString().padStart(2, '0')
  const minutes = date.getMinutes().toString().padStart(2, '0')
  return `${hours}:${minutes}`
}

const sessionPreview = (session: ChatSessionVO) => {
  if (session.scene?.trim()) return session.scene.trim()
  const title = session.title?.trim()
  if (title && title !== '新会话' && title.length > 20) {
    return `${title.slice(0, 20)}…`
  }
  return '线上问诊 · 含智能化分析'
}

onMounted(() => {
  fetchSessions()
})

defineExpose({
  refresh: fetchSessions
})
</script>

<template>
  <div class="session-sidebar" role="navigation" aria-label="问诊会话列表">
    <div class="sidebar-header">
      <h3 class="sidebar-title">
        <el-icon><ChatDotRound /></el-icon>
        问诊记录
      </h3>
      <el-button
        type="primary"
        :icon="Plus"
        size="small"
        class="new-btn"
        @click="handleCreateSession"
        aria-label="新建问诊会话"
      >
        新建
      </el-button>
    </div>

    <div class="session-list" v-loading="loading">
      <template v-if="hasSessions">
        <div v-if="sessionsData.today?.length" class="session-group">
          <div class="group-title">今天</div>
          <div
            v-for="session in sessionsData.today"
            :key="session.sessionId"
            class="session-item"
            :class="{ active: session.sessionId === currentSessionId }"
            role="button"
            tabindex="0"
            :aria-label="`会话: ${session.title || '新会话'}`"
            @click="handleSelectSession(session.sessionId!)"
            @keydown.enter="handleSelectSession(session.sessionId!)"
          >
            <div class="session-content">
              <el-icon class="session-icon"><Message /></el-icon>
              <div class="session-info">
                <div class="session-title">{{ session.title || '新会话' }}</div>
                <div class="session-preview">{{ sessionPreview(session) }}</div>
                <div class="session-time">{{ formatTime(session.updateTime || session.createTime) }}</div>
              </div>
            </div>
            <div class="session-actions">
              <el-popconfirm
                title="确定删除此会话？"
                confirm-button-text="删除"
                cancel-button-text="取消"
                :icon="Delete"
                @confirm="handleDeleteSession(session.sessionId!)"
              >
                <template #reference>
                  <el-button
                    type="danger"
                    :icon="Delete"
                    size="small"
                    circle
                    class="delete-btn"
                    @click.stop
                    aria-label="删除会话"
                  />
                </template>
              </el-popconfirm>
            </div>
          </div>
        </div>

        <div v-if="sessionsData.last30Days?.length" class="session-group">
          <div class="group-title">30天内</div>
          <div
            v-for="session in sessionsData.last30Days"
            :key="session.sessionId"
            class="session-item"
            :class="{ active: session.sessionId === currentSessionId }"
            role="button"
            tabindex="0"
            :aria-label="`会话: ${session.title || '新会话'}`"
            @click="handleSelectSession(session.sessionId!)"
            @keydown.enter="handleSelectSession(session.sessionId!)"
          >
            <div class="session-content">
              <el-icon class="session-icon"><Message /></el-icon>
              <div class="session-info">
                <div class="session-title">{{ session.title || '新会话' }}</div>
                <div class="session-preview">{{ sessionPreview(session) }}</div>
                <div class="session-time">{{ formatTime(session.updateTime || session.createTime) }}</div>
              </div>
            </div>
            <div class="session-actions">
              <el-popconfirm
                title="确定删除此会话？"
                confirm-button-text="删除"
                cancel-button-text="取消"
                :icon="Delete"
                @confirm="handleDeleteSession(session.sessionId!)"
              >
                <template #reference>
                  <el-button
                    type="danger"
                    :icon="Delete"
                    size="small"
                    circle
                    class="delete-btn"
                    @click.stop
                    aria-label="删除会话"
                  />
                </template>
              </el-popconfirm>
            </div>
          </div>
        </div>

        <div v-if="sessionsData.lastYear?.length" class="session-group">
          <div class="group-title">1年内</div>
          <div
            v-for="session in sessionsData.lastYear"
            :key="session.sessionId"
            class="session-item"
            :class="{ active: session.sessionId === currentSessionId }"
            role="button"
            tabindex="0"
            :aria-label="`会话: ${session.title || '新会话'}`"
            @click="handleSelectSession(session.sessionId!)"
            @keydown.enter="handleSelectSession(session.sessionId!)"
          >
            <div class="session-content">
              <el-icon class="session-icon"><Message /></el-icon>
              <div class="session-info">
                <div class="session-title">{{ session.title || '新会话' }}</div>
                <div class="session-preview">{{ sessionPreview(session) }}</div>
                <div class="session-time">{{ formatTime(session.updateTime || session.createTime) }}</div>
              </div>
            </div>
            <div class="session-actions">
              <el-popconfirm
                title="确定删除此会话？"
                confirm-button-text="删除"
                cancel-button-text="取消"
                :icon="Delete"
                @confirm="handleDeleteSession(session.sessionId!)"
              >
                <template #reference>
                  <el-button
                    type="danger"
                    :icon="Delete"
                    size="small"
                    circle
                    class="delete-btn"
                    @click.stop
                    aria-label="删除会话"
                  />
                </template>
              </el-popconfirm>
            </div>
          </div>
        </div>

        <div v-if="sessionsData.olderThanYear?.length" class="session-group">
          <div class="group-title">更早</div>
          <div
            v-for="session in sessionsData.olderThanYear"
            :key="session.sessionId"
            class="session-item"
            :class="{ active: session.sessionId === currentSessionId }"
            role="button"
            tabindex="0"
            :aria-label="`会话: ${session.title || '新会话'}`"
            @click="handleSelectSession(session.sessionId!)"
            @keydown.enter="handleSelectSession(session.sessionId!)"
          >
            <div class="session-content">
              <el-icon class="session-icon"><Message /></el-icon>
              <div class="session-info">
                <div class="session-title">{{ session.title || '新会话' }}</div>
                <div class="session-preview">{{ sessionPreview(session) }}</div>
                <div class="session-time">{{ formatTime(session.updateTime || session.createTime) }}</div>
              </div>
            </div>
            <div class="session-actions">
              <el-popconfirm
                title="确定删除此会话？"
                confirm-button-text="删除"
                cancel-button-text="取消"
                :icon="Delete"
                @confirm="handleDeleteSession(session.sessionId!)"
              >
                <template #reference>
                  <el-button
                    type="danger"
                    :icon="Delete"
                    size="small"
                    circle
                    class="delete-btn"
                    @click.stop
                    aria-label="删除会话"
                  />
                </template>
              </el-popconfirm>
            </div>
          </div>
        </div>
      </template>

      <el-empty
        v-else
        description="暂无问诊记录"
        :image-size="80"
        class="empty-state"
      >
        <el-button type="primary" :icon="Plus" @click="handleCreateSession">
          开始问诊
        </el-button>
      </el-empty>
    </div>
  </div>
</template>

<style scoped>
.session-sidebar {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: var(--consult-surface);
  border-right: 1px solid var(--consult-border);
}

.sidebar-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  border-bottom: 1px solid #f3f4f6;
}

.sidebar-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #1f2937;
}

.sidebar-title .el-icon {
  color: #0d9488;
}

.new-btn {
  background: linear-gradient(135deg, #0d9488 0%, #10b981 100%);
  border: none;
}

.new-btn:hover {
  background: linear-gradient(135deg, #0f766e 0%, #059669 100%);
}

.session-list {
  flex: 1;
  overflow-y: auto;
  padding: 12px;
}

.session-group {
  margin-bottom: 20px;
}

.group-title {
  font-size: 12px;
  font-weight: 600;
  color: #6b7280;
  padding: 8px 12px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.session-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px;
  margin-bottom: 8px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
  background: #f9fafb;
}

.session-item:hover {
  background: #f3f4f6;
}

.session-item:focus {
  outline: 2px solid #0d9488;
  outline-offset: 2px;
}

.session-item.active {
  background: var(--consult-success-bg);
  border-left: 4px solid var(--el-color-primary);
  padding-left: 8px;
}

.session-content {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
  min-width: 0;
}

.session-icon {
  font-size: 20px;
  color: #0d9488;
  flex-shrink: 0;
}

.session-info {
  flex: 1;
  min-width: 0;
}

.session-title {
  font-size: 14px;
  font-weight: 500;
  color: #1f2937;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.session-item.active .session-title {
  color: #0d9488;
  font-weight: 600;
}

.session-preview {
  font-size: 12px;
  color: var(--consult-text-muted);
  margin-top: 2px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.session-time {
  font-size: 11px;
  color: var(--consult-text-muted);
  margin-top: 2px;
}

.session-actions {
  opacity: 0;
  transition: opacity 0.2s ease;
  flex-shrink: 0;
  margin-left: 8px;
}

.session-item:hover .session-actions {
  opacity: 1;
}

.delete-btn {
  background: transparent;
  border: 1px solid #fecaca;
  color: #ef4444;
}

.delete-btn:hover {
  background: #fef2f2;
  border-color: #fca5a5;
  color: #dc2626;
}

.empty-state {
  margin-top: 60px;
}

.empty-state :deep(.el-empty__description) {
  color: #9ca3af;
  font-size: 14px;
}

@media (max-width: 768px) {
  .session-sidebar {
    border-right: none;
    border-bottom: 1px solid #e5e7eb;
  }

  .sidebar-header {
    padding: 12px;
  }

  .session-list {
    max-height: 300px;
  }

  .session-item {
    padding: 10px;
  }

  .session-actions {
    opacity: 1;
  }
}
</style>
