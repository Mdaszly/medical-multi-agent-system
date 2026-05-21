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

    private Integer userStatus;

    /**
     * 管理员账号类型：FORMAL-正式管理员, TEMPORARY-临时管理员
     * 仅对admin角色有效，非admin角色为NULL
     */
    private String adminAccountType;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer isDelete;
}
