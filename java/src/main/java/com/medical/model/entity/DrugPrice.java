package com.medical.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("drug_price")
public class DrugPrice implements Serializable {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("drug_id")
    private Long drugId;

    @TableField("price_type")
    private String priceType;

    @TableField("price")
    private BigDecimal price;

    @TableField("effective_date")
    private LocalDate effectiveDate;

    @TableField("expire_date")
    private LocalDate expireDate;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("create_user_id")
    private Long createUserId;

    private static final long serialVersionUID = 1L;
}