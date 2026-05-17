package com.medical.model.dto.bill;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 账单生成请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BillGenerateRequest {

    private Long userId;

    private Long appointmentId;

    private Long prescriptionId;
}