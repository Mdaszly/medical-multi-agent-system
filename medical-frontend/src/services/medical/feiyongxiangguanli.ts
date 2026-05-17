// @ts-ignore
/* eslint-disable */
import request from "../request";

/** 查询费用项详情 根据ID查询费用项详情 GET /api/fee-item/${param0} */
export async function getFeeItemById(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getFeeItemByIdParams,
  options?: { [key: string]: any }
) {
  const { id: param0, ...queryParams } = params;
  return request<API.BaseResponseFeeItemVO>(`/api/fee-item/${param0}`, {
    method: "GET",
    params: { ...queryParams },
    ...(options || {}),
  });
}

/** 查询预约费用项列表 根据预约ID查询费用项列表 GET /api/fee-item/list/appointment/${param0} */
export async function listByAppointmentId(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.listByAppointmentIdParams,
  options?: { [key: string]: any }
) {
  const { appointmentId: param0, ...queryParams } = params;
  return request<API.BaseResponseListFeeItemVO>(
    `/api/fee-item/list/appointment/${param0}`,
    {
      method: "GET",
      params: { ...queryParams },
      ...(options || {}),
    }
  );
}

/** 查询处方费用项列表 根据处方ID查询费用项列表 GET /api/fee-item/list/prescription/${param0} */
export async function listByPrescriptionId(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.listByPrescriptionIdParams,
  options?: { [key: string]: any }
) {
  const { prescriptionId: param0, ...queryParams } = params;
  return request<API.BaseResponseListFeeItemVO>(
    `/api/fee-item/list/prescription/${param0}`,
    {
      method: "GET",
      params: { ...queryParams },
      ...(options || {}),
    }
  );
}

/** 查询用户费用项列表 根据用户ID查询费用项列表 GET /api/fee-item/list/user/${param0} */
export async function listByUserId1(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.listByUserId1Params,
  options?: { [key: string]: any }
) {
  const { userId: param0, ...queryParams } = params;
  return request<API.BaseResponseListFeeItemVO>(
    `/api/fee-item/list/user/${param0}`,
    {
      method: "GET",
      params: { ...queryParams },
      ...(options || {}),
    }
  );
}

/** 查询用户未结算金额 根据用户ID查询未结算费用总和 GET /api/fee-item/unsettled-amount/${param0} */
export async function getUnsettledAmount(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getUnsettledAmountParams,
  options?: { [key: string]: any }
) {
  const { userId: param0, ...queryParams } = params;
  return request<API.BaseResponseBigDecimal>(
    `/api/fee-item/unsettled-amount/${param0}`,
    {
      method: "GET",
      params: { ...queryParams },
      ...(options || {}),
    }
  );
}
