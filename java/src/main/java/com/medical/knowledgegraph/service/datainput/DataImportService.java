package com.medical.knowledgegraph.service.datainput;

import com.medical.knowledgegraph.model.dto.ImportTaskDTO;
import com.medical.knowledgegraph.model.entity.*;
import com.medical.knowledgegraph.service.neo4j.KnowledgeGraphService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 数据导入服务 - 统一入口
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataImportService {

    private final CsvImportService csvImportService;
    private final JsonImportService jsonImportService;
    private final KnowledgeGraphService knowledgeGraphService;

    /**
     * 导入任务存储
     */
    private final Map<String, ImportTaskDTO> importTasks = new ConcurrentHashMap<>();

    /**
     * 上传并导入文件
     */
    public ImportTaskDTO importFile(MultipartFile file, String entityType, 
                                    ImportTaskDTO.ImportConfig config) {
        // 清理 entityType 参数：去除首尾空格、标点符号，并转为小写
        String cleanedEntityType = cleanEntityType(entityType);
        
        String taskId = UUID.randomUUID().toString();
        ImportTaskDTO task = ImportTaskDTO.builder()
                .taskId(taskId)
                .filePath(file.getOriginalFilename())
                .entityType(cleanedEntityType)
                .config(config)
                .status("PROCESSING")
                .startTime(LocalDateTime.now())
                .build();
        
        importTasks.put(taskId, task);
        
        try {
            // 保存上传文件
            Path tempFile = Files.createTempFile("import_", 
                    getFileExtension(file.getOriginalFilename()));
            file.transferTo(tempFile.toFile());
            task.setFilePath(tempFile.toString());
            
            // 根据实体类型选择导入策略
            switch (cleanedEntityType) {
                case "symptom":
                    return importSymptoms(tempFile.toString(), config, task);
                case "icd10":
                    return importIcdCodes(tempFile.toString(), config, task);
                case "drug":
                    return importDrugs(tempFile.toString(), config, task);
                case "drugeffect":
                    return importDrugEffects(tempFile.toString(), config, task);
                case "disease":
                    return importDiseases(tempFile.toString(), config, task);
                case "relation":
                    return importRelations(tempFile.toString(), config, task);
                default:
                    throw new IllegalArgumentException("不支持的实体类型: " + entityType + " (清理后: " + cleanedEntityType + ")");
            }
            
        } catch (Exception e) {
            log.error("导入失败: taskId={}", taskId, e);
            task.setStatus("FAILED");
            task.setErrors(Collections.singletonList(e.getMessage()));
            return task;
        }
    }

    /**
     * 导入症状数据
     */
    private ImportTaskDTO importSymptoms(String filePath, 
                                         ImportTaskDTO.ImportConfig config,
                                         ImportTaskDTO task) {
        String fileType = getFileType(filePath);
        
        if ("csv".equalsIgnoreCase(fileType)) {
            CsvImportService.ImportStats stats = csvImportService.importCsv(
                    filePath,
                    config != null && config.getSkipHeader() != null ? config.getSkipHeader() : true,
                    config != null && config.getDelimiter() != null ? config.getDelimiter() : ",",
                    config != null ? config.getEncoding() : null,
                    rowData -> {
                        Symptom symptom = mapToSymptom(rowData);
                        knowledgeGraphService.createNode(symptom);
                    }
            );
            updateTaskFromStats(task, stats);
        } else if ("json".equalsIgnoreCase(fileType)) {
            CsvImportService.ImportStats stats = jsonImportService.importJson(
                    filePath,
                    data -> {
                        Symptom symptom = mapToSymptomFromMap(data);
                        knowledgeGraphService.createNode(symptom);
                    }
            );
            updateTaskFromStats(task, stats);
        }
        
        return task;
    }

    /**
     * 导入ICD编码数据
     */
    private ImportTaskDTO importIcdCodes(String filePath,
                                          ImportTaskDTO.ImportConfig config,
                                          ImportTaskDTO task) {
        String fileType = getFileType(filePath);
        
        if ("csv".equalsIgnoreCase(fileType)) {
            CsvImportService.ImportStats stats = csvImportService.importCsv(
                    filePath,
                    config != null && config.getSkipHeader() != null ? config.getSkipHeader() : true,
                    config != null && config.getDelimiter() != null ? config.getDelimiter() : ",",
                    config != null ? config.getEncoding() : null,
                    rowData -> {
                        IcdCode icdCode = mapToIcdCode(rowData);
                        knowledgeGraphService.createNode(icdCode);
                    }
            );
            updateTaskFromStats(task, stats);
        } else if ("json".equalsIgnoreCase(fileType)) {
            CsvImportService.ImportStats stats = jsonImportService.importJson(
                    filePath,
                    data -> {
                        IcdCode icdCode = mapToIcdCodeFromMap(data);
                        knowledgeGraphService.createNode(icdCode);
                    }
            );
            updateTaskFromStats(task, stats);
        }
        
        return task;
    }

    /**
     * 导入药品数据
     */
    private ImportTaskDTO importDrugs(String filePath,
                                       ImportTaskDTO.ImportConfig config,
                                       ImportTaskDTO task) {
        String fileType = getFileType(filePath);
        
        if ("csv".equalsIgnoreCase(fileType)) {
            CsvImportService.ImportStats stats = csvImportService.importCsv(
                    filePath,
                    config != null && config.getSkipHeader() != null ? config.getSkipHeader() : true,
                    config != null && config.getDelimiter() != null ? config.getDelimiter() : ",",
                    config != null ? config.getEncoding() : null,
                    rowData -> {
                        Drug drug = mapToDrug(rowData);
                        knowledgeGraphService.createNode(drug);
                    }
            );
            updateTaskFromStats(task, stats);
        } else if ("json".equalsIgnoreCase(fileType)) {
            CsvImportService.ImportStats stats = jsonImportService.importJson(
                    filePath,
                    data -> {
                        Drug drug = mapToDrugFromMap(data);
                        knowledgeGraphService.createNode(drug);
                    }
            );
            updateTaskFromStats(task, stats);
        }
        
        return task;
    }

    /**
     * 导入药效数据
     */
    private ImportTaskDTO importDrugEffects(String filePath,
                                             ImportTaskDTO.ImportConfig config,
                                             ImportTaskDTO task) {
        String fileType = getFileType(filePath);
        
        if ("csv".equalsIgnoreCase(fileType)) {
            CsvImportService.ImportStats stats = csvImportService.importCsv(
                    filePath,
                    config != null && config.getSkipHeader() != null ? config.getSkipHeader() : true,
                    config != null && config.getDelimiter() != null ? config.getDelimiter() : ",",
                    config != null ? config.getEncoding() : null,
                    rowData -> {
                        DrugEffect effect = mapToDrugEffect(rowData);
                        knowledgeGraphService.createNode(effect);
                    }
            );
            updateTaskFromStats(task, stats);
        } else if ("json".equalsIgnoreCase(fileType)) {
            CsvImportService.ImportStats stats = jsonImportService.importJson(
                    filePath,
                    data -> {
                        DrugEffect effect = mapToDrugEffectFromMap(data);
                        knowledgeGraphService.createNode(effect);
                    }
            );
            updateTaskFromStats(task, stats);
        }
        
        return task;
    }

    /**
     * 导入疾病数据
     */
    private ImportTaskDTO importDiseases(String filePath,
                                          ImportTaskDTO.ImportConfig config,
                                          ImportTaskDTO task) {
        String fileType = getFileType(filePath);
        
        if ("csv".equalsIgnoreCase(fileType)) {
            CsvImportService.ImportStats stats = csvImportService.importCsv(
                    filePath,
                    config != null && config.getSkipHeader() != null ? config.getSkipHeader() : true,
                    config != null && config.getDelimiter() != null ? config.getDelimiter() : ",",
                    config != null ? config.getEncoding() : null,
                    rowData -> {
                        Disease disease = mapToDisease(rowData);
                        knowledgeGraphService.createNode(disease);
                    }
            );
            updateTaskFromStats(task, stats);
        } else if ("json".equalsIgnoreCase(fileType)) {
            CsvImportService.ImportStats stats = jsonImportService.importJson(
                    filePath,
                    data -> {
                        Disease disease = mapToDiseaseFromMap(data);
                        knowledgeGraphService.createNode(disease);
                    }
            );
            updateTaskFromStats(task, stats);
        }
        
        return task;
    }

    /**
     * 导入关系数据
     */
    private ImportTaskDTO importRelations(String filePath,
                                          ImportTaskDTO.ImportConfig config,
                                          ImportTaskDTO task) {
        String fileType = getFileType(filePath);
        
        if ("csv".equalsIgnoreCase(fileType)) {
            CsvImportService.ImportStats stats = csvImportService.importCsv(
                    filePath,
                    config != null && config.getSkipHeader() != null ? config.getSkipHeader() : true,
                    config != null && config.getDelimiter() != null ? config.getDelimiter() : ",",
                    config != null ? config.getEncoding() : null,
                    rowData -> {
                        KnowledgeRelation relation = mapToRelation(rowData);
                        if (relation != null) {
                            knowledgeGraphService.createRelationship(relation);
                        }
                    }
            );
            updateTaskFromStats(task, stats);
        } else if ("json".equalsIgnoreCase(fileType)) {
            CsvImportService.ImportStats stats = jsonImportService.importJson(
                    filePath,
                    data -> {
                        KnowledgeRelation relation = mapToRelationFromMap(data);
                        if (relation != null) {
                            knowledgeGraphService.createRelationship(relation);
                        }
                    }
            );
            updateTaskFromStats(task, stats);
        }
        
        return task;
    }

    // ==================== 数据映射方法 ====================

    private Symptom mapToSymptom(Map<String, String> row) {
        return Symptom.builder()
                .id(getStringWithFallback(row, "id"))
                .name(getStringWithFallback(row, "name"))
                .code(getStringWithFallback(row, "code"))
                .categoryCode(getStringWithFallback(row, "categoryCode", "category_code"))
                .categoryName(getStringWithFallback(row, "categoryName", "category_name"))
                .description(getStringWithFallback(row, "description"))
                .bodyPart(getStringWithFallback(row, "bodyPart", "body_part"))
                .pinyin(getStringWithFallback(row, "pinyin"))
                .frequency(parseInteger(getStringWithFallback(row, "frequency")))
                .severity(parseInteger(getStringWithFallback(row, "severity")))
                .urgent(parseBoolean(getStringWithFallback(row, "urgent")))
                .build();
    }

    private Symptom mapToSymptomFromMap(Map<String, Object> data) {
        return Symptom.builder()
                .id(getString(data, "id"))
                .name(getString(data, "name"))
                .code(getString(data, "code"))
                .categoryCode(getString(data, "category_code"))
                .categoryName(getString(data, "category_name"))
                .description(getString(data, "description"))
                .bodyPart(getString(data, "body_part"))
                .pinyin(getString(data, "pinyin"))
                .frequency(getInteger(data, "frequency"))
                .severity(getInteger(data, "severity"))
                .urgent(getBoolean(data, "urgent"))
                .build();
    }

    private IcdCode mapToIcdCode(Map<String, String> row) {
        return IcdCode.builder()
                .code(getStringWithFallback(row, "code"))
                .descriptionEn(getStringWithFallback(row, "descriptionEn", "description_en"))
                .descriptionCn(getStringWithFallback(row, "descriptionCn", "description_cn"))
                .chapterCode(getStringWithFallback(row, "chapterCode", "chapter_code"))
                .chapterName(getStringWithFallback(row, "chapterName", "chapter_name"))
                .sectionCode(getStringWithFallback(row, "sectionCode", "section_code"))
                .sectionName(getStringWithFallback(row, "sectionName", "section_name"))
                .diseaseClass(getStringWithFallback(row, "diseaseClass", "disease_class"))
                .drgCode(getStringWithFallback(row, "drgCode", "drg_code"))
                .medicalInsurance(parseBoolean(getStringWithFallback(row, "medicalInsurance", "medical_insurance")))
                .build();
    }

    private IcdCode mapToIcdCodeFromMap(Map<String, Object> data) {
        return IcdCode.builder()
                .code(getString(data, "code"))
                .descriptionEn(getString(data, "description_en"))
                .descriptionCn(getString(data, "description_cn"))
                .chapterCode(getString(data, "chapter_code"))
                .chapterName(getString(data, "chapter_name"))
                .sectionCode(getString(data, "section_code"))
                .sectionName(getString(data, "section_name"))
                .diseaseClass(getString(data, "disease_class"))
                .drgCode(getString(data, "drg_code"))
                .medicalInsurance(getBoolean(data, "medical_insurance"))
                .build();
    }

    private Drug mapToDrug(Map<String, String> row) {
        return Drug.builder()
                .id(getStringWithFallback(row, "id"))
                .name(getStringWithFallback(row, "name"))
                .drugCode(getStringWithFallback(row, "drugCode", "drug_code"))
                .genericName(getStringWithFallback(row, "genericName", "generic_name"))
                .brandName(getStringWithFallback(row, "brandName", "brand_name"))
                .englishName(getStringWithFallback(row, "englishName", "english_name"))
                .category(getStringWithFallback(row, "category"))
                .type(getStringWithFallback(row, "type"))
                .specification(getStringWithFallback(row, "specification"))
                .unit(getStringWithFallback(row, "unit"))
                .manufacturer(getStringWithFallback(row, "manufacturer"))
                .approvalNumber(getStringWithFallback(row, "approvalNumber", "approval_number"))
                .price(parseDouble(getStringWithFallback(row, "price")))
                .stock(parseInteger(getStringWithFallback(row, "stock")))
                .routeOfAdministration(getStringWithFallback(row, "routeOfAdministration", "route_of_administration"))
                .essential(parseBoolean(getStringWithFallback(row, "essential")))
                .medicalInsurance(parseBoolean(getStringWithFallback(row, "medicalInsurance", "medical_insurance")))
                .build();
    }

    private Drug mapToDrugFromMap(Map<String, Object> data) {
        return Drug.builder()
                .id(getString(data, "id"))
                .name(getString(data, "name"))
                .drugCode(getString(data, "drug_code"))
                .genericName(getString(data, "generic_name"))
                .brandName(getString(data, "brand_name"))
                .englishName(getString(data, "english_name"))
                .category(getString(data, "category"))
                .type(getString(data, "type"))
                .specification(getString(data, "specification"))
                .unit(getString(data, "unit"))
                .manufacturer(getString(data, "manufacturer"))
                .approvalNumber(getString(data, "approval_number"))
                .price(getDouble(data, "price"))
                .stock(getInteger(data, "stock"))
                .routeOfAdministration(getString(data, "route_of_administration"))
                .essential(getBoolean(data, "essential"))
                .medicalInsurance(getBoolean(data, "medical_insurance"))
                .build();
    }

    private DrugEffect mapToDrugEffect(Map<String, String> row) {
        return DrugEffect.builder()
                .id(row.get("id"))
                .name(row.get("name"))
                .effectCode(row.get("effect_code"))
                .category(row.get("category"))
                .description(row.get("description"))
                .indications(row.get("indications"))
                .adverseReactions(row.get("adverse_reactions"))
                .contraindications(row.get("contraindications"))
                .precautions(row.get("precautions"))
                .mechanism(row.get("mechanism"))
                .build();
    }

    private DrugEffect mapToDrugEffectFromMap(Map<String, Object> data) {
        return DrugEffect.builder()
                .id(getString(data, "id"))
                .name(getString(data, "name"))
                .effectCode(getString(data, "effect_code"))
                .category(getString(data, "category"))
                .description(getString(data, "description"))
                .indications(getString(data, "indications"))
                .adverseReactions(getString(data, "adverse_reactions"))
                .contraindications(getString(data, "contraindications"))
                .precautions(getString(data, "precautions"))
                .mechanism(getString(data, "mechanism"))
                .build();
    }

    private Disease mapToDisease(Map<String, String> row) {
        return Disease.builder()
                .id(getStringWithFallback(row, "id"))
                .name(getStringWithFallback(row, "name"))
                .diseaseCode(getStringWithFallback(row, "diseaseCode", "disease_code"))
                .icd10Code(getStringWithFallback(row, "icd10Code", "icd10_code"))
                .category(getStringWithFallback(row, "category"))
                .system(getStringWithFallback(row, "system"))
                .severity(getStringWithFallback(row, "severity"))
                .infectious(parseBoolean(getStringWithFallback(row, "infectious")))
                .hereditary(parseBoolean(getStringWithFallback(row, "hereditary")))
                .description(getStringWithFallback(row, "description"))
                .etiology(getStringWithFallback(row, "etiology"))
                .treatment(getStringWithFallback(row, "treatment"))
                .prognosis(getStringWithFallback(row, "prognosis"))
                .build();
    }

    private Disease mapToDiseaseFromMap(Map<String, Object> data) {
        return Disease.builder()
                .id(getStringWithFallbackFromObject(data, "id"))
                .name(getStringWithFallbackFromObject(data, "name"))
                .diseaseCode(getStringWithFallbackFromObject(data, "diseaseCode", "disease_code"))
                .icd10Code(getStringWithFallbackFromObject(data, "icd10Code", "icd10_code"))
                .category(getStringWithFallbackFromObject(data, "category"))
                .system(getStringWithFallbackFromObject(data, "system"))
                .severity(getStringWithFallbackFromObject(data, "severity"))
                .infectious(getBooleanWithFallback(data, "infectious"))
                .hereditary(getBooleanWithFallback(data, "hereditary"))
                .description(getStringWithFallbackFromObject(data, "description"))
                .etiology(getStringWithFallbackFromObject(data, "etiology"))
                .treatment(getStringWithFallbackFromObject(data, "treatment"))
                .prognosis(getStringWithFallbackFromObject(data, "prognosis"))
                .build();
    }

    private KnowledgeRelation mapToRelation(Map<String, String> row) {
        String sourceId = getStringWithFallback(row, "sourceId", "source_id");
        String sourceName = getStringWithFallback(row, "sourceName", "source_name");
        String sourceLabel = getStringWithFallback(row, "sourceLabel", "source_label");
        String targetId = getStringWithFallback(row, "targetId", "target_id");
        String targetName = getStringWithFallback(row, "targetName", "target_name");
        String targetLabel = getStringWithFallback(row, "targetLabel", "target_label");
        String type = getStringWithFallback(row, "type");
        String description = getStringWithFallback(row, "description");
        String weightStr = getStringWithFallback(row, "weight");
        String urgentStr = getStringWithFallback(row, "urgent");
        
        if (sourceName == null || targetName == null || type == null) {
            log.warn("关系数据不完整，跳过: {}", row);
            return null;
        }
        
        return KnowledgeRelation.builder()
                .sourceId(sourceId)
                .sourceName(sourceName)
                .sourceLabel(sourceLabel != null ? sourceLabel : "UNKNOWN")
                .targetId(targetId)
                .targetName(targetName)
                .targetLabel(targetLabel != null ? targetLabel : "UNKNOWN")
                .type(type)
                .description(description)
                .weight(parseDouble(weightStr))
                .urgent(parseBoolean(urgentStr))
                .build();
    }

    private KnowledgeRelation mapToRelationFromMap(Map<String, Object> data) {
        String sourceName = getString(data, "source_name");
        String targetName = getString(data, "target_name");
        String type = getString(data, "type");
        
        if (sourceName == null || targetName == null || type == null) {
            log.warn("关系数据不完整，跳过");
            return null;
        }
        
        return KnowledgeRelation.builder()
                .sourceId(getString(data, "source_id"))
                .sourceName(sourceName)
                .sourceLabel(getString(data, "source_label"))
                .targetId(getString(data, "target_id"))
                .targetName(targetName)
                .targetLabel(getString(data, "target_label"))
                .type(type)
                .description(getString(data, "description"))
                .weight(getDouble(data, "weight"))
                .priority(getInteger(data, "priority"))
                .build();
    }

    // ==================== 工具方法 ====================

    /**
     * 从Map<String, String>中获取值，支持多个可能的字段名（驼峰式和下划线式）
     */
    private String getStringWithFallback(Map<String, String> map, String... keys) {
        for (String key : keys) {
            String value = map.get(key);
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return null;
    }

    /**
     * 从Map<String, Object>中获取值，支持多个可能的字段名（驼峰式和下划线式）
     */
    private String getStringWithFallbackFromObject(Map<String, Object> map, String... keys) {
        for (String key : keys) {
            Object value = map.get(key);
            if (value != null) {
                String strValue = value.toString().trim();
                if (!strValue.isEmpty()) {
                    return strValue;
                }
            }
        }
        return null;
    }

    /**
     * 从Map<String, Object>中获取布尔值，支持多个可能的字段名
     */
    private Boolean getBooleanWithFallback(Map<String, Object> map, String... keys) {
        for (String key : keys) {
            Object value = map.get(key);
            if (value != null) {
                String strValue = value.toString().trim().toLowerCase();
                return "true".equals(strValue) || "1".equals(strValue);
            }
        }
        return null;
    }

    /**
     * 清理实体类型参数
     * - 去除首尾空格
     * - 去除常见标点符号（逗号、句号、顿号等）
     * - 转为小写
     */
    private String cleanEntityType(String entityType) {
        if (entityType == null) {
            return "";
        }
        return entityType.trim()
                .replaceAll("[,，。、;；\\s]+", "")  // 去除标点和空白字符
                .toLowerCase();
    }

    private void updateTaskFromStats(ImportTaskDTO task, CsvImportService.ImportStats stats) {
        task.setTotalRecords(stats.getTotalProcessed());
        task.setProcessedRecords(stats.getTotalProcessed());
        task.setSuccessCount(stats.getSuccessCount());
        task.setFailureCount(stats.getFailureCount());
        task.setErrors(stats.getErrors());
        task.setEndTime(LocalDateTime.now());
        task.setStatus("COMPLETED");
    }

    private String getFileType(String filePath) {
        if (filePath == null) return "";
        String lower = filePath.toLowerCase();
        if (lower.endsWith(".csv")) return "csv";
        if (lower.endsWith(".json") || lower.endsWith(".jsonl")) return "json";
        return "";
    }

    private String getFileExtension(String fileName) {
        if (fileName == null) return ".tmp";
        int lastDot = fileName.lastIndexOf('.');
        return lastDot > 0 ? fileName.substring(lastDot) : ".tmp";
    }

    private Integer parseInteger(String value) {
        if (value == null || value.trim().isEmpty()) return null;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Double parseDouble(String value) {
        if (value == null || value.trim().isEmpty()) return null;
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Boolean parseBoolean(String value) {
        if (value == null || value.trim().isEmpty()) return null;
        return "true".equalsIgnoreCase(value.trim()) || "1".equals(value.trim());
    }

    private String getString(Map<String, Object> data, String key) {
        Object value = data.get(key);
        return value != null ? value.toString() : null;
    }

    private Integer getInteger(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value == null) return null;
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return parseInteger(value.toString());
    }

    private Double getDouble(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value == null) return null;
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return parseDouble(value.toString());
    }

    private Boolean getBoolean(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value == null) return null;
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return parseBoolean(value.toString());
    }

    /**
     * 获取导入任务状态
     */
    public ImportTaskDTO getTaskStatus(String taskId) {
        return importTasks.get(taskId);
    }
}
