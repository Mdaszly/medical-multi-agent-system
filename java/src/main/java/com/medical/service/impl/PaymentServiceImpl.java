package com.medical.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.medical.common.ErrorCode;
import com.medical.common.RedissonLockUtil;
import com.medical.constant.RedisKeyConstant;
import com.medical.exception.BusinessException;
import com.medical.exception.ThrowUtils;
import com.medical.mapper.BillMapper;
import com.medical.mapper.PaymentMapper;
import com.medical.model.dto.payment.PaymentRequest;
import com.medical.model.dto.payment.RefundRequest;
import com.medical.model.entity.Bill;
import com.medical.model.entity.Payment;
import com.medical.model.vo.PaymentVO;
import com.medical.service.BillService;
import com.medical.service.PaymentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 支付服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentMapper paymentMapper;
    private final BillMapper billMapper;
    private final BillService billService;
    private final RedissonLockUtil redissonLockUtil;

    private static final String PAYMENT_NO_PREFIX = "PAY";
    private static final int STATUS_PENDING = 0;
    private static final int STATUS_PAID = 1;
    private static final int STATUS_CANCELLED = 2;
    private static final int STATUS_REFUNDED = 3;

    private static final String PAYMENT_TYPE_WECHAT = "WECHAT";
    private static final String PAYMENT_TYPE_ALIPAY = "ALIPAY";
    private static final String PAYMENT_TYPE_CASH = "CASH";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentVO createPayment(PaymentRequest request) {
        log.info("创建支付记录: billId={}, amount={}, paymentType={}",
                request.getBillId(), request.getAmount(), request.getPaymentType());

        // 参数校验
        ThrowUtils.throwIf(request.getBillId() == null || request.getBillId() <= 0,
                ErrorCode.PARAM_ERROR, "账单ID无效");
        ThrowUtils.throwIf(request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0,
                ErrorCode.PARAM_ERROR, "支付金额必须大于0");

        // 验证账单存在
        Bill bill = billMapper.selectById(request.getBillId());
        ThrowUtils.throwIf(bill == null, ErrorCode.PARAM_ERROR, "账单不存在");

        // 状态校验：账单必须是待支付状态
        ThrowUtils.throwIf(!bill.getStatus().equals("UNPAID"),
                ErrorCode.PARAM_ERROR, "账单已支付或已取消");

        // 金额校验：支付金额必须等于账单自付金额
        ThrowUtils.throwIf(request.getAmount().compareTo(bill.getSelfPayAmount()) != 0,
                ErrorCode.PARAM_ERROR, "支付金额与账单金额不符");

        // ✅ 重复支付防护：检查该账单是否已有待支付的支付记录
        LambdaQueryWrapper<Payment> existingWrapper = new LambdaQueryWrapper<>();
        existingWrapper.eq(Payment::getBillId, request.getBillId())
               .eq(Payment::getStatus, STATUS_PENDING);
        Payment existingPayment = paymentMapper.selectOne(existingWrapper);
        if (existingPayment != null) {
            log.warn("该账单已有待支付记录: paymentNo={}, 复用该记录", existingPayment.getPaymentNo());
            return PaymentVO.fromEntity(existingPayment);
        }

        // 确定支付方式（默认微信支付）
        String paymentType = request.getPaymentType();
        if (!StringUtils.hasText(paymentType)) {
            paymentType = PAYMENT_TYPE_WECHAT;
        }

        // 构建支付记录
        Payment payment = Payment.builder()
                .paymentNo(generatePaymentNo())     // 生成唯一支付编号
                .billId(request.getBillId())        // 关联账单
                .userId(bill.getUserId())           // 患者ID
                .userName(request.getUserName())    // 患者姓名
                .amount(request.getAmount())        // 支付金额
                .paymentType(paymentType)           // 支付方式
                .status(STATUS_PENDING)             // 初始状态：待支付
                .version(0)                          // 初始版本号
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();

        // 保存支付记录
        paymentMapper.insert(payment);
        log.info("支付记录创建成功: paymentNo={}", payment.getPaymentNo());

        return PaymentVO.fromEntity(payment);
    }

    @Override
    public PaymentVO getPaymentByNo(String paymentNo) {
        ThrowUtils.throwIf(!StringUtils.hasText(paymentNo), ErrorCode.PARAM_ERROR, "支付编号无效");
        Payment payment = paymentMapper.selectByPaymentNo(paymentNo);
        ThrowUtils.throwIf(payment == null, ErrorCode.PARAM_ERROR, "支付记录不存在");
        return PaymentVO.fromEntity(payment);
    }

    @Override
    public PaymentVO getPaymentById(Long id) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAM_ERROR, "支付ID无效");
        Payment payment = paymentMapper.selectById(id);
        ThrowUtils.throwIf(payment == null, ErrorCode.PARAM_ERROR, "支付记录不存在");
        return PaymentVO.fromEntity(payment);
    }

    @Override
    public List<PaymentVO> listByBillId(Long billId) {
        ThrowUtils.throwIf(billId == null || billId <= 0, ErrorCode.PARAM_ERROR, "账单ID无效");

        LambdaQueryWrapper<Payment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Payment::getBillId, billId)
               .orderByDesc(Payment::getCreateTime);

        return paymentMapper.selectList(wrapper).stream()
                .map(PaymentVO::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<PaymentVO> listByUserId(Long userId) {
        ThrowUtils.throwIf(userId == null || userId <= 0, ErrorCode.PARAM_ERROR, "用户ID无效");

        LambdaQueryWrapper<Payment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Payment::getUserId, userId)
               .orderByDesc(Payment::getCreateTime);

        return paymentMapper.selectList(wrapper).stream()
                .map(PaymentVO::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentVO simulatePayment(Long paymentId) {
        log.info("模拟支付: paymentId={}", paymentId);

        Payment payment = getPaymentEntityById(paymentId);

        // 状态检查
        ThrowUtils.throwIf(payment.getStatus() != STATUS_PENDING,
                ErrorCode.PARAM_ERROR, "支付状态不是待支付");

        // ✅ 使用Redisson分布式锁防止重复支付（用户级别）
        // 锁粒度：按用户ID+支付ID，防止同一用户重复支付同一订单
        String lockKey = String.format(RedisKeyConstant.LOCK_PAYMENT, payment.getUserId(), paymentId);
        boolean locked = redissonLockUtil.tryLock(lockKey, 5);
        if (!locked) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "支付操作正在进行中，请稍后重试");
        }

        try {
            // 双重检查：获取锁后再次确认状态
            payment = getPaymentEntityById(paymentId);
            ThrowUtils.throwIf(payment.getStatus() != STATUS_PENDING,
                    ErrorCode.PARAM_ERROR, "支付状态不是待支付");

            // ✅ 使用幂等键（支付编号）确保第三方调用幂等性
            String thirdPartyNo = generateThirdPartyNo(payment.getPaymentType());
            String idempotencyKey = "PAY_" + payment.getPaymentNo();
            log.info("第三方支付幂等键: idempotencyKey={}", idempotencyKey);

            // ✅ 乐观锁更新：只有版本号匹配且状态为待支付时才能更新
            Integer currentVersion = payment.getVersion();
            int affectedRows = paymentMapper.updateStatusWithVersion(
                    paymentId,
                    currentVersion,
                    STATUS_PAID,
                    thirdPartyNo
            );

            if (affectedRows == 0) {
                log.error("支付状态更新失败，可能存在并发问题: paymentId={}, version={}", paymentId, currentVersion);
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "支付状态已被其他操作修改，请刷新后重试");
            }

            // ✅ 乐观锁更新成功，重新查询最新的支付记录
            payment = paymentMapper.selectById(paymentId);

            // 更新账单状态（使用乐观锁）
            billService.payBillWithOptimisticLock(payment.getBillId(), payment.getAmount(), payment.getVersion());

            log.info("模拟支付成功: paymentNo={}, thirdPartyNo={}", payment.getPaymentNo(), thirdPartyNo);
            return PaymentVO.fromEntity(payment);

        } finally {
            // 释放分布式锁
            redissonLockUtil.unlock(lockKey);
        }
    }

    @Override
    public String getPaymentStatus(Long paymentId) {
        Payment payment = getPaymentEntityById(paymentId);
        return getStatusDescription(payment.getStatus());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentVO refund(RefundRequest request) {
        log.info("发起退款: paymentId={}, refundAmount={}, reason={}",
                request.getPaymentId(), request.getRefundAmount(), request.getReason());

        Payment payment = getPaymentEntityById(request.getPaymentId());

        ThrowUtils.throwIf(payment.getStatus() != STATUS_PAID,
                ErrorCode.PARAM_ERROR, "只有已支付的订单才能退款");

        BigDecimal refundAmount = request.getRefundAmount();
        ThrowUtils.throwIf(refundAmount == null || refundAmount.compareTo(BigDecimal.ZERO) <= 0,
                ErrorCode.PARAM_ERROR, "退款金额必须大于0");

        ThrowUtils.throwIf(refundAmount.compareTo(payment.getAmount()) > 0,
                ErrorCode.PARAM_ERROR, "退款金额超过支付金额");

        payment.setStatus(STATUS_REFUNDED);
        payment.setUpdateTime(LocalDateTime.now());
        paymentMapper.updateById(payment);

        billService.refundBill(payment.getBillId(), refundAmount, request.getReason());

        log.info("退款成功: paymentNo={}, refundAmount={}", payment.getPaymentNo(), refundAmount);
        return PaymentVO.fromEntity(payment);
    }

    @Override
    public Payment getPaymentEntityById(Long id) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAM_ERROR, "支付ID无效");
        Payment payment = paymentMapper.selectById(id);
        ThrowUtils.throwIf(payment == null, ErrorCode.PARAM_ERROR, "支付记录不存在");
        return payment;
    }

    private String generatePaymentNo() {
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String uuid = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return PAYMENT_NO_PREFIX + dateStr + uuid;
    }

    private String generateThirdPartyNo(String paymentType) {
        String prefix = switch (paymentType.toUpperCase()) {
            case PAYMENT_TYPE_WECHAT -> "WX";
            case PAYMENT_TYPE_ALIPAY -> "ALIPAY";
            case PAYMENT_TYPE_CASH -> "CASH";
            default -> "OTHER";
        };
        return prefix + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    private String getStatusDescription(int status) {
        return switch (status) {
            case STATUS_PENDING -> "待支付";
            case STATUS_PAID -> "已支付";
            case STATUS_CANCELLED -> "已取消";
            case STATUS_REFUNDED -> "已退款";
            default -> "未知";
        };
    }
}