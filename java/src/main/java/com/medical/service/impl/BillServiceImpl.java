package com.medical.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.medical.common.ErrorCode;
import com.medical.exception.BusinessException;
import com.medical.exception.ThrowUtils;
import com.medical.mapper.AppointmentMapper;
import com.medical.mapper.BillMapper;
import com.medical.mapper.FeeItemMapper;
import com.medical.model.dto.bill.BillGenerateRequest;
import com.medical.model.dto.bill.BillQueryRequest;
import com.medical.model.entity.Appointment;
import com.medical.model.entity.Bill;
import com.medical.model.entity.FeeItem;
import com.medical.model.vo.BillVO;
import com.medical.model.vo.FeeItemVO;
import com.medical.service.BillService;
import com.medical.service.FeeItemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 账单服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BillServiceImpl implements BillService {

    private final BillMapper billMapper;
    private final FeeItemMapper feeItemMapper;
    private final AppointmentMapper appointmentMapper;
    private final FeeItemService feeItemService;

    private static final String BILL_NO_PREFIX = "BILL";
    private static final String STATUS_UNPAID = "UNPAID";
    private static final String STATUS_PAID = "PAID";
    private static final String STATUS_REFUNDED = "REFUNDED";
    private static final String STATUS_CANCELLED = "CANCELLED";

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BillVO generateBill(Long appointmentId) {
        log.info("生成账单: appointmentId={}", appointmentId);

        // 验证预约存在
        Appointment appointment = appointmentMapper.selectById(appointmentId);
        ThrowUtils.throwIf(appointment == null, ErrorCode.PARAM_ERROR, "预约不存在");

        // 查询该预约下所有未结算的费用项
        // 费用项来源：处方药品费用、诊疗费用等
        List<FeeItem> unsettledItems = feeItemMapper.selectUnsettledByAppointmentId(appointmentId);
        if (CollectionUtils.isEmpty(unsettledItems)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "无可结算费用项");
        }

        // 创建账单
        return createBill(appointment.getUserId(), appointmentId, unsettledItems);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BillVO generateBillByRequest(BillGenerateRequest request) {
        log.info("根据请求生成账单: userId={}", request.getUserId());

        ThrowUtils.throwIf(request.getUserId() == null || request.getUserId() <= 0, 
                ErrorCode.PARAM_ERROR, "用户ID无效");

        List<FeeItem> unsettledItems = new ArrayList<>();

        if (request.getAppointmentId() != null) {
            unsettledItems.addAll(feeItemMapper.selectUnsettledByAppointmentId(request.getAppointmentId()));
        }

        if (request.getPrescriptionId() != null) {
            unsettledItems.addAll(feeItemMapper.selectUnsettledByPrescriptionId(request.getPrescriptionId()));
        }

        if (CollectionUtils.isEmpty(unsettledItems)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "无可结算费用项");
        }

        return createBill(request.getUserId(), request.getAppointmentId(), unsettledItems);
    }

    private BillVO createBill(Long userId, Long appointmentId, List<FeeItem> feeItems) {
        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal discountAmount = BigDecimal.ZERO;
        BigDecimal insuranceAmount = BigDecimal.ZERO;
        BigDecimal selfPayAmount = BigDecimal.ZERO;

        for (FeeItem item : feeItems) {
            totalAmount = totalAmount.add(item.getTotalAmount() != null ? item.getTotalAmount() : BigDecimal.ZERO);
            discountAmount = discountAmount.add(item.getDiscountAmount() != null ? item.getDiscountAmount() : BigDecimal.ZERO);
            insuranceAmount = insuranceAmount.add(item.getInsuranceAmount() != null ? item.getInsuranceAmount() : BigDecimal.ZERO);
            selfPayAmount = selfPayAmount.add(item.getSelfPayAmount() != null ? item.getSelfPayAmount() : BigDecimal.ZERO);
        }

        BigDecimal actualAmount = totalAmount.subtract(discountAmount).setScale(2, RoundingMode.HALF_UP);

        Bill bill = new Bill();
        bill.setBillNo(generateBillNo());
        bill.setUserId(userId);
        bill.setAppointmentId(appointmentId);
        bill.setTotalAmount(totalAmount.setScale(2, RoundingMode.HALF_UP));
        bill.setDiscountAmount(discountAmount.setScale(2, RoundingMode.HALF_UP));
        bill.setInsuranceAmount(insuranceAmount.setScale(2, RoundingMode.HALF_UP));
        bill.setSelfPayAmount(selfPayAmount.setScale(2, RoundingMode.HALF_UP));
        bill.setPaidAmount(BigDecimal.ZERO);
        bill.setStatus(STATUS_UNPAID);
        bill.setCreateTime(LocalDateTime.now());
        bill.setUpdateTime(LocalDateTime.now());

        billMapper.insert(bill);

        List<Long> feeItemIds = feeItems.stream().map(FeeItem::getId).collect(Collectors.toList());
        feeItemService.updateBillId(feeItemIds, bill.getId());

        log.info("账单生成成功: billNo={}, totalAmount={}", bill.getBillNo(), totalAmount);

        return buildBillVO(bill, feeItems);
    }

    @Override
    public BillVO getBillById(Long id) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAM_ERROR, "账单ID无效");
        Bill bill = billMapper.selectById(id);
        ThrowUtils.throwIf(bill == null, ErrorCode.PARAM_ERROR, "账单不存在");
        return buildBillVO(bill);
    }

    @Override
    public BillVO getBillByNo(String billNo) {
        ThrowUtils.throwIf(!StringUtils.hasText(billNo), ErrorCode.PARAM_ERROR, "账单编号无效");
        Bill bill = billMapper.selectByBillNo(billNo);
        ThrowUtils.throwIf(bill == null, ErrorCode.PARAM_ERROR, "账单不存在");
        return buildBillVO(bill);
    }

    @Override
    public IPage<BillVO> listBillPage(long current, long pageSize, BillQueryRequest request) {
        Page<Bill> page = new Page<>(current, pageSize);
        LambdaQueryWrapper<Bill> wrapper = new LambdaQueryWrapper<>();

        if (request != null) {
            if (request.getUserId() != null) {
                wrapper.eq(Bill::getUserId, request.getUserId());
            }
            if (request.getAppointmentId() != null) {
                wrapper.eq(Bill::getAppointmentId, request.getAppointmentId());
            }
            if (StringUtils.hasText(request.getBillNo())) {
                wrapper.eq(Bill::getBillNo, request.getBillNo());
            }
            if (StringUtils.hasText(request.getStatus())) {
                wrapper.eq(Bill::getStatus, request.getStatus());
            }
        }

        wrapper.orderByDesc(Bill::getCreateTime);
        return billMapper.selectPage(page, wrapper).convert(this::buildBillVO);
    }

    @Override
    public List<BillVO> listByUserId(Long userId) {
        ThrowUtils.throwIf(userId == null || userId <= 0, ErrorCode.PARAM_ERROR, "用户ID无效");
        
        LambdaQueryWrapper<Bill> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Bill::getUserId, userId)
               .orderByDesc(Bill::getCreateTime);
        
        return billMapper.selectList(wrapper).stream()
                .map(this::buildBillVO)
                .collect(Collectors.toList());
    }

    @Override
    public BillVO getByAppointmentId(Long appointmentId) {
        ThrowUtils.throwIf(appointmentId == null || appointmentId <= 0, ErrorCode.PARAM_ERROR, "预约ID无效");
        
        LambdaQueryWrapper<Bill> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Bill::getAppointmentId, appointmentId);
        
        Bill bill = billMapper.selectOne(wrapper);
        if (bill == null) {
            return null;
        }
        return buildBillVO(bill);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateBillStatus(Long id, String status) {
        Bill bill = getBillEntityById(id);
        bill.setStatus(status);
        bill.setUpdateTime(LocalDateTime.now());
        billMapper.updateById(bill);
        log.info("账单状态更新: billId={}, status={}", id, status);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void payBill(Long billId, BigDecimal amount) {
        Bill bill = getBillEntityById(billId);

        ThrowUtils.throwIf(!STATUS_UNPAID.equals(bill.getStatus()),
                ErrorCode.PARAM_ERROR, "账单状态不是待支付");

        ThrowUtils.throwIf(amount.compareTo(bill.getSelfPayAmount()) != 0,
                ErrorCode.PARAM_ERROR, "支付金额与账单金额不符");

        // ✅ 使用原子更新，确保金额累加和状态更新的原子性
        bill.setPaidAmount(bill.getPaidAmount().add(amount));
        bill.setStatus(STATUS_PAID);
        bill.setPayTime(LocalDateTime.now());
        bill.setUpdateTime(LocalDateTime.now());

        int affectedRows = billMapper.updateById(bill);
        if (affectedRows == 0) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "账单状态已变更，支付失败");
        }

        List<FeeItem> feeItems = feeItemMapper.selectByBillId(billId);
        if (!CollectionUtils.isEmpty(feeItems)) {
            List<Long> ids = feeItems.stream().map(FeeItem::getId).collect(Collectors.toList());
            feeItemService.markAsSettled(ids);
        }

        log.info("账单支付成功: billId={}, amount={}", billId, amount);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void payBillWithOptimisticLock(Long billId, BigDecimal amount, Integer expectedVersion) {
        log.info("使用乐观锁支付账单: billId={}, amount={}, expectedVersion={}", billId, amount, expectedVersion);

        // ✅ 使用乐观锁原子更新：只有版本号匹配且状态为UNPAID时才能更新
        int affectedRows = billMapper.updateStatusWithVersion(
                billId,
                expectedVersion,
                STATUS_PAID,
                amount
        );

        if (affectedRows == 0) {
            log.error("乐观锁更新失败，版本号不匹配或状态已变更: billId={}, expectedVersion={}", billId, expectedVersion);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "账单状态已被其他操作修改，请刷新后重试");
        }

        // 更新费用项状态
        List<FeeItem> feeItems = feeItemMapper.selectByBillId(billId);
        if (!CollectionUtils.isEmpty(feeItems)) {
            List<Long> ids = feeItems.stream().map(FeeItem::getId).collect(Collectors.toList());
            feeItemService.markAsSettled(ids);
        }

        log.info("账单支付成功（乐观锁）: billId={}, amount={}", billId, amount);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void refundBill(Long billId, BigDecimal refundAmount, String reason) {
        Bill bill = getBillEntityById(billId);

        ThrowUtils.throwIf(!STATUS_PAID.equals(bill.getStatus()), 
                ErrorCode.PARAM_ERROR, "账单状态不是已支付，无法退款");

        ThrowUtils.throwIf(refundAmount.compareTo(bill.getPaidAmount()) > 0, 
                ErrorCode.PARAM_ERROR, "退款金额超过已支付金额");

        bill.setStatus(STATUS_REFUNDED);
        bill.setRemark(reason);
        bill.setUpdateTime(LocalDateTime.now());
        billMapper.updateById(bill);

        log.info("账单退款成功: billId={}, refundAmount={}, reason={}", billId, refundAmount, reason);
    }

    @Override
    public String exportBillToCSV(BillQueryRequest request) {
        List<BillVO> bills;
        
        if (request.getUserId() != null) {
            bills = listByUserId(request.getUserId());
        } else {
            IPage<BillVO> page = listBillPage(1, Integer.MAX_VALUE, request);
            bills = page.getRecords();
        }

        StringBuilder csv = new StringBuilder();
        csv.append("账单编号,用户ID,预约ID,总金额,优惠金额,医保金额,自付金额,已支付金额,状态,创建时间\n");

        for (BillVO bill : bills) {
            csv.append(String.format("%s,%d,%s,%s,%s,%s,%s,%s,%s,%s\n",
                    bill.getBillNo(),
                    bill.getUserId(),
                    bill.getAppointmentId() != null ? bill.getAppointmentId() : "",
                    bill.getTotalAmount(),
                    bill.getDiscountAmount(),
                    bill.getInsuranceAmount(),
                    bill.getSelfPayAmount(),
                    bill.getPaidAmount(),
                    bill.getStatus(),
                    bill.getCreateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            ));
        }

        return csv.toString();
    }

    @Override
    public Bill getBillEntityById(Long id) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAM_ERROR, "账单ID无效");
        Bill bill = billMapper.selectById(id);
        ThrowUtils.throwIf(bill == null, ErrorCode.PARAM_ERROR, "账单不存在");
        return bill;
    }

    private BillVO buildBillVO(Bill bill) {
        List<FeeItem> feeItems = feeItemMapper.selectByBillId(bill.getId());
        return buildBillVO(bill, feeItems);
    }

    private BillVO buildBillVO(Bill bill, List<FeeItem> feeItems) {
        BillVO vo = BillVO.fromEntity(bill);
        if (!CollectionUtils.isEmpty(feeItems)) {
            vo.setFeeItems(feeItems.stream()
                    .map(FeeItemVO::fromEntity)
                    .collect(Collectors.toList()));
        }
        return vo;
    }

    private String generateBillNo() {
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String uuid = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return BILL_NO_PREFIX + dateStr + uuid;
    }
}