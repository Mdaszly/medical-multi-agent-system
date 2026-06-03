<script setup lang="ts">
import { computed } from "vue";
import type { UserNotificationVO } from "@/services/medical/tongzhiguanli";

const props = defineProps<{
  item: UserNotificationVO;
  compact?: boolean;
}>();

const emit = defineEmits<{
  click: [item: UserNotificationVO];
}>();

const isUnread = computed(() => props.item.readStatus === 0);

function formatTime(value?: string) {
  if (!value) return "";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return date.toLocaleString("zh-CN", {
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  });
}
</script>

<template>
  <div
    class="notification-item"
    :class="{ unread: isUnread, compact }"
    role="button"
    tabindex="0"
    @click="emit('click', item)"
    @keydown.enter="emit('click', item)"
  >
    <div class="item-body">
      <div class="item-title">{{ item.title }}</div>
      <div class="item-content">{{ item.content }}</div>
      <div class="item-time">{{ formatTime(item.createTime) }}</div>
    </div>
  </div>
</template>

<style scoped>
.notification-item {
  padding: 12px 14px;
  border-bottom: 1px solid #f3f4f6;
  cursor: pointer;
  transition: background 0.2s ease;
  border-left: 3px solid transparent;
}

.notification-item:hover {
  background: #f9fafb;
}

.notification-item.unread {
  border-left-color: #14b8a6;
  background: #f0fdfa;
}

.notification-item.compact {
  padding: 10px 12px;
}

.item-title {
  font-size: 14px;
  font-weight: 600;
  color: #374151;
  margin-bottom: 4px;
}

.notification-item.unread .item-title {
  color: #111827;
}

.item-content {
  font-size: 13px;
  color: #6b7280;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.item-time {
  margin-top: 6px;
  font-size: 12px;
  color: #9ca3af;
}
</style>
