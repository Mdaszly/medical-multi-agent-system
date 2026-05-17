package com.medical.service;

import com.medical.model.dto.payment.PaymentRequest;
import com.medical.model.dto.payment.RefundRequest;
import com.medical.model.entity.Payment;
import com.medical.model.vo.PaymentVO;

import java.util.List;

/**
 * 支付服务接口
 */
public interface PaymentService {

    /**
     * 创建支付记录
     *
     * @param request 支付请求
     * @return 支付VO
     */
    PaymentVO createPayment(PaymentRequest request);

    /**
     * 查询支付状态
     *
     * @param paymentNo 支付编号
     * @return 支付VO
     */
    PaymentVO getPaymentByNo(String paymentNo);

    /**
     * 根据ID查询支付记录
     *
     * @param id 支付ID
     * @return 支付VO
     */
    PaymentVO getPaymentById(Long id);

    /**
     * 根据账单ID查询支付记录
     *
     * @param billId 账单ID
     * @return 支付记录列表
     */
    List<PaymentVO> listByBillId(Long billId);

    /**
     * 根据用户ID查询支付记录
     *
     * @param userId 用户ID
     * @return 支付记录列表
     */
    List<PaymentVO> listByUserId(Long userId);

    /**
     * 模拟支付
     *
     * @param paymentId 支付ID
     * @return 支付VO
     */
    PaymentVO simulatePayment(Long paymentId);

    /**
     * 查询支付状态
     *
     * @param paymentId 支付ID
     * @return 支付状态
     */
    String getPaymentStatus(Long paymentId);

    /**
     * 发起退款
     *
     * @param request 退款请求
     * @return 支付VO
     */
    PaymentVO refund(RefundRequest request);

    /**
     * 获取支付实体
     *
     * @param id 支付ID
     * @return 支付实体
     */
    Payment getPaymentEntityById(Long id);
}