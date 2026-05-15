package com.medical.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.medical.common.DistributedLock;
import com.medical.common.ErrorCode;
import com.medical.common.RedisCacheUtil;
import com.medical.constant.AppointmentConstant;
import com.medical.constant.UserConstant;
import com.medical.exception.BusinessException;
import com.medical.exception.ThrowUtils;
import com.medical.mapper.*;
import com.medical.model.dto.appointment.AppointmentAddRequest;
import com.medical.model.dto.appointment.AppointmentCancelRequest;
import com.medical.model.dto.appointment.AppointmentQueryRequest;
import com.medical.model.entity.*;
import com.medical.model.vo.AppointmentSlotVO;
import com.medical.model.vo.AppointmentVO;
import com.medical.service.AppointmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 预约服务实现类
 * 
 * <p>核心职责：
 * 1. 处理预约挂号的完整业务流程
 * 2. 实现防超卖的并发控制机制
 * 3. 管理预约状态流转
 * 
 * <p>核心优化点：
 * - 双重锁机制：分布式锁 + 乐观锁，确保并发安全
 * - 幂等性设计：防止重复预约
 * - 事务管理：确保预约和号源更新的原子性
 * - 冗余数据设计：减少关联查询，提升性能
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AppointmentServiceImpl implements AppointmentService {

    private final AppointmentMapper appointmentMapper;
    private final AppointmentSlotMapper appointmentSlotMapper;
    private final ScheduleMapper scheduleMapper;
    private final DoctorMapper doctorMapper;
    private final UserMapper userMapper;
    private final DistributedLock distributedLock;
    private final RedisCacheUtil redisCacheUtil;

    /**
     * 创建预约 - 核心方法
     * 
     * <p>业务流程：
     * 1. 参数验证 → 获取锁 → 验证数据 → 扣减号源 → 创建预约记录 → 释放锁
     * 
     * <p>核心优化：
     * - 双重锁：分布式锁（防止多实例）+ 乐观锁（数据库层面）
     * - 幂等性检查：同一用户同时段只能预约一次
     * - 事务管理：确保数据一致性
     * - finally块释放锁：确保异常时也能释放锁
     * 
     * @param request 预约请求
     * @return 预约信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AppointmentVO createAppointment(AppointmentAddRequest request) {
        log.info("开始创建预约: {}", request);

        // 1. 获取当前登录用户ID
        Long userId = StpUtil.getLoginIdAsLong();
        Long scheduleId = request.getScheduleId();
        String timeSlot = request.getTimeSlot();

        // 2. 基础参数验证
        ThrowUtils.throwIf(scheduleId == null || scheduleId <= 0, ErrorCode.PARAM_ERROR, "排班ID无效");
        ThrowUtils.throwIf(!StringUtils.hasText(timeSlot), ErrorCode.PARAM_ERROR, "时段不能为空");

        // 3. 获取分布式锁 - 第一层并发控制
        // 优化：锁的粒度是排班ID，同一排班的请求串行化，不同排班并行
        String lockKey = String.format(AppointmentConstant.LOCK_APPOINTMENT_SLOT, scheduleId);
        boolean locked = distributedLock.tryLock(lockKey, 10, TimeUnit.SECONDS);
        ThrowUtils.throwIf(!locked, ErrorCode.SYSTEM_ERROR, "系统繁忙，请稍后重试");

        try {
            // 4. 验证排班是否存在
            Schedule schedule = scheduleMapper.selectById(scheduleId);
            ThrowUtils.throwIf(schedule == null, ErrorCode.PARAM_ERROR, "排班不存在");

            // 5. 查询并验证号源
            AppointmentSlot slot = getSlotByScheduleIdAndTimeSlot(scheduleId, timeSlot);
            ThrowUtils.throwIf(slot == null, ErrorCode.PARAM_ERROR, "号源不存在");
            ThrowUtils.throwIf(slot.getAvailableSlots() <= 0, ErrorCode.PARAM_ERROR, "该时段已约满");

            // 6. 幂等性检查 - 防止重复预约
            int count = appointmentMapper.countByUserIdAndScheduleIdAndTimeSlot(userId, scheduleId, timeSlot);
            ThrowUtils.throwIf(count > 0, ErrorCode.PARAM_ERROR, "您已预约该时段");

            // 7. 获取用户信息
            User user = userMapper.selectById(userId);
            ThrowUtils.throwIf(user == null, ErrorCode.USER_NOT_FOUND, "用户不存在");

            // 8. 乐观锁扣减号源 - 第二层并发控制
            // 优化：使用version字段，如果version不匹配则更新失败
            int updated = appointmentSlotMapper.decreaseAvailableSlotsWithLock(slot.getId(), slot.getVersion());
            ThrowUtils.throwIf(updated == 0, ErrorCode.SYSTEM_ERROR, "号源已被占用，请重试");

            // 9. 创建预约记录
            Appointment appointment = new Appointment();
            appointment.setAppointmentNo(generateAppointmentNo());
            appointment.setUserId(userId);
            appointment.setUserName(user.getUserName());
            appointment.setDoctorId(schedule.getDoctorId());
            appointment.setDoctorName(schedule.getDoctorName());
            appointment.setDepartment(schedule.getDepartment());
            appointment.setScheduleId(scheduleId);
            appointment.setScheduleDate(schedule.getScheduleDate());
            appointment.setShiftType(schedule.getShiftType());
            appointment.setTimeSlot(timeSlot);
            
            // 10. 获取医生并设置挂号费
            Doctor doctor = doctorMapper.selectById(schedule.getDoctorId());
            appointment.setConsultationFee(doctor != null ? doctor.getConsultationFee() : null);
            
            // 11. 初始化状态
            appointment.setStatus(AppointmentConstant.APPOINTMENT_STATUS_PENDING);
            appointment.setCheckInStatus(false);
            appointment.setRemark(request.getRemark());

            // 12. 保存预约记录
            appointmentMapper.insert(appointment);
            log.info("预约创建成功: {}", appointment.getId());

            return AppointmentVO.fromEntity(appointment);
        } finally {
            // 13. 释放分布式锁（保证锁一定会释放
            distributedLock.unlock(lockKey);
        }
    }

    /**
     * 取消预约
     * 
     * <p>流程：
     * 1. 权限验证 → 状态检查 → 取消预约 → 释放号源
     * 
     * @param request 取消请求
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelAppointment(AppointmentCancelRequest request) {
        log.info("开始取消预约: {}", request);

        Long userId = StpUtil.getLoginIdAsLong();
        Long appointmentId = request.getAppointmentId();

        ThrowUtils.throwIf(appointmentId == null || appointmentId <= 0, ErrorCode.PARAM_ERROR, "预约ID无效");

        // 验证是否是自己的预约
        Appointment appointment = getAppointmentEntityById(appointmentId);
        ThrowUtils.throwIf(!appointment.getUserId().equals(userId), ErrorCode.NO_AUTH, "无权取消此预约");
        
        // 只有待就诊状态可以取消
        ThrowUtils.throwIf(!appointment.getStatus().equals(AppointmentConstant.APPOINTMENT_STATUS_PENDING),
                ErrorCode.PARAM_ERROR, "该预约状态不可取消");

        // 更新预约状态
        appointment.setStatus(AppointmentConstant.APPOINTMENT_STATUS_CANCELLED);
        appointment.setCancelTime(LocalDateTime.now());
        appointment.setCancelReason(request.getCancelReason());
        appointmentMapper.updateById(appointment);

        // 释放号源
        AppointmentSlot slot = getSlotByScheduleIdAndTimeSlot(appointment.getScheduleId(), appointment.getTimeSlot());
        if (slot != null) {
            appointmentSlotMapper.increaseAvailableSlots(slot.getId());
        }

        log.info("预约取消成功: {}", appointmentId);
    }

    /**
     * 获取预约详情
     */
    @Override
    public AppointmentVO getAppointmentById(Long id) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAM_ERROR, "预约ID无效");
        Appointment appointment = appointmentMapper.selectById(id);
        ThrowUtils.throwIf(appointment == null, ErrorCode.PARAM_ERROR, "预约不存在");
        return AppointmentVO.fromEntity(appointment);
    }

    /**
     * 获取预约实体（内部使用）
     */
    @Override
    public Appointment getAppointmentEntityById(Long id) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAM_ERROR, "预约ID无效");
        Appointment appointment = appointmentMapper.selectById(id);
        ThrowUtils.throwIf(appointment == null, ErrorCode.PARAM_ERROR, "预约不存在");
        return appointment;
    }

    /**
     * 分页查询预约列表
     * 
     * <p>优化：使用MyBatis-Plus的LambdaQueryWrapper类型安全，避免SQL注入
     */
    @Override
    public IPage<AppointmentVO> listAppointmentPage(long current, long pageSize, AppointmentQueryRequest request) {
        log.info("分页查询预约列表: current={}, pageSize={}, request={}", current, pageSize, request);

        Page<Appointment> page = new Page<>(current, pageSize);
        LambdaQueryWrapper<Appointment> wrapper = new LambdaQueryWrapper<>();

        // 动态条件构建
        if (request != null) {
            if (request.getUserId() != null) {
                wrapper.eq(Appointment::getUserId, request.getUserId());
            }
            if (request.getDoctorId() != null) {
                wrapper.eq(Appointment::getDoctorId, request.getDoctorId());
            }
            if (StringUtils.hasText(request.getDepartment())) {
                wrapper.eq(Appointment::getDepartment, request.getDepartment());
            }
            if (request.getScheduleDate() != null) {
                wrapper.eq(Appointment::getScheduleDate, request.getScheduleDate());
            }
            if (request.getStatus() != null) {
                wrapper.eq(Appointment::getStatus, request.getStatus());
            }
        }

        // 按创建时间倒序
        wrapper.orderByDesc(Appointment::getCreateTime);

        IPage<Appointment> appointmentPage = appointmentMapper.selectPage(page, wrapper);

        // 转换为VO对象
        return appointmentPage.convert(AppointmentVO::fromEntity);
    }

    /**
     * 查询用户预约列表
     * 
     * <p>优化：默认查询范围默认查询前后30天，避免查询量过大
     */
    @Override
    public List<AppointmentVO> listAppointmentByUser(Long userId, LocalDate startDate, LocalDate endDate) {
        ThrowUtils.throwIf(userId == null || userId <= 0, ErrorCode.PARAM_ERROR, "用户ID无效");

        LocalDate today = LocalDate.now();
        // 默认查询前后30天
        startDate = startDate != null ? startDate : today.minusDays(30);
        endDate = endDate != null ? endDate : today.plusDays(30);

        List<Appointment> appointments = appointmentMapper.selectByUserIdAndDateRange(userId, startDate, endDate);

        return appointments.stream()
                .map(AppointmentVO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * 查询医生预约列表
     * 
     * <p>优化：医生默认只看未来7天，便于工作
     */
    @Override
    public List<AppointmentVO> listAppointmentByDoctor(Long doctorId, LocalDate startDate, LocalDate endDate) {
        ThrowUtils.throwIf(doctorId == null || doctorId <= 0, ErrorCode.PARAM_ERROR, "医生ID无效");

        LocalDate today = LocalDate.now();
        // 医生默认看今天到未来7天
        startDate = startDate != null ? startDate : today;
        endDate = endDate != null ? endDate : today.plusDays(7);

        List<Appointment> appointments = appointmentMapper.selectByDoctorIdAndDateRange(doctorId, startDate, endDate);

        // 按日期和时段排序
        return appointments.stream()
                .map(AppointmentVO::fromEntity)
                .sorted((a, b) -> {
                    int dateCompare = a.getScheduleDate().compareTo(b.getScheduleDate());
                    if (dateCompare != 0) {
                        return dateCompare;
                    }
                    return a.getTimeSlot().compareTo(b.getTimeSlot());
                })
                .collect(Collectors.toList());
    }

    /**
     * 获取排班的号源列表
     */
    @Override
    public List<AppointmentSlotVO> getAppointmentSlotsBySchedule(Long scheduleId) {
        ThrowUtils.throwIf(scheduleId == null || scheduleId <= 0, ErrorCode.PARAM_ERROR, "排班ID无效");

        List<AppointmentSlot> slots = appointmentSlotMapper.selectByScheduleId(scheduleId);

        return slots.stream()
                .map(AppointmentSlotVO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * 预约签到
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void checkInAppointment(Long appointmentId) {
        log.info("开始签到: appointmentId={}", appointmentId);

        Appointment appointment = getAppointmentEntityById(appointmentId);
        // 只有待就诊状态可以签到
        ThrowUtils.throwIf(!appointment.getStatus().equals(AppointmentConstant.APPOINTMENT_STATUS_PENDING),
                ErrorCode.PARAM_ERROR, "该预约状态不可签到");

        // 更新状态
        appointment.setStatus(AppointmentConstant.APPOINTMENT_STATUS_CHECKED_IN);
        appointment.setCheckInTime(LocalDateTime.now());
        appointment.setCheckInStatus(true);
        appointmentMapper.updateById(appointment);

        log.info("签到成功: {}", appointmentId);
    }

    /**
     * 更新预约状态
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateAppointmentStatus(Long appointmentId, Integer status) {
        log.info("更新预约状态: appointmentId={}, status={}", appointmentId, status);

        Appointment appointment = getAppointmentEntityById(appointmentId);
        appointment.setStatus(status);
        appointment.setUpdateTime(LocalDateTime.now());
        appointmentMapper.updateById(appointment);

        log.info("预约状态更新成功: {}", appointmentId);
    }

    /**
     * 根据排班ID和时段查找号源
     * 
     * @param scheduleId 排班ID
     * @param timeSlot 时段
     * @return 号源对象
     */
    private AppointmentSlot getSlotByScheduleIdAndTimeSlot(Long scheduleId, String timeSlot) {
        List<AppointmentSlot> slots = appointmentSlotMapper.selectByScheduleId(scheduleId);
        // 使用Stream API过滤，简洁高效
        return slots.stream()
                .filter(slot -> timeSlot.equals(slot.getTimeSlot()))
                .findFirst()
                .orElse(null);
    }

    /**
     * 生成预约编号
     * 
     * <p>格式：APT + yyyyMMddHHmmss + 8位UUID
     * 
     * <p>优化：
     * - 前缀标识业务类型
     * - 时间戳便于按时间排序
     * - UUID保证唯一性
     * 
     * @return 预约编号
     */
    private String generateAppointmentNo() {
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String uuid = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return AppointmentConstant.APPOINTMENT_NO_PREFIX + dateStr + uuid;
    }
}
