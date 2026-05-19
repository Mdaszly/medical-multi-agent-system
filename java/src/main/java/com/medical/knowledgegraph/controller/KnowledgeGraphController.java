package com.medical.knowledgegraph.controller;

import com.medical.knowledgegraph.model.dto.ImportTaskDTO;
import com.medical.knowledgegraph.model.dto.QueryResultDTO;
import com.medical.knowledgegraph.service.datainput.DataImportService;
import com.medical.knowledgegraph.service.extraction.EntityExtractionService;
import com.medical.knowledgegraph.service.neo4j.KnowledgeGraphService;
import com.medical.service.sync.SymptomIcdSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

/**
 * 知识图谱REST API控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/knowledge-graph")
@RequiredArgsConstructor
public class KnowledgeGraphController {

    private final DataImportService dataImportService;
    private final KnowledgeGraphService knowledgeGraphService;
    private final EntityExtractionService entityExtractionService;
    private final SymptomIcdSyncService symptomIcdSyncService;

    // ==================== 数据导入API ====================

    /**
     * 导入数据文件
     */
    @PostMapping("/import")
    public ResponseEntity<ImportTaskDTO> importFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("entityType") String entityType,
            @RequestParam(value = "skipHeader", defaultValue = "true") Boolean skipHeader,
            @RequestParam(value = "delimiter", defaultValue = ",") String delimiter) {
        
        log.info("接收导入请求: entityType={}, fileName={}", entityType, file.getOriginalFilename());
        
        ImportTaskDTO.ImportConfig config = ImportTaskDTO.ImportConfig.builder()
                .skipHeader(skipHeader)
                .delimiter(delimiter)
                .build();
        
        ImportTaskDTO result = dataImportService.importFile(file, entityType, config);
        return ResponseEntity.ok(result);
    }

    /**
     * 获取导入任务状态
     */
    @GetMapping("/import/{taskId}")
    public ResponseEntity<ImportTaskDTO> getImportTaskStatus(@PathVariable String taskId) {
        ImportTaskDTO task = dataImportService.getTaskStatus(taskId);
        if (task == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(task);
    }

    // ==================== 查询API ====================

    /**
     * 执行Cypher查询
     */
    @PostMapping("/query")
    public ResponseEntity<QueryResultDTO> executeQuery(@RequestBody Map<String, Object> request) {
        String cypher = (String) request.get("cypher");
        @SuppressWarnings("unchecked")
        Map<String, Object> params = (Map<String, Object>) request.get("params");
        
        if (cypher == null || cypher.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        log.info("执行Cypher查询: {}", cypher);
        QueryResultDTO result = knowledgeGraphService.executeQuery(cypher, params);
        return ResponseEntity.ok(result);
    }

    /**
     * 根据名称查询节点
     */
    @GetMapping("/node/{label}/{name}")
    public ResponseEntity<QueryResultDTO> findNodeByName(
            @PathVariable String label,
            @PathVariable String name) {
        
        // URL解码中文名称
        String decodedName = decodeUrlParam(name);
        log.info("查询节点: label={}, name={}", label, decodedName);
        QueryResultDTO result = knowledgeGraphService.findNodeByName(label, decodedName);
        return ResponseEntity.ok(result);
    }

    /**
     * 查询节点关联关系（Path参数版本）
     */
    @GetMapping("/relations/{label}/{name}")
    public ResponseEntity<QueryResultDTO> findNodeRelations(
            @PathVariable String label,
            @PathVariable String name,
            @RequestParam(value = "depth", defaultValue = "1") int depth) {
        
        // URL解码中文名称
        String decodedName = decodeUrlParam(name);
        log.info("查询节点关系: label={}, name={}, depth={}", label, decodedName, depth);
        QueryResultDTO result = knowledgeGraphService.findNodeRelations(label, decodedName, null, depth);
        return ResponseEntity.ok(result);
    }

    /**
     * 查询节点关联关系（Query参数版本，避免URL编码问题）
     */
    @GetMapping("/relations")
    public ResponseEntity<QueryResultDTO> findNodeRelationsByQuery(
            @RequestParam String label,
            @RequestParam String name,
            @RequestParam(value = "depth", defaultValue = "1") int depth) {
        
        log.info("查询节点关系(Query): label={}, name={}, depth={}", label, name, depth);
        QueryResultDTO result = knowledgeGraphService.findNodeRelations(label, name, null, depth);
        return ResponseEntity.ok(result);
    }

    /**
     * 查询症状诊断
     */
    @GetMapping("/diagnosis/{symptomName}")
    public ResponseEntity<QueryResultDTO> findDiagnoses(@PathVariable String symptomName) {
        // URL解码中文症状名称
        String decodedName = decodeUrlParam(symptomName);
        log.info("查询症状诊断: symptomName={}", decodedName);
        QueryResultDTO result = knowledgeGraphService.findSymptomDiagnoses(decodedName);
        return ResponseEntity.ok(result);
    }

    /**
     * 查询药品适应症
     */
    @GetMapping("/drug-indications/{drugName}")
    public ResponseEntity<QueryResultDTO> findDrugIndications(@PathVariable String drugName) {
        // URL解码中文药品名称
        String decodedName = decodeUrlParam(drugName);
        log.info("查询药品适应症: drugName={}", decodedName);
        QueryResultDTO result = knowledgeGraphService.findDrugIndications(decodedName);
        return ResponseEntity.ok(result);
    }

    /**
     * 查找两个节点间的所有路径
     */
    @GetMapping("/paths")
    public ResponseEntity<QueryResultDTO> findPaths(
            @RequestParam String sourceLabel,
            @RequestParam String sourceName,
            @RequestParam String targetLabel,
            @RequestParam String targetName,
            @RequestParam(value = "maxDepth", defaultValue = "5") int maxDepth) {
        
        log.info("查找路径: {} -> {}", sourceName, targetName);
        QueryResultDTO result = knowledgeGraphService.findPaths(
                sourceLabel, sourceName, targetLabel, targetName, maxDepth);
        return ResponseEntity.ok(result);
    }

    // ==================== 管理API ====================

    /**
     * 获取统计信息
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Long>> getStatistics() {
        Map<String, Long> stats = knowledgeGraphService.getStatistics();
        return ResponseEntity.ok(stats);
    }

    /**
     * 症状名称联想
     */
    @GetMapping("/symptoms/suggest")
    public ResponseEntity<java.util.List<String>> suggestSymptoms(
            @RequestParam String prefix,
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(knowledgeGraphService.suggestSymptomNames(prefix, limit));
    }

    /**
     * 将 Neo4j 权威数据同步至 PostgreSQL 症状-ICD 表
     */
    @PostMapping("/sync-to-rdb")
    public ResponseEntity<Map<String, Object>> syncToRdb() {
        int count = symptomIcdSyncService.syncFromNeo4j();
        Map<String, Object> body = new HashMap<>();
        body.put("syncedRelations", count);
        body.put("message", "同步完成");
        return ResponseEntity.ok(body);
    }

    /**
     * 创建索引
     */
    @PostMapping("/index")
    public ResponseEntity<Map<String, String>> createIndex(
            @RequestParam String label,
            @RequestParam String property) {
        
        log.info("创建索引: label={}, property={}", label, property);
        knowledgeGraphService.createIndex(label, property);
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "索引创建成功");
        return ResponseEntity.ok(response);
    }

    /**
     * 创建约束
     */
    @PostMapping("/constraint")
    public ResponseEntity<Map<String, String>> createConstraint(
            @RequestParam String label,
            @RequestParam String property) {
        
        log.info("创建约束: label={}, property={}", label, property);
        knowledgeGraphService.createConstraint(label, property);
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "约束创建成功");
        return ResponseEntity.ok(response);
    }

    /**
     * 清空所有数据
     */
    @DeleteMapping("/clear")
    public ResponseEntity<Map<String, String>> clearAll() {
        log.warn("清空所有知识图谱数据");
        knowledgeGraphService.clearAll();
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "数据已清空");
        return ResponseEntity.ok(response);
    }

    // ==================== 实体抽取API ====================

    /**
     * 从文本提取实体
     */
    @PostMapping("/extract")
    public ResponseEntity<Map<String, Object>> extractEntities(@RequestBody Map<String, String> request) {
        String text = request.get("text");
        if (text == null || text.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        log.info("从文本中提取实体: {}", text.substring(0, Math.min(50, text.length())));
        
        var symptoms = entityExtractionService.extractSymptoms(text);
        var diseases = entityExtractionService.extractDiseases(text);
        
        Map<String, Object> result = new HashMap<>();
        result.put("symptoms", symptoms);
        result.put("diseases", diseases);
        result.put("symptomCount", symptoms.size());
        result.put("diseaseCount", diseases.size());
        
        return ResponseEntity.ok(result);
    }

    /**
     * 从病历提取并构建知识图谱
     */
    @PostMapping("/extract-from-record")
    public ResponseEntity<Map<String, String>> extractFromMedicalRecord(@RequestBody Map<String, String> request) {
        String recordText = request.get("recordText");
        if (recordText == null || recordText.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        log.info("从病历中提取知识图谱");
        entityExtractionService.extractFromMedicalRecord(recordText);
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "知识图谱构建完成");
        return ResponseEntity.ok(response);
    }

    /**
     * 健康检查
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "Knowledge Graph Service");
        log.info("健康检查被调用");
        return ResponseEntity.ok(response);
    }

    /**
     * 简单的测试接口（验证API是否正常工作）
     */
    @GetMapping("/test")
    public ResponseEntity<Map<String, Object>> test() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "知识图谱API正常运行");
        response.put("timestamp", System.currentTimeMillis());
        log.info("测试接口被调用");
        return ResponseEntity.ok(response);
    }

    /**
     * URL解码工具方法
     * 处理URL路径中的中文参数
     */
    private String decodeUrlParam(String param) {
        if (param == null) {
            return null;
        }
        try {
            // 尝试URL解码（处理已编码的中文）
            String decoded = URLDecoder.decode(param, StandardCharsets.UTF_8.name());
            // 检查是否包含%字符（判断是否已经被编码）
            if (!param.contains("%")) {
                // 如果没有%字符，说明可能已经是解码后的中文，直接返回
                return param;
            }
            return decoded;
        } catch (UnsupportedEncodingException e) {
            log.warn("URL解码失败: {}", param, e);
            return param;
        }
    }
}
