package com.medical.service.kg;

import com.medical.agent.enums.MedicalAgentType;
import com.medical.config.MedicalGraphProperties;
import com.medical.model.ClinicalState;
import com.medical.model.kg.GraphEvidence;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeEnrichmentService {

    public static final String EXT_TOOL_CONTEXT = "toolContext";
    public static final String EXT_GRAPH_EVIDENCE = "graphEvidence";
    public static final String EXT_ICD_CANDIDATES = "icdCandidateCodes";
    public static final String EXT_GROUNDING_STATUS = "groundingStatus";

    private final KnowledgeGraphFacade knowledgeGraphFacade;
    private final MedicalGraphProperties graphProperties;

    public boolean supportsAgent(MedicalAgentType agentType) {
        if (!graphProperties.isEnabled() || !graphProperties.isPreEnrich()) {
            return false;
        }
        return switch (agentType) {
            case INITIAL, FOLLOWUP, HEALTH, MEDICATION, APPOINTMENT -> true;
            default -> false;
        };
    }

    public GraphEvidence enrich(ClinicalState state, MedicalAgentType agentType) {
        if (!supportsAgent(agentType)) {
            return GraphEvidence.builder().graphHit(false).build();
        }
        GraphEvidence evidence = knowledgeGraphFacade.extractAndQuery(state.getRawInput());
        applyToState(state, evidence, agentType);
        return evidence;
    }

    private void applyToState(ClinicalState state, GraphEvidence evidence, MedicalAgentType agentType) {
        appendToolContext(state, evidence.getFormattedText());
        state.getExtensions().put(EXT_GRAPH_EVIDENCE, evidence.getRows());
        state.getExtensions().put(EXT_ICD_CANDIDATES, new ArrayList<>(evidence.getIcdCandidateCodes()));
        if (!evidence.isGraphHit()) {
            state.getExtensions().put(EXT_GROUNDING_STATUS, "NO_HIT");
        } else {
            state.getExtensions().put(EXT_GROUNDING_STATUS, "CANDIDATES_READY");
        }
        appendTrace(state, "KnowledgeGraph", "enrich",
                "命中 " + evidence.getRows().size() + " 条, 耗时 " + evidence.getQueryTimeMs() + "ms, agent="
                        + agentType.getCode());
    }

    private void appendToolContext(ClinicalState state, String text) {
        if (!StringUtils.hasText(text)) {
            return;
        }
        Object existing = state.getExtensions().get(EXT_TOOL_CONTEXT);
        String merged = existing instanceof String s && StringUtils.hasText(s)
                ? s + "\n\n" + text
                : text;
        state.getExtensions().put(EXT_TOOL_CONTEXT, merged);
    }

    @SuppressWarnings("unchecked")
    private void appendTrace(ClinicalState state, String agent, String action, String detail) {
        List<Map<String, String>> trace = (List<Map<String, String>>) state.getExtensions()
                .computeIfAbsent("agentTrace", k -> new ArrayList<>());
        Map<String, String> item = new HashMap<>();
        item.put("agent", agent);
        item.put("action", action);
        item.put("detail", detail);
        trace.add(item);
    }
}
