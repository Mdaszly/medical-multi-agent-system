package com.medical.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.medical.common.ErrorCode;
import com.medical.constant.AppointmentConstant;
import com.medical.constant.DrugConstant;
import com.medical.constant.PrescriptionConstant;
import com.medical.exception.BusinessException;
import com.medical.exception.ThrowUtils;
import com.medical.mapper.*;
import com.medical.model.dto.prescription.PrescriptionAddRequest;
import com.medical.model.dto.prescription.PrescriptionQueryRequest;
import com.medical.model.dto.prescription.PrescriptionStatusUpdateRequest;
import com.medical.model.dto.prescription.PrescriptionUpdateRequest;
import com.medical.model.entity.*;
import com.medical.model.vo.PrescriptionVO;
import com.medical.service.AppointmentService;
import com.medical.service.BillService;
import com.medical.service.DrugService;
import com.medical.service.FeeItemService;
import com.medical.service.PrescriptionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PrescriptionServiceImpl implements PrescriptionService {

    private final PrescriptionMapper prescriptionMapper;
    private final PrescriptionItemMapper prescriptionItemMapper;
    private final AppointmentMapper appointmentMapper;
    private final DoctorMapper doctorMapper;
    private final UserMapper userMapper;
    private final DrugService drugService;
    private final FeeItemService feeItemService;
    private final BillService billService;
    private final AppointmentService appointmentService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PrescriptionVO createPrescription(PrescriptionAddRequest request) {
        // 获取当前登录医生ID
        Long doctorId = StpUtil.getLoginIdAsLong();
        Long appointmentId = request.getAppointmentId();

        // 参数校验
        ThrowUtils.throwIf(appointmentId == null || appointmentId <= 0, ErrorCode.PARAM_ERROR, "预约ID无效");
        ThrowUtils.throwIf(request.getDrugs() == null || request.getDrugs().isEmpty(), ErrorCode.PARAM_ERROR, "药品列表不能为空");

        // 步骤1：验证预约信息
        Appointment appointment = appointmentMapper.selectById(appointmentId);
        ThrowUtils.throwIf(appointment == null, ErrorCode.PARAM_ERROR, "预约不存在");

        ThrowUtils.throwIf(!doctorId.equals(appointment.getDoctorId()),
                ErrorCode.NO_AUTH, "只能为自己接诊的患者开方");

        Integer appointmentStatus = appointment.getStatus();
        boolean canPrescribe = AppointmentConstant.APPOINTMENT_STATUS_CHECKED_IN.equals(appointmentStatus)
                || AppointmentConstant.APPOINTMENT_STATUS_IN_CONSULTATION.equals(appointmentStatus);
        ThrowUtils.throwIf(!canPrescribe, ErrorCode.PARAM_ERROR, "患者尚未签到，无法开方");

        // 步骤2：验证患者信息
        User user = userMapper.selectById(appointment.getUserId());
        ThrowUtils.throwIf(user == null, ErrorCode.USER_NOT_FOUND, "患者不存在");

        // 步骤3：验证医生信息
        Doctor doctor = doctorMapper.selectById(doctorId);
        ThrowUtils.throwIf(doctor == null, ErrorCode.PARAM_ERROR, "医生不存在");

        // 步骤4：构建处方主记录
        Prescription prescription = new Prescription();
        prescription.setPrescriptionNo(generatePrescriptionNo());  // 生成唯一处方编号
        prescription.setAppointmentId(appointmentId);
        prescription.setUserId(user.getId());
        prescription.setUserName(user.getUserName());
        prescription.setDoctorId(doctorId);
        prescription.setDoctorName(doctor.getDoctorName());
        prescription.setDepartment(doctor.getDepartment());
        prescription.setDiagnosis(request.getDiagnosis());  // 诊断信息
        prescription.setStatus(PrescriptionConstant.PRESCRIPTION_STATUS_PENDING);  // 医生开具后需审核
        prescription.setRemark(request.getRemark());

        // 步骤5：处理药品明细，计算总金额
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<PrescriptionItem> items = new ArrayList<>();

        for (PrescriptionAddRequest.PrescriptionDrugItem drugItem : request.getDrugs()) {
            // 药品信息校验
            ThrowUtils.throwIf(!StringUtils.hasText(drugItem.getDrugCode()), ErrorCode.PARAM_ERROR, "药品编码不能为空");
            ThrowUtils.throwIf(!StringUtils.hasText(drugItem.getDrugName()), ErrorCode.PARAM_ERROR, "药品名称不能为空");
            ThrowUtils.throwIf(drugItem.getQuantity() == null || drugItem.getQuantity().compareTo(BigDecimal.ZERO) <= 0,
                    ErrorCode.PARAM_ERROR, "药品数量必须大于0");

            // 从缓存/数据库获取药品当前零售价格
            BigDecimal unitPrice = drugService.getCurrentPriceByCode(drugItem.getDrugCode(), DrugConstant.PRICE_TYPE_RETAIL);
            ThrowUtils.throwIf(unitPrice == null, ErrorCode.PARAM_ERROR, "药品价格不存在: " + drugItem.getDrugName());

            // 计算单项金额
            BigDecimal itemTotal = unitPrice.multiply(drugItem.getQuantity()).setScale(2, RoundingMode.HALF_UP);
            totalAmount = totalAmount.add(itemTotal);

            // 构建药品明细项
            PrescriptionItem item = new PrescriptionItem();
            item.setDrugCode(drugItem.getDrugCode());
            item.setDrugName(drugItem.getDrugName());
            item.setSpecification(drugItem.getSpecification());  // 规格
            item.setDosage(drugItem.getDosage());                // 剂量
            item.setUsage(drugItem.getUsage());                  // 用法
            item.setFrequency(drugItem.getFrequency());          // 频次
            item.setDuration(drugItem.getDuration());            // 用药时长
            item.setQuantity(drugItem.getQuantity());
            item.setUnitPrice(unitPrice);
            item.setTotalAmount(itemTotal);
            items.add(item);
        }

        prescription.setTotalAmount(totalAmount.setScale(2, RoundingMode.HALF_UP));
        
        // 将药品明细转为JSON存储（冗余存储，便于快速查看）
        try {
            String drugsJson = objectMapper.writeValueAsString(items);
            prescription.setDrugs(drugsJson);
        } catch (Exception e) {
            log.error("药品明细转JSON失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "药品明细序列化失败");
        }
        
        // 步骤6：保存处方主记录
        prescriptionMapper.insert(prescription);

        // 步骤7：保存处方明细记录
        for (PrescriptionItem item : items) {
            item.setPrescriptionId(prescription.getId());
            prescriptionItemMapper.insert(item);
        }

        // 步骤8：自动生成费用项（用于后续结算）
        createFeeItems(prescription, items);

        // 步骤9：更新预约状态为"诊疗中"
        try {
            appointmentService.updateAppointmentStatus(appointmentId, AppointmentConstant.APPOINTMENT_STATUS_IN_CONSULTATION);
            log.info("处方创建后更新预约状态为诊疗中: appointmentId={}", appointmentId);
        } catch (Exception e) {
            log.warn("更新预约状态失败: appointmentId={}, error={}", appointmentId, e.getMessage());
        }

        log.info("处方创建成功: prescriptionNo={}", prescription.getPrescriptionNo());

        // 构建返回VO
        PrescriptionVO vo = PrescriptionVO.fromEntity(prescription);
        vo.setItems(items.stream().map(PrescriptionVO.PrescriptionItemVO::fromEntity).collect(Collectors.toList()));
        return vo;
    }

    @Override
    public PrescriptionVO getPrescriptionById(Long id) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAM_ERROR, "处方ID无效");
        Prescription prescription = prescriptionMapper.selectById(id);
        ThrowUtils.throwIf(prescription == null, ErrorCode.PARAM_ERROR, "处方不存在");
        return buildPrescriptionVO(prescription);
    }

    @Override
    public PrescriptionVO getPrescriptionByNo(String prescriptionNo) {
        ThrowUtils.throwIf(!StringUtils.hasText(prescriptionNo), ErrorCode.PARAM_ERROR, "处方编号无效");
        Prescription prescription = prescriptionMapper.selectByPrescriptionNo(prescriptionNo);
        ThrowUtils.throwIf(prescription == null, ErrorCode.PARAM_ERROR, "处方不存在");
        return buildPrescriptionVO(prescription);
    }

    @Override
    public IPage<PrescriptionVO> listPrescriptionPage(long current, long pageSize, PrescriptionQueryRequest request) {
        Page<Prescription> page = new Page<>(current, pageSize);
        LambdaQueryWrapper<Prescription> wrapper = new LambdaQueryWrapper<>();

        if (request != null) {
            if (request.getId() != null) wrapper.eq(Prescription::getId, request.getId());
            if (StringUtils.hasText(request.getPrescriptionNo())) wrapper.eq(Prescription::getPrescriptionNo, request.getPrescriptionNo());
            if (request.getUserId() != null) wrapper.eq(Prescription::getUserId, request.getUserId());
            if (request.getDoctorId() != null) wrapper.eq(Prescription::getDoctorId, request.getDoctorId());
            if (StringUtils.hasText(request.getDepartment())) wrapper.eq(Prescription::getDepartment, request.getDepartment());
            if (request.getStatus() != null) wrapper.eq(Prescription::getStatus, request.getStatus());
            if (request.getAppointmentId() != null) wrapper.eq(Prescription::getAppointmentId, request.getAppointmentId());
        }

        wrapper.orderByDesc(Prescription::getCreateTime);
        return prescriptionMapper.selectPage(page, wrapper).convert(this::buildPrescriptionVO);
    }

    @Override
    public List<PrescriptionVO> listPrescriptionByUser(Long userId) {
        ThrowUtils.throwIf(userId == null || userId <= 0, ErrorCode.PARAM_ERROR, "用户ID无效");
        return prescriptionMapper.selectByUserId(userId).stream().map(this::buildPrescriptionVO).collect(Collectors.toList());
    }

    @Override
    public List<PrescriptionVO> listPrescriptionByDoctor(Long doctorId) {
        ThrowUtils.throwIf(doctorId == null || doctorId <= 0, ErrorCode.PARAM_ERROR, "医生ID无效");
        return prescriptionMapper.selectByDoctorId(doctorId).stream().map(this::buildPrescriptionVO).collect(Collectors.toList());
    }

    @Override
    public List<PrescriptionVO> listPrescriptionByAppointment(Long appointmentId) {
        ThrowUtils.throwIf(appointmentId == null || appointmentId <= 0, ErrorCode.PARAM_ERROR, "预约ID无效");
        return prescriptionMapper.selectByAppointmentId(appointmentId).stream().map(this::buildPrescriptionVO).collect(Collectors.toList());
    }

    @Override
    public List<PrescriptionVO> listPendingDispensePrescriptions() {
        LambdaQueryWrapper<Prescription> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Prescription::getStatus, PrescriptionConstant.PRESCRIPTION_STATUS_AUDITED);
        wrapper.orderByDesc(Prescription::getCreateTime);
        return prescriptionMapper.selectList(wrapper).stream().map(this::buildPrescriptionVO).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePrescriptionStatus(PrescriptionStatusUpdateRequest request) {
        Long prescriptionId = request.getPrescriptionId();
        Integer status = request.getStatus();

        ThrowUtils.throwIf(prescriptionId == null || prescriptionId <= 0, ErrorCode.PARAM_ERROR, "处方ID无效");
        ThrowUtils.throwIf(status == null, ErrorCode.PARAM_ERROR, "状态无效");

        Prescription prescription = prescriptionMapper.selectById(prescriptionId);
        ThrowUtils.throwIf(prescription == null, ErrorCode.PARAM_ERROR, "处方不存在");

        if (status.equals(PrescriptionConstant.PRESCRIPTION_STATUS_AUDITED)) {
            prescription.setAuditTime(LocalDateTime.now());
            prescription.setAuditUserId(StpUtil.getLoginIdAsLong());
            prescription.setAuditRemark(request.getRemark());
        }

        prescription.setStatus(status);
        prescriptionMapper.updateById(prescription);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void dispensePrescription(Long prescriptionId) {
        ThrowUtils.throwIf(prescriptionId == null || prescriptionId <= 0, ErrorCode.PARAM_ERROR, "处方ID无效");

        Prescription prescription = prescriptionMapper.selectById(prescriptionId);
        ThrowUtils.throwIf(prescription == null, ErrorCode.PARAM_ERROR, "处方不存在");

        ThrowUtils.throwIf(!prescription.getStatus().equals(PrescriptionConstant.PRESCRIPTION_STATUS_AUDITED),
                ErrorCode.PARAM_ERROR, "处方状态不是已审核，无法发药");

        prescription.setStatus(PrescriptionConstant.PRESCRIPTION_STATUS_DISPENSED);
        prescription.setDispenseTime(LocalDateTime.now());
        prescriptionMapper.updateById(prescription);

        if (prescription.getAppointmentId() != null) {
            generateBillAfterDispense(prescriptionId, prescription.getAppointmentId());
        }
    }

    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void generateBillAfterDispense(Long prescriptionId, Long appointmentId) {
        try {
            billService.generateBill(appointmentId);
            log.info("发药后自动生成账单成功: prescriptionId={}, appointmentId={}", 
                    prescriptionId, appointmentId);
        } catch (Exception e) {
            log.warn("发药后自动生成账单失败: prescriptionId={}, appointmentId={}, error={}", 
                    prescriptionId, appointmentId, e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelPrescription(Long prescriptionId) {
        ThrowUtils.throwIf(prescriptionId == null || prescriptionId <= 0, ErrorCode.PARAM_ERROR, "处方ID无效");

        Prescription prescription = prescriptionMapper.selectById(prescriptionId);
        ThrowUtils.throwIf(prescription == null, ErrorCode.PARAM_ERROR, "处方不存在");

        ThrowUtils.throwIf(!prescription.getStatus().equals(PrescriptionConstant.PRESCRIPTION_STATUS_PENDING),
                ErrorCode.PARAM_ERROR, "只有待审核状态的处方可以取消");

        prescription.setStatus(PrescriptionConstant.PRESCRIPTION_STATUS_CANCELLED);
        prescriptionMapper.updateById(prescription);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PrescriptionVO updatePrescription(PrescriptionUpdateRequest request) {
        Long prescriptionId = request.getId();
        ThrowUtils.throwIf(prescriptionId == null || prescriptionId <= 0, ErrorCode.PARAM_ERROR, "处方ID无效");

        Prescription prescription = prescriptionMapper.selectById(prescriptionId);
        ThrowUtils.throwIf(prescription == null, ErrorCode.PARAM_ERROR, "处方不存在");

        ThrowUtils.throwIf(!prescription.getStatus().equals(PrescriptionConstant.PRESCRIPTION_STATUS_PENDING),
                ErrorCode.PARAM_ERROR, "只有待审核状态的处方可以修改");

        Long doctorId = StpUtil.getLoginIdAsLong();
        ThrowUtils.throwIf(!prescription.getDoctorId().equals(doctorId),
                ErrorCode.NO_PERMISSION, "只能修改自己开具的处方");

        if (StringUtils.hasText(request.getDiagnosis())) {
            prescription.setDiagnosis(request.getDiagnosis());
        }
        if (StringUtils.hasText(request.getRemark())) {
            prescription.setRemark(request.getRemark());
        }

        BigDecimal totalAmount = BigDecimal.ZERO;
        List<PrescriptionItem> existingItems = prescriptionItemMapper.selectByPrescriptionId(prescriptionId);
        List<Long> existingItemIds = existingItems.stream().map(PrescriptionItem::getId).collect(Collectors.toList());

        List<Long> updatedItemIds = new ArrayList<>();

        if (request.getDrugs() != null && !request.getDrugs().isEmpty()) {
            for (PrescriptionUpdateRequest.PrescriptionDrugItem drugItem : request.getDrugs()) {
                ThrowUtils.throwIf(!StringUtils.hasText(drugItem.getDrugCode()), ErrorCode.PARAM_ERROR, "药品编码不能为空");
                ThrowUtils.throwIf(!StringUtils.hasText(drugItem.getDrugName()), ErrorCode.PARAM_ERROR, "药品名称不能为空");
                ThrowUtils.throwIf(drugItem.getQuantity() == null || drugItem.getQuantity().compareTo(BigDecimal.ZERO) <= 0,
                        ErrorCode.PARAM_ERROR, "药品数量必须大于0");

                BigDecimal unitPrice = drugService.getCurrentPriceByCode(drugItem.getDrugCode(), DrugConstant.PRICE_TYPE_RETAIL);
                ThrowUtils.throwIf(unitPrice == null, ErrorCode.PARAM_ERROR, "药品价格不存在: " + drugItem.getDrugName());

                BigDecimal itemTotal = unitPrice.multiply(drugItem.getQuantity()).setScale(2, RoundingMode.HALF_UP);
                totalAmount = totalAmount.add(itemTotal);

                if (drugItem.getId() != null) {
                    PrescriptionItem existingItem = existingItems.stream()
                            .filter(i -> i.getId().equals(drugItem.getId()))
                            .findFirst().orElse(null);
                    if (existingItem != null) {
                        existingItem.setDrugCode(drugItem.getDrugCode());
                        existingItem.setDrugName(drugItem.getDrugName());
                        existingItem.setSpecification(drugItem.getSpecification());
                        existingItem.setDosage(drugItem.getDosage());
                        existingItem.setUsage(drugItem.getUsage());
                        existingItem.setFrequency(drugItem.getFrequency());
                        existingItem.setDuration(drugItem.getDuration());
                        existingItem.setQuantity(drugItem.getQuantity());
                        existingItem.setUnitPrice(unitPrice);
                        existingItem.setTotalAmount(itemTotal);
                        prescriptionItemMapper.updateById(existingItem);
                        updatedItemIds.add(drugItem.getId());
                    }
                } else {
                    PrescriptionItem newItem = new PrescriptionItem();
                    newItem.setPrescriptionId(prescriptionId);
                    newItem.setDrugCode(drugItem.getDrugCode());
                    newItem.setDrugName(drugItem.getDrugName());
                    newItem.setSpecification(drugItem.getSpecification());
                    newItem.setDosage(drugItem.getDosage());
                    newItem.setUsage(drugItem.getUsage());
                    newItem.setFrequency(drugItem.getFrequency());
                    newItem.setDuration(drugItem.getDuration());
                    newItem.setQuantity(drugItem.getQuantity());
                    newItem.setUnitPrice(unitPrice);
                    newItem.setTotalAmount(itemTotal);
                    prescriptionItemMapper.insert(newItem);
                }
            }
        }

        List<Long> deletedItemIds = existingItemIds.stream()
                .filter(id -> !updatedItemIds.contains(id))
                .collect(Collectors.toList());
        if (!deletedItemIds.isEmpty()) {
            prescriptionItemMapper.deleteBatchIds(deletedItemIds);
        }

        prescription.setTotalAmount(totalAmount.setScale(2, RoundingMode.HALF_UP));
        prescription.setUpdateTime(LocalDateTime.now());
        prescriptionMapper.updateById(prescription);

        feeItemService.deleteByPrescriptionId(prescriptionId);
        List<PrescriptionItem> updatedItems = prescriptionItemMapper.selectByPrescriptionId(prescriptionId);
        createFeeItems(prescription, updatedItems);

        log.info("处方修改成功: prescriptionId={}", prescriptionId);

        return buildPrescriptionVO(prescription);
    }

    private PrescriptionVO buildPrescriptionVO(Prescription prescription) {
        PrescriptionVO vo = PrescriptionVO.fromEntity(prescription);
        vo.setItems(prescriptionItemMapper.selectByPrescriptionId(prescription.getId()).stream()
                .map(PrescriptionVO.PrescriptionItemVO::fromEntity).collect(Collectors.toList()));
        return vo;
    }

    private String generatePrescriptionNo() {
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String uuid = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return PrescriptionConstant.PRESCRIPTION_NO_PREFIX + dateStr + uuid;
    }

    private void createFeeItems(Prescription prescription, List<PrescriptionItem> items) {
        List<FeeItem> feeItems = new ArrayList<>();
        
        for (PrescriptionItem item : items) {
            FeeItem feeItem = FeeItem.builder()
                    .feeItemNo(generateFeeItemNo())
                    .userId(prescription.getUserId())
                    .appointmentId(prescription.getAppointmentId())
                    .prescriptionId(prescription.getId())
                    .itemType("PRESCRIPTION")
                    .itemName(item.getDrugName())
                    .itemCode(item.getDrugCode())
                    .quantity(item.getQuantity())
                    .unitPrice(item.getUnitPrice())
                    .totalAmount(item.getTotalAmount())
                    .discountAmount(BigDecimal.ZERO)
                    .actualAmount(item.getTotalAmount())
                    .insuranceAmount(BigDecimal.ZERO)
                    .selfPayAmount(item.getTotalAmount())
                    .status("UNSETTLED")
                    .settleFlag(false)
                    .createTime(LocalDateTime.now())
                    .updateTime(LocalDateTime.now())
                    .build();
            feeItems.add(feeItem);
        }
        
        feeItemService.batchCreate(feeItems);
        log.info("处方费用项创建成功: prescriptionId={}, count={}", prescription.getId(), feeItems.size());
    }

    private String generateFeeItemNo() {
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String uuid = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        return "FEE" + dateStr + uuid;
    }
}
