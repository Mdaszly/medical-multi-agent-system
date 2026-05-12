package com.medical.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class AuthRegisterRequest {

    @NotBlank(message = "账号不能为空")
    @Size(min = 4, max = 20, message = "账号长度必须在4-20位之间")
    private String userAccount;

    @NotBlank(message = "密码不能为空")
    @Size(min = 8, max = 20, message = "密码长度必须在8-20位之间")
    private String userPassword;

    @NotBlank(message = "确认密码不能为空")
    private String checkPassword;

    private String userName;

    private String phone;

    private String email;

    @NotBlank(message = "角色不能为空")
    private String userRole;

    private String department;

    private String title;

    private String licenseNo;

    private BigDecimal consultationFee;
}