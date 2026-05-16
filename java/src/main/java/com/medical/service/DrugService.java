package com.medical.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.medical.model.dto.drug.DrugAddRequest;
import com.medical.model.dto.drug.DrugQueryRequest;
import com.medical.model.dto.drug.DrugUpdateRequest;
import com.medical.model.dto.drug.FeeCalculationRequest;
import com.medical.model.dto.drug.PriceAddRequest;
import com.medical.model.vo.DrugVO;
import com.medical.model.vo.DrugWithPriceVO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface DrugService {

    DrugVO addDrug(DrugAddRequest request);

    DrugVO updateDrug(DrugUpdateRequest request);

    void deleteDrug(Long id);

    DrugVO getDrugById(Long id);

    DrugVO getDrugByCode(String drugCode);

    List<DrugWithPriceVO> listDrugs(DrugQueryRequest request);

    IPage<DrugVO> pageDrugs(DrugQueryRequest request);

    void addPrice(PriceAddRequest request);

    BigDecimal getCurrentPrice(Long drugId, String priceType);

    BigDecimal getCurrentPriceByCode(String drugCode, String priceType);

    BigDecimal getPriceByDate(Long drugId, String priceType, LocalDate date);

    BigDecimal calculateFee(List<FeeCalculationRequest> items);
}