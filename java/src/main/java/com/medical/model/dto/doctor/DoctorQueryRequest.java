package com.medical.model.dto.doctor;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "医生查询请求")
public class DoctorQueryRequest {

    @Schema(description = "当前页码", defaultValue = "1")
    private Long current = 1L;

    @Schema(description = "每页数量", defaultValue = "10")
    private Long pageSize = 10L;

    @Schema(description = "医生编号")
    private String doctorNo;

    @Schema(description = "医生姓名（模糊查询）")
    private String doctorName;

    @Schema(description = "科室")
    private String department;

    @Schema(description = "职称")
    private String title;

    @Schema(description = "工作状态：0-休假，1-在岗，2-离职")
    private Integer workStatus;

    @Schema(description = "是否仅查询在岗医生（患者端预约推荐 true）")
    private Boolean onlineOnly;
}