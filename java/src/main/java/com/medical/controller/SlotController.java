package com.medical.controller;

import com.medical.annotation.AuthCheck;
import com.medical.common.BaseResponse;
import com.medical.common.ResultUtils;
import com.medical.constant.UserConstant;
import com.medical.model.dto.slot.SlotAddRequest;
import com.medical.model.dto.slot.SlotBatchAddRequest;
import com.medical.model.vo.AppointmentSlotVO;
import com.medical.service.SlotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/slot")
@RequiredArgsConstructor
@Tag(name = "号源管理", description = "号源时段管理接口")
public class SlotController {

    private final SlotService slotService;

    @PostMapping("/add")
    @Operation(summary = "添加号源", description = "为排班添加一个号源时段")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<AppointmentSlotVO> addSlot(@RequestBody SlotAddRequest request) {
        log.info("添加号源: {}", request);
        AppointmentSlotVO slot = slotService.addSlot(request);
        return ResultUtils.success(slot);
    }

    @PostMapping("/batch/add")
    @Operation(summary = "批量添加号源", description = "为排班批量添加多个号源时段")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Void> batchAddSlots(@RequestBody SlotBatchAddRequest request) {
        log.info("批量添加号源: scheduleId={}, count={}", request.getScheduleId(),
                request.getSlots() != null ? request.getSlots().size() : 0);
        slotService.batchAddSlots(request);
        return ResultUtils.success(null);
    }

    @PostMapping("/generate/default")
    @Operation(summary = "生成默认号源", description = "为排班生成默认的号源时段（08:00-17:30，每30分钟一个时段）")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Void> generateDefaultSlots(
            @Parameter(description = "排班ID", required = true)
            @RequestParam("scheduleId") Long scheduleId) {
        log.info("生成默认号源: scheduleId={}", scheduleId);
        slotService.generateDefaultSlots(scheduleId);
        return ResultUtils.success(null);
    }

    @GetMapping("/get")
    @Operation(summary = "获取号源详情", description = "根据ID获取号源详情")
    @AuthCheck
    public BaseResponse<AppointmentSlotVO> getSlotById(
            @Parameter(description = "号源ID", required = true)
            @RequestParam("id") Long id) {
        log.info("获取号源详情: id={}", id);
        AppointmentSlotVO slot = slotService.getSlotById(id);
        return ResultUtils.success(slot);
    }

    @GetMapping("/list/schedule")
    @Operation(summary = "获取排班号源列表", description = "根据排班ID获取所有号源时段")
    @AuthCheck
    public BaseResponse<List<AppointmentSlotVO>> getSlotsBySchedule(
            @Parameter(description = "排班ID", required = true)
            @RequestParam("scheduleId") Long scheduleId) {
        log.info("获取排班号源列表: scheduleId={}", scheduleId);
        List<AppointmentSlotVO> slots = slotService.getSlotsBySchedule(scheduleId);
        return ResultUtils.success(slots);
    }

    @PostMapping("/update")
    @Operation(summary = "更新号源", description = "更新号源信息")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Void> updateSlot(
            @Parameter(description = "号源ID", required = true)
            @RequestParam("id") Long id,
            @RequestBody SlotAddRequest request) {
        log.info("更新号源: id={}, request={}", id, request);
        slotService.updateSlot(id, request);
        return ResultUtils.success(null);
    }

    @PostMapping("/delete")
    @Operation(summary = "删除号源", description = "删除号源")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Void> deleteSlot(
            @Parameter(description = "号源ID", required = true)
            @RequestParam("id") Long id) {
        log.info("删除号源: id={}", id);
        slotService.deleteSlot(id);
        return ResultUtils.success(null);
    }
}
