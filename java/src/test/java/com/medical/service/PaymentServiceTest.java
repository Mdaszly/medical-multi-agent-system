package com.medical.service;

import com.medical.model.dto.payment.PaymentRequest;
import com.medical.model.dto.payment.RefundRequest;
import com.medical.model.entity.Bill;
import com.medical.model.entity.Payment;
import com.medical.model.vo.PaymentVO;
import com.medical.service.impl.PaymentServiceImpl;
import com.medical.mapper.BillMapper;
import com.medical.mapper.PaymentMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

class PaymentServiceTest {

    @Mock
    private PaymentMapper paymentMapper;

    @Mock
    private BillMapper billMapper;

    @Mock
    private BillService billService;

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
        payment.setAmount(new BigDecimal("56.00"));
        payment.setStatus(0);

        when(paymentMapper.selectById(anyLong())).thenReturn(payment);

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

        when(paymentMapper.selectById(anyLong())).thenReturn(payment);

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