package com.medical.knowledgegraph.service.neo4j;

import com.medical.knowledgegraph.model.dto.SymptomDiagnosisRow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.Values;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KnowledgeGraphServiceTest {

    @Mock
    private Driver neo4jDriver;

    @Mock
    private Session session;

    @Mock
    private Result result;

    @Mock
    private Record record;

    private KnowledgeGraphService service;

    @BeforeEach
    void setUp() {
        service = new KnowledgeGraphService(neo4jDriver);
    }

    @Test
    void findSymptomDiagnosesRows_validSymptom_returnsRows() {
        when(neo4jDriver.session()).thenReturn(session);
        when(session.run(any(String.class), any(Map.class))).thenReturn(result);
        when(result.hasNext()).thenReturn(true, false);
        when(result.next()).thenReturn(record);
        
        when(record.get("symptom")).thenReturn(Values.value("头痛"));
        when(record.get("disease")).thenReturn(Values.value("偏头痛"));
        when(record.get("diseaseCode")).thenReturn(Values.value("G43"));
        when(record.get("icdCode")).thenReturn(Values.value("G43.909"));
        when(record.get("icdDescription")).thenReturn(Values.value("偏头痛，未特指"));
        when(record.get("weight")).thenReturn(Values.value(1.0));

        List<SymptomDiagnosisRow> rows = service.findSymptomDiagnosesRows("头痛");

        assertFalse(rows.isEmpty());
        assertEquals("头痛", rows.get(0).getSymptom());
        assertEquals("偏头痛", rows.get(0).getDisease());
        assertEquals("G43.909", rows.get(0).getIcdCode());
    }

    @Test
    void findSymptomDiagnosesRows_emptySymptom_returnsEmpty() {
        List<SymptomDiagnosisRow> rows = service.findSymptomDiagnosesRows("");
        assertTrue(rows.isEmpty());

        rows = service.findSymptomDiagnosesRows(null);
        assertTrue(rows.isEmpty());
    }

    @Test
    void findSymptomDiagnosesFuzzy_validKeyword_returnsRows() {
        when(neo4jDriver.session()).thenReturn(session);
        when(session.run(any(String.class), any(Map.class))).thenReturn(result);
        when(result.hasNext()).thenReturn(true, false);
        when(result.next()).thenReturn(record);
        
        when(record.get("symptom")).thenReturn(Values.value("头痛"));
        when(record.get("disease")).thenReturn(Values.value("偏头痛"));
        when(record.get("icdCode")).thenReturn(Values.value("G43.909"));

        List<SymptomDiagnosisRow> rows = service.findSymptomDiagnosesFuzzy("头", 10);

        assertFalse(rows.isEmpty());
    }

    @Test
    void findSymptomDiagnosesFuzzy_emptyKeyword_returnsEmpty() {
        List<SymptomDiagnosisRow> rows = service.findSymptomDiagnosesFuzzy("", 10);
        assertTrue(rows.isEmpty());
    }

    @Test
    void suggestSymptomNames_validPrefix_returnsNames() {
        when(neo4jDriver.session()).thenReturn(session);
        when(session.run(any(String.class), any(Map.class))).thenReturn(result);
        when(result.hasNext()).thenReturn(true, true, false);
        when(result.next()).thenReturn(record).thenReturn(record);
        when(record.get("name")).thenReturn(Values.value("头痛"), Values.value("头晕"));

        List<String> names = service.suggestSymptomNames("头", 10);

        assertFalse(names.isEmpty());
        assertTrue(names.contains("头痛"));
        assertTrue(names.contains("头晕"));
    }

    @Test
    void suggestSymptomNames_emptyPrefix_returnsEmpty() {
        List<String> names = service.suggestSymptomNames("", 10);
        assertTrue(names.isEmpty());
    }

    @Test
    void lookupByIcdCode_validCode_returnsRow() {
        when(neo4jDriver.session()).thenReturn(session);
        when(session.run(any(String.class), any(Map.class))).thenReturn(result);
        when(result.hasNext()).thenReturn(true, false);
        when(result.next()).thenReturn(record);
        
        when(record.get("symptom")).thenReturn(Values.value("头痛"));
        when(record.get("disease")).thenReturn(Values.value("偏头痛"));
        when(record.get("icdCode")).thenReturn(Values.value("G43.909"));
        when(record.get("icdDescription")).thenReturn(Values.value("偏头痛，未特指"));
        when(record.get("weight")).thenReturn(Values.value(1.0));

        Optional<SymptomDiagnosisRow> row = service.lookupByIcdCode("G43.909");

        assertTrue(row.isPresent());
        assertEquals("G43.909", row.get().getIcdCode());
    }

    @Test
    void lookupByIcdCode_invalidCode_returnsEmpty() {
        when(neo4jDriver.session()).thenReturn(session);
        when(session.run(any(String.class), any(Map.class))).thenReturn(result);
        when(result.hasNext()).thenReturn(false);

        Optional<SymptomDiagnosisRow> row = service.lookupByIcdCode("INVALID");

        assertFalse(row.isPresent());
    }

    @Test
    void isEmptyGraph_emptyGraph_returnsTrue() {
        when(neo4jDriver.session()).thenReturn(session);
        when(session.run(any(String.class))).thenReturn(result);
        when(result.hasNext()).thenReturn(true);
        when(result.next()).thenReturn(record);
        when(record.get("c")).thenReturn(Values.value(0L));

        assertTrue(service.isEmptyGraph());
    }

    @Test
    void isEmptyGraph_nonEmptyGraph_returnsFalse() {
        when(neo4jDriver.session()).thenReturn(session);
        when(session.run(any(String.class))).thenReturn(result);
        when(result.hasNext()).thenReturn(true);
        when(result.next()).thenReturn(record);
        when(record.get("c")).thenReturn(Values.value(10L));

        assertFalse(service.isEmptyGraph());
    }

    @Test
    void getStatistics_returnsStats() {
        when(neo4jDriver.session()).thenReturn(session);
        when(session.run(any(String.class))).thenReturn(result);
        when(result.hasNext()).thenReturn(true, true, true, true, true, true, false);
        when(result.next()).thenReturn(record);
        when(record.get("count")).thenReturn(Values.value(10L));

        var stats = service.getStatistics();

        assertNotNull(stats);
        assertTrue(stats.containsKey("Symptom"));
    }

    @Test
    void exportAllSymptomIcdRelations_returnsRelations() {
        when(neo4jDriver.session()).thenReturn(session);
        when(session.run(any(String.class), any(Map.class))).thenReturn(result);
        when(result.hasNext()).thenReturn(true, false);
        when(result.next()).thenReturn(record);
        
        when(record.get("symptom")).thenReturn(Values.value("头痛"));
        when(record.get("disease")).thenReturn(Values.value("偏头痛"));
        when(record.get("icdCode")).thenReturn(Values.value("G43.909"));
        when(record.get("icdDescription")).thenReturn(Values.value("偏头痛，未特指"));
        when(record.get("weight")).thenReturn(Values.value(1.0));
        when(record.get("priority")).thenReturn(Values.value(1));

        List<SymptomDiagnosisRow> rows = service.exportAllSymptomIcdRelations();

        assertFalse(rows.isEmpty());
        assertEquals("头痛", rows.get(0).getSymptom());
    }
}