// @ts-ignore
/* eslint-disable */
import request from "../request";

/** 绑定邮箱 用户绑定邮箱 POST /api/user/bind/email */
export async function bindEmail(
  body: API.BindEmailRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseVoid>("/api/user/bind/email", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 绑定手机号 用户绑定手机号 POST /api/user/bind/phone */
export async function bindPhone(
  body: API.BindPhoneRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseVoid>("/api/user/bind/phone", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 修改密码 用户修改自己的密码 POST /api/user/change/password */
export async function changePassword(
  body: API.ChangePasswordRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseVoid>("/api/user/change/password", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 删除用户 管理员删除用户 POST /api/user/delete */
export async function deleteUser(
  body: API.UserIdRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseVoid>("/api/user/delete", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 禁用用户 管理员禁用用户账号 POST /api/user/disable */
export async function disableUser(
  body: API.UserIdRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseVoid>("/api/user/disable", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 启用用户 管理员启用用户账号 POST /api/user/enable */
export async function enableUser(
  body: API.UserIdRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseVoid>("/api/user/enable", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 获取当前登录用户 获取当前登录用户的详细信息 GET /api/user/get/login */
export async function getCurrentUser(options?: { [key: string]: any }) {
  return request<API.BaseResponseUserVO>("/api/user/get/login", {
    method: "GET",
    ...(options || {}),
  });
}

/** 根据ID获取用户信息 根据用户ID获取用户详细信息 POST /api/user/get/vo */
export async function getUserById(
  body: API.UserIdRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseUserVO>("/api/user/get/vo", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 用户列表分页查询 分页查询用户列表，支持条件筛选 POST /api/user/list/page */
export async function listUserPage(
  body: API.UserQueryRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseIPageUserVO>("/api/user/list/page", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 更新用户信息 管理员更新用户信息 POST /api/user/update */
export async function updateUser(
  body: API.UserUpdateWithIdRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseUserVO>("/api/user/update", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 更新个人资料 用户更新自己的个人资料 POST /api/user/update/profile */
export async function updateProfile(
  body: API.UserUpdateRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseUserVO>("/api/user/update/profile", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}
