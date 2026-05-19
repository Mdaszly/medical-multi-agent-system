package com.medical.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("symptom_icd_rel")
public class SymptomIcdRelEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long symptomId;

    private String icdCode;

    private Integer priority;

    private LocalDateTime createTime;
}
