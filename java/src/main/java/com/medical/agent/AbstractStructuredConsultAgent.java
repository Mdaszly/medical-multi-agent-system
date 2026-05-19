package com.medical.agent;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.medical.agent.enums.MedicalAgentType;
import com.medical.constant.ConsultConstant;
import com.medical.model.ClinicalState;
import com.medical.service.LlmService;
import com.medical.service.kg.KnowledgeEnrichmentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public abstract class AbstractStructuredConsultAgent extends BaseMedicalAgent {

    @Autowired(required = false)
    protected KnowledgeEnrichmentService knowledgeEnrichmentService;

    protected AbstractStructuredConsultAgent(LlmService llmService, ObjectMapper objectMapper) {
        super(llmService, objectMapper);
    }

    protected abstract MedicalAgentType agentType();

    protected abstract String systemPrompt();

    protected void enrichContext(ClinicalState state) {
        enrichGraphContext(state);
        enrichAgentSpecificContext(state);
    }

    protected void enrichGraphContext(ClinicalState state) {
        if (knowledgeEnrichmentService != null) {
            knowledgeEnrichmentService.enrich(state, agentType());
        }
    }

    /** 子类追加场景化工具检索 */
    protected void enrichAgentSpecificContext(ClinicalState state) {
    }

    /** 流式问诊路由后、调用 LLM 前执行工具增强 */
    public void enrichContextForStream(ClinicalState state) {
        enrichContext(state);
    }

    @Override
    public MedicalAgentType getAgentType() {
        return agentType();
    }

    @Override
    public String getSystemMessage() {
        return systemPrompt();
    }

    @Override
    public ClinicalState process(ClinicalState state) {
        log.info("{} processing", agentType().getCode());
        state.setCurrentAgent(agentType().getCode());
        appendTrace(state, agentType().getName(), "reason", "执行结构化问诊推理");

        try {
            enrichContext(state);
            String response = generate(systemPrompt(), buildUserPrompt(state));
            applyLlmResponse(state, response);
        } catch (Exception e) {
            log.error("{} error", agentType().getCode(), e);
            state.getErrors().add(agentType().getName() + " error: " + e.getMessage());
        }
        return state;
    }

    public void applyLlmResponse(ClinicalState state, String response) throws Exception {
        state.setCurrentAgent(agentType().getCode());
        String content = cleanJsonResponse(response);
        Map<String, Object> consultResult = objectMapper.readValue(content, new TypeReference<>() {});

        state.setDiagnosis(consultResult);
        state.getExtensions().put("consultResult", consultResult);
        state.getExtensions().put("answer", renderAnswer(consultResult, state));

        appendTrace(state, agentType().getName(), "complete",
                "风险等级: " + consultResult.getOrDefault("risk_level", "未知"));
    }

    protected String buildUserPrompt(ClinicalState state) {
        StringBuilder sb = new StringBuilder();
        Object history = state.getExtensions().get("chatHistory");
        if (history instanceof String historyText && StringUtils.hasText(historyText)) {
            sb.append(historyText).append("\n");
        }
        Object toolContext = state.getExtensions().get("toolContext");
        if (toolContext instanceof String toolText && StringUtils.hasText(toolText)) {
            sb.append("工具检索结果：\n").append(toolText).append("\n");
        }
        sb.append("患者描述：\n").append(state.getRawInput()).append("\n");

        Object patientContext = state.getExtensions().get("patientContext");
        if (patientContext instanceof Map<?, ?> ctx && !ctx.isEmpty()) {
            sb.append("\n患者背景：\n").append(ctx).append("\n");
        }
        return sb.toString();
    }

    protected String renderAnswer(Map<String, Object> result, ClinicalState state) {
        String conclusion = String.valueOf(result.getOrDefault("conclusion", ""));
        String department = String.valueOf(result.getOrDefault("recommended_department", "全科"));
        String risk = String.valueOf(result.getOrDefault("risk_level", "低风险"));
        List<String> advice = toStringList(result.get("care_advice"));
        List<String> redFlags = toStringList(result.get("red_flags"));
        List<String> nextQuestions = toStringList(result.get("next_questions"));

        StringBuilder answer = new StringBuilder();
        answer.append("【").append(risk).append("】").append(conclusion).append("\n\n");
        if (StringUtils.hasText(department) && !"null".equals(department)) {
            answer.append("建议就诊科室：").append(department).append("\n\n");
        }
        if (!advice.isEmpty()) {
            answer.append("健康建议：\n");
            for (String item : advice) {
                answer.append("- ").append(item).append("\n");
            }
        }
        if (!redFlags.isEmpty()) {
            answer.append("\n请注意：\n");
            for (String flag : redFlags) {
                answer.append("- ").append(flag).append("\n");
            }
        }
        if (!nextQuestions.isEmpty()) {
            answer.append("\n建议补充信息：\n");
            for (String q : nextQuestions) {
                answer.append("- ").append(q).append("\n");
            }
        }
        if (StringUtils.hasText(String.valueOf(result.get("reasoning")))) {
            answer.append("\n参考依据：").append(result.get("reasoning"));
        }
        answer.append("\n\n").append(ConsultConstant.DISCLAIMER);
        return answer.toString();
    }

    @SuppressWarnings("unchecked")
    protected void appendTrace(ClinicalState state, String agent, String action, String detail) {
        List<Map<String, String>> trace = (List<Map<String, String>>) state.getExtensions()
                .computeIfAbsent("agentTrace", k -> new ArrayList<>());
        Map<String, String> item = new HashMap<>();
        item.put("agent", agent);
        item.put("action", action);
        item.put("detail", detail);
        trace.add(item);
    }

    @SuppressWarnings("unchecked")
    protected List<String> toStringList(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        return list.stream().map(String::valueOf).collect(Collectors.toList());
    }
}
