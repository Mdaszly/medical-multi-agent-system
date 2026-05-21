// @ts-ignore
/* eslint-disable */
import request from "../request";

/** 获取当前用户 获取当前登录用户的详细信息 GET /api/auth/current */
export async function getCurrentUser1(options?: { [key: string]: any }) {
  return request<API.BaseResponseAuthLoginVO>("/api/auth/current", {
    method: "GET",
    ...(options || {}),
  });
}

/** 统一登录 患者/医生/管理员统一登录接口，使用账号密码登录 POST /api/auth/login */
export async function login(
  body: API.AuthLoginRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseAuthLoginVO>("/api/auth/login", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 统一退出 清除当前用户的登录状态 POST /api/auth/logout */
export async function logout(options?: { [key: string]: any }) {
  return request<API.BaseResponseVoid>("/api/auth/logout", {
    method: "POST",
    ...(options || {}),
  });
}

/** 统一注册 患者/医生统一注册接口，用户角色通过userRole字段指定 POST /api/auth/register */
export async function register(
  body: API.AuthRegisterRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseAuthRegisterVO>("/api/auth/register", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 管理员自助注册（临时账号） 通过公开接口注册临时管理员账号 POST /api/auth/register/admin */
export async function registerAdmin(
  body: API.AuthRegisterRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseAuthRegisterVO>("/api/auth/register/admin", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}
