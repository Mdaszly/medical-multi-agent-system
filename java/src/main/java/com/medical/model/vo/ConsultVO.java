package com.medical.model.vo;

import com.medical.constant.ConsultConstant;
import com.medical.knowledgegraph.model.dto.SymptomDiagnosisRow;
import com.medical.model.ClinicalState;
import com.medical.model.kg.GraphEvidence;
import com.medical.service.kg.KnowledgeEnrichmentService;
import com.medical.service.kg.symptom.SymptomMatch;
import lombok.Data;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Data
public class ConsultVO {

    private String sessionId;
    private String answer;
    private String riskLevel;
    private String recommendedDepartment;
    private String conclusion;
    private String reasoning;
    private List<String> redFlags = new ArrayList<>();
    private List<String> nextQuestions = new ArrayList<>();
    private List<String> careAdvice = new ArrayList<>();
    private String evidenceSummary;
    private String disclaimer = ConsultConstant.DISCLAIMER;
    private String agentType;
    private List<AgentTraceVO> agentTrace = new ArrayList<>();
    private List<String> errors = new ArrayList<>();

    /** 是否命中知识图谱（症状-疾病-ICD 有关联记录） */
    private Boolean graphHit;
    /** 面向前端的图谱命中说明（已命中 / 未命中） */
    private String graphHitMessage;
    /** 图谱证据详情（含术语解析轨迹、检索行） */
    private GraphEvidenceVO graphEvidenceDetail;
    private String groundingStatus;

    /** @deprecated 请使用 graphEvidenceDetail.rows */
    @Deprecated
    private List<SymptomDiagnosisRowVO> graphEvidence = new ArrayList<>();

    @SuppressWarnings("unchecked")
    public static ConsultVO fromClinicalState(ClinicalState state, String sessionId) {
        ConsultVO vo = new ConsultVO();
        vo.setSessionId(sessionId);
        vo.setAgentType(state.getCurrentAgent());
        vo.setErrors(new ArrayList<>(state.getErrors()));

        Object answer = state.getExtensions().get("answer");
        if (answer != null) {
            vo.setAnswer(answer.toString());
        }

        Map<String, Object> result = null;
        if (state.getExtensions().get("consultResult") instanceof Map<?, ?> map) {
            result = (Map<String, Object>) map;
        } else if (state.getDiagnosis() != null) {
            result = state.getDiagnosis();
        }

        if (result != null) {
            vo.setRiskLevel(stringVal(result.get("risk_level")));
            vo.setRecommendedDepartment(stringVal(result.get("recommended_department")));
            vo.setConclusion(stringVal(result.get("conclusion")));
            vo.setReasoning(stringVal(result.get("reasoning")));
            vo.setEvidenceSummary(stringVal(result.get("evidence_summary")));
            vo.setRedFlags(toStringList(result.get("red_flags")));
            vo.setNextQuestions(toStringList(result.get("next_questions")));
            vo.setCareAdvice(toStringList(result.get("care_advice")));
            if (vo.getAnswer() == null || vo.getAnswer().isBlank()) {
                vo.setAnswer(buildFallbackAnswer(vo));
            }
        }

        if (state.getExtensions().get("agentTrace") instanceof List<?> traceList) {
            for (Object item : traceList) {
                if (item instanceof Map<?, ?> map) {
                    AgentTraceVO trace = new AgentTraceVO();
                    trace.setAgent(stringVal(map.get("agent")));
                    trace.setAction(stringVal(map.get("action")));
                    trace.setDetail(stringVal(map.get("detail")));
                    vo.getAgentTrace().add(trace);
                }
            }
        }

        Object grounding = state.getExtensions().get(KnowledgeEnrichmentService.EXT_GROUNDING_STATUS);
        if (grounding != null) {
            vo.setGroundingStatus(grounding.toString());
        }

        GraphEvidenceVO detail = buildGraphEvidenceDetail(state);
        vo.setGraphEvidenceDetail(detail);
        vo.setGraphHit(detail.isGraphHit());
        vo.setGraphHitMessage(buildGraphHitMessage(detail, vo.getGroundingStatus()));
        vo.setGraphEvidence(new ArrayList<>(detail.getRows()));

        return vo;
    }

    @SuppressWarnings("unchecked")
    private static GraphEvidenceVO buildGraphEvidenceDetail(ClinicalState state) {
        Object detailRaw = state.getExtensions().get(KnowledgeEnrichmentService.EXT_GRAPH_DETAIL);
        if (detailRaw instanceof GraphEvidence evidence) {
            return GraphEvidenceVO.from(evidence);
        }

        List<SymptomDiagnosisRowVO> rows = new ArrayList<>();
        if (state.getExtensions().get(KnowledgeEnrichmentService.EXT_GRAPH_EVIDENCE) instanceof List<?> evidenceList) {
            for (Object item : evidenceList) {
                if (item instanceof SymptomDiagnosisRow row) {
                    rows.add(SymptomDiagnosisRowVO.from(row));
                }
            }
        }

        Boolean hit = state.getExtensions().get(KnowledgeEnrichmentService.EXT_GRAPH_HIT) instanceof Boolean b
                ? b
                : !rows.isEmpty();

        Set<String> extracted = new LinkedHashSet<>();
        List<SymptomMatchVO> matches = new ArrayList<>();
        Long queryTimeMs = null;
        String formattedText = null;
        String trace = null;

        if (detailRaw instanceof Map<?, ?> map) {
            Object symptoms = map.get("extractedSymptoms");
            if (symptoms instanceof List<?> list) {
                for (Object s : list) {
                    if (s != null) {
                        extracted.add(String.valueOf(s));
                    }
                }
            }
            trace = stringVal(map.get("symptomResolutionTrace"));
            formattedText = stringVal(map.get("formattedText"));
            Object qtm = map.get("queryTimeMs");
            if (qtm instanceof Number n) {
                queryTimeMs = n.longValue();
            }
        }

        GraphEvidenceVO vo = GraphEvidenceVO.fromRows(rows, extracted, trace, matches, queryTimeMs, formattedText);
        if (hit != null) {
            vo.setGraphHit(hit);
        }
        return vo;
    }

    private static String buildGraphHitMessage(GraphEvidenceVO detail, String groundingStatus) {
        if (detail == null || !detail.isGraphHit()) {
            if (detail != null && StringUtils.hasText(detail.getGraphSkipReason())) {
                return switch (detail.getGraphSkipReason()) {
                    case "NON_CLINICAL_UTTERANCE" ->
                            "【知识图谱·未触发】当前输入为寒暄或非主诉内容，未进行症状映射。"
                                    + "请在「症状描述」中补充具体不适，或继续对话说明哪里不舒服。";
                    case "NO_CLINICAL_SPAN" ->
                            "【知识图谱·未触发】未识别到可检索的临床症状。"
                                    + "请补充具体部位与表现（如「头痛 2 天、伴恶心」）。";
                    default ->
                            "【知识图谱·未命中】未在医学知识图谱中找到与您描述相匹配的症状-疾病-ICD 关联。"
                                    + "以下 AI 建议主要依据大模型通用医学知识生成，请谨慎参考并及时就医。";
                };
            }
            return "【知识图谱·未命中】未在医学知识图谱中找到与您描述相匹配的症状-疾病-ICD 关联。"
                    + "以下 AI 建议主要依据大模型通用医学知识生成，请谨慎参考并及时就医。";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("【知识图谱·已命中】");
        if (detail.getExtractedSymptoms() != null && !detail.getExtractedSymptoms().isEmpty()) {
            sb.append("已识别标准症状：").append(String.join("、", detail.getExtractedSymptoms())).append("；");
        }
        int rowCount = detail.getRows() != null ? detail.getRows().size() : 0;
        sb.append("检索到 ").append(rowCount).append(" 条症状-疾病-ICD 关联证据");
        if (StringUtils.hasText(detail.getSymptomResolutionTrace())) {
            sb.append("（").append(detail.getSymptomResolutionTrace()).append("）");
        }
        sb.append("。");
        if ("VERIFIED".equals(groundingStatus)) {
            sb.append(" ICD 编码已通过图谱校验。");
        } else if ("WARNING".equals(groundingStatus)) {
            sb.append(" 部分 ICD 引用未通过图谱校验，请以医生建议为准。");
        }
        return sb.toString();
    }

    private static String buildFallbackAnswer(ConsultVO vo) {
        StringBuilder sb = new StringBuilder();
        if (vo.getConclusion() != null) {
            sb.append(vo.getConclusion());
        }
        if (vo.getRecommendedDepartment() != null) {
            sb.append("\n建议科室：").append(vo.getRecommendedDepartment());
        }
        sb.append("\n\n").append(ConsultConstant.DISCLAIMER);
        return sb.toString();
    }

    private static String stringVal(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    @SuppressWarnings("unchecked")
    private static List<String> toStringList(Object value) {
        if (!(value instanceof List<?> list)) {
            return new ArrayList<>();
        }
        List<String> result = new ArrayList<>();
        for (Object item : list) {
            result.add(String.valueOf(item));
        }
        return result;
    }
}
