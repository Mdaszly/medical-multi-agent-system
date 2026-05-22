package com.medical.service.kg.symptom.eval;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medical.config.MedicalGraphProperties;
import com.medical.service.kg.symptom.ScoredSymptomCandidate;
import com.medical.service.kg.symptom.SymptomVectorSearchService;
import com.medical.service.kg.symptom.SymptomVocabularyEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SymptomVectorTopKEvaluatorTest {

    @Mock
    private SymptomVectorSearchService vectorSearchService;

    private SymptomVectorTopKEvaluator evaluator;

    @BeforeEach
    void setUp() {
        MedicalGraphProperties props = new MedicalGraphProperties();
        props.getSymptomResolver().setVectorMinScore(0.72);
        VectorEvalDatasetLoader loader = new VectorEvalDatasetLoader(new ObjectMapper());
        evaluator = new SymptomVectorTopKEvaluator(loader, vectorSearchService, props);
        when(vectorSearchService.isIndexReady()).thenReturn(true);
    }

    @Test
    void evaluate_perfectRecallCase() {
        SymptomVocabularyEntry headache = SymptomVocabularyEntry.builder().name("头痛").code("S001").build();
        when(vectorSearchService.searchTopK(anyString(), anyInt())).thenReturn(List.of(
                ScoredSymptomCandidate.builder().entry(headache).score(0.91).build()
        ));

        VectorEvalDataset dataset = new VectorEvalDataset();
        dataset.setVersion("test");
        VectorEvalCase c = new VectorEvalCase();
        c.setId("t1");
        c.setBucket("oral_paraphrase");
        c.setQuery("脑袋胀痛");
        c.setExpected(List.of("头痛"));
        dataset.setCases(List.of(c));

        VectorTopKEvalReport report = evaluator.evaluate(5, dataset, true);
        assertEquals(1.0, report.getMacroRecallAtK(), 0.001);
        assertEquals(1.0, report.getMacroHitAt1(), 0.001);
        assertEquals(1, report.getCaseDetails().get(0).getHitCount());
    }

    @Test
    void evaluate_defaultDataset_loads() {
        VectorEvalDataset dataset = new VectorEvalDatasetLoader(new ObjectMapper()).loadDefault();
        assertEquals(45, dataset.getCases().size());
        assertTrue(dataset.getDistribution().containsKey("oral_paraphrase"));
    }
}
