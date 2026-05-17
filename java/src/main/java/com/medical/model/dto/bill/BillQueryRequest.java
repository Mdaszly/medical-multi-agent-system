package com.medical.model.dto.bill;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 账单查询请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BillQueryRequest {

    private Long userId;

    private Long appointmentId;

    private String billNo;

    private String status;
}