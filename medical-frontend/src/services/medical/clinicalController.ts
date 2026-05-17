// @ts-ignore
/* eslint-disable */
import request from "../request";

/** 此处后端没有提供注释 POST /api/v1/clinical/analyze */
export async function analyze(
  body: API.AnalyzeRequest,
  options?: { [key: string]: any }
) {
  return request<API.ClinicalState>("/api/v1/clinical/analyze", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 GET /api/v1/clinical/health */
export async function health(options?: { [key: string]: any }) {
  return request<Record<string, any>>("/api/v1/clinical/health", {
    method: "GET",
    ...(options || {}),
  });
}
