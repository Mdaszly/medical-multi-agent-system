// @ts-ignore
/* eslint-disable */
import request from "../request";

/** 提交问诊 路由 Agent + 专业 Agent 同步推理 POST /api/v1/consult */
export async function consult(
  body: API.ConsultRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseConsultVO>("/api/v1/consult", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 流式问诊 百炼 SSE 流式输出，事件：chunk / done / error POST /api/v1/consult/stream */
export async function consultStream(
  body: API.ConsultRequest,
  options?: { [key: string]: any }
) {
  return request<API.SseEmitter>("/api/v1/consult/stream", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}
