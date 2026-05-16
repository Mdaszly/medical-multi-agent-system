package com.medical.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.medical.annotation.AuthCheck;
import com.medical.common.BaseResponse;
import com.medical.common.ResultUtils;
import com.medical.constant.UserConstant;
import com.medical.model.dto.prescription.PrescriptionAddRequest;
import com.medical.model.dto.prescription.PrescriptionQueryRequest;
import com.medical.model.dto.prescription.PrescriptionStatusUpdateRequest;
import com.medical.model.vo.PrescriptionVO;
import com.medical.service.PrescriptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/prescription")
@RequiredArgsConstructor
@Tag(name = "处方管理", description = "医院处方管理接口")
public class PrescriptionController {

    private final PrescriptionService prescriptionService;

    @PostMapping("/create")
    @Operation(summary = "创建处方", description = "医生为患者开具处方，处方状态直接为已审核")
    @AuthCheck(mustRoles = {UserConstant.DOCTOR_ROLE})
    public BaseResponse<PrescriptionVO> createPrescription(@RequestBody PrescriptionAddRequest request) {
        log.info("创建处方: {}", request);
        PrescriptionVO prescription = prescriptionService.createPrescription(request);
        return ResultUtils.success(prescription);
    }

    @GetMapping("/get")
    @Operation(summary = "获取处方详情", description = "根据ID获取处方详情（含明细）")
    @AuthCheck
    public BaseResponse<PrescriptionVO> getPrescriptionById(
            @Parameter(description = "处方ID", required = true)
            @RequestParam("id") Long id) {
        log.info("获取处方详情: id={}", id);
        PrescriptionVO prescription = prescriptionService.getPrescriptionById(id);
        return ResultUtils.success(prescription);
    }

    @GetMapping("/get/byNo")
    @Operation(summary = "根据编号获取处方", description = "根据处方编号获取处方详情")
    @AuthCheck
    public BaseResponse<PrescriptionVO> getPrescriptionByNo(
            @Parameter(description = "处方编号", required = true)
            @RequestParam("prescriptionNo") String prescriptionNo) {
        log.info("获取处方详情: prescriptionNo={}", prescriptionNo);
        PrescriptionVO prescription = prescriptionService.getPrescriptionByNo(prescriptionNo);
        return ResultUtils.success(prescription);
    }

    @GetMapping("/list/user")
    @Operation(summary = "查询用户处方", description = "查询当前患者的处方列表")
    @AuthCheck(mustRoles = {UserConstant.USER_ROLE, UserConstant.ADMIN_ROLE})
    public BaseResponse<List<PrescriptionVO>> listPrescriptionByUser() {
        Long userId = cn.dev33.satoken.stp.StpUtil.getLoginIdAsLong();
        log.info("查询用户处方: userId={}", userId);
        List<PrescriptionVO> prescriptions = prescriptionService.listPrescriptionByUser(userId);
        return ResultUtils.success(prescriptions);
    }

    @GetMapping("/list/doctor")
    @Operation(summary = "查询医生处方", description = "查询医生开具的处方列表")
    @AuthCheck(mustRoles = {UserConstant.DOCTOR_ROLE, UserConstant.ADMIN_ROLE})
    public BaseResponse<List<PrescriptionVO>> listPrescriptionByDoctor() {
        Long doctorId = cn.dev33.satoken.stp.StpUtil.getLoginIdAsLong();
        log.info("查询医生处方: doctorId={}", doctorId);
        List<PrescriptionVO> prescriptions = prescriptionService.listPrescriptionByDoctor(doctorId);
        return ResultUtils.success(prescriptions);
    }

    @GetMapping("/list/appointment")
    @Operation(summary = "查询预约处方", description = "查询指定预约关联的处方列表")
    @AuthCheck(mustRoles = {UserConstant.DOCTOR_ROLE, UserConstant.PHARMACIST_ROLE, UserConstant.ADMIN_ROLE})
    public BaseResponse<List<PrescriptionVO>> listPrescriptionByAppointment(
            @Parameter(description = "预约ID", required = true)
            @RequestParam("appointmentId") Long appointmentId) {
        log.info("查询预约处方: appointmentId={}", appointmentId);
        List<PrescriptionVO> prescriptions = prescriptionService.listPrescriptionByAppointment(appointmentId);
        return ResultUtils.success(prescriptions);
    }

    @GetMapping("/list/pending-dispense")
    @Operation(summary = "查询待发药处方", description = "药师查询待发药的处方列表")
    @AuthCheck(mustRoles = {UserConstant.PHARMACIST_ROLE, UserConstant.ADMIN_ROLE})
    public BaseResponse<List<PrescriptionVO>> listPendingDispensePrescriptions() {
        log.info("查询待发药处方列表");
        List<PrescriptionVO> prescriptions = prescriptionService.listPendingDispensePrescriptions();
        return ResultUtils.success(prescriptions);
    }

    @PostMapping("/list/page")
    @Operation(summary = "分页查询处方", description = "管理员分页查询处方列表")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<IPage<PrescriptionVO>> listPrescriptionPage(@RequestBody PrescriptionQueryRequest request) {
        log.info("分页查询处方: request={}", request);
        long current = request.getCurrent() != null ? request.getCurrent() : 1;
        long pageSize = request.getPageSize() != null ? request.getPageSize() : 10;
        IPage<PrescriptionVO> prescriptionPage = prescriptionService.listPrescriptionPage(current, pageSize, request);
        return ResultUtils.success(prescriptionPage);
    }

    @PostMapping("/status/update")
    @Operation(summary = "更新处方状态", description = "更新处方状态（如审核）")
    @AuthCheck(mustRoles = {UserConstant.PHARMACIST_ROLE, UserConstant.ADMIN_ROLE})
    public BaseResponse<Void> updatePrescriptionStatus(@RequestBody PrescriptionStatusUpdateRequest request) {
        log.info("更新处方状态: {}", request);
        prescriptionService.updatePrescriptionStatus(request);
        return ResultUtils.success(null);
    }

    @PostMapping("/dispense")
    @Operation(summary = "发药", description = "药房人员发药，处方状态从已审核变为已发药")
    @AuthCheck(mustRoles = {UserConstant.PHARMACIST_ROLE, UserConstant.ADMIN_ROLE})
    public BaseResponse<Void> dispensePrescription(
            @Parameter(description = "处方ID", required = true)
            @RequestParam("prescriptionId") Long prescriptionId) {
        log.info("发药: prescriptionId={}", prescriptionId);
        prescriptionService.dispensePrescription(prescriptionId);
        return ResultUtils.success(null);
    }

    @PostMapping("/cancel")
    @Operation(summary = "取消处方", description = "取消处方（仅可取消待审核的处方）")
    @AuthCheck(mustRoles = {UserConstant.DOCTOR_ROLE, UserConstant.ADMIN_ROLE})
    public BaseResponse<Void> cancelPrescription(
            @Parameter(description = "处方ID", required = true)
            @RequestParam("prescriptionId") Long prescriptionId) {
        log.info("取消处方: prescriptionId={}", prescriptionId);
        prescriptionService.cancelPrescription(prescriptionId);
        return ResultUtils.success(null);
    }
}
