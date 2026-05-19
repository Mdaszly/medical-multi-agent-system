package com.medical.knowledgegraph.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 查询结果数据传输对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueryResultDTO {

    /**
     * 查询ID
     */
    private String queryId;

    /**
     * 查询语句
     */
    private String query;

    /**
     * 查询类型
     */
    private String queryType;

    /**
     * 查询执行时间 (毫秒)
     */
    private Long executionTime;

    /**
     * 返回结果总数
     */
    private Integer totalCount;

    /**
     * 节点列表
     */
    private List<NodeResult> nodes;

    /**
     * 关系列表
     */
    private List<RelationResult> relations;

    /**
     * 路径列表
     */
    private List<PathResult> paths;

    /**
     * 表格化标量结果（症状-疾病-ICD 等）
     */
    private List<Map<String, Object>> records;

    /**
     * 分页信息
     */
    private Pagination pagination;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NodeResult {
        private String id;
        private String label;
        private String name;
        private Map<String, Object> properties;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RelationResult {
        private String sourceId;
        private String targetId;
        private String type;
        private Map<String, Object> properties;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PathResult {
        private List<NodeResult> nodes;
        private List<RelationResult> relationships;
        private Double weight;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Pagination {
        private Integer page;
        private Integer pageSize;
        private Integer totalPages;
        private Boolean hasNext;
        private Boolean hasPrevious;
    }
}
