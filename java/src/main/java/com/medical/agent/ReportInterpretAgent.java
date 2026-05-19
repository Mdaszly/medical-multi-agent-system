package com.medical.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medical.agent.enums.MedicalAgentType;
import com.medical.model.ClinicalState;
import com.medical.service.LlmService;
import com.medical.service.MedicalKnowledgeService;
import org.springframework.stereotype.Component;

@Component
public class ReportInterpretAgent extends AbstractStructuredConsultAgent {

    private static final String SYSTEM_PROMPT = """
        你是一位检验检查报告解读助手。用通俗语言解释报告异常项的可能意义与建议复查方向。
        不要给出确诊，强调需由医生结合临床判断。
        
        只返回 JSON：
        {
          "risk_level": "低风险/中风险/高风险",
          "recommended_department": "建议就诊科室",
          "conclusion": "报告解读摘要",
          "reasoning": "依据",
          "red_flags": ["需尽快就医指标"],
          "next_questions": ["需补充检查"],
          "care_advice": ["生活建议"],
          "evidence_summary": "证据摘要",
          "abnormal_items": [{"item":"项目","meaning":"含义","suggestion":"建议"}]
        }
        """;

    private final MedicalKnowledgeService knowledgeService;

    public ReportInterpretAgent(LlmService llmService, ObjectMapper objectMapper,
                                MedicalKnowledgeService knowledgeService) {
        super(llmService, objectMapper);
        this.knowledgeService = knowledgeService;
    }

    @Override
    protected MedicalAgentType agentType() {
        return MedicalAgentType.REPORT;
    }

    @Override
    protected String systemPrompt() {
        return SYSTEM_PROMPT;
    }

    @Override
    protected void enrichAgentSpecificContext(ClinicalState state) {
        String knowledge = knowledgeService.search(state.getRawInput());
        Object existing = state.getExtensions().get("toolContext");
        String merged = existing instanceof String s && !s.isBlank()
                ? s + "\n\n医学知识库：\n" + knowledge
                : "医学知识库：\n" + knowledge;
        state.getExtensions().put("toolContext", merged);
        appendTrace(state, "Tool", "searchMedicalKnowledge", "检索报告相关知识点");
    }
}
