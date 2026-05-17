package com.medical.model.vo;

import com.medical.model.entity.FeeItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 费用项视图对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeeItemVO {

    private Long id;

    private String feeItemNo;

    private Long userId;

    private Long appointmentId;

    private Long prescriptionId;

    private Long billId;

    private String itemType;

    private String itemName;

    private String itemCode;

    private BigDecimal quantity;

    private BigDecimal unitPrice;

    private BigDecimal totalAmount;

    private BigDecimal discountAmount;

    private BigDecimal actualAmount;

    private BigDecimal insuranceAmount;

    private BigDecimal selfPayAmount;

    private String status;

    private Boolean settleFlag;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    public static FeeItemVO fromEntity(FeeItem entity) {
        if (entity == null) {
            return null;
        }
        return FeeItemVO.builder()
                .id(entity.getId())
                .feeItemNo(entity.getFeeItemNo())
                .userId(entity.getUserId())
                .appointmentId(entity.getAppointmentId())
                .prescriptionId(entity.getPrescriptionId())
                .billId(entity.getBillId())
                .itemType(entity.getItemType())
                .itemName(entity.getItemName())
                .itemCode(entity.getItemCode())
                .quantity(entity.getQuantity())
                .unitPrice(entity.getUnitPrice())
                .totalAmount(entity.getTotalAmount())
                .discountAmount(entity.getDiscountAmount())
                .actualAmount(entity.getActualAmount())
                .insuranceAmount(entity.getInsuranceAmount())
                .selfPayAmount(entity.getSelfPayAmount())
                .status(entity.getStatus())
                .settleFlag(entity.getSettleFlag())
                .createTime(entity.getCreateTime())
                .updateTime(entity.getUpdateTime())
                .build();
    }
}