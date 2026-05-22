/** 问诊模块与 OpenAPI 类型统一导出 */

export type ChatSessionHistoryVO = API.ChatSessionHistoryVO
export type ChatSessionVO = API.ChatSessionVO
export type PatientContext = Record<string, unknown>

export type {
  ConsultResult,
  ConsultStreamEvent,
  ConsultVORaw,
  GraphEvidence,
  SymptomDiagnosisRow,
  SymptomMatch,
  AgentTrace,
} from './consultTypes'

export { mapConsultVo } from './consultTypes'
