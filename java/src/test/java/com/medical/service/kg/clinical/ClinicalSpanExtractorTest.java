package com.medical.service.kg.clinical;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medical.config.MedicalGraphProperties;
import com.medical.knowledgegraph.service.extraction.EntityExtractionService;
import com.medical.model.ClinicalState;
import com.medical.service.kg.symptom.SymptomSynonymRegistry;
import com.medical.service.kg.symptom.SymptomVocabularyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClinicalSpanExtractorTest {

    @Mock
    private SymptomVocabularyService vocabularyService;
    @Mock
    private EntityExtractionService entityExtractionService;
    @Mock
    private ClinicalSpanLlmExtractor llmExtractor;

    private ClinicalSpanExtractor extractor;
    private SymptomSynonymRegistry synonymRegistry;

    @BeforeEach
    void setUp() {
        MedicalGraphProperties props = new MedicalGraphProperties();
        props.getClinicalSpan().setEnabled(true);
        props.getClinicalSpan().setLlmEnabled(false);

        synonymRegistry = new SymptomSynonymRegistry(new ObjectMapper());
        synonymRegistry.load();

        extractor = new ClinicalSpanExtractor(
                props,
                new NonClinicalPhraseFilter(),
                synonymRegistry,
                vocabularyService,
                entityExtractionService,
                llmExtractor);

        when(vocabularyService.getCachedVocabulary()).thenReturn(List.of());
        when(entityExtractionService.extractSymptoms(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(List.of());
    }

    @Test
    @DisplayName("你好医生应被识别为非临床话语并跳过")
    void extract_greeting_skipsGraph() {
        ClinicalState state = ClinicalState.builder().rawInput("你好医生").build();
        ClinicalSpanExtractionResult result = extractor.extract(state);

        assertFalse(result.isHasClinicalText());
        assertEquals("NON_CLINICAL_UTTERANCE", result.getSkipReason());
    }

    @Test
    @DisplayName("结构化症状字段优先于聊天寒暄")
    void extract_structuredSymptomField_priority() {
        Map<String, Object> ctx = new HashMap<>();
        ctx.put("symptom", "头疼");
        ClinicalState state = ClinicalState.builder()
                .rawInput("你好医生")
                .build();
        state.getExtensions().put("patientContext", ctx);

        ClinicalSpanExtractionResult result = extractor.extract(state);

        assertTrue(result.isHasClinicalText());
        assertEquals("STRUCTURED_FIELD", result.getSource());
        assertTrue(result.getClinicalText().contains("头疼"));
    }

    @Test
    @DisplayName("混合句应只抽取症状片段")
    void extract_mixedUtterance_extractsSymptomOnly() {
        ClinicalState state = ClinicalState.builder()
                .rawInput("医生你好，我这两天老是头疼")
                .build();

        ClinicalSpanExtractionResult result = extractor.extract(state);

        assertTrue(result.isHasClinicalText());
        assertTrue(result.getSymptomSpans().stream().anyMatch(s -> s.contains("头疼")));
    }

    @Test
    @DisplayName("纯非临床文本且无结构化主诉应跳过")
    void extract_appointmentIntent_skips() {
        ClinicalState state = ClinicalState.builder().rawInput("想预约下周体检").build();
        ClinicalSpanExtractionResult result = extractor.extract(state);

        assertFalse(result.isHasClinicalText());
    }
}
