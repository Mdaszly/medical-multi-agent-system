package com.medical.knowledgegraph.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 导入任务数据传输对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportTaskDTO {

    /**
     * 任务ID
     */
    private String taskId;

    /**
     * 文件路径
     */
    private String filePath;

    /**
     * 文件类型 (csv, json)
     */
    private String fileType;

    /**
     * 实体类型 (symptom, icd10, drug, drugeffect, disease)
     */
    private String entityType;

    /**
     * 总记录数
     */
    private Integer totalRecords;

    /**
     * 已处理记录数
     */
    private Integer processedRecords;

    /**
     * 成功导入数
     */
    private Integer successCount;

    /**
     * 失败数
     */
    private Integer failureCount;

    /**
     * 任务状态 (PENDING, PROCESSING, COMPLETED, FAILED)
     */
    private String status;

    /**
     * 开始时间
     */
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    private LocalDateTime endTime;

    /**
     * 错误信息列表
     */
    private List<String> errors;

    /**
     * 导入配置
     */
    private ImportConfig config;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ImportConfig {
        /**
         * 是否跳过标题行
         */
        private Boolean skipHeader;

        /**
         * 分隔符 (CSV)
         */
        private String delimiter;

        /**
         * 字段映射
         */
        private Map<String, String> fieldMapping;

        /**
         * 编码
         */
        private String encoding;

        /**
         * 批量大小
         */
        private Integer batchSize;
    }
}
