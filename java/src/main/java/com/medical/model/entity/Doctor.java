package com.medical.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("doctor")
public class Doctor {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String doctorNo;

    private String doctorName;

    private String department;

    private String title;

    private String specialty;

    private String licenseNo;

    private Long hospitalId;

    private String hospitalName;

    private BigDecimal consultationFee;

    private String description;

    private String phone;

    private String email;

    private Integer workStatus;

    private Integer gender;

    private String userAccount;

    private String userPassword;

    private String salt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer isDelete;
}