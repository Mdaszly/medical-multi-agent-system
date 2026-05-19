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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SymptomIcdSyncService {

    private final KnowledgeGraphService knowledgeGraphService;
    private final Icd10CodeMapper icd10CodeMapper;
    private final SymptomMapper symptomMapper;
    private final SymptomIcdRelMapper symptomIcdRelMapper;

    @Transactional
    public int syncFromNeo4j() {
        List<SymptomDiagnosisRow> rows = knowledgeGraphService.exportAllSymptomIcdRelations();
        if (rows.isEmpty()) {
            log.warn("Neo4j 无 symptom-ICD 数据可同步");
            return 0;
        }

        Map<String, Long> symptomIdByName = new HashMap<>();
        int relCount = 0;

        for (SymptomDiagnosisRow row : rows) {
            if (row.getIcdCode() == null || row.getIcdCode().isBlank()) {
                continue;
            }
            upsertIcd(row);
            Long symptomId = symptomIdByName.computeIfAbsent(row.getSymptom(),
                    name -> upsertSymptom(name));
            if (symptomId == null) {
                continue;
            }
            upsertRelation(symptomId, row);
            relCount++;
        }

        log.info("Neo4j -> RDB 同步完成: {} 条 symptom-ICD 关系", relCount);
        return relCount;
    }

    private void upsertIcd(SymptomDiagnosisRow row) {
        Icd10CodeEntity existing = icd10CodeMapper.selectById(row.getIcdCode());
        if (existing != null) {
            return;
        }
        Icd10CodeEntity entity = new Icd10CodeEntity();
        entity.setCode(row.getIcdCode());
        entity.setDescription(row.getIcdDescription());
        entity.setDescriptionCn(row.getIcdDescription());
        entity.setCreateTime(LocalDateTime.now());
        icd10CodeMapper.insert(entity);
    }

    private Long upsertSymptom(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        SymptomEntity found = symptomMapper.selectOne(
                new LambdaQueryWrapper<SymptomEntity>().eq(SymptomEntity::getName, name));
        if (found != null) {
            return found.getId();
        }
        SymptomEntity entity = new SymptomEntity();
        entity.setName(name);
        entity.setFrequency(0);
        entity.setStatus(1);
        entity.setCreateTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());
        symptomMapper.insert(entity);
        return entity.getId();
    }

    private void upsertRelation(Long symptomId, SymptomDiagnosisRow row) {
        SymptomIcdRelEntity existing = symptomIcdRelMapper.selectOne(
                new LambdaQueryWrapper<SymptomIcdRelEntity>()
                        .eq(SymptomIcdRelEntity::getSymptomId, symptomId)
                        .eq(SymptomIcdRelEntity::getIcdCode, row.getIcdCode()));
        if (existing != null) {
            if (row.getPriority() != null && !row.getPriority().equals(existing.getPriority())) {
                existing.setPriority(row.getPriority());
                symptomIcdRelMapper.updateById(existing);
            }
            return;
        }
        SymptomIcdRelEntity rel = new SymptomIcdRelEntity();
        rel.setSymptomId(symptomId);
        rel.setIcdCode(row.getIcdCode());
        rel.setPriority(row.getPriority() != null ? row.getPriority() : 1);
        rel.setCreateTime(LocalDateTime.now());
        symptomIcdRelMapper.insert(rel);
    }
}
