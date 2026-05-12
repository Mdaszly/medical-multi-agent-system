package com.medical.model.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "用户查询请求")
public class UserQueryRequest {

    @Schema(description = "当前页码", defaultValue = "1")
    private Long current = 1L;

    @Schema(description = "每页数量", defaultValue = "10")
    private Long pageSize = 10L;

    @Schema(description = "用户账号（模糊查询）")
    private String userAccount;

    @Schema(description = "用户姓名（模糊查询）")
    private String userName;

    @Schema(description = "角色：user/admin")
    private String userRole;

    @Schema(description = "账号状态：0-禁用，1-正常")
    private Integer userStatus;
}