package com.medical.model.vo;

import com.medical.constant.ConsultConstant;
import com.medical.model.ClinicalState;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
        return vo;
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
