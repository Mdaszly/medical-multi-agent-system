<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Edit, Plus, ArrowRight } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import {
  getHealthProfile,
  createHealthProfile,
  updateHealthProfile,
} from '@/services/medical/jiankangdanganguanli'
import HealthProfileMetrics from '@/components/patient/health-profile/HealthProfileMetrics.vue'
import HealthProfileFormDialog from '@/components/patient/health-profile/HealthProfileFormDialog.vue'
import {
  HEALTH_NAV_SECTIONS,
  FIELD_LABELS,
  buildHealthSummary,
  isFieldVisible,
  type HealthProfileField,
} from '@/constants/healthProfile'

const route = useRoute()
const authStore = useAuthStore()

const loading = ref(false)
const saving = ref(false)
const profile = ref<API.HealthProfileVO | null>(null)
const activeSection = ref('vitals')
const formVisible = ref(false)
const formMode = ref<'create' | 'edit'>('create')

const currentRole = computed(() => authStore.userInfo?.userRole || 'user')
const targetUserId = computed(() => {
  const param = route.params.userId
  if (param) return Number(param)
  return authStore.userInfo?.id
})
const canEdit = computed(
  () => currentRole.value === 'user' || currentRole.value === 'admin',
)

const displayName = computed(
  () => profile.value?.userName || authStore.userInfo?.userName || '用户',
)

const summaryLines = computed(() => buildHealthSummary(profile.value))

const visibleSections = computed(() =>
  HEALTH_NAV_SECTIONS.filter((section) =>
    section.fields.some((field) => isFieldVisible(currentRole.value, field)),
  ),
)

const activeSectionMeta = computed(
  () => visibleSections.value.find((s) => s.id === activeSection.value) || visibleSections.value[0],
)

const getFieldValue = (field: HealthProfileField): string => {
  const value = profile.value?.[field]
  if (value == null || value === '') return ''
  return String(value)
}

const hasFieldContent = (field: HealthProfileField): boolean => {
  return Boolean(getFieldValue(field))
}

const sectionHasContent = (sectionId: string): boolean => {
  const section = HEALTH_NAV_SECTIONS.find((s) => s.id === sectionId)
  if (!section) return false
  return section.fields.some(
    (field) => isFieldVisible(currentRole.value, field) && hasFieldContent(field),
  )
}

const loadProfile = async () => {
  const userId = targetUserId.value
  if (!userId) {
    ElMessage.warning('无法获取用户信息，请重新登录')
    return
  }

  loading.value = true
  try {
    const res = await getHealthProfile({ userId })
    profile.value = res.data ?? null
  } catch {
    profile.value = null
  } finally {
    loading.value = false
  }
}

const openCreate = () => {
  formMode.value = 'create'
  formVisible.value = true
}

const openEdit = () => {
  formMode.value = 'edit'
  formVisible.value = true
}

const handleFormSubmit = async (payload: API.HealthProfile) => {
  saving.value = true
  try {
    if (formMode.value === 'create') {
      const res = await createHealthProfile(payload)
      profile.value = res.data ?? null
      ElMessage.success('健康档案已创建')
    } else {
      const res = await updateHealthProfile({
        ...payload,
        id: profile.value?.id,
      })
      profile.value = res.data ?? null
      ElMessage.success('健康档案已更新')
    }
    formVisible.value = false
  } catch {
    // request 层已提示
  } finally {
    saving.value = false
  }
}

const selectSection = (id: string) => {
  activeSection.value = id
  const el = document.getElementById(`health-section-${id}`)
  el?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

onMounted(() => {
  const first = visibleSections.value[0]
  if (first) {
    activeSection.value = first.id
  }
  loadProfile()
})
</script>

<template>
  <div class="health-dashboard" v-loading="loading">
    <header class="profile-hero">
      <div class="hero-main">
        <el-avatar :size="64" class="hero-avatar" aria-hidden="true">
          {{ displayName.charAt(0) }}
        </el-avatar>
        <div class="hero-text">
          <h1 class="hero-name">{{ displayName }}</h1>
          <p class="hero-meta">
            <span v-if="profile?.updateTime">最近更新：{{ profile.updateTime }}</span>
            <span v-else>尚未建立健康档案</span>
          </p>
        </div>
      </div>
      <div class="hero-actions">
        <el-button
          v-if="canEdit && !profile"
          type="primary"
          :icon="Plus"
          @click="openCreate"
        >
          创建档案
        </el-button>
        <el-button
          v-else-if="canEdit && profile"
          type="primary"
          :icon="Edit"
          @click="openEdit"
        >
          编辑档案
        </el-button>
      </div>
    </header>

    <template v-if="profile">
      <div class="dashboard-grid">
        <aside class="nav-panel" aria-label="健康档案分类">
          <h2 class="panel-title">健康档案</h2>
          <ul class="nav-list">
            <li
              v-for="section in visibleSections"
              :key="section.id"
              class="nav-item"
              :class="{ active: activeSection === section.id }"
            >
              <button
                type="button"
                class="nav-button"
                :aria-current="activeSection === section.id ? 'true' : undefined"
                @click="selectSection(section.id)"
              >
                <el-icon class="nav-icon" aria-hidden="true">
                  <component :is="section.icon" />
                </el-icon>
                <span class="nav-text">
                  <span class="nav-title">{{ section.title }}</span>
                  <span class="nav-sub">{{ section.subtitle }}</span>
                </span>
                <span v-if="sectionHasContent(section.id)" class="nav-dot" aria-hidden="true" />
                <el-icon class="nav-chevron" aria-hidden="true"><ArrowRight /></el-icon>
              </button>
            </li>
          </ul>
        </aside>

        <main class="content-panel">
          <HealthProfileMetrics
            v-if="
              isFieldVisible(currentRole, 'height') ||
              isFieldVisible(currentRole, 'weight') ||
              isFieldVisible(currentRole, 'bloodType') ||
              isFieldVisible(currentRole, 'bloodPressure')
            "
            class="metrics-block"
            :height="profile.height"
            :weight="profile.weight"
            :blood-type="profile.bloodType"
            :blood-pressure="profile.bloodPressure"
          />

          <section class="summary-card" aria-label="健康摘要">
            <h3 class="summary-title">健康摘要</h3>
            <ul v-if="summaryLines.length" class="summary-list">
              <li v-for="(line, idx) in summaryLines" :key="idx">{{ line }}</li>
            </ul>
            <p v-else class="summary-empty">完善档案后，将在此生成简要健康摘要。</p>
          </section>

          <section
            v-for="section in visibleSections"
            :id="`health-section-${section.id}`"
            :key="section.id"
            class="detail-section"
          >
            <header class="detail-header">
              <el-icon aria-hidden="true"><component :is="section.icon" /></el-icon>
              <h3>{{ section.title }}</h3>
            </header>
            <div class="detail-body">
              <template v-for="field in section.fields" :key="field">
                <div
                  v-if="isFieldVisible(currentRole, field)"
                  class="field-block"
                >
                  <span class="field-label">{{ FIELD_LABELS[field] }}</span>
                  <p v-if="hasFieldContent(field)" class="field-value">
                    {{ getFieldValue(field) }}
                  </p>
                  <p v-else class="field-empty">暂无记录</p>
                </div>
              </template>
            </div>
          </section>
        </main>
      </div>
    </template>

    <el-empty v-else class="empty-block" description="暂无健康档案">
      <el-button v-if="canEdit" type="primary" :icon="Plus" @click="openCreate">
        创建我的健康档案
      </el-button>
    </el-empty>

    <HealthProfileFormDialog
      v-if="targetUserId && canEdit"
      v-model:visible="formVisible"
      :mode="formMode"
      :initial="profile"
      :user-id="targetUserId"
      :user-name="displayName"
      :role="currentRole"
      :saving="saving"
      @submit="handleFormSubmit"
    />
  </div>
</template>

<style scoped>
.health-dashboard {
  max-width: 1200px;
  margin: 0 auto;
}

.profile-hero {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 24px;
  margin-bottom: 24px;
  border-radius: 16px;
  background: linear-gradient(135deg, #0d9488 0%, #14b8a6 50%, #0891b2 100%);
  color: #fff;
  box-shadow: 0 8px 24px rgba(13, 148, 136, 0.2);
}

.hero-main {
  display: flex;
  align-items: center;
  gap: 16px;
}

.hero-avatar {
  background: rgba(255, 255, 255, 0.25);
  color: #fff;
  font-size: 24px;
  font-weight: 600;
}

.hero-name {
  margin: 0 0 4px;
  font-size: 24px;
  font-weight: 700;
}

.hero-meta {
  margin: 0;
  font-size: 14px;
  opacity: 0.9;
}

.dashboard-grid {
  display: grid;
  grid-template-columns: 280px 1fr;
  gap: 24px;
  align-items: start;
}

.nav-panel {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  padding: 16px;
  position: sticky;
  top: 16px;
}

.panel-title {
  margin: 0 0 12px;
  font-size: 15px;
  font-weight: 600;
  color: #134e4a;
}

.nav-list {
  list-style: none;
  margin: 0;
  padding: 0;
}

.nav-item {
  margin-bottom: 4px;
}

.nav-button {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  min-height: 44px;
  padding: 10px 12px;
  border: none;
  border-radius: 8px;
  background: transparent;
  cursor: pointer;
  text-align: left;
  transition: background 0.2s ease;
}

.nav-button:hover {
  background: #f0fdfa;
}

.nav-item.active .nav-button {
  background: #ccfbf1;
}

.nav-icon {
  color: #0d9488;
  font-size: 18px;
}

.nav-text {
  flex: 1;
  min-width: 0;
}

.nav-title {
  display: block;
  font-size: 14px;
  font-weight: 600;
  color: #1f2937;
}

.nav-sub {
  display: block;
  font-size: 12px;
  color: #6b7280;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.nav-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #10b981;
  flex-shrink: 0;
}

.nav-chevron {
  color: #9ca3af;
  font-size: 14px;
}

.content-panel {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.summary-card {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  padding: 20px;
}

.summary-title {
  margin: 0 0 12px;
  font-size: 16px;
  font-weight: 600;
  color: #134e4a;
}

.summary-list {
  margin: 0;
  padding-left: 20px;
  color: #374151;
  line-height: 1.75;
}

.summary-empty {
  margin: 0;
  color: #6b7280;
  font-size: 14px;
}

.detail-section {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  padding: 20px;
  scroll-margin-top: 16px;
}

.detail-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
  color: #0d9488;
}

.detail-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #1f2937;
}

.field-block {
  margin-bottom: 12px;
}

.field-block:last-child {
  margin-bottom: 0;
}

.field-label {
  display: block;
  font-size: 12px;
  color: #6b7280;
  margin-bottom: 4px;
}

.field-value {
  margin: 0;
  font-size: 14px;
  color: #374151;
  line-height: 1.6;
  white-space: pre-wrap;
}

.field-empty {
  margin: 0;
  font-size: 14px;
  color: #9ca3af;
}

.empty-block {
  padding: 48px 0;
}

@media (max-width: 900px) {
  .dashboard-grid {
    grid-template-columns: 1fr;
  }

  .nav-panel {
    position: static;
  }

  .nav-list {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
    gap: 8px;
  }

  .nav-item {
    margin-bottom: 0;
  }
}

@media (prefers-reduced-motion: reduce) {
  .nav-button {
    transition: none;
  }
}
</style>
