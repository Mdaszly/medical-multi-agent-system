package com.medical.model.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "用户注册请求")
public class UserRegisterRequest {

    @NotBlank(message = "账号不能为空")
    @Size(min = 4, max = 20, message = "账号长度必须在4-20位之间")
    @Schema(description = "用户账号", example = "zhangsan", requiredMode = Schema.RequiredMode.REQUIRED)
    private String userAccount;

    @NotBlank(message = "密码不能为空")
    @Size(min = 8, max = 20, message = "密码长度必须在8-20位之间")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$", message = "密码必须包含大小写字母和数字")
    @Schema(description = "登录密码", example = "Abc123456", requiredMode = Schema.RequiredMode.REQUIRED)
    private String userPassword;

    @NotBlank(message = "确认密码不能为空")
    @Schema(description = "确认密码", example = "Abc123456", requiredMode = Schema.RequiredMode.REQUIRED)
    private String checkPassword;

    @Schema(description = "用户昵称", example = "张三")
    private String userName;

    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    @Schema(description = "手机号", example = "13812345678")
    private String phone;

    @Schema(description = "邮箱", example = "zhangsan@example.com")
    private String email;
}
