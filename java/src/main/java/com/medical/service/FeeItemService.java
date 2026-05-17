package com.medical.service;

import com.medical.model.entity.FeeItem;
import com.medical.model.vo.FeeItemVO;

import java.math.BigDecimal;
import java.util.List;

/**
 * 费用项服务接口
 */
public interface FeeItemService {

    /**
     * 创建费用项
     *
     * @param feeItem 费用项实体
     * @return 费用项VO
     */
    FeeItemVO createFeeItem(FeeItem feeItem);

    /**
     * 根据ID获取费用项
     *
     * @param id 费用项ID
     * @return 费用项VO
     */
    FeeItemVO getFeeItemById(Long id);

    /**
     * 根据用户ID查询费用项列表
     *
     * @param userId 用户ID
     * @return 费用项列表
     */
    List<FeeItemVO> listByUserId(Long userId);

    /**
     * 根据预约ID查询费用项列表
     *
     * @param appointmentId 预约ID
     * @return 费用项列表
     */
    List<FeeItemVO> listByAppointmentId(Long appointmentId);

    /**
     * 根据处方ID查询费用项列表
     *
     * @param prescriptionId 处方ID
     * @return 费用项列表
     */
    List<FeeItemVO> listByPrescriptionId(Long prescriptionId);

    /**
     * 根据账单ID查询费用项列表
     *
     * @param billId 账单ID
     * @return 费用项列表
     */
    List<FeeItemVO> listByBillId(Long billId);

    /**
     * 计算用户未结算费用总和
     *
     * @param userId 用户ID
     * @return 未结算费用总和
     */
    BigDecimal calculateUnsettledAmount(Long userId);

    /**
     * 计算预约相关费用总和
     *
     * @param appointmentId 预约ID
     * @return 费用总和
     */
    BigDecimal calculateByAppointmentId(Long appointmentId);

    /**
     * 批量创建费用项
     *
     * @param feeItems 费用项列表
     * @return 创建的数量
     */
    int batchCreate(List<FeeItem> feeItems);

    /**
     * 更新费用项关联的账单ID
     *
     * @param ids    费用项ID列表
     * @param billId 账单ID
     */
    void updateBillId(List<Long> ids, Long billId);

    /**
     * 标记费用项已结算
     *
     * @param ids 费用项ID列表
     */
    void markAsSettled(List<Long> ids);
}