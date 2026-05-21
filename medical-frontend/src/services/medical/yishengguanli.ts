// @ts-ignore
/* eslint-disable */
import request from "../request";

/** 新增医生 管理员新增医生 POST /api/doctor/add */
export async function addDoctor(
  body: API.DoctorUpdateRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseDoctorVO>("/api/doctor/add", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 删除医生 管理员删除医生 POST /api/doctor/delete */
export async function deleteDoctor(
  body: API.DoctorIdRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseVoid>("/api/doctor/delete", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 禁用医生 管理员禁用医生账号 POST /api/doctor/disable */
export async function disableDoctor(
  body: API.DoctorIdRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseVoid>("/api/doctor/disable", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 启用医生 管理员启用医生账号 POST /api/doctor/enable */
export async function enableDoctor(
  body: API.DoctorIdRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseVoid>("/api/doctor/enable", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 根据ID获取医生信息 根据医生ID获取医生详细信息 POST /api/doctor/get */
export async function getDoctorById(
  body: API.DoctorIdRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseDoctorVO>("/api/doctor/get", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 获取当前登录医生 获取当前登录医生的详细信息 GET /api/doctor/get/login */
export async function getCurrentDoctor(options?: { [key: string]: any }) {
  return request<API.BaseResponseDoctorVO>("/api/doctor/get/login", {
    method: "GET",
    ...(options || {}),
  });
}

/** 医生列表查询 分页查询医生列表，支持条件筛选 POST /api/doctor/list */
export async function listDoctorPage(
  body: API.DoctorQueryRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseIPageDoctorVO>("/api/doctor/list", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 按科室查询医生 查询指定科室的在岗医生列表 GET /api/doctor/list/department */
export async function listDoctorByDepartment(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.listDoctorByDepartmentParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseListDoctorVO>("/api/doctor/list/department", {
    method: "GET",
    params: {
      ...params,
    },
    ...(options || {}),
  });
}

/** 查询可预约科室 返回有在岗医生的科室列表 GET /api/doctor/list/departments */
export async function listDepartments(options?: { [key: string]: any }) {
  return request<API.BaseResponseListString>("/api/doctor/list/departments", {
    method: "GET",
    ...(options || {}),
  });
}

/** 更新医生信息 管理员更新医生信息 POST /api/doctor/update */
export async function updateDoctor(
  body: API.DoctorUpdateWithIdRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseDoctorVO>("/api/doctor/update", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 更新个人资料 医生更新自己的个人资料 POST /api/doctor/update/profile */
export async function updateProfile1(
  body: API.DoctorUpdateRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseDoctorVO>("/api/doctor/update/profile", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}
