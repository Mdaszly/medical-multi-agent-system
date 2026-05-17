package com.medical.service;

import com.medical.model.dto.bill.BillGenerateRequest;
import com.medical.model.entity.FeeItem;
import com.medical.model.vo.BillVO;
import com.medical.service.impl.BillServiceImpl;
import com.medical.service.impl.FeeItemServiceImpl;
import com.medical.mapper.BillMapper;
import com.medical.mapper.FeeItemMapper;
import com.medical.mapper.AppointmentMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

class BillServiceTest {

    @Mock
    private BillMapper billMapper;

    @Mock
    private FeeItemMapper feeItemMapper;

    @Mock
    private AppointmentMapper appointmentMapper;

    @Mock
    private FeeItemService feeItemService;

    @InjectMocks
    private BillServiceImpl billService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testGenerateBillByRequest() {
        BillGenerateRequest request = new BillGenerateRequest();
        request.setUserId(1L);
        request.setPrescriptionId(100L);

        List<FeeItem> feeItems = new ArrayList<>();
        FeeItem feeItem = new FeeItem();
        feeItem.setId(1L);
        feeItem.setUserId(1L);
        feeItem.setItemName("阿莫西林胶囊");
        feeItem.setUnitPrice(new BigDecimal("28.00"));
        feeItem.setQuantity(new BigDecimal("2"));
        feeItem.setTotalAmount(new BigDecimal("56.00"));
        feeItem.setDiscountAmount(BigDecimal.ZERO);
        feeItem.setInsuranceAmount(BigDecimal.ZERO);
        feeItem.setSelfPayAmount(new BigDecimal("56.00"));
        feeItems.add(feeItem);

        when(feeItemMapper.selectUnsettledByPrescriptionId(anyLong())).thenReturn(feeItems);

        BillVO bill = billService.generateBillByRequest(request);

        assertNotNull(bill);
        assertEquals(new BigDecimal("56.00"), bill.getTotalAmount());
        assertEquals(new BigDecimal("56.00"), bill.getSelfPayAmount());
        assertEquals("UNPAID", bill.getStatus());
    }

    @Test
    void testPayBill() {
        com.medical.model.entity.Bill bill = new com.medical.model.entity.Bill();
        bill.setId(1L);
        bill.setSelfPayAmount(new BigDecimal("56.00"));
        bill.setPaidAmount(BigDecimal.ZERO);
        bill.setStatus("UNPAID");

        when(billMapper.selectById(anyLong())).thenReturn(bill);

        assertDoesNotThrow(() -> billService.payBill(1L, new BigDecimal("56.00")));
    }

    @Test
    void testRefundBill() {
        com.medical.model.entity.Bill bill = new com.medical.model.entity.Bill();
        bill.setId(1L);
        bill.setPaidAmount(new BigDecimal("56.00"));
        bill.setStatus("PAID");

        when(billMapper.selectById(anyLong())).thenReturn(bill);

        assertDoesNotThrow(() -> billService.refundBill(1L, new BigDecimal("56.00"), "患者申请退款"));
    }

    @Test
    void testRefundBillNotPaid() {
        com.medical.model.entity.Bill bill = new com.medical.model.entity.Bill();
        bill.setId(1L);
        bill.setPaidAmount(BigDecimal.ZERO);
        bill.setStatus("UNPAID");

        when(billMapper.selectById(anyLong())).thenReturn(bill);

        assertThrows(com.medical.exception.BusinessException.class, 
                () -> billService.refundBill(1L, new BigDecimal("56.00"), "申请退款"));
    }
}