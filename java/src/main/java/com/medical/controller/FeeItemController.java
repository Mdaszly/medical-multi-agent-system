package com.medical.controller;

import com.medical.annotation.AuthCheck;
import com.medical.common.BaseResponse;
import com.medical.common.ResultUtils;
import com.medical.constant.UserConstant;
import com.medical.model.vo.FeeItemVO;
import com.medical.service.FeeItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * 费用项控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/fee-item")
@RequiredArgsConstructor
@Tag(name = "费用项管理", description = "费用项相关接口")
public class FeeItemController {

    private final FeeItemService feeItemService;

    @GetMapping("/{id}")
    @Operation(summary = "查询费用项详情", description = "根据ID查询费用项详情")
    @AuthCheck(mustRoles = {UserConstant.USER_ROLE, UserConstant.DOCTOR_ROLE, UserConstant.ADMIN_ROLE})
    public BaseResponse<FeeItemVO> getFeeItemById(
            @Parameter(description = "费用项ID", required = true)
            @PathVariable Long id) {
        log.info("查询费用项详情: id={}", id);
        FeeItemVO feeItem = feeItemService.getFeeItemById(id);
        return ResultUtils.success(feeItem);
    }

    @GetMapping("/list/user/{userId}")
    @Operation(summary = "查询用户费用项列表", description = "根据用户ID查询费用项列表")
    @AuthCheck(mustRoles = {UserConstant.USER_ROLE, UserConstant.DOCTOR_ROLE, UserConstant.ADMIN_ROLE})
    public BaseResponse<List<FeeItemVO>> listByUserId(
            @Parameter(description = "用户ID", required = true)
            @PathVariable Long userId) {
        log.info("查询用户费用项列表: userId={}", userId);
        List<FeeItemVO> feeItems = feeItemService.listByUserId(userId);
        return ResultUtils.success(feeItems);
    }

    @GetMapping("/list/appointment/{appointmentId}")
    @Operation(summary = "查询预约费用项列表", description = "根据预约ID查询费用项列表")
    @AuthCheck(mustRoles = {UserConstant.USER_ROLE, UserConstant.DOCTOR_ROLE, UserConstant.ADMIN_ROLE})
    public BaseResponse<List<FeeItemVO>> listByAppointmentId(
            @Parameter(description = "预约ID", required = true)
            @PathVariable Long appointmentId) {
        log.info("查询预约费用项列表: appointmentId={}", appointmentId);
        List<FeeItemVO> feeItems = feeItemService.listByAppointmentId(appointmentId);
        return ResultUtils.success(feeItems);
    }

    @GetMapping("/list/prescription/{prescriptionId}")
    @Operation(summary = "查询处方费用项列表", description = "根据处方ID查询费用项列表")
    @AuthCheck(mustRoles = {UserConstant.USER_ROLE, UserConstant.DOCTOR_ROLE, UserConstant.PHARMACIST_ROLE, UserConstant.ADMIN_ROLE})
    public BaseResponse<List<FeeItemVO>> listByPrescriptionId(
            @Parameter(description = "处方ID", required = true)
            @PathVariable Long prescriptionId) {
        log.info("查询处方费用项列表: prescriptionId={}", prescriptionId);
        List<FeeItemVO> feeItems = feeItemService.listByPrescriptionId(prescriptionId);
        return ResultUtils.success(feeItems);
    }

    @GetMapping("/unsettled-amount/{userId}")
    @Operation(summary = "查询用户未结算金额", description = "根据用户ID查询未结算费用总和")
    @AuthCheck(mustRoles = {UserConstant.USER_ROLE, UserConstant.DOCTOR_ROLE, UserConstant.ADMIN_ROLE})
    public BaseResponse<BigDecimal> getUnsettledAmount(
            @Parameter(description = "用户ID", required = true)
            @PathVariable Long userId) {
        log.info("查询用户未结算金额: userId={}", userId);
        BigDecimal amount = feeItemService.calculateUnsettledAmount(userId);
        return ResultUtils.success(amount);
    }
}