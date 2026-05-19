package com.medical.service.kg;

import com.medical.config.MedicalGraphProperties;
import com.medical.model.ClinicalState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IcdGroundingValidatorTest {

    private IcdGroundingValidator validator;

    @BeforeEach
    void setUp() {
        MedicalGraphProperties props = new MedicalGraphProperties();
        props.setValidateIcd(true);
        validator = new IcdGroundingValidator(props);
    }

    @Test
    void validate_noCandidatesWithMentionedIcd_returnsWarning() {
        ClinicalState state = ClinicalState.builder().rawInput("头痛").build();
        Map<String, Object> consultResult = new HashMap<>();
        consultResult.put("icd_references", List.of(Map.of("code", "G43.909", "disease", "偏头痛")));
        state.getExtensions().put("consultResult", consultResult);
        state.getExtensions().put(KnowledgeEnrichmentService.EXT_ICD_CANDIDATES, List.of());

        String status = validator.validate(state);

        assertEquals("WARNING", status);
        assertEquals("WARNING", state.getExtensions().get(KnowledgeEnrichmentService.EXT_GROUNDING_STATUS));
    }

    @Test
    void validate_matchingCandidate_returnsVerified() {
        ClinicalState state = ClinicalState.builder().rawInput("头痛").build();
        Map<String, Object> consultResult = new HashMap<>();
        consultResult.put("icd_references", List.of(Map.of("code", "G43.909")));
        state.getExtensions().put("consultResult", consultResult);
        state.getExtensions().put(KnowledgeEnrichmentService.EXT_ICD_CANDIDATES, List.of("G43.909"));

        String status = validator.validate(state);

        assertEquals("VERIFIED", status);
    }
}
