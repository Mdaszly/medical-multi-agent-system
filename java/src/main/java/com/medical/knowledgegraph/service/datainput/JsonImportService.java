package com.medical.knowledgegraph.service.datainput;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.medical.knowledgegraph.exception.KnowledgeGraphException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;

/**
 * JSON数据导入服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JsonImportService {

    private final ObjectMapper objectMapper;

    /**
     * 导入JSON文件
     *
     * @param filePath 文件路径
     * @param consumer 数据处理器
     * @return 导入统计
     */
    public CsvImportService.ImportStats importJson(String filePath, 
                                                    Consumer<Map<String, Object>> consumer) {
        log.info("开始导入JSON文件: {}", filePath);
        
        CsvImportService.ImportStats stats = new CsvImportService.ImportStats();
        stats.setStartTime(System.currentTimeMillis());
        
        try {
            Path path = Path.of(filePath);
            if (!Files.exists(path)) {
                throw new KnowledgeGraphException("FILE_NOT_FOUND", "文件不存在: " + filePath);
            }
            
            String content = Files.readString(path);
            JsonNode rootNode = objectMapper.readTree(content);
            
            if (rootNode.isArray()) {
                // JSON数组格式
                ArrayNode arrayNode = (ArrayNode) rootNode;
                int index = 0;
                for (JsonNode node : arrayNode) {
                    index++;
                    try {
                        Map<String, Object> data = objectMapper.convertValue(
                                node, 
                                new TypeReference<Map<String, Object>>() {}
                        );
                        consumer.accept(data);
                        stats.incrementSuccess();
                    } catch (Exception e) {
                        log.warn("处理JSON对象 {} 失败: {}", index, e.getMessage());
                        stats.incrementFailure();
                        stats.addError("对象 " + index + ": " + e.getMessage());
                    }
                }
            } else if (rootNode.isObject()) {
                // JSON对象格式
                ObjectNode objectNode = (ObjectNode) rootNode;
                
                // 检查是否有data数组字段
                if (objectNode.has("data")) {
                    JsonNode dataNode = objectNode.get("data");
                    if (dataNode.isArray()) {
                        int index = 0;
                        for (JsonNode node : dataNode) {
                            index++;
                            try {
                                Map<String, Object> data = objectMapper.convertValue(
                                        node,
                                        new TypeReference<Map<String, Object>>() {}
                                );
                                consumer.accept(data);
                                stats.incrementSuccess();
                            } catch (Exception e) {
                                log.warn("处理JSON对象 {} 失败: {}", index, e.getMessage());
                                stats.incrementFailure();
                                stats.addError("对象 " + index + ": " + e.getMessage());
                            }
                        }
                    }
                } else if (objectNode.has("records")) {
                    // 另一种常见格式
                    JsonNode recordsNode = objectNode.get("records");
                    if (recordsNode.isArray()) {
                        int index = 0;
                        for (JsonNode node : recordsNode) {
                            index++;
                            try {
                                Map<String, Object> data = objectMapper.convertValue(
                                        node,
                                        new TypeReference<Map<String, Object>>() {}
                                );
                                consumer.accept(data);
                                stats.incrementSuccess();
                            } catch (Exception e) {
                                log.warn("处理JSON对象 {} 失败: {}", index, e.getMessage());
                                stats.incrementFailure();
                                stats.addError("对象 " + index + ": " + e.getMessage());
                            }
                        }
                    }
                } else {
                    // 单个对象
                    try {
                        Map<String, Object> data = objectMapper.convertValue(
                                rootNode,
                                new TypeReference<Map<String, Object>>() {}
                        );
                        consumer.accept(data);
                        stats.incrementSuccess();
                    } catch (Exception e) {
                        log.warn("处理JSON对象失败: {}", e.getMessage());
                        stats.incrementFailure();
                        stats.addError(e.getMessage());
                    }
                }
            }
            
            stats.setEndTime(System.currentTimeMillis());
            stats.setTotalProcessed(stats.getSuccessCount());
            log.info("JSON导入完成: 总记录={}, 成功={}, 失败={}", 
                    stats.getTotalProcessed(), stats.getSuccessCount(), stats.getFailureCount());
            
        } catch (Exception e) {
            log.error("JSON导入失败", e);
            throw new KnowledgeGraphException("JSON_IMPORT_ERROR", "JSON导入失败: " + e.getMessage(), e);
        }
        
        return stats;
    }

    /**
     * 导入JSON Lines格式文件 (每行一个JSON对象)
     *
     * @param filePath 文件路径
     * @param consumer 数据处理器
     * @return 导入统计
     */
    public CsvImportService.ImportStats importJsonLines(String filePath,
                                                         Consumer<Map<String, Object>> consumer) {
        log.info("开始导入JSON Lines文件: {}", filePath);
        
        CsvImportService.ImportStats stats = new CsvImportService.ImportStats();
        stats.setStartTime(System.currentTimeMillis());
        
        try {
            Path path = Path.of(filePath);
            if (!Files.exists(path)) {
                throw new KnowledgeGraphException("FILE_NOT_FOUND", "文件不存在: " + filePath);
            }
            
            List<String> lines = Files.readAllLines(path);
            int lineNumber = 0;
            
            for (String line : lines) {
                lineNumber++;
                
                if (line.trim().isEmpty()) {
                    continue;
                }
                
                try {
                    Map<String, Object> data = objectMapper.readValue(
                            line,
                            new TypeReference<Map<String, Object>>() {}
                    );
                    consumer.accept(data);
                    stats.incrementSuccess();
                } catch (Exception e) {
                    log.warn("处理JSON Line {} 失败: {}", lineNumber, e.getMessage());
                    stats.incrementFailure();
                    stats.addError("行 " + lineNumber + ": " + e.getMessage());
                }
                
                if (stats.getSuccessCount() % 1000 == 0) {
                    log.info("已处理 {} 条记录", stats.getSuccessCount());
                }
            }
            
            stats.setEndTime(System.currentTimeMillis());
            stats.setTotalProcessed(stats.getSuccessCount());
            log.info("JSON Lines导入完成: 总记录={}, 成功={}, 失败={}", 
                    stats.getTotalProcessed(), stats.getSuccessCount(), stats.getFailureCount());
            
        } catch (Exception e) {
            log.error("JSON Lines导入失败", e);
            throw new KnowledgeGraphException("JSON_LINES_IMPORT_ERROR", 
                    "JSON Lines导入失败: " + e.getMessage(), e);
        }
        
        return stats;
    }
}
