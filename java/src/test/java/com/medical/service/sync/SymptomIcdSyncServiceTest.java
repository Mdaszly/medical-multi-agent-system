package com.medical.service.sync;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.medical.knowledgegraph.model.dto.SymptomDiagnosisRow;
import com.medical.knowledgegraph.service.neo4j.KnowledgeGraphService;
import com.medical.mapper.Icd10CodeMapper;
import com.medical.mapper.SymptomIcdRelMapper;
import com.medical.mapper.SymptomMapper;
import com.medical.model.entity.Icd10CodeEntity;
import com.medical.model.entity.SymptomEntity;
import com.medical.model.entity.SymptomIcdRelEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SymptomIcdSyncServiceTest {

    @Mock
    private KnowledgeGraphService knowledgeGraphService;

    @Mock
    private Icd10CodeMapper icd10CodeMapper;

    @Mock
    private SymptomMapper symptomMapper;

    @Mock
    private SymptomIcdRelMapper symptomIcdRelMapper;

    private SymptomIcdSyncService service;

    @BeforeEach
    void setUp() {
        service = new SymptomIcdSyncService(knowledgeGraphService, icd10CodeMapper, symptomMapper, symptomIcdRelMapper);
    }

    @Test
    void syncFromNeo4j_emptyData_returnsZero() {
        when(knowledgeGraphService.exportAllSymptomIcdRelations()).thenReturn(List.of());

        int count = service.syncFromNeo4j();

        assertEquals(0, count);
        verify(icd10CodeMapper, never()).insert(any());
        verify(symptomMapper, never()).insert(any());
    }

    @Test
    void syncFromNeo4j_validData_syncsSuccessfully() {
        List<SymptomDiagnosisRow> rows = List.of(
                SymptomDiagnosisRow.builder()
                        .symptom("头痛")
                        .disease("偏头痛")
                        .icdCode("G43.909")
                        .icdDescription("偏头痛，未特指")
                        .priority(1)
                        .build()
        );

        when(knowledgeGraphService.exportAllSymptomIcdRelations()).thenReturn(rows);
        when(icd10CodeMapper.selectById("G43.909")).thenReturn(null);
        when(symptomMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        
        SymptomEntity newSymptom = new SymptomEntity();
        newSymptom.setId(1L);
        when(symptomMapper.insert(any(SymptomEntity.class))).thenAnswer(invocation -> {
            SymptomEntity entity = invocation.getArgument(0);
            entity.setId(1L);
            return 1;
        });
        when(symptomIcdRelMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        int count = service.syncFromNeo4j();

        assertEquals(1, count);
        verify(icd10CodeMapper).insert(any(Icd10CodeEntity.class));
        verify(symptomMapper).insert(any(SymptomEntity.class));
        verify(symptomIcdRelMapper).insert(any(SymptomIcdRelEntity.class));
    }

    @Test
    void syncFromNeo4j_existingData_upsertsCorrectly() {
        List<SymptomDiagnosisRow> rows = List.of(
                SymptomDiagnosisRow.builder()
                        .symptom("头痛")
                        .disease("偏头痛")
                        .icdCode("G43.909")
                        .icdDescription("偏头痛，未特指")
                        .priority(2)
                        .build()
        );

        when(knowledgeGraphService.exportAllSymptomIcdRelations()).thenReturn(rows);
        when(icd10CodeMapper.selectById("G43.909")).thenReturn(new Icd10CodeEntity());
        
        SymptomEntity existingSymptom = new SymptomEntity();
        existingSymptom.setId(1L);
        when(symptomMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existingSymptom);

        SymptomIcdRelEntity existingRel = new SymptomIcdRelEntity();
        existingRel.setPriority(1);
        when(symptomIcdRelMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existingRel);

        int count = service.syncFromNeo4j();

        assertEquals(1, count);
        verify(icd10CodeMapper, never()).insert(any());
        verify(symptomMapper, never()).insert(any());
        verify(symptomIcdRelMapper).updateById(any(SymptomIcdRelEntity.class));
    }

    @Test
    void syncFromNeo4j_nullIcdCode_skipsRow() {
        List<SymptomDiagnosisRow> rows = List.of(
                SymptomDiagnosisRow.builder()
                        .symptom("头痛")
                        .disease("偏头痛")
                        .icdCode(null)
                        .build()
        );

        when(knowledgeGraphService.exportAllSymptomIcdRelations()).thenReturn(rows);

        int count = service.syncFromNeo4j();

        assertEquals(0, count);
        verify(icd10CodeMapper, never()).insert(any());
    }
}