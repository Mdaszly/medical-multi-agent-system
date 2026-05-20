package com.medical.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AuthLoginRequest {

    @NotBlank(message = "账号不能为空")
    private String userAccount;

    @NotBlank(message = "密码不能为空")
    private String password;
}