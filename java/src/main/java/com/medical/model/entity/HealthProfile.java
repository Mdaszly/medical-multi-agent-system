package com.medical.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("health_profile")
public class HealthProfile {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String userName;

    private String chronicDiseases;

    private String allergyHistory;

    private String medicationHistory;

    private String familyHistory;

    private String surgicalHistory;

    private String vaccinationHistory;

    private String physicalExam;

    private BigDecimal height;

    private BigDecimal weight;

    private String bloodType;

    private String bloodPressure;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}