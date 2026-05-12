package com.medical.model.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "管理员查询请求")
public class AdminQueryRequest {

    @Schema(description = "当前页码", defaultValue = "1")
    private Long current = 1L;

    @Schema(description = "每页数量", defaultValue = "10")
    private Long pageSize = 10L;

    @Schema(description = "管理员账号（模糊查询）")
    private String userAccount;

    @Schema(description = "管理员姓名（模糊查询）")
    private String userName;

    @Schema(description = "账号状态：0-禁用，1-正常")
    private Integer userStatus;
}