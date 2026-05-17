// @ts-ignore
/* eslint-disable */
import request from "../request";

/** 取消预约 患者取消预约，管理员可取消任何预约 POST /api/appointment/cancel */
export async function cancelAppointment(
  body: API.AppointmentCancelRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseVoid>("/api/appointment/cancel", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 预约签到 患者或医生签到 POST /api/appointment/checkin */
export async function checkInAppointment(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.checkInAppointmentParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseVoid>("/api/appointment/checkin", {
    method: "POST",
    params: {
      ...params,
    },
    ...(options || {}),
  });
}

/** 创建预约 患者创建预约挂号，管理员可代为创建 POST /api/appointment/create */
export async function createAppointment(
  body: API.AppointmentAddRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseAppointmentVO>("/api/appointment/create", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 获取预约详情 根据ID获取预约详情 GET /api/appointment/get */
export async function getAppointmentById(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getAppointmentByIdParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseAppointmentVO>("/api/appointment/get", {
    method: "GET",
    params: {
      ...params,
    },
    ...(options || {}),
  });
}

/** 查询医生预约 查询指定医生的预约列表 GET /api/appointment/list/doctor */
export async function listAppointmentByDoctor(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.listAppointmentByDoctorParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseListAppointmentVO>(
    "/api/appointment/list/doctor",
    {
      method: "GET",
      params: {
        ...params,
      },
      ...(options || {}),
    }
  );
}

/** 分页查询预约 管理员分页查询预约列表 POST /api/appointment/list/page */
export async function listAppointmentPage(
  body: API.AppointmentQueryRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseIPageAppointmentVO>(
    "/api/appointment/list/page",
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

/** 查询用户预约 查询指定用户的预约列表 GET /api/appointment/list/user */
export async function listAppointmentByUser(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.listAppointmentByUserParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseListAppointmentVO>(
    "/api/appointment/list/user",
    {
      method: "GET",
      params: {
        ...params,
      },
      ...(options || {}),
    }
  );
}

/** 获取号源列表 根据排班ID获取号源列表 GET /api/appointment/slots */
export async function getAppointmentSlots(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getAppointmentSlotsParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseListAppointmentSlotVO>(
    "/api/appointment/slots",
    {
      method: "GET",
      params: {
        ...params,
      },
      ...(options || {}),
    }
  );
}

/** 更新预约状态 管理员更新预约状态 POST /api/appointment/status/update */
export async function updateAppointmentStatus(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.updateAppointmentStatusParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseVoid>("/api/appointment/status/update", {
    method: "POST",
    params: {
      ...params,
    },
    ...(options || {}),
  });
}
