/** 问诊 API 与 UI 共用类型（与后端 ConsultVO / GraphEvidenceVO 对齐） */

export interface SymptomDiagnosisRow {
  symptom?: string
  disease?: string
  diseaseCode?: string
  icdCode?: string
  icdDescription?: string
  weight?: number
}

export interface SymptomMatch {
  userPhrase?: string
  canonicalName?: string
  symptomCode?: string
  confidence?: number
  method?: string
  rationale?: string
}

export interface GraphEvidence {
  rows?: SymptomDiagnosisRow[]
  extractedSymptoms?: string[]
  icdCandidateCodes?: string[]
  graphHit?: boolean
  queryTimeMs?: number
  formattedText?: string
  symptomResolutionTrace?: string
  symptomMatches?: SymptomMatch[]
  clinicalTextUsed?: string
  clinicalSpanSource?: string
  graphSkipReason?: string
}

export interface AgentTrace {
  agent?: string
  action?: string
  detail?: string
  timestamp?: string | Date
}

/** 后端 ConsultVO 原始结构 */
export interface ConsultVORaw {
  sessionId?: string
  answer?: string
  riskLevel?: string
  recommendedDepartment?: string
  conclusion?: string
  reasoning?: string
  redFlags?: string[]
  nextQuestions?: string[]
  careAdvice?: string[]
  evidenceSummary?: string
  disclaimer?: string
  agentType?: string
  agentTrace?: AgentTrace[]
  errors?: string[]
  graphHit?: boolean
  graphHitMessage?: string
  graphEvidenceDetail?: GraphEvidence
  graphEvidence?: SymptomDiagnosisRow[]
  groundingStatus?: string
}

/** 前端问诊结果（ConsultResultCard / 图谱面板使用） */
export interface ConsultResult {
  sessionId?: string
  answer?: string
  riskLevel?: string
  department?: string
  conclusion?: string
  reasoning?: string
  redFlags?: string[]
  nextQuestions?: string[]
  careAdvice?: string[]
  suggestions?: string[]
  evidenceSummary?: string
  disclaimer?: string
  agentType?: string
  agentTrace?: AgentTrace[]
  errors?: string[]
  graphHit: boolean
  graphHitMessage: string
  graphEvidence?: GraphEvidence
  groundingStatus?: string
}

export type ConsultStreamEvent =
  | { type: 'content'; content: string }
  | { type: 'done'; data: ConsultResult }
  | { type: 'error'; error: string }

export function mapConsultVo(raw: ConsultVORaw): ConsultResult {
  const detail = raw.graphEvidenceDetail
  const graphHit = raw.graphHit ?? detail?.graphHit ?? (detail?.rows?.length ?? 0) > 0
  const graphEvidence: GraphEvidence = detail ?? {
    graphHit,
    rows: raw.graphEvidence ?? [],
    extractedSymptoms: [],
    symptomMatches: [],
  }

  if (graphEvidence.graphHit === undefined) {
    graphEvidence.graphHit = graphHit
  }

  const suggestions = [
    ...(raw.careAdvice ?? []),
    ...(raw.redFlags?.length ? [`请注意：${raw.redFlags.join('；')}`] : []),
  ]

  return {
    sessionId: raw.sessionId,
    answer: raw.answer,
    riskLevel: raw.riskLevel,
    department: raw.recommendedDepartment,
    conclusion: raw.conclusion,
    reasoning: raw.reasoning,
    redFlags: raw.redFlags,
    nextQuestions: raw.nextQuestions,
    careAdvice: raw.careAdvice,
    suggestions,
    evidenceSummary: raw.evidenceSummary,
    disclaimer: raw.disclaimer,
    agentType: raw.agentType,
    agentTrace: raw.agentTrace,
    errors: raw.errors,
    graphHit,
    graphHitMessage:
      raw.graphHitMessage ??
      (graphHit
        ? '【知识图谱·已命中】已关联到医学知识图谱中的症状-疾病证据。'
        : '【知识图谱·未命中】未找到匹配的症状-疾病关联，建议仅供参考。'),
    graphEvidence,
    groundingStatus: raw.groundingStatus,
  }
}
