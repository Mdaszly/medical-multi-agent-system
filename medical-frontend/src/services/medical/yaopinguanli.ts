// @ts-ignore
/* eslint-disable */
import request from "../request";

/** 新增药品 新增药品信息 POST /api/drug/add */
export async function addDrug(
  body: API.DrugAddRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseDrugVO>("/api/drug/add", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 获取药品类别 获取药品类别列表 GET /api/drug/categories */
export async function getCategories(options?: { [key: string]: any }) {
  return request<API.BaseResponseListMapStringString>("/api/drug/categories", {
    method: "GET",
    ...(options || {}),
  });
}

/** 删除药品 删除药品（软删除） POST /api/drug/delete */
export async function deleteDrug(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.deleteDrugParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseVoid>("/api/drug/delete", {
    method: "POST",
    params: {
      ...params,
    },
    ...(options || {}),
  });
}

/** 计算费用 计算药品费用：总金额 = Σ(单价 × 数量) POST /api/drug/fee/calculate */
export async function calculateFee(
  body: API.FeeCalculationRequest[],
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBigDecimal>("/api/drug/fee/calculate", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 查询药品详情 根据ID查询药品详情 GET /api/drug/get */
export async function getDrug(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getDrugParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseDrugVO>("/api/drug/get", {
    method: "GET",
    params: {
      ...params,
    },
    ...(options || {}),
  });
}

/** 根据编码查询药品 根据药品编码查询药品详情 GET /api/drug/getByCode */
export async function getDrugByCode(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getDrugByCodeParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseDrugVO>("/api/drug/getByCode", {
    method: "GET",
    params: {
      ...params,
    },
    ...(options || {}),
  });
}

/** 药品列表 查询药品列表（带价格） POST /api/drug/list */
export async function listDrugs(
  body: API.DrugQueryRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseListDrugWithPriceVO>("/api/drug/list", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 新增药品价格 为药品添加新价格 POST /api/drug/price/add */
export async function addPrice(
  body: API.PriceAddRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseVoid>("/api/drug/price/add", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 获取药品当前价格 获取药品当前有效价格 GET /api/drug/price/get */
export async function getCurrentPrice(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getCurrentPriceParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBigDecimal>("/api/drug/price/get", {
    method: "GET",
    params: {
      // priceType has a default value: RETAIL
      priceType: "RETAIL",
      ...params,
    },
    ...(options || {}),
  });
}

/** 根据编码获取价格 根据药品编码获取当前价格 GET /api/drug/price/getByCode */
export async function getCurrentPriceByCode(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getCurrentPriceByCodeParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBigDecimal>("/api/drug/price/getByCode", {
    method: "GET",
    params: {
      // priceType has a default value: RETAIL
      priceType: "RETAIL",
      ...params,
    },
    ...(options || {}),
  });
}

/** 更新药品 更新药品信息 POST /api/drug/update */
export async function updateDrug(
  body: API.DrugUpdateRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseDrugVO>("/api/drug/update", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}
