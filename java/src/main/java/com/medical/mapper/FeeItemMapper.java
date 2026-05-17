package com.medical.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.medical.model.entity.FeeItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;
import java.util.List;

/**
 * 费用项Mapper接口
 */
@Mapper
public interface FeeItemMapper extends BaseMapper<FeeItem> {

    /**
     * 计算用户未结算费用总和
     */
    BigDecimal calculateUnsettledAmount(@Param("userId") Long userId);

    /**
     * 计算预约相关费用总和
     */
    BigDecimal calculateByAppointmentId(@Param("appointmentId") Long appointmentId);

    /**
     * 计算处方相关费用总和
     */
    BigDecimal calculateByPrescriptionId(@Param("prescriptionId") Long prescriptionId);

    /**
     * 批量更新账单ID
     */
    @Update("<script>" +
            "UPDATE fee_item SET bill_id = #{billId}, update_time = CURRENT_TIMESTAMP " +
            "WHERE id IN " +
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach>" +
            "</script>")
    void updateBillId(@Param("ids") List<Long> ids, @Param("billId") Long billId);

    /**
     * 批量标记已结算
     */
    @Update("<script>" +
            "UPDATE fee_item SET settle_flag = true, status = 'PAID', update_time = CURRENT_TIMESTAMP " +
            "WHERE id IN " +
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach>" +
            "</script>")
    void markAsSettled(@Param("ids") List<Long> ids);

    /**
     * 查询用户未结算的费用项
     */
    List<FeeItem> selectUnsettledByUserId(@Param("userId") Long userId);

    /**
     * 查询预约未结算的费用项
     */
    List<FeeItem> selectUnsettledByAppointmentId(@Param("appointmentId") Long appointmentId);

    /**
     * 查询处方未结算的费用项
     */
    List<FeeItem> selectUnsettledByPrescriptionId(@Param("prescriptionId") Long prescriptionId);

    /**
     * 根据账单ID查询费用项列表
     */
    @Select("SELECT * FROM fee_item WHERE bill_id = #{billId}")
    List<FeeItem> selectByBillId(@Param("billId") Long billId);
}