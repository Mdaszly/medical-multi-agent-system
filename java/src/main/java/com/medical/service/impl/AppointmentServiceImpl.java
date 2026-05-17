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
import org.springframework.aop.framework.AopContext;
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

    @Override
    public AppointmentVO createAppointment(AppointmentAddRequest request) {
        // 获取当前登录用户ID（患者ID）
        Long userId = StpUtil.getLoginIdAsLong();
        Long scheduleId = request.getScheduleId();
        String timeSlot = request.getTimeSlot();

        // 参数校验：排班ID和时段不能为空
        ThrowUtils.throwIf(scheduleId == null || scheduleId <= 0, ErrorCode.PARAM_ERROR, "排班ID无效");
        ThrowUtils.throwIf(!StringUtils.hasText(timeSlot), ErrorCode.PARAM_ERROR, "时段不能为空");

        // 获取分布式锁，防止多实例并发预约导致号源超卖
        // 锁的粒度：按排班ID加锁，允许不同排班同时预约
        String lockKey = String.format(AppointmentConstant.LOCK_APPOINTMENT_SLOT, scheduleId);
        boolean locked = distributedLock.tryLock(lockKey, 10, TimeUnit.SECONDS);
        ThrowUtils.throwIf(!locked, ErrorCode.SYSTEM_ERROR, "系统繁忙，请稍后重试");

        try {
            // 通过AOP代理调用事务方法，确保锁在事务提交后再释放
            // 避免事务未提交就释放锁导致的数据不一致问题
            AppointmentServiceImpl proxy = (AppointmentServiceImpl) AopContext.currentProxy();
            return proxy.doCreateAppointment(request, userId);
        } finally {
            // 无论成功失败都释放锁
            distributedLock.unlock(lockKey);
        }
    }

    /**
     * 实际创建预约的方法（事务内执行）
     * 业务规则：一个用户在同一时段只能预约一次（幂等性）
     */
    @Transactional(rollbackFor = Exception.class)
    public AppointmentVO doCreateAppointment(AppointmentAddRequest request, Long userId) {
        Long scheduleId = request.getScheduleId();
        String timeSlot = request.getTimeSlot();

        // 步骤1：验证排班信息是否存在
        Schedule schedule = scheduleMapper.selectById(scheduleId);
        ThrowUtils.throwIf(schedule == null, ErrorCode.PARAM_ERROR, "排班不存在");

        // 步骤2：查询指定时段的号源信息
        AppointmentSlot slot = getSlotByScheduleIdAndTimeSlot(scheduleId, timeSlot);
        ThrowUtils.throwIf(slot == null, ErrorCode.PARAM_ERROR, "号源不存在");
        ThrowUtils.throwIf(slot.getAvailableSlots() <= 0, ErrorCode.PARAM_ERROR, "该时段已约满");

        // 步骤3：幂等性检查 - 防止重复预约
        // 同一用户在同一排班的同一时段只能预约一次
        int count = appointmentMapper.countByUserIdAndScheduleIdAndTimeSlot(userId, scheduleId, timeSlot);
        ThrowUtils.throwIf(count > 0, ErrorCode.PARAM_ERROR, "您已预约该时段");

        // 步骤4：验证患者信息
        User user = userMapper.selectById(userId);
        ThrowUtils.throwIf(user == null, ErrorCode.USER_NOT_FOUND, "用户不存在");

        // 步骤5：乐观锁扣减号源数量
        // 使用version字段实现乐观锁，防止并发场景下的号源超卖
        int updated = appointmentSlotMapper.decreaseAvailableSlotsWithLock(slot.getId(), slot.getVersion());
        ThrowUtils.throwIf(updated == 0, ErrorCode.SYSTEM_ERROR, "号源已被占用，请重试");

        // 步骤6：构建预约记录
        Appointment appointment = new Appointment();
        appointment.setAppointmentNo(generateAppointmentNo());  // 生成唯一预约编号
        appointment.setUserId(userId);
        appointment.setUserName(user.getUserName());
        appointment.setDoctorId(schedule.getDoctorId());
        appointment.setDoctorName(schedule.getDoctorName());
        appointment.setDepartment(schedule.getDepartment());
        appointment.setScheduleId(scheduleId);
        appointment.setScheduleDate(schedule.getScheduleDate());
        appointment.setShiftType(schedule.getShiftType());
        appointment.setTimeSlot(timeSlot);

        // 获取医生挂号费
        Doctor doctor = doctorMapper.selectById(schedule.getDoctorId());
        appointment.setConsultationFee(doctor != null ? doctor.getConsultationFee() : null);

        // 设置初始状态：待就诊
        appointment.setStatus(AppointmentConstant.APPOINTMENT_STATUS_PENDING);
        appointment.setCheckInStatus(false);
        appointment.setRemark(request.getRemark());

        // 步骤7：保存预约记录
        appointmentMapper.insert(appointment);
        return AppointmentVO.fromEntity(appointment);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelAppointment(AppointmentCancelRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        Long appointmentId = request.getAppointmentId();

        // 参数校验
        ThrowUtils.throwIf(appointmentId == null || appointmentId <= 0, ErrorCode.PARAM_ERROR, "预约ID无效");

        // 获取预约记录
        Appointment appointment = getAppointmentEntityById(appointmentId);

        // 权限校验：只能取消自己的预约
        ThrowUtils.throwIf(!appointment.getUserId().equals(userId), ErrorCode.NO_AUTH, "无权取消此预约");

        // 状态校验：只能取消待就诊的预约
        ThrowUtils.throwIf(!appointment.getStatus().equals(AppointmentConstant.APPOINTMENT_STATUS_PENDING),
                ErrorCode.PARAM_ERROR, "该预约状态不可取消");

        // 更新为取消状态
        appointment.setStatus(AppointmentConstant.APPOINTMENT_STATUS_CANCELLED);
        appointment.setCancelTime(LocalDateTime.now());
        appointment.setCancelReason(request.getCancelReason());
        appointmentMapper.updateById(appointment);

        // 恢复号源数量
        AppointmentSlot slot = getSlotByScheduleIdAndTimeSlot(appointment.getScheduleId(), appointment.getTimeSlot());
        if (slot != null) {
            appointmentSlotMapper.increaseAvailableSlots(slot.getId());
        }
    }

    @Override
    public AppointmentVO getAppointmentById(Long id) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAM_ERROR, "预约ID无效");
        Appointment appointment = appointmentMapper.selectById(id);
        ThrowUtils.throwIf(appointment == null, ErrorCode.PARAM_ERROR, "预约不存在");
        return AppointmentVO.fromEntity(appointment);
    }

    @Override
    public Appointment getAppointmentEntityById(Long id) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAM_ERROR, "预约ID无效");
        Appointment appointment = appointmentMapper.selectById(id);
        ThrowUtils.throwIf(appointment == null, ErrorCode.PARAM_ERROR, "预约不存在");
        return appointment;
    }

    @Override
    public IPage<AppointmentVO> listAppointmentPage(long current, long pageSize, AppointmentQueryRequest request) {
        Page<Appointment> page = new Page<>(current, pageSize);
        LambdaQueryWrapper<Appointment> wrapper = new LambdaQueryWrapper<>();

        // 动态构建查询条件
        if (request != null) {
            if (request.getUserId() != null) wrapper.eq(Appointment::getUserId, request.getUserId());
            if (request.getDoctorId() != null) wrapper.eq(Appointment::getDoctorId, request.getDoctorId());
            if (StringUtils.hasText(request.getDepartment())) wrapper.eq(Appointment::getDepartment, request.getDepartment());
            if (request.getScheduleDate() != null) wrapper.eq(Appointment::getScheduleDate, request.getScheduleDate());
            if (request.getStatus() != null) wrapper.eq(Appointment::getStatus, request.getStatus());
        }

        // 按创建时间倒序
        wrapper.orderByDesc(Appointment::getCreateTime);
        return appointmentMapper.selectPage(page, wrapper).convert(AppointmentVO::fromEntity);
    }

    @Override
    public List<AppointmentVO> listAppointmentByUser(Long userId, LocalDate startDate, LocalDate endDate) {
        ThrowUtils.throwIf(userId == null || userId <= 0, ErrorCode.PARAM_ERROR, "用户ID无效");

        // 默认查询近30天到未来30天
        LocalDate today = LocalDate.now();
        startDate = startDate != null ? startDate : today.minusDays(30);
        endDate = endDate != null ? endDate : today.plusDays(30);

        return appointmentMapper.selectByUserIdAndDateRange(userId, startDate, endDate).stream()
                .map(AppointmentVO::fromEntity).collect(Collectors.toList());
    }

    @Override
    public List<AppointmentVO> listAppointmentByDoctor(Long doctorId, LocalDate startDate, LocalDate endDate) {
        ThrowUtils.throwIf(doctorId == null || doctorId <= 0, ErrorCode.PARAM_ERROR, "医生ID无效");

        // 默认查询今天到未来7天
        LocalDate today = LocalDate.now();
        startDate = startDate != null ? startDate : today;
        endDate = endDate != null ? endDate : today.plusDays(7);

        return appointmentMapper.selectByDoctorIdAndDateRange(doctorId, startDate, endDate).stream()
                .map(AppointmentVO::fromEntity)
                .sorted((a, b) -> {
                    // 先按日期排序，再按时段排序
                    int dateCompare = a.getScheduleDate().compareTo(b.getScheduleDate());
                    return dateCompare != 0 ? dateCompare : a.getTimeSlot().compareTo(b.getTimeSlot());
                }).collect(Collectors.toList());
    }

    @Override
    public List<AppointmentSlotVO> getAppointmentSlotsBySchedule(Long scheduleId) {
        ThrowUtils.throwIf(scheduleId == null || scheduleId <= 0, ErrorCode.PARAM_ERROR, "排班ID无效");
        return appointmentSlotMapper.selectByScheduleId(scheduleId).stream()
                .map(AppointmentSlotVO::fromEntity).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void checkInAppointment(Long appointmentId) {
        Appointment appointment = getAppointmentEntityById(appointmentId);

        // 状态校验：只有待就诊才能签到
        ThrowUtils.throwIf(!appointment.getStatus().equals(AppointmentConstant.APPOINTMENT_STATUS_PENDING),
                ErrorCode.PARAM_ERROR, "该预约状态不可签到");

        // 更新为已签到状态
        appointment.setStatus(AppointmentConstant.APPOINTMENT_STATUS_CHECKED_IN);
        appointment.setCheckInTime(LocalDateTime.now());
        appointment.setCheckInStatus(true);
        appointmentMapper.updateById(appointment);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateAppointmentStatus(Long appointmentId, Integer status) {
        Appointment appointment = getAppointmentEntityById(appointmentId);
        appointment.setStatus(status);
        appointment.setUpdateTime(LocalDateTime.now());
        appointmentMapper.updateById(appointment);
    }

    /**
     * 根据排班ID和时段获取号源
     */
    private AppointmentSlot getSlotByScheduleIdAndTimeSlot(Long scheduleId, String timeSlot) {
        return appointmentSlotMapper.selectByScheduleId(scheduleId).stream()
                .filter(slot -> timeSlot.equals(slot.getTimeSlot()))
                .findFirst().orElse(null);
    }

    /**
     * 生成预约编号
     */
    private String generateAppointmentNo() {
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String uuid = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return AppointmentConstant.APPOINTMENT_NO_PREFIX + dateStr + uuid;
    }
}