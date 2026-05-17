package com.medical.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 号源库存表实体类
 * 
 * <p>核心功能：
 * 1. 管理排班的具体时段号源
 * 2. 实现精细化的号源管理（比原排班表的粒度更细）
 * 3. 提供乐观锁机制防止超卖
 * 
 * <p>设计要点：
 * - 乐观锁（version字段）：在数据库层面防止并发超卖
 * - 三重量化指标：maxSlots、availableSlots、lockedSlots，精确跟踪号源状态
 * - 状态标识：AVAILABLE/LOCKED/SOLD_OUT，便于业务判断
 * 
 * <p>为什么需要这个表？
 * 原排班表的号源控制粒度太粗，只能按班次控制。这个表可以细分到具体时段（如08:00-08:30）。
 */
@Data
@TableName("appointment_slot")
public class AppointmentSlot {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 排班ID，关联排班表
     */
    private Long scheduleId;

    /**
     * 医生ID（冗余字段）
     * 优化：避免查询时关联排班表+医生表，提升性能
     */
    private Long doctorId;

    /**
     * 医生姓名（冗余字段）
     * 优化：避免查询时关联医生表，直接显示医生姓名
     */
    private String doctorName;

    /**
     * 具体时段，如：08:00-08:30
     */
    private String timeSlot;

    /**
     * 开始时间，如：08:00
     */
    private String timeStart;

    /**
     * 结束时间，如：08:30
     */
    private String timeEnd;

    /**
     * 最大号源数，初始设置
     */
    private Integer maxSlots;

    /**
     * 可用号源数，实时更新
     * 优化：这个字段是核心，每次预约都会扣减
     */
    private Integer availableSlots;

    /**
     * 锁定号源数（预扣减）
     * 用于：用户进入支付页面时锁定，超时未支付则释放
     */
    private Integer lockedSlots;

    /**
     * 号源状态
     * AVAILABLE-可用，LOCKED-锁定，SOLD_OUT-售罄
     */
    private String status;

    /**
     * 乐观锁版本号
     * 核心优化：防止并发超卖的关键机制
     * 
     * <p>乐观锁工作原理：
     * 1. 查询时获取当前version值
     * 2. 更新时带上WHERE version = xxx
     * 3. 如果版本不匹配，说明其他线程已更新，本次更新失败
     * 4. 失败后可以选择重试或提示用户
     */
    @Version
    private Integer version;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
