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
 * 支付实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("payment")
public class Payment {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("payment_no")
    private String paymentNo;

    @TableField("bill_id")
    private Long billId;

    @TableField("user_id")
    private Long userId;

    @TableField("user_name")
    private String userName;

    @TableField("amount")
    private BigDecimal amount;

    @TableField("payment_type")
    private String paymentType;

    @TableField("third_party_no")
    private String thirdPartyNo;

    @TableField("status")
    private Integer status;

    @TableField("pay_time")
    private LocalDateTime payTime;

    @TableField("callback_data")
    private String callbackData;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;

    @TableField("version")
    private Integer version;
}