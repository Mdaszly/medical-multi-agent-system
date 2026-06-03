package com.medical.service.kg.symptom.benchmark;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medical.service.kg.symptom.SymptomMatch;
import com.medical.service.kg.symptom.SymptomSynonymRegistry;
import com.medical.service.kg.symptom.SymptomVocabularyEntry;
import com.medical.service.kg.symptom.SymptomVocabularyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SymptomSynonymOnlyEvaluatorTest {

    @Mock
    private SymptomSynonymRegistry synonymRegistry;

    @Mock
    private SymptomVocabularyService vocabularyService;

    private SymptomSynonymOnlyEvaluator evaluator;

    @BeforeEach
    void setUp() {
        VectorEvalDatasetLoader loader = new VectorEvalDatasetLoader(new ObjectMapper());
        evaluator = new SymptomSynonymOnlyEvaluator(loader, synonymRegistry, vocabularyService);
        when(synonymRegistry.getAliasToCanonical()).thenReturn(Map.of("脑袋胀�?, "头痛"));
    }

    @Test
    void evaluate_synonymExactMatch() {
        SymptomVocabularyEntry headache = SymptomVocabularyEntry.builder().name("头痛").code("S001").build();
        when(vocabularyService.getCachedVocabulary()).thenReturn(List.of(headache));
        when(synonymRegistry.resolveExact("脑袋胀�?)).thenReturn(Optional.of(
                SymptomMatch.builder().canonicalName("头痛").method("SYNONYM").confidence(0.98).build()));

        VectorEvalDataset dataset = new VectorEvalDataset();
        dataset.setVersion("test");
        VectorEvalCase c = new VectorEvalCase();
        c.setId("t1");
        c.setBucket("oral_paraphrase");
        c.setQuery("脑袋胀�?);
        c.setExpected(List.of("头痛"));
        dataset.setCases(List.of(c));

        VectorTopKEvalReport report = evaluator.evaluate(5, dataset, true);
        assertEquals("SYNONYM_ONLY", report.getEvalMode());
        assertEquals(1.0, report.getMacroRecallAtK(), 0.001);
        assertEquals(1, report.getSynonymTableSize());
        assertNull(report.getEmbeddingModel());
    }

    @Test
    void evaluate_standardNameExactMatch() {
        SymptomVocabularyEntry headache = SymptomVocabularyEntry.builder().name("头痛").code("S001").build();
        when(vocabularyService.getCachedVocabulary()).thenReturn(List.of(headache));

        VectorEvalDataset dataset = new VectorEvalDataset();
        dataset.setVersion("test");
        VectorEvalCase c = new VectorEvalCase();
        c.setId("t2");
        c.setBucket("exact_standard");
        c.setQuery("头痛");
        c.setExpected(List.of("头痛"));
        dataset.setCases(List.of(c));

        VectorTopKEvalReport report = evaluator.evaluate(5, dataset, false);
        assertEquals(1.0, report.getMacroRecallAtK(), 0.001);
        assertEquals(1.0, report.getMacroHitAt1(), 0.001);
    }

    @Test
    void evaluate_noMatch_returnsZeroRecall() {
        when(vocabularyService.getCachedVocabulary()).thenReturn(List.of());
        when(synonymRegistry.resolveExact("完全无关的描�?)).thenReturn(Optional.empty());

        VectorEvalDataset dataset = new VectorEvalDataset();
        dataset.setVersion("test");
        VectorEvalCase c = new VectorEvalCase();
        c.setId("t3");
        c.setBucket("oral_paraphrase");
        c.setQuery("完全无关的描�?);
        c.setExpected(List.of("头痛"));
        dataset.setCases(List.of(c));

        VectorTopKEvalReport report = evaluator.evaluate(5, dataset, false);
        assertEquals(0.0, report.getMacroRecallAtK(), 0.001);
    }
}
