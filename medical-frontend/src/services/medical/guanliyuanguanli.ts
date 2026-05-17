// @ts-ignore
/* eslint-disable */
import request from "../request";

/** 新增管理员 超级管理员新增管理员 POST /api/admin/add */
export async function addAdmin(
  body: API.AdminUpdateRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseAdminVO>("/api/admin/add", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 删除管理员 超级管理员删除管理员 POST /api/admin/delete */
export async function deleteAdmin(
  body: API.AdminIdRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseVoid>("/api/admin/delete", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 根据ID获取管理员信息 根据管理员ID获取管理员详细信息 POST /api/admin/get */
export async function getAdminById(
  body: API.AdminIdRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseAdminVO>("/api/admin/get", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 获取当前登录管理员 获取当前登录管理员的详细信息 GET /api/admin/get/login */
export async function getCurrentAdmin(options?: { [key: string]: any }) {
  return request<API.BaseResponseAdminVO>("/api/admin/get/login", {
    method: "GET",
    ...(options || {}),
  });
}

/** 管理员列表分页查询 分页查询管理员列表，支持条件筛选 POST /api/admin/list/page */
export async function listAdminPage(
  body: API.AdminQueryRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseIPageAdminVO>("/api/admin/list/page", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 重置密码 超级管理员重置管理员密码 POST /api/admin/reset/password */
export async function resetPassword(
  body: API.ResetPasswordRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseVoid>("/api/admin/reset/password", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 更新管理员信息 超级管理员更新管理员信息 POST /api/admin/update */
export async function updateAdmin(
  body: API.AdminUpdateWithIdRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseAdminVO>("/api/admin/update", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}
