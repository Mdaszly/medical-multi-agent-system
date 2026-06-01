package com.medical.service.kg;

import com.medical.agent.enums.MedicalAgentType;
import com.medical.config.MedicalGraphProperties;
import com.medical.knowledgegraph.model.dto.SymptomDiagnosisRow;
import com.medical.model.ClinicalState;
import com.medical.model.kg.GraphEvidence;
import com.medical.service.kg.clinical.ClinicalSpanExtractionResult;
import com.medical.service.kg.clinical.ClinicalSpanExtractor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("KnowledgeEnrichmentService 知识增强服务测试")
class KnowledgeEnrichmentServiceTest {

    @Mock
    private KnowledgeGraphFacade knowledgeGraphFacade;

    @Mock
    private ClinicalSpanExtractor clinicalSpanExtractor;

    private KnowledgeEnrichmentService service;

    @BeforeEach
    void setUp() {
        MedicalGraphProperties props = new MedicalGraphProperties();
        props.setEnabled(true);
        props.setPreEnrich(true);
        service = new KnowledgeEnrichmentService(knowledgeGraphFacade, clinicalSpanExtractor, props);

        org.mockito.Mockito.lenient().when(clinicalSpanExtractor.extract(org.mockito.ArgumentMatchers.any()))
                .thenAnswer(invocation -> {
                    ClinicalState state = invocation.getArgument(0);
                    String text = state.getRawInput();
                    return ClinicalSpanExtractionResult.ok(text, java.util.List.of(text), "TEST", "test=" + text);
                });
    }

    @Test
    @DisplayName("非临床话语应跳过图谱检索")
    void enrich_nonClinical_skipsGraph() {
        ClinicalState state = ClinicalState.builder().rawInput("你好医生").build();
        when(clinicalSpanExtractor.extract(state)).thenReturn(
                ClinicalSpanExtractionResult.skipped("NON_CLINICAL_UTTERANCE", "非临床话语"));

        GraphEvidence result = service.enrich(state, MedicalAgentType.INITIAL);

        assertFalse(result.isGraphHit());
        assertEquals("NON_CLINICAL_UTTERANCE", result.getGraphSkipReason());
        assertEquals("NO_HIT", state.getExtensions().get(KnowledgeEnrichmentService.EXT_GROUNDING_STATUS));
    }

    @Test
    void supportsAgent_initialAgent_returnsTrue() {
        assertTrue(service.supportsAgent(MedicalAgentType.INITIAL));
    }

    @Test
    @DisplayName("FOLLOWUP agent 应该被支持")
    void supportsAgent_followupAgent_returnsTrue() {
        assertTrue(service.supportsAgent(MedicalAgentType.FOLLOWUP));
    }

    @Test
    @DisplayName("ROUTER agent 不应该被支持")
    void supportsAgent_routerAgent_returnsFalse() {
        assertFalse(service.supportsAgent(MedicalAgentType.ROUTER));
    }

    @Test
    @DisplayName("图谱禁用时所有 agent 都不应该被支持")
    void supportsAgent_graphDisabled_returnsFalse() {
        MedicalGraphProperties props = new MedicalGraphProperties();
        props.setEnabled(false);
        service = new KnowledgeEnrichmentService(knowledgeGraphFacade, props);
        
        assertFalse(service.supportsAgent(MedicalAgentType.INITIAL));
    }

    @Test
    @DisplayName("图谱命中时应该返回证据并设置 CANDIDATES_READY 状态")
    void enrich_graphHit_returnsEvidence() {
        ClinicalState state = ClinicalState.builder().rawInput("头痛").build();
        
        GraphEvidence evidence = GraphEvidence.builder()
                .graphHit(true)
                .rows(List.of(SymptomDiagnosisRow.builder()
                        .symptom("头痛")
                        .disease("偏头痛")
                        .icdCode("G43.909")
                        .build()))
                .icdCandidateCodes(Set.of("G43.909"))
                .build();
        evidence.setFormattedText("测试证据文本");
        
        when(knowledgeGraphFacade.extractAndQuery(any())).thenReturn(evidence);

        GraphEvidence result = service.enrich(state, MedicalAgentType.INITIAL);

        assertTrue(result.isGraphHit());
        assertEquals("CANDIDATES_READY", state.getExtensions().get(KnowledgeEnrichmentService.EXT_GROUNDING_STATUS));
        assertNotNull(state.getExtensions().get(KnowledgeEnrichmentService.EXT_GRAPH_EVIDENCE));
        assertNotNull(state.getExtensions().get(KnowledgeEnrichmentService.EXT_ICD_CANDIDATES));
    }

    @Test
    @DisplayName("图谱未命中时应该设置 NO_HIT 状态")
    void enrich_noGraphHit_setsNoHitStatus() {
        ClinicalState state = ClinicalState.builder().rawInput("未知症状").build();
        
        GraphEvidence evidence = GraphEvidence.builder()
                .graphHit(false)
                .rows(List.of())
                .build();
        
        when(knowledgeGraphFacade.extractAndQuery(any())).thenReturn(evidence);

        GraphEvidence result = service.enrich(state, MedicalAgentType.INITIAL);

        assertFalse(result.isGraphHit());
        assertEquals("NO_HIT", state.getExtensions().get(KnowledgeEnrichmentService.EXT_GROUNDING_STATUS));
    }

    @Test
    @DisplayName("不支持的 agent 类型应该返回空证据")
    void enrich_notSupportedAgent_returnsEmptyEvidence() {
        ClinicalState state = ClinicalState.builder().rawInput("头痛").build();

        GraphEvidence result = service.enrich(state, MedicalAgentType.ROUTER);

        assertFalse(result.isGraphHit());
    }

    @Test
    @DisplayName("图谱禁用时 enrich 应该返回空证据")
    void enrich_graphDisabled_returnsEmptyEvidence() {
        MedicalGraphProperties props = new MedicalGraphProperties();
        props.setEnabled(false);
        service = new KnowledgeEnrichmentService(knowledgeGraphFacade, props);
        
        ClinicalState state = ClinicalState.builder().rawInput("头痛").build();

        GraphEvidence result = service.enrich(state, MedicalAgentType.INITIAL);

        assertFalse(result.isGraphHit());
    }

    @Test
    @DisplayName("应该正确追加 toolContext")
    void enrich_appendsToolContext() {
        ClinicalState state = ClinicalState.builder()
                .rawInput("头痛")
                .extensions(new HashMap<>())
                .build();
        
        GraphEvidence evidence = GraphEvidence.builder()
                .graphHit(true)
                .rows(List.of(SymptomDiagnosisRow.builder()
                        .symptom("头痛")
                        .disease("偏头痛")
                        .icdCode("G43.909")
                        .build()))
                .icdCandidateCodes(Set.of("G43.909"))
                .build();
        evidence.setFormattedText("【知识图谱检索结果】\n症状: 头痛 | 疾病: 偏头痛 | ICD: G43.909");
        
        when(knowledgeGraphFacade.extractAndQuery(any())).thenReturn(evidence);

        service.enrich(state, MedicalAgentType.INITIAL);

        assertNotNull(state.getExtensions().get(KnowledgeEnrichmentService.EXT_TOOL_CONTEXT));
        assertTrue(state.getExtensions().get(KnowledgeEnrichmentService.EXT_TOOL_CONTEXT).toString().contains("头痛"));
    }

    @Test
    @DisplayName("应该正确记录 agentTrace")
    void enrich_recordsAgentTrace() {
        ClinicalState state = ClinicalState.builder()
                .rawInput("头痛")
                .extensions(new HashMap<>())
                .build();
        
        GraphEvidence evidence = GraphEvidence.builder()
                .graphHit(true)
                .rows(List.of(SymptomDiagnosisRow.builder()
                        .symptom("头痛")
                        .disease("偏头痛")
                        .icdCode("G43.909")
                        .build()))
                .icdCandidateCodes(Set.of("G43.909"))
                .queryTimeMs(50)
                .build();
        evidence.setFormattedText("测试证据");
        
        when(knowledgeGraphFacade.extractAndQuery(any())).thenReturn(evidence);

        service.enrich(state, MedicalAgentType.INITIAL);

        @SuppressWarnings("unchecked")
        List<Map<String, String>> trace = (List<Map<String, String>>) state.getExtensions().get("agentTrace");
        assertNotNull(trace);
        assertFalse(trace.isEmpty());
        
        Map<String, String> lastTrace = trace.get(trace.size() - 1);
        assertEquals("KnowledgeGraph", lastTrace.get("agent"));
        assertEquals("enrich", lastTrace.get("action"));
    }
}