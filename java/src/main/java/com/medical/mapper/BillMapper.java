package com.medical.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.medical.model.entity.Bill;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;

/**
 * 账单Mapper接口
 */
@Mapper
public interface BillMapper extends BaseMapper<Bill> {

    /**
     * 根据账单编号查询
     */
    Bill selectByBillNo(@Param("billNo") String billNo);

    /**
     * 原子更新账单支付状态
     * 使用乐观锁确保并发安全
     * @param billId 账单ID
     * @param currentVersion 当前版本号
     * @param newStatus 新状态
     * @param amount 支付金额
     * @return 更新行数，0表示版本号不匹配（并发冲突）
     */
    int updateStatusWithVersion(@Param("billId") Long billId,
                                 @Param("currentVersion") Integer currentVersion,
                                 @Param("newStatus") String newStatus,
                                 @Param("amount") BigDecimal amount);
}