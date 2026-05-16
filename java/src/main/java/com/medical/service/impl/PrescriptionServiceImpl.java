package com.medical.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.medical.common.ErrorCode;
import com.medical.constant.DrugConstant;
import com.medical.constant.PrescriptionConstant;
import com.medical.exception.BusinessException;
import com.medical.exception.ThrowUtils;
import com.medical.mapper.*;
import com.medical.model.dto.prescription.PrescriptionAddRequest;
import com.medical.model.dto.prescription.PrescriptionQueryRequest;
import com.medical.model.dto.prescription.PrescriptionStatusUpdateRequest;
import com.medical.model.entity.*;
import com.medical.model.vo.PrescriptionVO;
import com.medical.service.DrugService;
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
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PrescriptionVO createPrescription(PrescriptionAddRequest request) {
        Long doctorId = StpUtil.getLoginIdAsLong();
        Long appointmentId = request.getAppointmentId();

        ThrowUtils.throwIf(appointmentId == null || appointmentId <= 0, ErrorCode.PARAM_ERROR, "预约ID无效");
        ThrowUtils.throwIf(request.getDrugs() == null || request.getDrugs().isEmpty(), ErrorCode.PARAM_ERROR, "药品列表不能为空");

        Appointment appointment = appointmentMapper.selectById(appointmentId);
        ThrowUtils.throwIf(appointment == null, ErrorCode.PARAM_ERROR, "预约不存在");

        User user = userMapper.selectById(appointment.getUserId());
        ThrowUtils.throwIf(user == null, ErrorCode.USER_NOT_FOUND, "患者不存在");

        Doctor doctor = doctorMapper.selectById(doctorId);
        ThrowUtils.throwIf(doctor == null, ErrorCode.PARAM_ERROR, "医生不存在");

        Prescription prescription = new Prescription();
        prescription.setPrescriptionNo(generatePrescriptionNo());
        prescription.setAppointmentId(appointmentId);
        prescription.setUserId(user.getId());
        prescription.setUserName(user.getUserName());
        prescription.setDoctorId(doctorId);
        prescription.setDoctorName(doctor.getDoctorName());
        prescription.setDepartment(doctor.getDepartment());
        prescription.setDiagnosis(request.getDiagnosis());
        prescription.setStatus(PrescriptionConstant.PRESCRIPTION_STATUS_AUDITED);
        prescription.setAuditTime(LocalDateTime.now());
        prescription.setAuditUserId(doctorId);
        prescription.setRemark(request.getRemark());

        BigDecimal totalAmount = BigDecimal.ZERO;
        List<PrescriptionItem> items = new ArrayList<>();

        for (PrescriptionAddRequest.PrescriptionDrugItem drugItem : request.getDrugs()) {
            ThrowUtils.throwIf(!StringUtils.hasText(drugItem.getDrugCode()), ErrorCode.PARAM_ERROR, "药品编码不能为空");
            ThrowUtils.throwIf(!StringUtils.hasText(drugItem.getDrugName()), ErrorCode.PARAM_ERROR, "药品名称不能为空");
            ThrowUtils.throwIf(drugItem.getQuantity() == null || drugItem.getQuantity().compareTo(BigDecimal.ZERO) <= 0,
                    ErrorCode.PARAM_ERROR, "药品数量必须大于0");

            BigDecimal unitPrice = drugService.getCurrentPriceByCode(drugItem.getDrugCode(), DrugConstant.PRICE_TYPE_RETAIL);
            ThrowUtils.throwIf(unitPrice == null, ErrorCode.PARAM_ERROR, "药品价格不存在: " + drugItem.getDrugName());

            BigDecimal itemTotal = unitPrice.multiply(drugItem.getQuantity()).setScale(2, RoundingMode.HALF_UP);
            totalAmount = totalAmount.add(itemTotal);

            PrescriptionItem item = new PrescriptionItem();
            item.setDrugCode(drugItem.getDrugCode());
            item.setDrugName(drugItem.getDrugName());
            item.setSpecification(drugItem.getSpecification());
            item.setDosage(drugItem.getDosage());
            item.setUsage(drugItem.getUsage());
            item.setFrequency(drugItem.getFrequency());
            item.setDuration(drugItem.getDuration());
            item.setQuantity(drugItem.getQuantity());
            item.setUnitPrice(unitPrice);
            item.setTotalAmount(itemTotal);
            items.add(item);
        }

        prescription.setTotalAmount(totalAmount.setScale(2, RoundingMode.HALF_UP));
        
        // 将药品明细转为 JSON 存储到 drugs 字段
        try {
            String drugsJson = objectMapper.writeValueAsString(items);
            prescription.setDrugs(drugsJson);
        } catch (Exception e) {
            log.error("药品明细转JSON失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "药品明细序列化失败");
        }
        
        prescriptionMapper.insert(prescription);

        for (PrescriptionItem item : items) {
            item.setPrescriptionId(prescription.getId());
            prescriptionItemMapper.insert(item);
        }

        log.info("处方创建成功: prescriptionNo={}", prescription.getPrescriptionNo());

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
}
