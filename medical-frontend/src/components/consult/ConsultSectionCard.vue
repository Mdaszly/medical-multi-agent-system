<script setup lang="ts">
import type { Component } from 'vue'
import { ArrowDown } from '@element-plus/icons-vue'

interface Props {
  title: string
  icon?: Component
  collapsible?: boolean
  defaultExpanded?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  collapsible: false,
  defaultExpanded: true,
})

const expanded = defineModel<boolean>('expanded', { default: true })

if (!props.collapsible) {
  expanded.value = true
}
</script>

<template>
  <section class="consult-section-card">
    <header
      class="section-header"
      :class="{ 'is-clickable': collapsible }"
      @click="collapsible && (expanded = !expanded)"
    >
      <div class="header-left">
        <el-icon v-if="icon" class="header-icon" aria-hidden="true">
          <component :is="icon" />
        </el-icon>
        <h3 class="header-title">{{ title }}</h3>
      </div>
      <div class="header-actions">
        <slot name="actions" />
        <el-icon
          v-if="collapsible"
          class="collapse-icon"
          :class="{ 'is-collapsed': !expanded }"
          aria-hidden="true"
        >
          <ArrowDown />
        </el-icon>
      </div>
    </header>
    <el-collapse-transition>
      <div v-show="expanded" class="section-body">
        <slot />
      </div>
    </el-collapse-transition>
  </section>
</template>

<style scoped>
.consult-section-card {
  background: var(--consult-surface);
  border: 1px solid var(--consult-border);
  border-radius: var(--consult-radius-lg);
  box-shadow: var(--consult-shadow-sm);
  overflow: hidden;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--consult-spacing-md) var(--consult-spacing-lg);
  border-bottom: 1px solid var(--consult-border-light);
  background: var(--consult-surface);
}

.section-header.is-clickable {
  cursor: pointer;
  transition: background var(--consult-transition);
}

.section-header.is-clickable:hover {
  background: var(--consult-bg);
}

.header-left {
  display: flex;
  align-items: center;
  gap: var(--consult-spacing-sm);
}

.header-icon {
  color: var(--consult-info);
  font-size: 20px;
}

.header-title {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: var(--consult-text-primary);
}

.header-actions {
  display: flex;
  align-items: center;
  gap: var(--consult-spacing-sm);
}

.collapse-icon {
  transition: transform var(--consult-transition);
  color: var(--consult-text-muted);
}

.collapse-icon.is-collapsed {
  transform: rotate(-90deg);
}

.section-body {
  padding: var(--consult-spacing-lg);
}

:deep(.consult-section-card .section-body) {
  padding: var(--consult-spacing-lg);
}
</style>
