package com.medical.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.medical.annotation.AuthCheck;
import com.medical.common.BaseResponse;
import com.medical.common.ResultUtils;
import com.medical.constant.UserConstant;
import com.medical.model.dto.bill.BillGenerateRequest;
import com.medical.model.dto.bill.BillQueryRequest;
import com.medical.model.vo.BillVO;
import com.medical.service.BillService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 账单控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/bill")
@RequiredArgsConstructor
@Tag(name = "账单管理", description = "账单相关接口")
public class BillController {

    private final BillService billService;

    @GetMapping("/{id}")
    @Operation(summary = "查询账单详情", description = "根据ID查询账单详情")
    @AuthCheck(mustRoles = {UserConstant.USER_ROLE, UserConstant.DOCTOR_ROLE, UserConstant.ADMIN_ROLE})
    public BaseResponse<BillVO> getBillById(
            @Parameter(description = "账单ID", required = true)
            @PathVariable Long id) {
        log.info("查询账单详情: id={}", id);
        BillVO bill = billService.getBillById(id);
        return ResultUtils.success(bill);
    }

    @GetMapping("/no/{billNo}")
    @Operation(summary = "根据账单编号查询", description = "根据账单编号查询账单详情")
    @AuthCheck(mustRoles = {UserConstant.USER_ROLE, UserConstant.DOCTOR_ROLE, UserConstant.ADMIN_ROLE})
    public BaseResponse<BillVO> getBillByNo(
            @Parameter(description = "账单编号", required = true)
            @PathVariable String billNo) {
        log.info("根据账单编号查询: billNo={}", billNo);
        BillVO bill = billService.getBillByNo(billNo);
        return ResultUtils.success(bill);
    }

    @PostMapping("/generate")
    @Operation(summary = "生成账单", description = "根据预约或处方生成账单")
    @AuthCheck(mustRoles = {UserConstant.USER_ROLE, UserConstant.DOCTOR_ROLE, UserConstant.ADMIN_ROLE})
    public BaseResponse<BillVO> generateBill(@RequestBody BillGenerateRequest request) {
        log.info("生成账单: userId={}, appointmentId={}, prescriptionId={}", 
                request.getUserId(), request.getAppointmentId(), request.getPrescriptionId());
        BillVO bill = billService.generateBillByRequest(request);
        return ResultUtils.success(bill);
    }

    @PostMapping("/generate/appointment/{appointmentId}")
    @Operation(summary = "根据预约生成账单", description = "根据预约ID生成账单")
    @AuthCheck(mustRoles = {UserConstant.USER_ROLE, UserConstant.DOCTOR_ROLE, UserConstant.ADMIN_ROLE})
    public BaseResponse<BillVO> generateBillByAppointment(
            @Parameter(description = "预约ID", required = true)
            @PathVariable Long appointmentId) {
        log.info("根据预约生成账单: appointmentId={}", appointmentId);
        BillVO bill = billService.generateBill(appointmentId);
        return ResultUtils.success(bill);
    }

    @GetMapping("/list")
    @Operation(summary = "分页查询账单列表", description = "分页查询账单列表")
    @AuthCheck(mustRoles = {UserConstant.USER_ROLE, UserConstant.DOCTOR_ROLE, UserConstant.ADMIN_ROLE})
    public BaseResponse<IPage<BillVO>> listBillPage(
            @Parameter(description = "当前页") @RequestParam(defaultValue = "1") long current,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") long pageSize,
            @Parameter(description = "用户ID") @RequestParam(required = false) Long userId,
            @Parameter(description = "预约ID") @RequestParam(required = false) Long appointmentId,
            @Parameter(description = "账单编号") @RequestParam(required = false) String billNo,
            @Parameter(description = "状态") @RequestParam(required = false) String status) {
        log.info("分页查询账单列表: current={}, pageSize={}, userId={}", current, pageSize, userId);
        BillQueryRequest request = BillQueryRequest.builder()
                .userId(userId)
                .appointmentId(appointmentId)
                .billNo(billNo)
                .status(status)
                .build();
        IPage<BillVO> page = billService.listBillPage(current, pageSize, request);
        return ResultUtils.success(page);
    }

    @GetMapping("/list/user/{userId}")
    @Operation(summary = "查询用户账单列表", description = "根据用户ID查询账单列表")
    @AuthCheck(mustRoles = {UserConstant.USER_ROLE, UserConstant.DOCTOR_ROLE, UserConstant.ADMIN_ROLE})
    public BaseResponse<List<BillVO>> listByUserId(
            @Parameter(description = "用户ID", required = true)
            @PathVariable Long userId) {
        log.info("查询用户账单列表: userId={}", userId);
        List<BillVO> bills = billService.listByUserId(userId);
        return ResultUtils.success(bills);
    }

    @GetMapping("/list/appointment/{appointmentId}")
    @Operation(summary = "查询预约账单", description = "根据预约ID查询账单")
    @AuthCheck(mustRoles = {UserConstant.USER_ROLE, UserConstant.DOCTOR_ROLE, UserConstant.ADMIN_ROLE})
    public BaseResponse<BillVO> getByAppointmentId(
            @Parameter(description = "预约ID", required = true)
            @PathVariable Long appointmentId) {
        log.info("查询预约账单: appointmentId={}", appointmentId);
        BillVO bill = billService.getByAppointmentId(appointmentId);
        return ResultUtils.success(bill);
    }

    @GetMapping("/export")
    @Operation(summary = "导出账单", description = "导出账单为CSV格式")
    @AuthCheck(mustRoles = {UserConstant.ADMIN_ROLE})
    public ResponseEntity<byte[]> exportBill(
            @Parameter(description = "用户ID") @RequestParam(required = false) Long userId,
            @Parameter(description = "状态") @RequestParam(required = false) String status) {
        log.info("导出账单: userId={}, status={}", userId, status);
        BillQueryRequest request = BillQueryRequest.builder()
                .userId(userId)
                .status(status)
                .build();
        String csv = billService.exportBillToCSV(request);
        
        byte[] csvBytes = csv.getBytes(StandardCharsets.UTF_8);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv; charset=UTF-8"));
        headers.setContentDispositionFormData("attachment", "bills.csv");
        headers.setContentLength(csvBytes.length);
        
        return ResponseEntity.ok()
                .headers(headers)
                .body(csvBytes);
    }
}