import request from "../request";

export interface UserNotificationVO {
  id?: number;
  userId?: number;
  title?: string;
  content?: string;
  bizType?: string;
  bizId?: number;
  eventType?: string;
  readStatus?: number;
  createTime?: string;
}

/** 当前用户通知列表 GET /api/notification/list */
export async function listNotifications(
  params?: { limit?: number },
  options?: { [key: string]: any },
) {
  return request<{ code: number; data: UserNotificationVO[]; message: string }>(
    "/api/notification/list",
    {
      method: "GET",
      params,
      ...(options || {}),
    },
  );
}

/** 未读通知数 GET /api/notification/unread-count */
export async function getUnreadNotificationCount(options?: { [key: string]: any }) {
  return request<{ code: number; data: number; message: string }>(
    "/api/notification/unread-count",
    {
      method: "GET",
      ...(options || {}),
    },
  );
}

/** 标记通知已读 PUT /api/notification/{id}/read */
export async function markNotificationRead(
  id: number,
  options?: { [key: string]: any },
) {
  return request<{ code: number; data: null; message: string }>(
    `/api/notification/${id}/read`,
    {
      method: "PUT",
      ...(options || {}),
    },
  );
}
