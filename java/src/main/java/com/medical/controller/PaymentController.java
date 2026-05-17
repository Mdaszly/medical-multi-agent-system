package com.medical.controller;

import com.medical.annotation.AuthCheck;
import com.medical.common.BaseResponse;
import com.medical.common.ResultUtils;
import com.medical.constant.UserConstant;
import com.medical.model.dto.payment.PaymentRequest;
import com.medical.model.dto.payment.RefundRequest;
import com.medical.model.vo.PaymentVO;
import com.medical.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 支付控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
@Tag(name = "支付管理", description = "支付相关接口")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/create")
    @Operation(summary = "创建支付记录", description = "创建支付记录，准备支付")
    @AuthCheck(mustRoles = {UserConstant.USER_ROLE})
    public BaseResponse<PaymentVO> createPayment(@RequestBody PaymentRequest request) {
        log.info("创建支付记录: billId={}, amount={}, paymentType={}", 
                request.getBillId(), request.getAmount(), request.getPaymentType());
        PaymentVO payment = paymentService.createPayment(request);
        return ResultUtils.success(payment);
    }

    @PostMapping("/pay/{paymentId}")
    @Operation(summary = "发起支付", description = "模拟支付，更新支付状态")
    @AuthCheck(mustRoles = {UserConstant.USER_ROLE})
    public BaseResponse<PaymentVO> pay(
            @Parameter(description = "支付ID", required = true)
            @PathVariable Long paymentId) {
        log.info("发起支付: paymentId={}", paymentId);
        PaymentVO payment = paymentService.simulatePayment(paymentId);
        return ResultUtils.success(payment);
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询支付记录", description = "根据ID查询支付记录")
    @AuthCheck(mustRoles = {UserConstant.USER_ROLE, UserConstant.DOCTOR_ROLE, UserConstant.ADMIN_ROLE})
    public BaseResponse<PaymentVO> getPaymentById(
            @Parameter(description = "支付ID", required = true)
            @PathVariable Long id) {
        log.info("查询支付记录: id={}", id);
        PaymentVO payment = paymentService.getPaymentById(id);
        return ResultUtils.success(payment);
    }

    @GetMapping("/no/{paymentNo}")
    @Operation(summary = "根据支付编号查询", description = "根据支付编号查询支付记录")
    @AuthCheck(mustRoles = {UserConstant.USER_ROLE, UserConstant.DOCTOR_ROLE, UserConstant.ADMIN_ROLE})
    public BaseResponse<PaymentVO> getPaymentByNo(
            @Parameter(description = "支付编号", required = true)
            @PathVariable String paymentNo) {
        log.info("根据支付编号查询: paymentNo={}", paymentNo);
        PaymentVO payment = paymentService.getPaymentByNo(paymentNo);
        return ResultUtils.success(payment);
    }

    @GetMapping("/status/{paymentId}")
    @Operation(summary = "查询支付状态", description = "查询支付状态描述")
    @AuthCheck(mustRoles = {UserConstant.USER_ROLE, UserConstant.DOCTOR_ROLE, UserConstant.ADMIN_ROLE})
    public BaseResponse<String> getPaymentStatus(
            @Parameter(description = "支付ID", required = true)
            @PathVariable Long paymentId) {
        log.info("查询支付状态: paymentId={}", paymentId);
        String status = paymentService.getPaymentStatus(paymentId);
        return ResultUtils.success(status);
    }

    @GetMapping("/list/bill/{billId}")
    @Operation(summary = "查询账单支付记录", description = "根据账单ID查询支付记录列表")
    @AuthCheck(mustRoles = {UserConstant.USER_ROLE, UserConstant.DOCTOR_ROLE, UserConstant.ADMIN_ROLE})
    public BaseResponse<List<PaymentVO>> listByBillId(
            @Parameter(description = "账单ID", required = true)
            @PathVariable Long billId) {
        log.info("查询账单支付记录: billId={}", billId);
        List<PaymentVO> payments = paymentService.listByBillId(billId);
        return ResultUtils.success(payments);
    }

    @GetMapping("/list/user/{userId}")
    @Operation(summary = "查询用户支付记录", description = "根据用户ID查询支付记录列表")
    @AuthCheck(mustRoles = {UserConstant.USER_ROLE, UserConstant.DOCTOR_ROLE, UserConstant.ADMIN_ROLE})
    public BaseResponse<List<PaymentVO>> listByUserId(
            @Parameter(description = "用户ID", required = true)
            @PathVariable Long userId) {
        log.info("查询用户支付记录: userId={}", userId);
        List<PaymentVO> payments = paymentService.listByUserId(userId);
        return ResultUtils.success(payments);
    }

    @PostMapping("/refund")
    @Operation(summary = "发起退款", description = "对已支付的订单发起退款")
    @AuthCheck(mustRoles = {UserConstant.USER_ROLE, UserConstant.ADMIN_ROLE})
    public BaseResponse<PaymentVO> refund(@RequestBody RefundRequest request) {
        log.info("发起退款: paymentId={}, refundAmount={}, reason={}", 
                request.getPaymentId(), request.getRefundAmount(), request.getReason());
        PaymentVO payment = paymentService.refund(request);
        return ResultUtils.success(payment);
    }
}