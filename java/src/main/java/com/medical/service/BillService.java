package com.medical.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.medical.model.dto.bill.BillGenerateRequest;
import com.medical.model.dto.bill.BillQueryRequest;
import com.medical.model.entity.Bill;
import com.medical.model.vo.BillVO;

import java.util.List;

/**
 * 账单服务接口
 */
public interface BillService {

    /**
     * 根据预约ID生成账单
     *
     * @param appointmentId 预约ID
     * @return 账单VO
     */
    BillVO generateBill(Long appointmentId);

    /**
     * 生成或刷新预约账单：无账单则新建；有待支付账单则合并未结算费用项并重算金额
     *
     * @param appointmentId 预约ID
     * @return 账单VO
     */
    BillVO generateOrRefreshBill(Long appointmentId);

    /**
     * 根据预约ID列表生成账单
     *
     * @param request 账单生成请求
     * @return 账单VO
     */
    BillVO generateBillByRequest(BillGenerateRequest request);

    /**
     * 根据ID获取账单
     *
     * @param id 账单ID
     * @return 账单VO
     */
    BillVO getBillById(Long id);

    /**
     * 根据账单编号获取账单
     *
     * @param billNo 账单编号
     * @return 账单VO
     */
    BillVO getBillByNo(String billNo);

    /**
     * 分页查询账单列表
     *
     * @param current   当前页
     * @param pageSize  每页大小
     * @param request   查询条件
     * @return 分页结果
     */
    IPage<BillVO> listBillPage(long current, long pageSize, BillQueryRequest request);

    /**
     * 根据用户ID查询账单列表
     *
     * @param userId 用户ID
     * @return 账单列表
     */
    List<BillVO> listByUserId(Long userId);

    /**
     * 根据预约ID查询账单
     *
     * @param appointmentId 预约ID
     * @return 账单VO
     */
    BillVO getByAppointmentId(Long appointmentId);

    /**
     * 更新账单状态
     *
     * @param id     账单ID
     * @param status 状态
     */
    void updateBillStatus(Long id, String status);

    /**
     * 支付账单
     *
     * @param billId 账单ID
     * @param amount 支付金额
     */
    void payBill(Long billId, java.math.BigDecimal amount);

    /**
     * 使用乐观锁支付账单
     *
     * @param billId 账单ID
     * @param amount 支付金额
     * @param expectedVersion 期望的版本号（用于乐观锁检查）
     */
    void payBillWithOptimisticLock(Long billId, java.math.BigDecimal amount, Integer expectedVersion);

    /**
     * 退款
     *
     * @param billId       账单ID
     * @param refundAmount 退款金额
     * @param reason       退款原因
     */
    void refundBill(Long billId, java.math.BigDecimal refundAmount, String reason);

    /**
     * 导出账单为CSV
     *
     * @param request 查询条件
     * @return CSV字符串
     */
    String exportBillToCSV(BillQueryRequest request);

    /**
     * 获取账单实体
     *
     * @param id 账单ID
     * @return 账单实体
     */
    Bill getBillEntityById(Long id);
}