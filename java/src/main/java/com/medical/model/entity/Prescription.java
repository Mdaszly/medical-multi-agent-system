package com.medical.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("prescription")
public class Prescription implements Serializable {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("prescription_no")
    private String prescriptionNo;

    @TableField("appointment_id")
    private Long appointmentId;

    @TableField("user_id")
    private Long userId;

    @TableField("user_name")
    private String userName;

    @TableField("doctor_id")
    private Long doctorId;

    @TableField("doctor_name")
    private String doctorName;

    @TableField("department")
    private String department;

    @TableField("diagnosis")
    private String diagnosis;

    @TableField("drugs")
    private String drugs;

    @TableField("total_amount")
    private BigDecimal totalAmount;

    @TableField("status")
    private Integer status;

    @TableField("audit_time")
    private LocalDateTime auditTime;

    @TableField("audit_user_id")
    private Long auditUserId;

    @TableField("audit_remark")
    private String auditRemark;

    @TableField("dispense_time")
    private LocalDateTime dispenseTime;

    @TableField("remark")
    private String remark;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    private static final long serialVersionUID = 1L;
}