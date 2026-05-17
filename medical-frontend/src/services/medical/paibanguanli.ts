// @ts-ignore
/* eslint-disable */
import request from "../request";

/** 添加排班 为医生添加排班信息 POST /api/schedule/add */
export async function addSchedule(
  body: API.ScheduleAddRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseScheduleVO>("/api/schedule/add", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 批量添加排班 批量为多个医生添加排班信息 POST /api/schedule/batch/add */
export async function batchAddSchedules(
  body: API.ScheduleAddRequest[],
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseVoid>("/api/schedule/batch/add", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 检查排班冲突 检查医生在指定时间是否已有排班 POST /api/schedule/check/conflict */
export async function checkScheduleConflict(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.checkScheduleConflictParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>("/api/schedule/check/conflict", {
    method: "POST",
    params: {
      ...params,
    },
    ...(options || {}),
  });
}

/** 删除排班 删除排班信息 POST /api/schedule/delete */
export async function deleteSchedule(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.deleteScheduleParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseVoid>("/api/schedule/delete", {
    method: "POST",
    params: {
      ...params,
    },
    ...(options || {}),
  });
}

/** 获取排班详情 根据排班ID获取排班详细信息 GET /api/schedule/get */
export async function getScheduleById(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getScheduleByIdParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseScheduleVO>("/api/schedule/get", {
    method: "GET",
    params: {
      ...params,
    },
    ...(options || {}),
  });
}

/** 按科室查询排班 查询指定科室在指定日期的排班信息 GET /api/schedule/list/department */
export async function listScheduleByDepartment(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.listScheduleByDepartmentParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseListScheduleVO>(
    "/api/schedule/list/department",
    {
      method: "GET",
      params: {
        ...params,
      },
      ...(options || {}),
    }
  );
}

/** 查询医生排班 查询指定医生在日期范围内的排班信息 GET /api/schedule/list/doctor */
export async function listScheduleByDoctor(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.listScheduleByDoctorParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseListScheduleVO>("/api/schedule/list/doctor", {
    method: "GET",
    params: {
      ...params,
    },
    ...(options || {}),
  });
}

/** 查询在岗医生 查询指定日期和班次的在岗医生列表 GET /api/schedule/list/on-duty */
export async function listOnDutyDoctors(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.listOnDutyDoctorsParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseListScheduleVO>("/api/schedule/list/on-duty", {
    method: "GET",
    params: {
      ...params,
    },
    ...(options || {}),
  });
}

/** 分页查询排班 分页查询排班信息，支持多条件筛选 POST /api/schedule/list/page */
export async function listSchedulePage(
  body: API.ScheduleQueryRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseIPageScheduleVO>("/api/schedule/list/page", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 医生负载均衡 获取指定科室医生的排班负载情况 GET /api/schedule/load-balance */
export async function getDoctorLoadBalance(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getDoctorLoadBalanceParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseMapStringObject>(
    "/api/schedule/load-balance",
    {
      method: "GET",
      params: {
        ...params,
      },
      ...(options || {}),
    }
  );
}

/** 更新排班 更新排班信息 POST /api/schedule/update */
export async function updateSchedule(
  body: API.ScheduleUpdateRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseScheduleVO>("/api/schedule/update", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}
