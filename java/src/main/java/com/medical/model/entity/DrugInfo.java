package com.medical.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("drug_info")
public class DrugInfo implements Serializable {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("drug_code")
    private String drugCode;

    @TableField("drug_name")
    private String drugName;

    @TableField("generic_name")
    private String genericName;

    @TableField("trade_name")
    private String tradeName;

    @TableField("specification")
    private String specification;

    @TableField("unit")
    private String unit;

    @TableField("manufacturer")
    private String manufacturer;

    @TableField("category")
    private String category;

    @TableField("category_code")
    private String categoryCode;

    @TableField("dosage_form")
    private String dosageForm;

    @TableField("prescription_flag")
    private Boolean prescriptionFlag;

    @TableField("status")
    private Integer status;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;

    @TableField("remark")
    private String remark;

    private static final long serialVersionUID = 1L;
}