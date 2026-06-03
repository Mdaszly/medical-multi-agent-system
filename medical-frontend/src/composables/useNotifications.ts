import { ref, computed } from "vue";
import {
  getUnreadNotificationCount,
  listNotifications,
  markNotificationRead,
  type UserNotificationVO,
} from "@/services/medical/tongzhiguanli";

const notifications = ref<UserNotificationVO[]>([]);
const unreadCount = ref(0);
const loading = ref(false);

export function useNotifications() {
  const hasUnread = computed(() => unreadCount.value > 0);

  async function fetchNotifications(limit = 50) {
    loading.value = true;
    try {
      const res = await listNotifications({ limit }, { showError: false });
      notifications.value = res.data ?? [];
      unreadCount.value = notifications.value.filter((n) => n.readStatus === 0).length;
      return notifications.value;
    } finally {
      loading.value = false;
    }
  }

  async function refreshUnreadCount() {
    try {
      const res = await getUnreadNotificationCount({ showError: false });
      unreadCount.value = res.data ?? 0;
    } catch {
      unreadCount.value = notifications.value.filter((n) => n.readStatus === 0).length;
    }
  }

  async function markAsRead(id: number) {
    await markNotificationRead(id, { showError: true });
    const item = notifications.value.find((n) => n.id === id);
    if (item && item.readStatus === 0) {
      item.readStatus = 1;
      unreadCount.value = Math.max(0, unreadCount.value - 1);
    }
  }

  return {
    notifications,
    unreadCount,
    hasUnread,
    loading,
    fetchNotifications,
    refreshUnreadCount,
    markAsRead,
  };
}
