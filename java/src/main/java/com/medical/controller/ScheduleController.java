package com.medical.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.medical.annotation.AuthCheck;
import com.medical.common.BaseResponse;
import com.medical.common.ErrorCode;
import com.medical.common.ResultUtils;
import com.medical.constant.UserConstant;
import com.medical.exception.BusinessException;
import com.medical.model.dto.schedule.ScheduleAddRequest;
import com.medical.model.dto.schedule.ScheduleQueryRequest;
import com.medical.model.dto.schedule.ScheduleUpdateRequest;
import com.medical.model.vo.ScheduleVO;
import com.medical.service.ScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/schedule")
@RequiredArgsConstructor
@Tag(name = "排班管理", description = "医院排班系统接口")
public class ScheduleController {

    private final ScheduleService scheduleService;

    @GetMapping("/get")
    @Operation(summary = "获取排班详情", description = "根据排班ID获取排班详细信息")
    public BaseResponse<ScheduleVO> getScheduleById(
            @Parameter(description = "排班ID", required = true)
            @RequestParam("id") Long id) {
        log.info("Get schedule by id: {}", id);
        ScheduleVO schedule = scheduleService.getScheduleById(id);
        return ResultUtils.success(schedule);
    }

    @GetMapping("/list/doctor")
    @Operation(summary = "查询医生排班", description = "查询指定医生在日期范围内的排班信息")
    @AuthCheck(mustRole = UserConstant.DOCTOR_ROLE)
    public BaseResponse<List<ScheduleVO>> listScheduleByDoctor(
            @Parameter(description = "医生ID", required = true)
            @RequestParam("doctorId") Long doctorId,
            @Parameter(description = "开始日期")
            @RequestParam(value = "startDate", required = false) LocalDate startDate,
            @Parameter(description = "结束日期")
            @RequestParam(value = "endDate", required = false) LocalDate endDate) {
        log.info("List schedule by doctor: doctorId={}, startDate={}, endDate={}", doctorId, startDate, endDate);
        
        LocalDate today = LocalDate.now();
        startDate = startDate != null ? startDate : today;
        endDate = endDate != null ? endDate : today.plusDays(7);

        List<ScheduleVO> schedules = scheduleService.listScheduleByDoctor(doctorId, startDate, endDate);
        return ResultUtils.success(schedules);
    }

    @GetMapping("/list/department")
    @Operation(summary = "按科室查询排班", description = "查询指定科室在指定日期的排班信息")
    public BaseResponse<List<ScheduleVO>> listScheduleByDepartment(
            @Parameter(description = "科室名称", required = true)
            @RequestParam("department") String department,
            @Parameter(description = "排班日期", required = true)
            @RequestParam("scheduleDate") LocalDate scheduleDate) {
        log.info("List schedule by department: department={}, scheduleDate={}", department, scheduleDate);
        List<ScheduleVO> schedules = scheduleService.listScheduleByDepartment(department, scheduleDate);
        return ResultUtils.success(schedules);
    }

    @GetMapping("/list/on-duty")
    @Operation(summary = "查询在岗医生", description = "查询指定日期和班次的在岗医生列表")
    public BaseResponse<List<ScheduleVO>> listOnDutyDoctors(
            @Parameter(description = "排班日期", required = true)
            @RequestParam("scheduleDate") LocalDate scheduleDate,
            @Parameter(description = "班次类型")
            @RequestParam(value = "shiftType", required = false) String shiftType) {
        log.info("List on duty doctors: scheduleDate={}, shiftType={}", scheduleDate, shiftType);
        List<ScheduleVO> schedules = scheduleService.listOnDutyDoctors(scheduleDate, shiftType);
        return ResultUtils.success(schedules);
    }

    @PostMapping("/list/page")
    @Operation(summary = "分页查询排班", description = "分页查询排班信息，支持多条件筛选")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<IPage<ScheduleVO>> listSchedulePage(
            @RequestBody ScheduleQueryRequest request) {
        log.info("List schedule page: request={}", request);
        
        long current = request.getCurrent() != null ? request.getCurrent() : 1;
        long pageSize = request.getPageSize() != null ? request.getPageSize() : 10;

        IPage<ScheduleVO> schedulePage = scheduleService.listSchedulePage(current, pageSize, request);
        return ResultUtils.success(schedulePage);
    }

    @PostMapping("/add")
    @Operation(summary = "添加排班", description = "为医生添加排班信息")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<ScheduleVO> addSchedule(@RequestBody ScheduleAddRequest request) {
        log.info("Add schedule: request={}", request);
        ScheduleVO schedule = scheduleService.addSchedule(request);
        return ResultUtils.success(schedule);
    }

    @PostMapping("/update")
    @Operation(summary = "更新排班", description = "更新排班信息")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<ScheduleVO> updateSchedule(@RequestBody ScheduleUpdateRequest request) {
        log.info("Update schedule: request={}", request);
        
        if (request.getId() == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "排班ID不能为空");
        }

        ScheduleVO schedule = scheduleService.updateSchedule(request.getId(), request);
        return ResultUtils.success(schedule);
    }

    @PostMapping("/delete")
    @Operation(summary = "删除排班", description = "删除排班信息")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Void> deleteSchedule(
            @Parameter(description = "排班ID", required = true)
            @RequestParam("id") Long id) {
        log.info("Delete schedule: id={}", id);
        scheduleService.deleteSchedule(id);
        return ResultUtils.success(null);
    }

    @PostMapping("/batch/add")
    @Operation(summary = "批量添加排班", description = "批量为多个医生添加排班信息")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Void> batchAddSchedules(@RequestBody List<ScheduleAddRequest> requests) {
        log.info("Batch add schedules: count={}", requests.size());
        scheduleService.batchAddSchedules(requests);
        return ResultUtils.success(null);
    }

    @GetMapping("/load-balance")
    @Operation(summary = "医生负载均衡", description = "获取指定科室医生的排班负载情况")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Map<String, Object>> getDoctorLoadBalance(
            @Parameter(description = "科室名称", required = true)
            @RequestParam("department") String department) {
        log.info("Get doctor load balance: department={}", department);
        Map<String, Object> loadBalance = scheduleService.getDoctorLoadBalance(department);
        return ResultUtils.success(loadBalance);
    }

    @PostMapping("/check/conflict")
    @Operation(summary = "检查排班冲突", description = "检查医生在指定时间是否已有排班")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> checkScheduleConflict(
            @Parameter(description = "医生ID", required = true)
            @RequestParam("doctorId") Long doctorId,
            @Parameter(description = "排班日期", required = true)
            @RequestParam("scheduleDate") LocalDate scheduleDate,
            @Parameter(description = "班次类型", required = true)
            @RequestParam("shiftType") String shiftType) {
        log.info("Check schedule conflict: doctorId={}, scheduleDate={}, shiftType={}",
                doctorId, scheduleDate, shiftType);
        boolean hasConflict = scheduleService.checkScheduleConflict(doctorId, scheduleDate, shiftType);
        return ResultUtils.success(hasConflict);
    }
}
