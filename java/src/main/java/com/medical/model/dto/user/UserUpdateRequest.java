package com.medical.model.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "用户更新请求")
public class UserUpdateRequest {

    @Schema(description = "用户姓名")
    private String userName;

    @Schema(description = "性别：0-未知，1-男，2-女")
    private Integer gender;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "紧急联系人")
    private String emergencyContact;

    @Schema(description = "紧急联系电话")
    private String emergencyPhone;

    @Schema(description = "用户状态：0-禁用，1-正常")
    private Integer userStatus;
}