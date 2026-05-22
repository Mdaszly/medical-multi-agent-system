package com.medical.knowledgegraph.service.neo4j;

import com.medical.knowledgegraph.model.dto.QueryResultDTO;
import com.medical.knowledgegraph.model.dto.SymptomDiagnosisRow;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DiseasePropertyEnricherTest {

    private final DiseasePropertyEnricher enricher = new DiseasePropertyEnricher();

    @Test
    void diagnosisDedupKey_prefersDiseaseCode() {
        SymptomDiagnosisRow row = SymptomDiagnosisRow.builder()
                .disease("偏头痛")
                .diseaseCode("D_G43_909")
                .icdCode("G43.909")
                .build();
        assertEquals("dc:D_G43_909", enricher.diagnosisDedupKey(row));
    }

    @Test
    void enrichDiagnosisRow_fillsDiseaseCodeFromIcd() {
        SymptomDiagnosisRow row = SymptomDiagnosisRow.builder()
                .disease("偏头痛")
                .icdCode("G43.909")
                .build();
        SymptomDiagnosisRow enriched = enricher.enrichDiagnosisRow(row);
        assertEquals("D_G43_909", enriched.getDiseaseCode());
        assertEquals("G43.909", enriched.getIcdCode());
    }

    @Test
    void mergeDiagnosisRows_keepsHigherWeightAndCodes() {
        SymptomDiagnosisRow sparse = SymptomDiagnosisRow.builder()
                .disease("偏头痛")
                .weight(0.5)
                .build();
        SymptomDiagnosisRow rich = SymptomDiagnosisRow.builder()
                .disease("偏头痛")
                .diseaseCode("D_G43_909")
                .icdCode("G43.909")
                .weight(1.0)
                .build();
        SymptomDiagnosisRow merged = enricher.mergeDiagnosisRows(sparse, rich);
        assertEquals("D_G43_909", merged.getDiseaseCode());
        assertEquals(1.0, merged.getWeight());
    }

    @Test
    void enrichDiseaseNodeResult_addsMissingCodes() {
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("name", "偏头痛");
        props.put("severity", "中等");
        QueryResultDTO.NodeResult node = QueryResultDTO.NodeResult.builder()
                .id("99")
                .label("Disease")
                .name("偏头痛")
                .properties(props)
                .build();
        SymptomDiagnosisRow row = SymptomDiagnosisRow.builder()
                .disease("偏头痛")
                .icdCode("G43.909")
                .diseaseCode("D_G43_909")
                .build();
        QueryResultDTO.NodeResult enriched = enricher.enrichDiseaseNodeResult(node, row);
        assertEquals("D_G43_909", enriched.getProperties().get("diseaseCode"));
        assertEquals("G43.909", enriched.getProperties().get("icd10Code"));
    }

    @Test
    void isValidIcdCode_acceptsStandardFormat() {
        assertTrue(enricher.isValidIcdCode("G43.909"));
        assertTrue(enricher.isValidIcdCode("I10"));
        assertFalse(enricher.isValidIcdCode("invalid"));
    }
}
