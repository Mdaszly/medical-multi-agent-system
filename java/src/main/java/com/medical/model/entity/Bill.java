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
 * 账单实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("bill")
public class Bill {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("bill_no")
    private String billNo;

    @TableField("user_id")
    private Long userId;

    @TableField("appointment_id")
    private Long appointmentId;

    @TableField("total_amount")
    private BigDecimal totalAmount;

    @TableField("discount_amount")
    private BigDecimal discountAmount;

    @TableField("insurance_amount")
    private BigDecimal insuranceAmount;

    @TableField("self_pay_amount")
    private BigDecimal selfPayAmount;

    @TableField("paid_amount")
    private BigDecimal paidAmount;

    @TableField("status")
    private String status;

    @TableField("pay_time")
    private LocalDateTime payTime;

    @TableField("remark")
    private String remark;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;

    @TableField("version")
    private Integer version;
}