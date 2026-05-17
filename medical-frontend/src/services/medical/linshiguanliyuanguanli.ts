// @ts-ignore
/* eslint-disable */
import request from "../request";

/** 【临时】注册管理员 开发测试用 - 无需认证 - 2026-06-13前删除 POST /api/temp/admin/register */
export async function tempRegisterAdmin(
  body: API.AdminUpdateRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseAdminVO>("/api/temp/admin/register", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}
