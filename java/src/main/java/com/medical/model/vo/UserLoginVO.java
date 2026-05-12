package com.medical.model.vo;

import com.medical.model.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Schema(description = "用户登录响应")
public class UserLoginVO {

    @Schema(description = "用户ID", example = "1")
    private Long id;

    @Schema(description = "登录Token", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String token;

    @Schema(description = "用户昵称", example = "张三")
    private String userName;

    @Schema(description = "用户头像", example = "https://example.com/avatar.jpg")
    private String userAvatar;

    @Schema(description = "用户角色", example = "user")
    private String userRole;

    @Schema(description = "Token过期时间", example = "2026-05-19T10:30:00")
    private LocalDateTime expireTime;
}
