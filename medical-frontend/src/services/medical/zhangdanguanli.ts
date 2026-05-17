// @ts-ignore
/* eslint-disable */
import request from "../request";

/** 查询账单详情 根据ID查询账单详情 GET /api/bill/${param0} */
export async function getBillById(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getBillByIdParams,
  options?: { [key: string]: any }
) {
  const { id: param0, ...queryParams } = params;
  return request<API.BaseResponseBillVO>(`/api/bill/${param0}`, {
    method: "GET",
    params: { ...queryParams },
    ...(options || {}),
  });
}

/** 导出账单 导出账单为CSV格式 GET /api/bill/export */
export async function exportBill(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.exportBillParams,
  options?: { [key: string]: any }
) {
  return request<string[]>("/api/bill/export", {
    method: "GET",
    params: {
      ...params,
    },
    ...(options || {}),
  });
}

/** 生成账单 根据预约或处方生成账单 POST /api/bill/generate */
export async function generateBill(
  body: API.BillGenerateRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBillVO>("/api/bill/generate", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 根据预约生成账单 根据预约ID生成账单 POST /api/bill/generate/appointment/${param0} */
export async function generateBillByAppointment(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.generateBillByAppointmentParams,
  options?: { [key: string]: any }
) {
  const { appointmentId: param0, ...queryParams } = params;
  return request<API.BaseResponseBillVO>(
    `/api/bill/generate/appointment/${param0}`,
    {
      method: "POST",
      params: { ...queryParams },
      ...(options || {}),
    }
  );
}

/** 分页查询账单列表 分页查询账单列表 GET /api/bill/list */
export async function listBillPage(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.listBillPageParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseIPageBillVO>("/api/bill/list", {
    method: "GET",
    params: {
      // current has a default value: 1
      current: "1",
      // pageSize has a default value: 10
      pageSize: "10",

      ...params,
    },
    ...(options || {}),
  });
}

/** 查询预约账单 根据预约ID查询账单 GET /api/bill/list/appointment/${param0} */
export async function getByAppointmentId(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getByAppointmentIdParams,
  options?: { [key: string]: any }
) {
  const { appointmentId: param0, ...queryParams } = params;
  return request<API.BaseResponseBillVO>(
    `/api/bill/list/appointment/${param0}`,
    {
      method: "GET",
      params: { ...queryParams },
      ...(options || {}),
    }
  );
}

/** 查询用户账单列表 根据用户ID查询账单列表 GET /api/bill/list/user/${param0} */
export async function listByUserId2(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.listByUserId2Params,
  options?: { [key: string]: any }
) {
  const { userId: param0, ...queryParams } = params;
  return request<API.BaseResponseListBillVO>(`/api/bill/list/user/${param0}`, {
    method: "GET",
    params: { ...queryParams },
    ...(options || {}),
  });
}

/** 根据账单编号查询 根据账单编号查询账单详情 GET /api/bill/no/${param0} */
export async function getBillByNo(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getBillByNoParams,
  options?: { [key: string]: any }
) {
  const { billNo: param0, ...queryParams } = params;
  return request<API.BaseResponseBillVO>(`/api/bill/no/${param0}`, {
    method: "GET",
    params: { ...queryParams },
    ...(options || {}),
  });
}
