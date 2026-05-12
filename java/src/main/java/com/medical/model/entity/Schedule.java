package com.medical.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("schedule")
public class Schedule {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long doctorId;

    private String doctorName;

    private String department;

    private LocalDate scheduleDate;

    private String shiftType;

    private String shiftName;

    private String timeRange;

    private String timeStart;

    private String timeEnd;

    private Integer maxAppointments;

    private Integer currentAppointments;

    private Integer status;

    private Integer onDutyStatus;

    private String description;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer isDelete;
}