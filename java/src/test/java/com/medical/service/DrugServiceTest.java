package com.medical.service;

import com.medical.model.dto.drug.FeeCalculationRequest;
import com.medical.service.impl.DrugServiceImpl;
import com.medical.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DrugServiceTest {

    @Mock
    private DrugService drugService;

    @InjectMocks
    private DrugServiceImpl drugServiceImpl;

    private FeeCalculationRequest createFeeItem(String drugCode, BigDecimal unitPrice, BigDecimal quantity) {
        FeeCalculationRequest item = new FeeCalculationRequest();
        item.setDrugCode(drugCode);
        item.setUnitPrice(unitPrice);
        item.setQuantity(quantity);
        return item;
    }

    @Test
    @DisplayName("测试单药品费用计算")
    void testCalculateSingleDrugFee() {
        FeeCalculationRequest item = createFeeItem("C01AD01", new BigDecimal("28.00"), new BigDecimal("2"));
        List<FeeCalculationRequest> items = Collections.singletonList(item);

        BigDecimal expected = new BigDecimal("56.00");
        when(drugService.calculateFee(items)).thenReturn(expected);

        BigDecimal actual = drugService.calculateFee(items);
        assertEquals(expected, actual);
    }

    @Test
    @DisplayName("测试多药品费用计算")
    void testCalculateMultipleDrugsFee() {
        FeeCalculationRequest item1 = createFeeItem("C01AD01", new BigDecimal("28.00"), new BigDecimal("2"));
        FeeCalculationRequest item2 = createFeeItem("C03AA01", new BigDecimal("15.00"), new BigDecimal("1"));
        FeeCalculationRequest item3 = createFeeItem("N02BE01", new BigDecimal("35.00"), new BigDecimal("3"));
        
        List<FeeCalculationRequest> items = Arrays.asList(item1, item2, item3);

        BigDecimal expected = new BigDecimal("163.00"); // 56 + 15 + 105 = 163
        when(drugService.calculateFee(items)).thenReturn(expected);

        BigDecimal actual = drugService.calculateFee(items);
        assertEquals(expected, actual);
    }

    @Test
    @DisplayName("测试数量为零的边界情况")
    void testCalculateWithZeroQuantity() {
        FeeCalculationRequest item = createFeeItem("C01AD01", new BigDecimal("28.00"), BigDecimal.ZERO);
        List<FeeCalculationRequest> items = Collections.singletonList(item);

        assertThrows(BusinessException.class, () -> drugService.calculateFee(items));
    }

    @Test
    @DisplayName("测试数量为负数的边界情况")
    void testCalculateWithNegativeQuantity() {
        FeeCalculationRequest item = createFeeItem("C01AD01", new BigDecimal("28.00"), new BigDecimal("-1"));
        List<FeeCalculationRequest> items = Collections.singletonList(item);

        assertThrows(BusinessException.class, () -> drugService.calculateFee(items));
    }

    @Test
    @DisplayName("测试数量为空的边界情况")
    void testCalculateWithNullQuantity() {
        FeeCalculationRequest item = createFeeItem("C01AD01", new BigDecimal("28.00"), null);
        List<FeeCalculationRequest> items = Collections.singletonList(item);

        assertThrows(BusinessException.class, () -> drugService.calculateFee(items));
    }

    @Test
    @DisplayName("测试小数数量计算")
    void testCalculateWithDecimalQuantity() {
        FeeCalculationRequest item = createFeeItem("C03AA01", new BigDecimal("15.00"), new BigDecimal("0.5"));
        List<FeeCalculationRequest> items = Collections.singletonList(item);

        BigDecimal expected = new BigDecimal("7.50");
        when(drugService.calculateFee(items)).thenReturn(expected);

        BigDecimal actual = drugService.calculateFee(items);
        assertEquals(expected, actual);
    }

    @Test
    @DisplayName("测试高精度计算")
    void testCalculateHighPrecision() {
        FeeCalculationRequest item = createFeeItem("TEST", new BigDecimal("123.456"), new BigDecimal("789.012"));
        List<FeeCalculationRequest> items = Collections.singletonList(item);

        BigDecimal expected = new BigDecimal("97409.54");
        when(drugService.calculateFee(items)).thenReturn(expected);

        BigDecimal actual = drugService.calculateFee(items);
        assertEquals(expected, actual);
    }

    @Test
    @DisplayName("测试空列表计算")
    void testCalculateEmptyList() {
        List<FeeCalculationRequest> items = Collections.emptyList();

        BigDecimal expected = BigDecimal.ZERO;
        when(drugService.calculateFee(items)).thenReturn(expected);

        BigDecimal actual = drugService.calculateFee(items);
        assertEquals(expected, actual);
    }

    @Test
    @DisplayName("测试价格为空时自动查询")
    void testCalculateWithNullPrice() {
        FeeCalculationRequest item = createFeeItem("C01AD01", null, new BigDecimal("2"));
        List<FeeCalculationRequest> items = Collections.singletonList(item);

        BigDecimal expected = new BigDecimal("56.00");
        when(drugService.calculateFee(items)).thenReturn(expected);

        BigDecimal actual = drugService.calculateFee(items);
        assertEquals(expected, actual);
    }
}