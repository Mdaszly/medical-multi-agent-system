package com.medical.model.vo;

import com.medical.model.entity.Bill;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 账单视图对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BillVO {

    private Long id;

    private String billNo;

    private Long userId;

    private Long appointmentId;

    private BigDecimal totalAmount;

    private BigDecimal discountAmount;

    private BigDecimal insuranceAmount;

    private BigDecimal selfPayAmount;

    private BigDecimal paidAmount;

    private String status;

    private LocalDateTime payTime;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private List<FeeItemVO> feeItems;

    public static BillVO fromEntity(Bill entity) {
        if (entity == null) {
            return null;
        }
        return BillVO.builder()
                .id(entity.getId())
                .billNo(entity.getBillNo())
                .userId(entity.getUserId())
                .appointmentId(entity.getAppointmentId())
                .totalAmount(entity.getTotalAmount())
                .discountAmount(entity.getDiscountAmount())
                .insuranceAmount(entity.getInsuranceAmount())
                .selfPayAmount(entity.getSelfPayAmount())
                .paidAmount(entity.getPaidAmount())
                .status(entity.getStatus())
                .payTime(entity.getPayTime())
                .remark(entity.getRemark())
                .createTime(entity.getCreateTime())
                .updateTime(entity.getUpdateTime())
                .build();
    }
}