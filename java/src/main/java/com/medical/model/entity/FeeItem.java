package com.medical.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 费用项实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("fee_item")
public class FeeItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("fee_item_no")
    private String feeItemNo;

    @TableField("user_id")
    private Long userId;

    @TableField("appointment_id")
    private Long appointmentId;

    @TableField("prescription_id")
    private Long prescriptionId;

    @TableField("bill_id")
    private Long billId;

    @TableField("item_type")
    private String itemType;

    @TableField("item_name")
    private String itemName;

    @TableField("item_code")
    private String itemCode;

    @TableField("quantity")
    private BigDecimal quantity;

    @TableField("unit_price")
    private BigDecimal unitPrice;

    @TableField("total_amount")
    private BigDecimal totalAmount;

    @TableField("discount_amount")
    private BigDecimal discountAmount;

    @TableField("actual_amount")
    private BigDecimal actualAmount;

    @TableField("insurance_amount")
    private BigDecimal insuranceAmount;

    @TableField("self_pay_amount")
    private BigDecimal selfPayAmount;

    @TableField("status")
    private String status;

    @TableField("settle_flag")
    private Boolean settleFlag;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;
}