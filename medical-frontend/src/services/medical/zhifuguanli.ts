// @ts-ignore
/* eslint-disable */
import request from "../request";

/** 查询支付记录 根据ID查询支付记录 GET /api/payment/${param0} */
export async function getPaymentById(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getPaymentByIdParams,
  options?: { [key: string]: any }
) {
  const { id: param0, ...queryParams } = params;
  return request<API.BaseResponsePaymentVO>(`/api/payment/${param0}`, {
    method: "GET",
    params: { ...queryParams },
    ...(options || {}),
  });
}

/** 创建支付记录 创建支付记录，准备支付 POST /api/payment/create */
export async function createPayment(
  body: API.PaymentRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePaymentVO>("/api/payment/create", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 查询账单支付记录 根据账单ID查询支付记录列表 GET /api/payment/list/bill/${param0} */
export async function listByBillId(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.listByBillIdParams,
  options?: { [key: string]: any }
) {
  const { billId: param0, ...queryParams } = params;
  return request<API.BaseResponseListPaymentVO>(
    `/api/payment/list/bill/${param0}`,
    {
      method: "GET",
      params: { ...queryParams },
      ...(options || {}),
    }
  );
}

/** 查询用户支付记录 根据用户ID查询支付记录列表 GET /api/payment/list/user/${param0} */
export async function listByUserId(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.listByUserIdParams,
  options?: { [key: string]: any }
) {
  const { userId: param0, ...queryParams } = params;
  return request<API.BaseResponseListPaymentVO>(
    `/api/payment/list/user/${param0}`,
    {
      method: "GET",
      params: { ...queryParams },
      ...(options || {}),
    }
  );
}

/** 根据支付编号查询 根据支付编号查询支付记录 GET /api/payment/no/${param0} */
export async function getPaymentByNo(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getPaymentByNoParams,
  options?: { [key: string]: any }
) {
  const { paymentNo: param0, ...queryParams } = params;
  return request<API.BaseResponsePaymentVO>(`/api/payment/no/${param0}`, {
    method: "GET",
    params: { ...queryParams },
    ...(options || {}),
  });
}

/** 发起支付 模拟支付，更新支付状态 POST /api/payment/pay/${param0} */
export async function pay(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.payParams,
  options?: { [key: string]: any }
) {
  const { paymentId: param0, ...queryParams } = params;
  return request<API.BaseResponsePaymentVO>(`/api/payment/pay/${param0}`, {
    method: "POST",
    params: { ...queryParams },
    ...(options || {}),
  });
}

/** 发起退款 对已支付的订单发起退款 POST /api/payment/refund */
export async function refund(
  body: API.RefundRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePaymentVO>("/api/payment/refund", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 查询支付状态 查询支付状态描述 GET /api/payment/status/${param0} */
export async function getPaymentStatus(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getPaymentStatusParams,
  options?: { [key: string]: any }
) {
  const { paymentId: param0, ...queryParams } = params;
  return request<API.BaseResponseString>(`/api/payment/status/${param0}`, {
    method: "GET",
    params: { ...queryParams },
    ...(options || {}),
  });
}
