package com.medical.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "medical.ai.graph")
public class MedicalGraphProperties {

    private boolean enabled = true;
    private boolean preEnrich = true;
    private boolean validateIcd = true;
    private int fuzzySymptomLimit = 5;
    private boolean fallbackToStaticIcd = true;
    private boolean bootstrapOnStartup = true;
    private boolean syncToRdbOnStartup = false;

    private SymptomResolver symptomResolver = new SymptomResolver();
    private ClinicalSpan clinicalSpan = new ClinicalSpan();

    @Data
    public static class ClinicalSpan {
        /** 启用临床片段抽取（结构化主诉 + 规则/LLM 抽槽） */
        private boolean enabled = true;
        /** 规则未命中时是否调用 LLM 抽槽 */
        private boolean llmEnabled = false;
    }

    @Data
    public static class SymptomResolver {
        /** 启用症状语义解析（向量 + LLM 双层） */
        private boolean enabled = true;
        /** 同义词表快速路径 */
        private boolean synonymEnabled = true;
        /** 向量召回 Top-K */
        private int vectorTopK = 5;
        /** 向量最高分低于该阈值时触发 LLM 消歧 */
        private double vectorAmbiguityGap = 0.05;
        /** 向量相似度接受阈值（余弦） */
        private double vectorMinScore = 0.72;
        /** 最终写入图谱的最低置信度（与 vectorMinScore 对齐，避免弱匹配误召） */
        private double acceptMinConfidence = 0.72;
        /** 是否在模糊候选时调用 LLM */
        private boolean llmDisambiguate = true;
        /** 向量已高置信时跳过 LLM */
        private boolean llmOnlyWhenAmbiguous = true;
        /** 禁止在向量无候选时对全量词表做 LLM 硬配 */
        private boolean llmRequireVectorCandidates = true;
        private String embeddingModel = "text-embedding-v3";
        /** 启动时构建内存向量索引 */
        private boolean buildIndexOnStartup = true;
        /** 索引批大小 */
        private int embeddingBatchSize = 16;
    }

}
