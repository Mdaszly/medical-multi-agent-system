<script setup lang="ts">
import { onMounted } from "vue";
import { useRouter } from "vue-router";
import NotificationListItem from "@/components/notification/NotificationListItem.vue";
import { useNotifications } from "@/composables/useNotifications";
import type { UserNotificationVO } from "@/services/medical/tongzhiguanli";
import { resolveNotificationRoute } from "@/utils/notificationNavigation";

const props = defineProps<{
  role: "user" | "doctor";
}>();

const router = useRouter();
const { notifications, loading, fetchNotifications, markAsRead, refreshUnreadCount } =
  useNotifications();

onMounted(async () => {
  await fetchNotifications(50);
  await refreshUnreadCount();
});

function resolveTargetRoute(item: UserNotificationVO) {
  return resolveNotificationRoute(item, props.role);
}

async function handleItemClick(item: UserNotificationVO) {
  if (item.id && item.readStatus === 0) {
    await markAsRead(item.id);
  }
  const target = resolveTargetRoute(item);
  if (target) {
    router.push(target);
  }
}
</script>

<template>
  <el-card shadow="never" class="notification-center">
    <template #header>
      <div class="card-header">
        <span>消息通知</span>
        <span class="hint">预约、账单等领域事件通知</span>
      </div>
    </template>

    <div v-loading="loading">
      <div v-if="notifications.length === 0" class="empty-state">
        暂无消息通知。预约、待支付账单或结算完成后会在这里显示。
      </div>
      <NotificationListItem
        v-for="item in notifications"
        :key="item.id"
        :item="item"
        @click="handleItemClick"
      />
    </div>
  </el-card>
</template>

<style scoped>
.notification-center {
  border-radius: 12px;
}

.card-header {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.card-header span:first-child {
  font-size: 18px;
  font-weight: 600;
  color: #111827;
}

.hint {
  font-size: 13px;
  color: #6b7280;
}

.empty-state {
  padding: 48px 16px;
  text-align: center;
  color: #9ca3af;
  line-height: 1.6;
}
</style>
