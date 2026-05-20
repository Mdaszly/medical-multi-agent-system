package com.medical.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.medical.common.ErrorCode;
import com.medical.exception.ThrowUtils;
import com.medical.mapper.FeeItemMapper;
import com.medical.model.entity.FeeItem;
import com.medical.model.vo.FeeItemVO;
import com.medical.service.FeeItemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 费用项服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeeItemServiceImpl implements FeeItemService {

    private final FeeItemMapper feeItemMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FeeItemVO createFeeItem(FeeItem feeItem) {
        log.info("创建费用项: userId={}, itemType={}, amount={}", 
                feeItem.getUserId(), feeItem.getItemType(), feeItem.getTotalAmount());
        
        feeItemMapper.insert(feeItem);
        return FeeItemVO.fromEntity(feeItem);
    }

    @Override
    public FeeItemVO getFeeItemById(Long id) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAM_ERROR, "费用项ID无效");
        FeeItem feeItem = feeItemMapper.selectById(id);
        ThrowUtils.throwIf(feeItem == null, ErrorCode.PARAM_ERROR, "费用项不存在");
        return FeeItemVO.fromEntity(feeItem);
    }

    @Override
    public List<FeeItemVO> listByUserId(Long userId) {
        ThrowUtils.throwIf(userId == null || userId <= 0, ErrorCode.PARAM_ERROR, "用户ID无效");
        
        LambdaQueryWrapper<FeeItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FeeItem::getUserId, userId)
               .orderByDesc(FeeItem::getCreateTime);
        
        return feeItemMapper.selectList(wrapper).stream()
                .map(FeeItemVO::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<FeeItemVO> listByAppointmentId(Long appointmentId) {
        ThrowUtils.throwIf(appointmentId == null || appointmentId <= 0, ErrorCode.PARAM_ERROR, "预约ID无效");
        
        LambdaQueryWrapper<FeeItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FeeItem::getAppointmentId, appointmentId)
               .orderByDesc(FeeItem::getCreateTime);
        
        return feeItemMapper.selectList(wrapper).stream()
                .map(FeeItemVO::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<FeeItemVO> listByPrescriptionId(Long prescriptionId) {
        ThrowUtils.throwIf(prescriptionId == null || prescriptionId <= 0, ErrorCode.PARAM_ERROR, "处方ID无效");
        
        LambdaQueryWrapper<FeeItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FeeItem::getPrescriptionId, prescriptionId)
               .orderByDesc(FeeItem::getCreateTime);
        
        return feeItemMapper.selectList(wrapper).stream()
                .map(FeeItemVO::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<FeeItemVO> listByBillId(Long billId) {
        ThrowUtils.throwIf(billId == null || billId <= 0, ErrorCode.PARAM_ERROR, "账单ID无效");
        
        LambdaQueryWrapper<FeeItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FeeItem::getBillId, billId)
               .orderByDesc(FeeItem::getCreateTime);
        
        return feeItemMapper.selectList(wrapper).stream()
                .map(FeeItemVO::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public BigDecimal calculateUnsettledAmount(Long userId) {
        ThrowUtils.throwIf(userId == null || userId <= 0, ErrorCode.PARAM_ERROR, "用户ID无效");
        return feeItemMapper.calculateUnsettledAmount(userId);
    }

    @Override
    public BigDecimal calculateByAppointmentId(Long appointmentId) {
        ThrowUtils.throwIf(appointmentId == null || appointmentId <= 0, ErrorCode.PARAM_ERROR, "预约ID无效");
        return feeItemMapper.calculateByAppointmentId(appointmentId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchCreate(List<FeeItem> feeItems) {
        ThrowUtils.throwIf(feeItems == null || feeItems.isEmpty(), ErrorCode.PARAM_ERROR, "费用项列表不能为空");
        
        for (FeeItem feeItem : feeItems) {
            feeItemMapper.insert(feeItem);
        }
        log.info("批量创建费用项完成: count={}", feeItems.size());
        return feeItems.size();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateBillId(List<Long> ids, Long billId) {
        ThrowUtils.throwIf(ids == null || ids.isEmpty(), ErrorCode.PARAM_ERROR, "费用项ID列表不能为空");
        ThrowUtils.throwIf(billId == null || billId <= 0, ErrorCode.PARAM_ERROR, "账单ID无效");
        
        feeItemMapper.updateBillId(ids, billId);
        log.info("更新费用项账单ID: count={}, billId={}", ids.size(), billId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markAsSettled(List<Long> ids) {
        ThrowUtils.throwIf(ids == null || ids.isEmpty(), ErrorCode.PARAM_ERROR, "费用项ID列表不能为空");
        
        feeItemMapper.markAsSettled(ids);
        log.info("标记费用项已结算: count={}", ids.size());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteByPrescriptionId(Long prescriptionId) {
        ThrowUtils.throwIf(prescriptionId == null || prescriptionId <= 0, ErrorCode.PARAM_ERROR, "处方ID无效");
        
        LambdaQueryWrapper<FeeItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FeeItem::getPrescriptionId, prescriptionId);
        feeItemMapper.delete(wrapper);
        log.info("删除处方费用项: prescriptionId={}", prescriptionId);
    }
}