package com.medical.model.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "管理员更新请求")
public class AdminUpdateRequest {

    @Schema(description = "管理员账号")
    private String userAccount;

    @Schema(description = "管理员姓名")
    private String userName;

    @Schema(description = "密码")
    private String password;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "账号状态：0-禁用，1-正常")
    private Integer userStatus;
}