package com.medical.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.medical.model.entity.Payment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 支付Mapper接口
 */
@Mapper
public interface PaymentMapper extends BaseMapper<Payment> {

    /**
     * 根据支付编号查询
     */
    Payment selectByPaymentNo(@Param("paymentNo") String paymentNo);

    /**
     * 乐观锁更新支付状态
     * @param paymentId 支付ID
     * @param currentVersion 当前版本号
     * @param newStatus 新状态
     * @param thirdPartyNo 第三方流水号
     * @return 更新行数，0表示版本号不匹配（并发冲突）
     */
    int updateStatusWithVersion(@Param("paymentId") Long paymentId,
                                 @Param("currentVersion") Integer currentVersion,
                                 @Param("newStatus") Integer newStatus,
                                 @Param("thirdPartyNo") String thirdPartyNo);
}