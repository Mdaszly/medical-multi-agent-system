package com.medical.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 预约记录实体类
 * 
 * <p>功能说明：
 * 1. 记录患者的预约挂号信息
 * 2. 维护预约状态流转（待就诊->已签到->已完成）
 * 3. 关联排班、医生、科室等核心业务数据
 * 
 * <p>设计优化：
 * - 冗余字段设计（userName、doctorName）：避免多表关联查询，提升查询性能
 * - 状态机设计：规范预约流程的完整生命周期
 * - 时间维度记录：签到、取消等关键节点都有时间戳记录
 */
@Data
@TableName("appointment")
public class Appointment {

    /**
     * 主键ID，自增
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 预约编号，唯一业务标识
     * 格式：APT + yyyyMMddHHmmss + 8位随机字符
     */
    private String appointmentNo;

    /**
     * 用户ID（患者ID）
     * 关联用户表
     */
    private Long userId;

    /**
     * 用户姓名（冗余字段）
     * 优化：避免查询时关联用户表，提升性能
     */
    private String userName;

    /**
     * 医生ID
     * 关联医生表
     */
    private Long doctorId;

    /**
     * 医生姓名（冗余字段）
     * 优化：避免查询时无需关联医生表
     */
    private String doctorName;

    /**
     * 科室名称
     */
    private String department;

    /**
     * 排班ID
     * 关联排班表
     */
    private Long scheduleId;

    /**
     * 就诊日期
     */
    private LocalDate scheduleDate;

    /**
     * 班次类型
     * 早班/中班/晚班
     */
    private String shiftType;

    /**
     * 具体时段
     * 如：08:00-08:30
     */
    private String timeSlot;

    /**
     * 挂号费用
     */
    private BigDecimal consultationFee;

    /**
     * 预约状态
     * 0-待就诊，1-已签到，2-诊疗中，3-已完成，4-已取消，5-已过期，6-已结算
     */
    private Integer status;

    /**
     * 签到时间
     */
    private LocalDateTime checkInTime;

    /**
     * 签到状态
     */
    private Boolean checkInStatus;

    /**
     * 取消时间
     */
    private LocalDateTime cancelTime;

    /**
     * 取消原因
     */
    private String cancelReason;

    /**
     * 备注
     */
    private String remark;

    /**
     * 创建时间
     * MyBatis-Plus自动填充（插入时）
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     * MyBatis-Plus自动填充（插入和更新时）
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
