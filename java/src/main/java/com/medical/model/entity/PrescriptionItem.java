package com.medical.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("prescription_item")
public class PrescriptionItem implements Serializable {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("prescription_id")
    private Long prescriptionId;

    @TableField("drug_code")
    private String drugCode;

    @TableField("drug_name")
    private String drugName;

    @TableField("specification")
    private String specification;

    @TableField("dosage")
    private String dosage;

    @TableField("usage")
    private String usage;

    @TableField("frequency")
    private String frequency;

    @TableField("duration")
    private String duration;

    @TableField("quantity")
    private BigDecimal quantity;

    @TableField("unit_price")
    private BigDecimal unitPrice;

    @TableField("total_amount")
    private BigDecimal totalAmount;

    @TableField("remark")
    private String remark;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    private static final long serialVersionUID = 1L;
}