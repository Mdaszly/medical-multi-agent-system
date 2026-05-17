package com.medical.model.vo;

import com.medical.model.entity.Payment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付视图对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentVO {

    private Long id;

    private String paymentNo;

    private Long billId;

    private Long userId;

    private String userName;

    private BigDecimal amount;

    private String paymentType;

    private String thirdPartyNo;

    private Integer status;

    private String statusDesc;

    private LocalDateTime payTime;

    private String callbackData;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    public static PaymentVO fromEntity(Payment entity) {
        if (entity == null) {
            return null;
        }
        return PaymentVO.builder()
                .id(entity.getId())
                .paymentNo(entity.getPaymentNo())
                .billId(entity.getBillId())
                .userId(entity.getUserId())
                .userName(entity.getUserName())
                .amount(entity.getAmount())
                .paymentType(entity.getPaymentType())
                .thirdPartyNo(entity.getThirdPartyNo())
                .status(entity.getStatus())
                .statusDesc(getStatusDescription(entity.getStatus()))
                .payTime(entity.getPayTime())
                .callbackData(entity.getCallbackData())
                .createTime(entity.getCreateTime())
                .updateTime(entity.getUpdateTime())
                .build();
    }

    private static String getStatusDescription(Integer status) {
        if (status == null) {
            return "未知";
        }
        return switch (status) {
            case 0 -> "待支付";
            case 1 -> "已支付";
            case 2 -> "已取消";
            case 3 -> "已退款";
            default -> "未知";
        };
    }
}