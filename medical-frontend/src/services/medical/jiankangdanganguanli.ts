// @ts-ignore
/* eslint-disable */
import request from "../request";

/** 创建健康档案 为用户创建健康档案 POST /api/health-profile/create */
export async function createHealthProfile(
  body: API.HealthProfile,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseHealthProfileVO>(
    "/api/health-profile/create",
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

/** 获取健康档案 根据用户ID获取健康档案 GET /api/health-profile/get */
export async function getHealthProfile(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getHealthProfileParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseHealthProfileVO>("/api/health-profile/get", {
    method: "GET",
    params: {
      ...params,
    },
    ...(options || {}),
  });
}

/** 更新健康档案 更新用户健康档案信息 POST /api/health-profile/update */
export async function updateHealthProfile(
  body: API.HealthProfile,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseHealthProfileVO>(
    "/api/health-profile/update",
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
