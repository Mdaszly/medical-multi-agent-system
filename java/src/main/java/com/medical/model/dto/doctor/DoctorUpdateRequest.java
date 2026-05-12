package com.medical.model.dto.doctor;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "医生更新请求")
public class DoctorUpdateRequest {

    @Schema(description = "医生姓名")
    private String doctorName;

    @Schema(description = "性别：0-未知，1-男，2-女")
    private Integer gender;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "科室")
    private String department;

    @Schema(description = "职称")
    private String title;

    @Schema(description = "擅长领域")
    private String specialty;

    @Schema(description = "执业医师证号")
    private String licenseNo;

    @Schema(description = "所属医院名称")
    private String hospitalName;

    @Schema(description = "挂号费用")
    private BigDecimal consultationFee;

    @Schema(description = "医生简介")
    private String description;

    @Schema(description = "工作状态：0-休假，1-在岗，2-离职")
    private Integer workStatus;
}