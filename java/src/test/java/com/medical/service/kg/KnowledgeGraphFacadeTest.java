package com.medical.service.kg;

import com.medical.config.MedicalGraphProperties;
import com.medical.knowledgegraph.model.dto.SymptomDiagnosisRow;
import com.medical.knowledgegraph.service.extraction.EntityExtractionService;
import com.medical.knowledgegraph.service.neo4j.KnowledgeGraphService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KnowledgeGraphFacadeTest {

    @Mock
    private KnowledgeGraphService knowledgeGraphService;

    @Mock
    private EntityExtractionService entityExtractionService;

    private KnowledgeGraphFacade facade;

    @BeforeEach
    void setUp() {
        MedicalGraphProperties props = new MedicalGraphProperties();
        props.setEnabled(true);
        props.setFuzzySymptomLimit(5);
        facade = new KnowledgeGraphFacade(knowledgeGraphService, entityExtractionService, props);
    }

    @Test
    void extractAndQuery_headache_containsG43() {
        when(entityExtractionService.extractSymptoms(anyString())).thenReturn(List.of());
        when(knowledgeGraphService.findSymptomDiagnosesRows("头痛")).thenReturn(List.of(
                SymptomDiagnosisRow.builder()
                        .symptom("头痛")
                        .disease("偏头痛")
                        .icdCode("G43.909")
                        .icdDescription("偏头痛，未特指，非顽固性")
                        .build()
        ));

        var evidence = facade.extractAndQuery("我最近经常头痛");

        assertTrue(evidence.isGraphHit());
        assertTrue(evidence.getIcdCandidateCodes().contains("G43.909"));
        assertTrue(evidence.getFormattedText().contains("G43.909"));
    }
}
