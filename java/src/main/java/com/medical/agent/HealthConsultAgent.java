package com.medical.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medical.agent.enums.MedicalAgentType;
import com.medical.model.ClinicalState;
import com.medical.service.LlmService;
import com.medical.service.MedicalKnowledgeService;
import org.springframework.stereotype.Component;

@Component
public class HealthConsultAgent extends AbstractStructuredConsultAgent {

    private static final String SYSTEM_PROMPT = """
        你是一位健康科普顾问。提供生活方式、预防保健方面的科学建议，避免诊断与处方。
        
        只返回 JSON：
        {
          "risk_level": "低风险/中风险/高风险",
          "recommended_department": "如需就诊的科室或全科",
          "conclusion": "健康建议摘要",
          "reasoning": "依据",
          "red_flags": ["需就医信号"],
          "next_questions": ["可进一步了解的问题"],
          "care_advice": ["具体健康建议"],
          "evidence_summary": "证据摘要"
        }
        """;

    private final MedicalKnowledgeService knowledgeService;

    public HealthConsultAgent(LlmService llmService, ObjectMapper objectMapper,
                              MedicalKnowledgeService knowledgeService) {
        super(llmService, objectMapper);
        this.knowledgeService = knowledgeService;
    }

    @Override
    protected MedicalAgentType agentType() {
        return MedicalAgentType.HEALTH;
    }

    @Override
    protected String systemPrompt() {
        return SYSTEM_PROMPT;
    }

    @Override
    protected void enrichAgentSpecificContext(ClinicalState state) {
        String knowledge = knowledgeService.search(state.getRawInput());
        appendToolContext(state, "健康知识库：\n" + knowledge);
        appendTrace(state, "Tool", "searchMedicalKnowledge", "检索健康科普知识");
    }

    private void appendToolContext(ClinicalState state, String text) {
        Object existing = state.getExtensions().get("toolContext");
        String merged = existing instanceof String s && !s.isBlank() ? s + "\n\n" + text : text;
        state.getExtensions().put("toolContext", merged);
    }
}
