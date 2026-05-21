// @ts-ignore
/* eslint-disable */
import request from "../request";

/** 取消处方 取消处方（仅可取消待审核的处方） POST /api/prescription/cancel */
export async function cancelPrescription(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.cancelPrescriptionParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseVoid>("/api/prescription/cancel", {
    method: "POST",
    params: {
      ...params,
    },
    ...(options || {}),
  });
}

/** 创建处方 医生为患者开具处方，处方状态直接为已审核 POST /api/prescription/create */
export async function createPrescription(
  body: API.PrescriptionAddRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePrescriptionVO>("/api/prescription/create", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 发药 药房人员发药，处方状态从已审核变为已发药 POST /api/prescription/dispense */
export async function dispensePrescription(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.dispensePrescriptionParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseVoid>("/api/prescription/dispense", {
    method: "POST",
    params: {
      ...params,
    },
    ...(options || {}),
  });
}

/** 获取处方详情 根据ID获取处方详情（含明细） GET /api/prescription/get */
export async function getPrescriptionById(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getPrescriptionByIdParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePrescriptionVO>("/api/prescription/get", {
    method: "GET",
    params: {
      ...params,
    },
    ...(options || {}),
  });
}

/** 根据编号获取处方 根据处方编号获取处方详情 GET /api/prescription/get/byNo */
export async function getPrescriptionByNo(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getPrescriptionByNoParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePrescriptionVO>("/api/prescription/get/byNo", {
    method: "GET",
    params: {
      ...params,
    },
    ...(options || {}),
  });
}

/** 查询预约处方 查询指定预约关联的处方列表 GET /api/prescription/list/appointment */
export async function listPrescriptionByAppointment(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.listPrescriptionByAppointmentParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseListPrescriptionVO>(
    "/api/prescription/list/appointment",
    {
      method: "GET",
      params: {
        ...params,
      },
      ...(options || {}),
    }
  );
}

/** 查询医生处方 查询医生开具的处方列表 GET /api/prescription/list/doctor */
export async function listPrescriptionByDoctor(options?: {
  [key: string]: any;
}) {
  return request<API.BaseResponseListPrescriptionVO>(
    "/api/prescription/list/doctor",
    {
      method: "GET",
      ...(options || {}),
    }
  );
}

/** 分页查询处方 管理员分页查询处方列表 POST /api/prescription/list/page */
export async function listPrescriptionPage(
  body: API.PrescriptionQueryRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseIPagePrescriptionVO>(
    "/api/prescription/list/page",
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

/** 查询待发药处方 药师查询待发药的处方列表 GET /api/prescription/list/pending-dispense */
export async function listPendingDispensePrescriptions(options?: {
  [key: string]: any;
}) {
  return request<API.BaseResponseListPrescriptionVO>(
    "/api/prescription/list/pending-dispense",
    {
      method: "GET",
      ...(options || {}),
    }
  );
}

/** 查询用户处方 查询当前患者的处方列表 GET /api/prescription/list/user */
export async function listPrescriptionByUser(options?: { [key: string]: any }) {
  return request<API.BaseResponseListPrescriptionVO>(
    "/api/prescription/list/user",
    {
      method: "GET",
      ...(options || {}),
    }
  );
}

/** 更新处方状态 更新处方状态（如审核） POST /api/prescription/status/update */
export async function updatePrescriptionStatus(
  body: API.PrescriptionStatusUpdateRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseVoid>("/api/prescription/status/update", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 修改处方 修改处方信息（仅可修改待审核状态的处方） POST /api/prescription/update */
export async function updatePrescription(
  body: API.PrescriptionUpdateRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePrescriptionVO>("/api/prescription/update", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}
