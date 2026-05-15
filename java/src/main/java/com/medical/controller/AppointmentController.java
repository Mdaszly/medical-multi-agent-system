package com.medical.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.medical.annotation.AuthCheck;
import com.medical.common.BaseResponse;
import com.medical.common.ResultUtils;
import com.medical.constant.UserConstant;
import com.medical.model.dto.appointment.AppointmentAddRequest;
import com.medical.model.dto.appointment.AppointmentCancelRequest;
import com.medical.model.dto.appointment.AppointmentQueryRequest;
import com.medical.model.vo.AppointmentSlotVO;
import com.medical.model.vo.AppointmentVO;
import com.medical.service.AppointmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/appointment")
@RequiredArgsConstructor
@Tag(name = "预约管理", description = "医院预约挂号接口")
public class AppointmentController {

    private final AppointmentService appointmentService;

    /**
     * 创建预约
     *
     * <p>权限说明：
     * - user角色：患者可以创建自己的预约
     * - admin角色：管理员可以代患者创建预约
     */
    @PostMapping("/create")
    @Operation(summary = "创建预约", description = "患者创建预约挂号，管理员可代为创建")
    @AuthCheck(mustRoles = {UserConstant.USER_ROLE, UserConstant.ADMIN_ROLE})
    public BaseResponse<AppointmentVO> createAppointment(@RequestBody AppointmentAddRequest request) {
        log.info("创建预约: {}", request);
        AppointmentVO appointment = appointmentService.createAppointment(request);
        return ResultUtils.success(appointment);
    }

    /**
     * 取消预约
     *
     * <p>权限说明：
     * - user角色：患者可以取消自己的预约
     * - admin角色：管理员可以取消任何预约
     */
    @PostMapping("/cancel")
    @Operation(summary = "取消预约", description = "患者取消预约，管理员可取消任何预约")
    @AuthCheck(mustRoles = {UserConstant.USER_ROLE, UserConstant.ADMIN_ROLE})
    public BaseResponse<Void> cancelAppointment(@RequestBody AppointmentCancelRequest request) {
        log.info("取消预约: {}", request);
        appointmentService.cancelAppointment(request);
        return ResultUtils.success(null);
    }

    /**
     * 获取预约详情
     *
     * <p>权限说明：只要登录就能访问
     */
    @GetMapping("/get")
    @Operation(summary = "获取预约详情", description = "根据ID获取预约详情")
    @AuthCheck
    public BaseResponse<AppointmentVO> getAppointmentById(
            @Parameter(description = "预约ID", required = true)
            @RequestParam("id") Long id) {
        log.info("获取预约详情: id={}", id);
        AppointmentVO appointment = appointmentService.getAppointmentById(id);
        return ResultUtils.success(appointment);
    }

    /**
     * 查询用户预约列表
     *
     * <p>权限说明：
     * - user角色：查询自己的预约
     * - admin角色：可以查询所有预约
     */
    @GetMapping("/list/user")
    @Operation(summary = "查询用户预约", description = "查询指定用户的预约列表")
    @AuthCheck(mustRoles = {UserConstant.USER_ROLE, UserConstant.ADMIN_ROLE})
    public BaseResponse<List<AppointmentVO>> listAppointmentByUser(
            @Parameter(description = "开始日期")
            @RequestParam(value = "startDate", required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @Parameter(description = "结束日期")
            @RequestParam(value = "endDate", required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        Long userId = cn.dev33.satoken.stp.StpUtil.getLoginIdAsLong();
        log.info("查询用户预约: userId={}, startDate={}, endDate={}", userId, startDate, endDate);
        List<AppointmentVO> appointments = appointmentService.listAppointmentByUser(userId, startDate, endDate);
        return ResultUtils.success(appointments);
    }

    /**
     * 查询医生预约列表
     *
     * <p>权限说明：
     * - doctor角色：查询自己的预约患者
     * - admin角色：可以查询所有医生的预约
     */
    @GetMapping("/list/doctor")
    @Operation(summary = "查询医生预约", description = "查询指定医生的预约列表")
    @AuthCheck(mustRoles = {UserConstant.DOCTOR_ROLE, UserConstant.ADMIN_ROLE})
    public BaseResponse<List<AppointmentVO>> listAppointmentByDoctor(
            @Parameter(description = "开始日期")
            @RequestParam(value = "startDate", required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @Parameter(description = "结束日期")
            @RequestParam(value = "endDate", required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        Long doctorId = cn.dev33.satoken.stp.StpUtil.getLoginIdAsLong();
        log.info("查询医生预约: doctorId={}, startDate={}, endDate={}", doctorId, startDate, endDate);
        List<AppointmentVO> appointments = appointmentService.listAppointmentByDoctor(doctorId, startDate, endDate);
        return ResultUtils.success(appointments);
    }

    /**
     * 分页查询预约
     *
     * <p>权限说明：只有管理员可以访问
     */
    @PostMapping("/list/page")
    @Operation(summary = "分页查询预约", description = "管理员分页查询预约列表")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<IPage<AppointmentVO>> listAppointmentPage(
            @RequestBody AppointmentQueryRequest request) {
        log.info("分页查询预约: request={}", request);
        long current = request.getCurrent() != null ? request.getCurrent() : 1;
        long pageSize = request.getPageSize() != null ? request.getPageSize() : 10;
        IPage<AppointmentVO> appointmentPage = appointmentService.listAppointmentPage(current, pageSize, request);
        return ResultUtils.success(appointmentPage);
    }

    /**
     * 获取号源列表
     *
     * <p>权限说明：只要登录就能访问（公开查询）
     */
    @GetMapping("/slots")
    @Operation(summary = "获取号源列表", description = "根据排班ID获取号源列表")
    @AuthCheck
    public BaseResponse<List<AppointmentSlotVO>> getAppointmentSlots(
            @Parameter(description = "排班ID", required = true)
            @RequestParam("scheduleId") Long scheduleId) {
        log.info("获取号源列表: scheduleId={}", scheduleId);
        List<AppointmentSlotVO> slots = appointmentService.getAppointmentSlotsBySchedule(scheduleId);
        return ResultUtils.success(slots);
    }

    /**
     * 预约签到
     *
     * <p>权限说明：
     * - user角色：患者可以签到自己的预约
     * - doctor角色：医生可以签到患者的预约
     * - admin角色：管理员可以签到任何预约
     */
    @PostMapping("/checkin")
    @Operation(summary = "预约签到", description = "患者或医生签到")
    @AuthCheck(mustRoles = {UserConstant.USER_ROLE, UserConstant.DOCTOR_ROLE, UserConstant.ADMIN_ROLE})
    public BaseResponse<Void> checkInAppointment(
            @Parameter(description = "预约ID", required = true)
            @RequestParam("appointmentId") Long appointmentId) {
        log.info("预约签到: appointmentId={}", appointmentId);
        appointmentService.checkInAppointment(appointmentId);
        return ResultUtils.success(null);
    }

    /**
     * 更新预约状态
     *
     * <p>权限说明：只有管理员可以访问
     */
    @PostMapping("/status/update")
    @Operation(summary = "更新预约状态", description = "管理员更新预约状态")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Void> updateAppointmentStatus(
            @Parameter(description = "预约ID", required = true)
            @RequestParam("appointmentId") Long appointmentId,
            @Parameter(description = "状态", required = true)
            @RequestParam("status") Integer status) {
        log.info("更新预约状态: appointmentId={}, status={}", appointmentId, status);
        appointmentService.updateAppointmentStatus(appointmentId, status);
        return ResultUtils.success(null);
    }
}
