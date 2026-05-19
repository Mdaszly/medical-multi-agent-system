package com.medical.knowledgegraph.service.datainput;

import com.medical.knowledgegraph.exception.KnowledgeGraphException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;

/**
 * CSV数据导入服务
 */
@Slf4j
@Service
public class CsvImportService {

    /**
     * 导入CSV文件
     *
     * @param filePath 文件路径
     * @param skipHeader 是否跳过标题行
     * @param delimiter 分隔符
     * @param encoding 编码
     * @param consumer 行数据处理器
     * @return 导入统计
     */
    public ImportStats importCsv(String filePath, boolean skipHeader, 
                                  String delimiter, String encoding,
                                  Consumer<Map<String, String>> consumer) {
        log.info("开始导入CSV文件: {}", filePath);
        
        ImportStats stats = new ImportStats();
        stats.setStartTime(System.currentTimeMillis());
        
        try {
            Path path = Path.of(filePath);
            if (!Files.exists(path)) {
                throw new KnowledgeGraphException("FILE_NOT_FOUND", "文件不存在: " + filePath);
            }
            
            List<String> headers = new ArrayList<>();
            int lineNumber = 0;
            int processedCount = 0;
            
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(
                            new FileInputStream(filePath),
                            (encoding != null ? encoding : StandardCharsets.UTF_8).toString()))) {
                
                String line;
                boolean firstLine = true;
                
                while ((line = reader.readLine()) != null) {
                    lineNumber++;
                    
                    if (line.trim().isEmpty()) {
                        continue;
                    }
                    
                    // 解析CSV行
                    String[] values = parseCsvLine(line, delimiter);
                    
                    if (firstLine) {
                        if (skipHeader) {
                            headers = Arrays.asList(values);
                            firstLine = false;
                            stats.incrementSkipped();
                            continue;
                        } else {
                            // 使用行号作为列名
                            for (int i = 0; i < values.length; i++) {
                                headers.add("column_" + i);
                            }
                        }
                    }
                    
                    // 转换为Map
                    Map<String, String> rowData = new LinkedHashMap<>();
                    for (int i = 0; i < headers.size() && i < values.length; i++) {
                        rowData.put(headers.get(i), values[i].trim());
                    }
                    
                    try {
                        consumer.accept(rowData);
                        processedCount++;
                        stats.incrementSuccess();
                    } catch (Exception e) {
                        log.warn("处理行 {} 失败: {}", lineNumber, e.getMessage());
                        stats.incrementFailure();
                        stats.addError("行 " + lineNumber + ": " + e.getMessage());
                    }
                    
                    if (processedCount % 1000 == 0) {
                        log.info("已处理 {} 条记录", processedCount);
                    }
                }
            }
            
            stats.setEndTime(System.currentTimeMillis());
            stats.setTotalProcessed(processedCount);
            log.info("CSV导入完成: 总记录={}, 成功={}, 失败={}", 
                    processedCount, stats.getSuccessCount(), stats.getFailureCount());
            
        } catch (Exception e) {
            log.error("CSV导入失败", e);
            throw new KnowledgeGraphException("CSV_IMPORT_ERROR", "CSV导入失败: " + e.getMessage(), e);
        }
        
        return stats;
    }

    /**
     * 解析CSV行（处理引号）
     */
    private String[] parseCsvLine(String line, String delimiter) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == delimiter.charAt(0) && !inQuotes) {
                result.add(current.toString().replace("\"\"", "\""));
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        
        result.add(current.toString().replace("\"\"", "\""));
        return result.toArray(new String[0]);
    }

    /**
     * 导入统计
     */
    @lombok.Data
    public static class ImportStats {
        private long startTime;
        private long endTime;
        private int totalProcessed;
        private int successCount;
        private int failureCount;
        private int skipped;
        private List<String> errors = new ArrayList<>();

        public void incrementSuccess() {
            this.successCount++;
        }

        public void incrementFailure() {
            this.failureCount++;
        }

        public void incrementSkipped() {
            this.skipped++;
        }

        public void addError(String error) {
            this.errors.add(error);
        }

        public long getDuration() {
            return endTime - startTime;
        }
    }
}
