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

/**
 * 问诊前知识图谱预增强：从用户原文抽取症状、查询 Neo4j，将证据写入 {@link ClinicalState} extensions。
 * <p>与 {@link com.medical.agent.AbstractStructuredConsultAgent} 配合，结果进入 {@code toolContext} 供 LLM 引用。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeEnrichmentService {

    /** extensions：拼接给 LLM 的工具/图谱文本 */
    public static final String EXT_TOOL_CONTEXT = "toolContext";
    /** extensions：图谱行数据列表 */
    public static final String EXT_GRAPH_EVIDENCE = "graphEvidence";
    /** extensions：是否命中图谱 */
    public static final String EXT_GRAPH_HIT = "graphHit";
    /** extensions：完整 {@link GraphEvidence} 对象 */
    public static final String EXT_GRAPH_DETAIL = "graphEvidenceDetail";
    /** extensions：ICD 候选编码集合 */
    public static final String EXT_ICD_CANDIDATES = "icdCandidateCodes";
    /** extensions：ICD  grounding 状态（如 NO_HIT / CANDIDATES_READY） */
    public static final String EXT_GROUNDING_STATUS = "groundingStatus";

    private final KnowledgeGraphFacade knowledgeGraphFacade;
    private final MedicalGraphProperties graphProperties;

    /**
     * boolean supportsAgent(MedicalAgentType agentType)
     * <p>配置开启 pre-enrich 且 Agent 类型在允许列表内时返回 true。</p>
     */
    public boolean supportsAgent(MedicalAgentType agentType) {
        if (!graphProperties.isEnabled() || !graphProperties.isPreEnrich()) {
            return false;
        }
        return switch (agentType) {
            case INITIAL, FOLLOWUP, HEALTH, MEDICATION, APPOINTMENT -> true;
            default -> false;
        };
    }

    /**
     * GraphEvidence enrich(ClinicalState state, MedicalAgentType agentType)
     * <p>对原文做图谱检索并将格式化证据、ICD 候选写入 state；不支持时返回空证据。</p>
     */
    public GraphEvidence enrich(ClinicalState state, MedicalAgentType agentType) {
        if (!supportsAgent(agentType)) {
            return GraphEvidence.builder().graphHit(false).build();
        }
        GraphEvidence evidence = knowledgeGraphFacade.extractAndQuery(state.getRawInput());
        applyToState(state, evidence, agentType);
        return evidence;
    }

    /**
     * void applyToState(ClinicalState state, GraphEvidence evidence, MedicalAgentType agentType)
     * <p>将检索结果映射到 extensions 并追加 agentTrace。</p>
     */
    private void applyToState(ClinicalState state, GraphEvidence evidence, MedicalAgentType agentType) {
        appendToolContext(state, evidence.getFormattedText());
        state.getExtensions().put(EXT_GRAPH_EVIDENCE, evidence.getRows());
        state.getExtensions().put(EXT_GRAPH_HIT, evidence.isGraphHit());
        state.getExtensions().put(EXT_GRAPH_DETAIL, evidence);
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

    /**
     * void appendToolContext(ClinicalState state, String text)
     * <p>将图谱文本追加到 {@link #EXT_TOOL_CONTEXT}，与 Agent 自有工具结果合并。</p>
     */
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

    /**
     * void appendTrace(ClinicalState state, String agent, String action, String detail)
     * <p>向 {@code agentTrace} 列表追加一条可观测性记录。</p>
     */
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
