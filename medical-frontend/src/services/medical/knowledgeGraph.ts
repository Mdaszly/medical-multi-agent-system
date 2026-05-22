/** 知识图谱 API 门面（供 SymptomInput、管理端等复用） */
import * as kg from './knowledgeGraphController'

interface ApiResponse<T> {
  code?: number
  data?: T
  message?: string
}

function unwrap<T>(res: ApiResponse<T>): T {
  if (res.code !== 0 && res.code !== undefined) {
    throw new Error(res.message || '请求失败')
  }
  return res.data as T
}

export async function getSuggestions(prefix: string, limit = 10): Promise<string[]> {
  const res = await kg.suggestSymptoms({ prefix, limit })
  const data = unwrap(res)
  return Array.isArray(data) ? data : []
}

export async function getGraphStatistics(): Promise<{
  nodes: number
  relations: number
  symptoms: number
  diseases: number
  nodeCount: number
  relationCount: number
  symptomCount: number
  diseaseCount: number
}> {
  const res = await kg.getStatistics()
  const data = unwrap<Record<string, number>>(res) || {}
  const nodes = data.nodes ?? data.nodeCount ?? 0
  const relations = data.relations ?? data.relationCount ?? 0
  const symptoms = data.symptoms ?? data.symptomCount ?? 0
  const diseases = data.diseases ?? data.diseaseCount ?? 0
  return {
    nodes,
    relations,
    symptoms,
    diseases,
    nodeCount: nodes,
    relationCount: relations,
    symptomCount: symptoms,
    diseaseCount: diseases,
  }
}

export async function queryDiagnosis(symptomName: string): Promise<{
  records: API.SymptomDiagnosisRowVO[]
}> {
  const res = await kg.findDiagnoses({ symptomName })
  const data = unwrap<API.QueryResultDTO>(res)
  const records = (data?.records ?? []) as API.SymptomDiagnosisRowVO[]
  return { records }
}

export async function syncData(): Promise<void> {
  await kg.syncToRdb()
}

export async function testConnection(): Promise<{ status: string }> {
  const res = await kg.test()
  const data = unwrap<Record<string, unknown>>(res)
  const ok =
    data?.success === true ||
    data?.status === 'ok' ||
    data?.status === 'UP' ||
    res.code === 0
  return { status: ok ? 'UP' : 'DOWN' }
}
