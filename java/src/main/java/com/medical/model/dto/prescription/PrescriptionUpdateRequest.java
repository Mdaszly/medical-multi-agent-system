package com.medical.model.dto.prescription;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 处方修改请求DTO
 */
@Data
@Schema(description = "处方修改请求")
public class PrescriptionUpdateRequest {

    @Schema(description = "处方ID", required = true)
    private Long id;

    @Schema(description = "诊断信息")
    private String diagnosis;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "药品列表")
    private List<PrescriptionDrugItem> drugs;

    @Schema(description = "处方药品项")
    @Data
    public static class PrescriptionDrugItem {
        @Schema(description = "明细ID（修改时必填）")
        private Long id;

        @Schema(description = "药品编码", required = true)
        private String drugCode;

        @Schema(description = "药品名称", required = true)
        private String drugName;

        @Schema(description = "规格")
        private String specification;

        @Schema(description = "剂量")
        private String dosage;

        @Schema(description = "用法", required = true)
        private String usage;

        @Schema(description = "频次", required = true)
        private String frequency;

        @Schema(description = "用药时长（天数）", required = true)
        private String duration;

        @Schema(description = "数量", required = true)
        private BigDecimal quantity;
    }
}