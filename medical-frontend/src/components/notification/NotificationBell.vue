<script setup lang="ts">
import { ref, onMounted } from "vue";
import { useRouter } from "vue-router";
import { Bell } from "@element-plus/icons-vue";
import NotificationListItem from "./NotificationListItem.vue";
import { useNotifications } from "@/composables/useNotifications";
import type { UserNotificationVO } from "@/services/medical/tongzhiguanli";
import { resolveNotificationRoute } from "@/utils/notificationNavigation";

const props = defineProps<{
  role: "user" | "doctor";
}>();

const router = useRouter();
const popoverVisible = ref(false);
const {
  unreadCount,
  loading,
  fetchNotifications,
  refreshUnreadCount,
  markAsRead,
} = useNotifications();

const previewList = ref<UserNotificationVO[]>([]);

onMounted(() => {
  refreshUnreadCount();
});

async function openPopover() {
  popoverVisible.value = true;
  const list = await fetchNotifications(5);
  previewList.value = list.slice(0, 5);
  await refreshUnreadCount();
}

function resolveTargetRoute(item: UserNotificationVO) {
  return resolveNotificationRoute(item, props.role);
}

async function handleItemClick(item: UserNotificationVO) {
  if (item.id && item.readStatus === 0) {
    await markAsRead(item.id);
  }
  const target = resolveTargetRoute(item);
  popoverVisible.value = false;
  if (target) {
    router.push(target);
  }
}

function goToCenter() {
  popoverVisible.value = false;
  router.push(props.role === "doctor" ? "/doctor/notifications" : "/patient/notifications");
}
</script>

<template>
  <el-popover
    v-model:visible="popoverVisible"
    placement="bottom-end"
    :width="360"
    trigger="click"
    @before-enter="openPopover"
  >
    <template #reference>
      <button
        type="button"
        class="bell-button"
        :aria-label="`消息通知，未读 ${unreadCount} 条`"
      >
        <el-badge :value="unreadCount" :hidden="unreadCount === 0" :max="99">
          <el-icon :size="20"><Bell /></el-icon>
        </el-badge>
      </button>
    </template>

    <div v-loading="loading" class="popover-panel">
      <div class="popover-header">消息通知</div>
      <div v-if="previewList.length === 0" class="empty-state">暂无消息通知</div>
      <NotificationListItem
        v-for="item in previewList"
        :key="item.id"
        :item="item"
        compact
        @click="handleItemClick"
      />
      <button type="button" class="view-all" @click="goToCenter">查看全部通知</button>
    </div>
  </el-popover>
</template>

<style scoped>
.bell-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  margin-right: 8px;
  border: none;
  border-radius: 8px;
  background: transparent;
  cursor: pointer;
  color: #374151;
  transition: background 0.2s ease;
}

.bell-button:hover {
  background: #f3f4f6;
}

.bell-button:focus-visible {
  outline: 2px solid #14b8a6;
  outline-offset: 2px;
}

.popover-panel {
  min-height: 80px;
}

.popover-header {
  font-size: 15px;
  font-weight: 600;
  color: #111827;
  padding: 4px 4px 10px;
  border-bottom: 1px solid #e5e7eb;
  margin-bottom: 4px;
}

.empty-state {
  padding: 24px 8px;
  text-align: center;
  color: #9ca3af;
  font-size: 14px;
}

.view-all {
  width: 100%;
  margin-top: 8px;
  padding: 10px;
  border: none;
  border-top: 1px solid #e5e7eb;
  background: transparent;
  color: #0d9488;
  font-size: 14px;
  cursor: pointer;
  transition: color 0.2s ease;
}

.view-all:hover {
  color: #0f766e;
}
</style>
