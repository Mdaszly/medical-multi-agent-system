package com.medical.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("icd10_code")
public class Icd10CodeEntity {

    @TableId(type = IdType.INPUT)
    private String code;

    private String description;

    private String descriptionCn;

    private String chapterCode;

    private String chapterName;

    private LocalDateTime createTime;
}
