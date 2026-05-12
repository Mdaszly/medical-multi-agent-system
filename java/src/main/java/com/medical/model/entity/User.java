package com.medical.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("\"user\"")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String userAccount;

    private String userPassword;

    private String salt;

    private String userName;

    private String userRole;

    private String phone;

    private String email;

    private Integer gender;

    private LocalDate birthDate;

    private String idCard;

    private String emergencyContact;

    private String emergencyPhone;

    private String medicalHistory;

    private String allergyHistory;

    private Integer userStatus;

    private LocalDateTime lastLoginTime;

    private String lastLoginIp;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer isDelete;
}