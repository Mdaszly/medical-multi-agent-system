package com.medical.service.kg.clinical;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
public class ClinicalSpanExtractionResult {

    /** 是否识别到可送入 SymptomResolver 的临床文本 */
    private boolean hasClinicalText;

    /** 合并后的临床文本（多个 span 用顿号连接） */
    private String clinicalText;

    @Builder.Default
    private List<String> symptomSpans = new ArrayList<>();

    /** STRUCTURED_FIELD | CHAT_SPAN | LLM_SPAN */
    private String source;

    /** 跳过图谱检索时的原因码 */
    private String skipReason;

    /** 可观测轨迹，写入 GraphEvidence */
    private String traceSummary;

    public static ClinicalSpanExtractionResult skipped(String skipReason, String traceSummary) {
        return ClinicalSpanExtractionResult.builder()
                .hasClinicalText(false)
                .skipReason(skipReason)
                .traceSummary(traceSummary)
                .build();
    }

    public static ClinicalSpanExtractionResult ok(String clinicalText,
                                                  List<String> spans,
                                                  String source,
                                                  String traceSummary) {
        return ClinicalSpanExtractionResult.builder()
                .hasClinicalText(true)
                .clinicalText(clinicalText)
                .symptomSpans(spans)
                .source(source)
                .traceSummary(traceSummary)
                .build();
    }
}
