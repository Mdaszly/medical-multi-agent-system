// @ts-ignore
/* eslint-disable */
import request from "../request";

/** 创建会话 POST /api/v1/consult/sessions */
export async function createSession(
  body: API.ChatSessionCreateRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseChatSessionVO>("/api/v1/consult/sessions", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 查询会话消息 GET /api/v1/consult/sessions/${param0}/messages */
export async function listMessages(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.listMessagesParams,
  options?: { [key: string]: any }
) {
  const { sessionId: param0, ...queryParams } = params;
  return request<API.BaseResponseListChatMessageVO>(
    `/api/v1/consult/sessions/${param0}/messages`,
    {
      method: "GET",
      params: {
        // limit has a default value: 100
        limit: "100",
        ...queryParams,
      },
      ...(options || {}),
    }
  );
}

/** 删除会话 POST /api/v1/consult/sessions/delete */
export async function deleteSession(
  body: API.DeleteSessionRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>("/api/v1/consult/sessions/delete", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 查询历史会话 GET /api/v1/consult/sessions/history */
export async function queryHistory(options?: { [key: string]: any }) {
  return request<API.BaseResponseChatSessionHistoryVO>(
    "/api/v1/consult/sessions/history",
    {
      method: "GET",
      ...(options || {}),
    }
  );
}

/** 保存消息 POST /api/v1/consult/sessions/messages */
export async function saveMessage(
  body: API.ChatMessageSaveRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseChatMessageVO>(
    "/api/v1/consult/sessions/messages",
    {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      data: body,
      ...(options || {}),
    }
  );
}

/** 更新会话标题 PUT /api/v1/consult/sessions/title */
export async function updateTitle(
  body: API.ChatSessionTitleUpdateRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>("/api/v1/consult/sessions/title", {
    method: "PUT",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}
