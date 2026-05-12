package com.medical.model.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "用户登录请求")
public class UserLoginRequest {

    @Schema(description = "用户账号", example = "zhangsan")
    private String userAccount;

    @Schema(description = "手机号", example = "13812345678")
    private String phone;

    @NotBlank(message = "密码不能为空")
    @Schema(description = "登录密码", example = "Abc123456", requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;

    @NotBlank(message = "登录类型不能为空")
    @Schema(description = "登录类型", example = "account", requiredMode = Schema.RequiredMode.REQUIRED)
    private String loginType;
}
