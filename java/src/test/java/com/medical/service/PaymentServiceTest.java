package com.medical.service;

import com.medical.common.RedissonLockUtil;
import com.medical.model.dto.payment.PaymentRequest;
import com.medical.model.dto.payment.RefundRequest;
import com.medical.model.entity.Bill;
import com.medical.model.entity.Payment;
import com.medical.model.vo.PaymentVO;
import com.medical.service.impl.PaymentServiceImpl;
import com.medical.mapper.BillMapper;
import com.medical.mapper.PaymentMapper;
import com.medical.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

class PaymentServiceTest {

    @Mock
    private PaymentMapper paymentMapper;

    @Mock
    private BillMapper billMapper;

    @Mock
    private BillService billService;

    @Mock
    private UserMapper userMapper;

    @Mock
    private RedissonLockUtil redissonLockUtil;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testCreatePayment() {
        PaymentRequest request = new PaymentRequest();
        request.setBillId(1L);
        request.setAmount(new BigDecimal("56.00"));
        request.setPaymentType("WECHAT");
        request.setUserName("张三");

        Bill bill = new Bill();
        bill.setId(1L);
        bill.setUserId(1L);
        bill.setSelfPayAmount(new BigDecimal("56.00"));
        bill.setStatus("UNPAID");

        when(billMapper.selectById(anyLong())).thenReturn(bill);
        when(paymentMapper.selectOne(any())).thenReturn(null);
        when(paymentMapper.insert(any(Payment.class))).thenAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            payment.setId(100L);
            return 1;
        });

        PaymentVO payment = paymentService.createPayment(request);

        assertNotNull(payment);
        assertEquals("WECHAT", payment.getPaymentType());
        assertEquals(new BigDecimal("56.00"), payment.getAmount());
        assertEquals(0, payment.getStatus());
    }

    @Test
    void testSimulatePayment() {
        Payment payment = new Payment();
        payment.setId(1L);
        payment.setBillId(1L);
        payment.setUserId(1L);
        payment.setPaymentNo("PAY20260101000001");
        payment.setAmount(new BigDecimal("56.00"));
        payment.setStatus(0);
        payment.setVersion(0);
        payment.setPaymentType("WECHAT");

        Bill bill = new Bill();
        bill.setId(1L);
        bill.setVersion(0);
        bill.setStatus("UNPAID");

        when(redissonLockUtil.tryLock(anyString(), anyLong())).thenReturn(true);
        when(paymentMapper.updateStatusWithVersion(eq(1L), eq(0), eq(1), anyString())).thenReturn(1);
        when(billMapper.selectById(1L)).thenReturn(bill);

        Payment paid = new Payment();
        paid.setId(1L);
        paid.setBillId(1L);
        paid.setUserId(1L);
        paid.setPaymentNo("PAY20260101000001");
        paid.setAmount(new BigDecimal("56.00"));
        paid.setStatus(1);
        paid.setVersion(1);
        paid.setPaymentType("WECHAT");
        when(paymentMapper.selectById(1L)).thenReturn(payment, payment, paid);

        doNothing().when(billService).payBillWithOptimisticLock(eq(1L), any(), eq(0));
        doNothing().when(redissonLockUtil).unlock(anyString());

        PaymentVO result = paymentService.simulatePayment(1L);

        assertNotNull(result);
        assertEquals(1, result.getStatus());
        assertEquals("已支付", result.getStatusDesc());
    }

    @Test
    void testRefund() {
        Payment payment = new Payment();
        payment.setId(1L);
        payment.setBillId(1L);
        payment.setAmount(new BigDecimal("56.00"));
        payment.setStatus(1);
        payment.setVersion(0);

        when(paymentMapper.selectById(anyLong())).thenReturn(payment);
        when(paymentMapper.updateById(any(Payment.class))).thenReturn(1);
        doNothing().when(billService).refundBill(eq(1L), any(), anyString());

        RefundRequest request = new RefundRequest();
        request.setPaymentId(1L);
        request.setRefundAmount(new BigDecimal("56.00"));
        request.setReason("患者申请退款");

        PaymentVO result = paymentService.refund(request);

        assertNotNull(result);
        assertEquals(3, result.getStatus());
        assertEquals("已退款", result.getStatusDesc());
    }

    @Test
    void testRefundNotPaid() {
        Payment payment = new Payment();
        payment.setId(1L);
        payment.setAmount(new BigDecimal("56.00"));
        payment.setStatus(0);

        when(paymentMapper.selectById(anyLong())).thenReturn(payment);

        RefundRequest request = new RefundRequest();
        request.setPaymentId(1L);
        request.setRefundAmount(new BigDecimal("56.00"));

        assertThrows(com.medical.exception.BusinessException.class, () -> paymentService.refund(request));
    }
}