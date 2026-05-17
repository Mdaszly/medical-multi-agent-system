// @ts-ignore
/* eslint-disable */
import request from "../request";

/** 添加号源 为排班添加一个号源时段 POST /api/slot/add */
export async function addSlot(
  body: API.SlotAddRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseAppointmentSlotVO>("/api/slot/add", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 批量添加号源 为排班批量添加多个号源时段 POST /api/slot/batch/add */
export async function batchAddSlots(
  body: API.SlotBatchAddRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseVoid>("/api/slot/batch/add", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 删除号源 删除号源 POST /api/slot/delete */
export async function deleteSlot(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.deleteSlotParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseVoid>("/api/slot/delete", {
    method: "POST",
    params: {
      ...params,
    },
    ...(options || {}),
  });
}

/** 生成默认号源 为排班生成默认的号源时段（08:00-17:30，每30分钟一个时段） POST /api/slot/generate/default */
export async function generateDefaultSlots(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.generateDefaultSlotsParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseVoid>("/api/slot/generate/default", {
    method: "POST",
    params: {
      ...params,
    },
    ...(options || {}),
  });
}

/** 获取号源详情 根据ID获取号源详情 GET /api/slot/get */
export async function getSlotById(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getSlotByIdParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseAppointmentSlotVO>("/api/slot/get", {
    method: "GET",
    params: {
      ...params,
    },
    ...(options || {}),
  });
}

/** 获取排班号源列表 根据排班ID获取所有号源时段 GET /api/slot/list/schedule */
export async function getSlotsBySchedule(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getSlotsByScheduleParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseListAppointmentSlotVO>(
    "/api/slot/list/schedule",
    {
      method: "GET",
      params: {
        ...params,
      },
      ...(options || {}),
    }
  );
}

/** 更新号源 更新号源信息 POST /api/slot/update */
export async function updateSlot(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.updateSlotParams,
  body: API.SlotAddRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseVoid>("/api/slot/update", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    params: {
      ...params,
    },
    data: body,
    ...(options || {}),
  });
}
