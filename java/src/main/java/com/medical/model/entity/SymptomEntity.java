package com.medical.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("symptom")
public class SymptomEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String namePinyin;

    private String categoryCode;

    private String categoryName;

    private Integer frequency;

    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
