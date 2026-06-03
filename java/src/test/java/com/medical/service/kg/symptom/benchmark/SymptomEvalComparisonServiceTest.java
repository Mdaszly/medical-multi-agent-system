package com.medical.service.kg.symptom.benchmark;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SymptomEvalComparisonServiceTest {

    @Mock
    private VectorEvalDatasetLoader datasetLoader;

    @Mock
    private SymptomSynonymOnlyEvaluator synonymOnlyEvaluator;

    @Mock
    private SymptomVectorTopKEvaluator vectorTopKEvaluator;

    @InjectMocks
    private SymptomEvalComparisonService comparisonService;

    @Test
    void compare_computesRelativeGain() {
        VectorEvalDataset dataset = new VectorEvalDataset();
        dataset.setVersion("1.0");
        dataset.setCases(java.util.List.of(new VectorEvalCase()));

        VectorTopKEvalReport baseline = VectorTopKEvalReport.builder()
                .evalMode("SYNONYM_ONLY")
                .macroRecallAtK(0.4)
                .macroHitAt1(0.4)
                .macroMrr(0.45)
                .microRecallAtK(0.35)
                .build();
        VectorTopKEvalReport vector = VectorTopKEvalReport.builder()
                .evalMode("VECTOR")
                .macroRecallAtK(0.6)
                .macroHitAt1(0.62)
                .macroMrr(0.63)
                .microRecallAtK(0.55)
                .build();

        when(datasetLoader.loadDefault()).thenReturn(dataset);
        when(synonymOnlyEvaluator.evaluate(anyInt(), org.mockito.ArgumentMatchers.eq(dataset), anyBoolean()))
                .thenReturn(baseline);
        when(vectorTopKEvaluator.evaluate(anyInt(), org.mockito.ArgumentMatchers.eq(dataset), anyBoolean()))
                .thenReturn(vector);

        SymptomEvalComparisonReport report = comparisonService.compare(5, dataset, false);
        assertNotNull(report.getImprovement());
        assertEquals(0.2, report.getImprovement().getMacroRecallAtKDelta(), 0.001);
        assertEquals(50.0, report.getImprovement().getMacroRecallAtKRelativeGainPct(), 0.001);
    }
}
