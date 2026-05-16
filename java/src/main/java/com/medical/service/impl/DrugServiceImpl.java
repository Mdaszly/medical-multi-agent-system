package com.medical.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.medical.common.ErrorCode;
import com.medical.common.RedisCacheUtil;
import com.medical.constant.DrugConstant;
import com.medical.exception.BusinessException;
import com.medical.exception.ThrowUtils;
import com.medical.mapper.DrugInfoMapper;
import com.medical.mapper.DrugPriceMapper;
import com.medical.model.dto.drug.DrugAddRequest;
import com.medical.model.dto.drug.DrugQueryRequest;
import com.medical.model.dto.drug.DrugUpdateRequest;
import com.medical.model.dto.drug.FeeCalculationRequest;
import com.medical.model.dto.drug.PriceAddRequest;
import com.medical.model.entity.DrugInfo;
import com.medical.model.entity.DrugPrice;
import com.medical.model.vo.DrugVO;
import com.medical.model.vo.DrugWithPriceVO;
import com.medical.service.DrugService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 药品服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DrugServiceImpl extends ServiceImpl<DrugInfoMapper, DrugInfo> implements DrugService {

    private final DrugInfoMapper drugInfoMapper;
    private final DrugPriceMapper drugPriceMapper;
    private final RedisCacheUtil redisCacheUtil;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DrugVO addDrug(DrugAddRequest request) {
        DrugInfo existing = drugInfoMapper.selectByDrugCode(request.getDrugCode());
        ThrowUtils.throwIf(existing != null, ErrorCode.PARAM_ERROR, "药品编码已存在");

        DrugInfo drug = new DrugInfo();
        BeanUtils.copyProperties(request, drug);
        drug.setStatus(DrugConstant.DRUG_STATUS_ENABLED);
        drug.setCreateTime(LocalDateTime.now());
        drug.setUpdateTime(LocalDateTime.now());

        drugInfoMapper.insert(drug);
        return convertToVO(drug);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DrugVO updateDrug(DrugUpdateRequest request) {
        DrugInfo drug = drugInfoMapper.selectById(request.getId());
        ThrowUtils.throwIf(drug == null, ErrorCode.PARAM_ERROR, "药品不存在");

        BeanUtils.copyProperties(request, drug, "id", "drugCode", "createTime");
        drug.setUpdateTime(LocalDateTime.now());
        drugInfoMapper.updateById(drug);

        redisCacheUtil.delete(String.format(DrugConstant.CACHE_KEY_DRUG_INFO, drug.getDrugCode()));
        return convertToVO(drug);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDrug(Long id) {
        DrugInfo drug = drugInfoMapper.selectById(id);
        ThrowUtils.throwIf(drug == null, ErrorCode.PARAM_ERROR, "药品不存在");

        drug.setStatus(DrugConstant.DRUG_STATUS_DISABLED);
        drug.setUpdateTime(LocalDateTime.now());
        drugInfoMapper.updateById(drug);

        redisCacheUtil.delete(String.format(DrugConstant.CACHE_KEY_DRUG_INFO, drug.getDrugCode()));
    }

    @Override
    public DrugVO getDrugById(Long id) {
        DrugInfo drug = drugInfoMapper.selectById(id);
        ThrowUtils.throwIf(drug == null, ErrorCode.PARAM_ERROR, "药品不存在");
        return convertToVO(drug);
    }

    @Override
    public DrugVO getDrugByCode(String drugCode) {
        String cacheKey = String.format(DrugConstant.CACHE_KEY_DRUG_INFO, drugCode);
        DrugVO cacheVO = redisCacheUtil.get(cacheKey, DrugVO.class);
        if (cacheVO != null) return cacheVO;

        DrugInfo drug = drugInfoMapper.selectByDrugCode(drugCode);
        ThrowUtils.throwIf(drug == null, ErrorCode.PARAM_ERROR, "药品不存在");

        DrugVO vo = convertToVO(drug);
        redisCacheUtil.set(cacheKey, vo, Duration.ofSeconds(24 * 60 * 60));
        return vo;
    }

    @Override
    public List<DrugWithPriceVO> listDrugs(DrugQueryRequest request) {
        return drugInfoMapper.selectDrugWithPriceList(
                request.getDrugCode(), request.getDrugName(), request.getCategoryCode(), DrugConstant.DRUG_STATUS_ENABLED);
    }

    @Override
    public IPage<DrugVO> pageDrugs(DrugQueryRequest request) {
        Page<DrugInfo> page = new Page<>(request.getCurrent(), request.getPageSize());
        return page(page).convert(this::convertToVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addPrice(PriceAddRequest request) {
        DrugInfo drug = drugInfoMapper.selectById(request.getDrugId());
        ThrowUtils.throwIf(drug == null, ErrorCode.PARAM_ERROR, "药品不存在");

        LocalDate effectiveDate = request.getEffectiveDate() != null ? request.getEffectiveDate() : LocalDate.now();
        LocalDate expireDate = effectiveDate.minusDays(1);
        drugPriceMapper.updateExpireDate(request.getDrugId(), request.getPriceType(), expireDate);

        DrugPrice price = new DrugPrice();
        price.setDrugId(request.getDrugId());
        price.setPriceType(request.getPriceType());
        price.setPrice(request.getPrice());
        price.setEffectiveDate(effectiveDate);
        price.setExpireDate(null);
        price.setCreateTime(LocalDateTime.now());
        price.setCreateUserId(1L);

        drugPriceMapper.insert(price);
        redisCacheUtil.delete(String.format(DrugConstant.CACHE_KEY_DRUG_PRICE, drug.getDrugCode(), request.getPriceType()));
    }

    @Override
    public BigDecimal getCurrentPrice(Long drugId, String priceType) {
        DrugInfo drug = drugInfoMapper.selectById(drugId);
        ThrowUtils.throwIf(drug == null, ErrorCode.PARAM_ERROR, "药品不存在");
        return getCurrentPriceByCode(drug.getDrugCode(), priceType);
    }

    @Override
    public BigDecimal getCurrentPriceByCode(String drugCode, String priceType) {
        String cacheKey = String.format(DrugConstant.CACHE_KEY_DRUG_PRICE, drugCode, priceType);
        BigDecimal cachePrice = redisCacheUtil.get(cacheKey, BigDecimal.class);
        if (cachePrice != null) return cachePrice;

        DrugInfo drug = drugInfoMapper.selectByDrugCode(drugCode);
        ThrowUtils.throwIf(drug == null, ErrorCode.PARAM_ERROR, "药品不存在");

        BigDecimal price = drugPriceMapper.selectCurrentPrice(drug.getId(), priceType);
        if (price == null) throw new BusinessException(ErrorCode.PARAM_ERROR, "药品价格不存在");

        redisCacheUtil.set(cacheKey, price, Duration.ofSeconds(60 * 60));
        return price;
    }

    @Override
    public BigDecimal getPriceByDate(Long drugId, String priceType, LocalDate date) {
        ThrowUtils.throwIf(date == null, ErrorCode.PARAM_ERROR, "查询日期不能为空");
        return drugPriceMapper.selectPriceByDate(drugId, priceType, date);
    }

    @Override
    public BigDecimal calculateFee(List<FeeCalculationRequest> items) {
        BigDecimal total = BigDecimal.ZERO;
        for (FeeCalculationRequest item : items) {
            BigDecimal unitPrice = item.getUnitPrice();
            BigDecimal quantity = item.getQuantity();

            if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "药品数量必须大于0");
            }

            if (unitPrice == null) {
                ThrowUtils.throwIf(item.getDrugCode() == null || item.getDrugCode().isEmpty(),
                        ErrorCode.PARAM_ERROR, "药品编码不能为空");
                unitPrice = getCurrentPriceByCode(item.getDrugCode(), DrugConstant.PRICE_TYPE_RETAIL);
            }

            BigDecimal itemTotal = unitPrice.multiply(quantity).setScale(2, RoundingMode.HALF_UP);
            total = total.add(itemTotal);
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }

    private DrugVO convertToVO(DrugInfo drug) {
        DrugVO vo = new DrugVO();
        BeanUtils.copyProperties(drug, vo);
        return vo;
    }
}