<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { Menu } from '@element-plus/icons-vue'

const drawerVisible = ref(false)
const isMobile = ref(false)

const checkMobile = () => {
  isMobile.value = window.innerWidth < 768
}

const openDrawer = () => {
  if (isMobile.value) {
    drawerVisible.value = true
  }
}

onMounted(() => {
  checkMobile()
  window.addEventListener('resize', checkMobile)
})

onUnmounted(() => {
  window.removeEventListener('resize', checkMobile)
})
</script>

<template>
  <div class="consult-layout">
    <div class="consult-layout__container">
      <div class="consult-layout__sidebar">
        <slot name="sidebar">
          <div class="sidebar-placeholder">
            <p>会话列表</p>
          </div>
        </slot>
      </div>

      <div class="consult-layout__main">
        <header v-if="$slots.toolbar" class="consult-layout__toolbar">
          <slot name="toolbar" />
        </header>

        <div class="consult-layout__content">
          <slot name="main">
            <div class="main-placeholder">
              <p>聊天主区</p>
            </div>
          </slot>
        </div>

        <footer class="consult-layout__footer" role="contentinfo" aria-label="免责声明">
          <div class="footer-content">
            <svg class="footer-icon" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
              <circle cx="12" cy="12" r="10" />
              <line x1="12" y1="16" x2="12" y2="12" />
              <line x1="12" y1="8" x2="12.01" y2="8" />
            </svg>
            <span>AI 辅助诊断仅供参考，不作为医疗建议。如有不适请及时就医。</span>
          </div>
        </footer>
      </div>

      <button
        class="mobile-toggle"
        type="button"
        aria-label="打开会话列表"
        @click="openDrawer"
      >
        <el-icon :size="24"><Menu /></el-icon>
      </button>

      <el-drawer
        v-model="drawerVisible"
        direction="ltr"
        size="80%"
        title="会话列表"
        :modal="true"
        :show-close="true"
        append-to-body
        class="consult-drawer"
      >
        <slot name="sidebar" />
      </el-drawer>
    </div>
  </div>
</template>

<style scoped>
.consult-layout {
  width: 100%;
  height: 100vh;
  overflow: hidden;
  background: var(--consult-bg);
}

.consult-layout__container {
  display: flex;
  height: 100%;
  width: 100%;
}

.consult-layout__sidebar {
  width: 30%;
  min-width: 280px;
  max-width: 400px;
  height: 100%;
  background: var(--consult-surface);
  border-right: 1px solid var(--consult-border);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.consult-layout__main {
  flex: 1;
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
  min-width: 0;
}

.consult-layout__toolbar {
  flex-shrink: 0;
  background: var(--consult-surface);
  border-bottom: 1px solid var(--consult-border);
}

.consult-layout__content {
  flex: 1;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.consult-layout__footer {
  width: 100%;
  padding: 12px 16px;
  background: var(--consult-surface);
  border-top: 1px solid var(--consult-border);
  flex-shrink: 0;
}

.footer-content {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  font-size: 12px;
  color: var(--consult-text-muted);
  text-align: center;
}

.footer-icon {
  flex-shrink: 0;
  color: var(--consult-info);
}

.mobile-toggle {
  display: none;
  position: fixed;
  bottom: calc(80px + env(safe-area-inset-bottom, 0px));
  left: 20px;
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: var(--el-color-primary);
  color: white;
  border: none;
  box-shadow: 0 4px 12px rgba(13, 148, 136, 0.3);
  cursor: pointer;
  z-index: 1000;
  transition: transform var(--consult-transition), box-shadow var(--consult-transition);
  align-items: center;
  justify-content: center;
}

.mobile-toggle:hover {
  transform: scale(1.05);
  box-shadow: 0 6px 16px rgba(13, 148, 136, 0.4);
}

.sidebar-placeholder,
.main-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: var(--consult-text-muted);
  font-size: 16px;
}

@media (max-width: 767px) {
  .consult-layout__container {
    display: block;
  }

  .consult-layout__sidebar {
    display: none;
  }

  .consult-layout__main {
    width: 100%;
  }

  .mobile-toggle {
    display: flex;
  }

  .consult-layout__footer {
    padding: 10px 12px;
  }

  .footer-content {
    font-size: 11px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .mobile-toggle {
    transition: none;
  }

  .mobile-toggle:hover {
    transform: none;
  }
}

:deep(.consult-drawer) {
  .el-drawer__header {
    background: var(--el-color-primary);
    color: white;
    margin-bottom: 0;
    padding: 16px 20px;
  }

  .el-drawer__header .el-drawer__title {
    color: white;
  }

  .el-drawer__header .el-drawer__close-btn {
    color: white;
  }

  .el-drawer__body {
    padding: 0;
    overflow: hidden;
  }
}
</style>
