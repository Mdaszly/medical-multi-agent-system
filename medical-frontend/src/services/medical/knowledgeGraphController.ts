// @ts-ignore
/* eslint-disable */
import request from "../request";

/** 此处后端没有提供注释 DELETE /api/knowledge-graph/clear */
export async function clearAll(options?: { [key: string]: any }) {
  return request<Record<string, any>>("/api/knowledge-graph/clear", {
    method: "DELETE",
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /api/knowledge-graph/constraint */
export async function createConstraint(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.createConstraintParams,
  options?: { [key: string]: any }
) {
  return request<Record<string, any>>("/api/knowledge-graph/constraint", {
    method: "POST",
    params: {
      ...params,
    },
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 GET /api/knowledge-graph/diagnosis/${param0} */
export async function findDiagnoses(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.findDiagnosesParams,
  options?: { [key: string]: any }
) {
  const { symptomName: param0, ...queryParams } = params;
  return request<API.QueryResultDTO>(
    `/api/knowledge-graph/diagnosis/${param0}`,
    {
      method: "GET",
      params: { ...queryParams },
      ...(options || {}),
    }
  );
}

/** 此处后端没有提供注释 GET /api/knowledge-graph/drug-indications/${param0} */
export async function findDrugIndications(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.findDrugIndicationsParams,
  options?: { [key: string]: any }
) {
  const { drugName: param0, ...queryParams } = params;
  return request<API.QueryResultDTO>(
    `/api/knowledge-graph/drug-indications/${param0}`,
    {
      method: "GET",
      params: { ...queryParams },
      ...(options || {}),
    }
  );
}

/** 此处后端没有提供注释 POST /api/knowledge-graph/extract */
export async function extractEntities(
  body: Record<string, any>,
  options?: { [key: string]: any }
) {
  return request<Record<string, any>>("/api/knowledge-graph/extract", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /api/knowledge-graph/extract-from-record */
export async function extractFromMedicalRecord(
  body: Record<string, any>,
  options?: { [key: string]: any }
) {
  return request<Record<string, any>>(
    "/api/knowledge-graph/extract-from-record",
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

/** 此处后端没有提供注释 GET /api/knowledge-graph/health */
export async function health1(options?: { [key: string]: any }) {
  return request<Record<string, any>>("/api/knowledge-graph/health", {
    method: "GET",
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /api/knowledge-graph/import */
export async function importFile(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.importFileParams,
  body: {},
  options?: { [key: string]: any }
) {
  return request<API.ImportTaskDTO>("/api/knowledge-graph/import", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    params: {
      // skipHeader has a default value: true
      skipHeader: "true",
      // delimiter has a default value: ,
      delimiter: ",",
      ...params,
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 GET /api/knowledge-graph/import/${param0} */
export async function getImportTaskStatus(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getImportTaskStatusParams,
  options?: { [key: string]: any }
) {
  const { taskId: param0, ...queryParams } = params;
  return request<API.ImportTaskDTO>(`/api/knowledge-graph/import/${param0}`, {
    method: "GET",
    params: { ...queryParams },
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /api/knowledge-graph/index */
export async function createIndex(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.createIndexParams,
  options?: { [key: string]: any }
) {
  return request<Record<string, any>>("/api/knowledge-graph/index", {
    method: "POST",
    params: {
      ...params,
    },
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 GET /api/knowledge-graph/node/${param0}/${param1} */
export async function findNodeByName(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.findNodeByNameParams,
  options?: { [key: string]: any }
) {
  const { label: param0, name: param1, ...queryParams } = params;
  return request<API.QueryResultDTO>(
    `/api/knowledge-graph/node/${param0}/${param1}`,
    {
      method: "GET",
      params: { ...queryParams },
      ...(options || {}),
    }
  );
}

/** 此处后端没有提供注释 GET /api/knowledge-graph/paths */
export async function findPaths(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.findPathsParams,
  options?: { [key: string]: any }
) {
  return request<API.QueryResultDTO>("/api/knowledge-graph/paths", {
    method: "GET",
    params: {
      // maxDepth has a default value: 5
      maxDepth: "5",
      ...params,
    },
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /api/knowledge-graph/query */
export async function executeQuery(
  body: Record<string, any>,
  options?: { [key: string]: any }
) {
  return request<API.QueryResultDTO>("/api/knowledge-graph/query", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 GET /api/knowledge-graph/relations */
export async function findNodeRelationsByQuery(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.findNodeRelationsByQueryParams,
  options?: { [key: string]: any }
) {
  return request<API.QueryResultDTO>("/api/knowledge-graph/relations", {
    method: "GET",
    params: {
      // depth has a default value: 1
      depth: "1",
      ...params,
    },
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 GET /api/knowledge-graph/relations/${param0}/${param1} */
export async function findNodeRelations(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.findNodeRelationsParams,
  options?: { [key: string]: any }
) {
  const { label: param0, name: param1, ...queryParams } = params;
  return request<API.QueryResultDTO>(
    `/api/knowledge-graph/relations/${param0}/${param1}`,
    {
      method: "GET",
      params: {
        // depth has a default value: 1
        depth: "1",
        ...queryParams,
      },
      ...(options || {}),
    }
  );
}

/** 此处后端没有提供注释 GET /api/knowledge-graph/statistics */
export async function getStatistics(options?: { [key: string]: any }) {
  return request<Record<string, any>>("/api/knowledge-graph/statistics", {
    method: "GET",
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 GET /api/knowledge-graph/symptoms/suggest */
export async function suggestSymptoms(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.suggestSymptomsParams,
  options?: { [key: string]: any }
) {
  return request<string[]>("/api/knowledge-graph/symptoms/suggest", {
    method: "GET",
    params: {
      // limit has a default value: 10
      limit: "10",
      ...params,
    },
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /api/knowledge-graph/sync-to-rdb */
export async function syncToRdb(options?: { [key: string]: any }) {
  return request<Record<string, any>>("/api/knowledge-graph/sync-to-rdb", {
    method: "POST",
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 GET /api/knowledge-graph/test */
export async function test(options?: { [key: string]: any }) {
  return request<Record<string, any>>("/api/knowledge-graph/test", {
    method: "GET",
    ...(options || {}),
  });
}
