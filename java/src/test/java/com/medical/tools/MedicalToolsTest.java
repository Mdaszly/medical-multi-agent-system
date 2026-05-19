package com.medical.tools;

import com.medical.service.AppointmentGuideService;
import com.medical.service.DrugInteractionService;
import com.medical.service.Icd10Service;
import com.medical.service.MedicalKnowledgeService;
import com.medical.service.kg.KnowledgeGraphFacade;
import com.medical.model.vo.Icd10CodeVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MedicalToolsTest {

    @Mock
    private Icd10Service icd10Service;

    @Mock
    private DrugInteractionService drugInteractionService;

    @Mock
    private MedicalKnowledgeService medicalKnowledgeService;

    @Mock
    private AppointmentGuideService appointmentGuideService;

    @Mock
    private KnowledgeGraphFacade knowledgeGraphFacade;

    private MedicalTools tools;

    @BeforeEach
    void setUp() {
        tools = new MedicalTools(icd10Service, drugInteractionService, medicalKnowledgeService, appointmentGuideService, knowledgeGraphFacade);
    }

    @Test
    void querySymptomDiagnosis_validSymptom_returnsResult() {
        when(knowledgeGraphFacade.querySymptomDiagnosisAsText("头痛")).thenReturn("症状: 头痛 | 疾病: 偏头痛 | ICD: G43.909");

        String result = tools.querySymptomDiagnosis("头痛");

        assertTrue(result.contains("头痛"));
        assertTrue(result.contains("G43.909"));
    }

    @Test
    void suggestSymptoms_validPrefix_returnsSuggestions() {
        when(knowledgeGraphFacade.suggestSymptomsAsText("头")).thenReturn("症状联想：头痛、头晕");

        String result = tools.suggestSymptoms("头");

        assertTrue(result.contains("头痛"));
        assertTrue(result.contains("头晕"));
    }

    @Test
    void lookupIcd10ByCode_graphHit_returnsGraphResult() {
        when(knowledgeGraphFacade.lookupIcdAsText("G43.909")).thenReturn("G43.909 - 偏头痛，未特指（疾病: 偏头痛）");

        String result = tools.lookupIcd10ByCode("G43.909");

        assertTrue(result.contains("G43.909"));
        assertTrue(result.contains("偏头痛"));
        verify(icd10Service, never()).lookupByCode(any());
    }

    @Test
    void lookupIcd10ByCode_graphMiss_returnsRdbResult() {
        when(knowledgeGraphFacade.lookupIcdAsText("G43.909")).thenReturn("图谱中未找到 ICD: G43.909");
        
        Icd10CodeVO vo = new Icd10CodeVO();
        vo.setCode("G43.909");
        vo.setDescription("偏头痛，未特指");
        when(icd10Service.lookupByCode("G43.909")).thenReturn(vo);

        String result = tools.lookupIcd10ByCode("G43.909");

        assertTrue(result.contains("G43.909"));
        assertTrue(result.contains("偏头痛，未特指"));
    }

    @Test
    void queryIcd10_graphHit_returnsGraphResult() {
        when(knowledgeGraphFacade.querySymptomDiagnosisAsText("偏头痛")).thenReturn("ICD: G43.909 | 描述: 偏头痛");

        String result = tools.queryIcd10("偏头痛");

        assertTrue(result.contains("ICD:"));
        verify(icd10Service, never()).searchAsText(any());
    }

    @Test
    void queryIcd10_graphMiss_returnsRdbResult() {
        when(knowledgeGraphFacade.querySymptomDiagnosisAsText("偏头痛")).thenReturn("未命中相关症状");
        when(icd10Service.searchAsText("偏头痛")).thenReturn("G43.909 - 偏头痛");

        String result = tools.queryIcd10("偏头痛");

        assertTrue(result.contains("G43.909"));
    }

    @Test
    void checkDrugInteraction_validDrugs_returnsResult() {
        when(drugInteractionService.checkAsText(List.of("布洛芬"), List.of("阿司匹林"))).thenReturn("无明显相互作用");

        String result = tools.checkDrugInteraction("布洛芬", "阿司匹林");

        assertTrue(result.contains("无明显相互作用"));
    }

    @Test
    void searchMedicalKnowledge_validQuery_returnsResult() {
        when(medicalKnowledgeService.search("发热")).thenReturn("发热相关知识...");

        String result = tools.searchMedicalKnowledge("发热");

        assertTrue(result.contains("发热"));
    }

    @Test
    void queryAppointmentSlots_validDepartment_returnsResult() {
        when(appointmentGuideService.suggestDoctors("内科")).thenReturn("内科可预约医生列表...");

        String result = tools.queryAppointmentSlots("内科");

        assertTrue(result.contains("内科"));
    }
}