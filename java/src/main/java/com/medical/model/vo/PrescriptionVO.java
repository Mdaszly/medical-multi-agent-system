package com.medical.model.vo;

import com.medical.model.entity.Prescription;
import com.medical.model.entity.PrescriptionItem;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class PrescriptionVO {

    private Long id;

    private String prescriptionNo;

    private Long appointmentId;

    private Long userId;

    private String userName;

    private Long doctorId;

    private String doctorName;

    private String department;

    private String diagnosis;

    private BigDecimal totalAmount;

    private Integer status;

    private String statusText;

    private LocalDateTime auditTime;

    private Long auditUserId;

    private String auditRemark;

    private LocalDateTime dispenseTime;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private List<PrescriptionItemVO> items;

    public static PrescriptionVO fromEntity(Prescription prescription) {
        if (prescription == null) {
            return null;
        }
        PrescriptionVO vo = new PrescriptionVO();
        vo.setId(prescription.getId());
        vo.setPrescriptionNo(prescription.getPrescriptionNo());
        vo.setAppointmentId(prescription.getAppointmentId());
        vo.setUserId(prescription.getUserId());
        vo.setUserName(prescription.getUserName());
        vo.setDoctorId(prescription.getDoctorId());
        vo.setDoctorName(prescription.getDoctorName());
        vo.setDepartment(prescription.getDepartment());
        vo.setDiagnosis(prescription.getDiagnosis());
        vo.setTotalAmount(prescription.getTotalAmount());
        vo.setStatus(prescription.getStatus());
        vo.setStatusText(getStatusText(prescription.getStatus()));
        vo.setAuditTime(prescription.getAuditTime());
        vo.setAuditUserId(prescription.getAuditUserId());
        vo.setAuditRemark(prescription.getAuditRemark());
        vo.setDispenseTime(prescription.getDispenseTime());
        vo.setRemark(prescription.getRemark());
        vo.setCreateTime(prescription.getCreateTime());
        vo.setUpdateTime(prescription.getUpdateTime());
        return vo;
    }

    private static String getStatusText(Integer status) {
        if (status == null) {
            return "未知";
        }
        return switch (status) {
            case 0 -> "待审核";
            case 1 -> "已审核";
            case 2 -> "已发药";
            case 3 -> "已完成";
            case 4 -> "已取消";
            default -> "未知";
        };
    }

    @Data
    public static class PrescriptionItemVO {
        private Long id;
        private Long prescriptionId;
        private String drugCode;
        private String drugName;
        private String specification;
        private String dosage;
        private String usage;
        private String frequency;
        private String duration;
        private BigDecimal quantity;
        private BigDecimal unitPrice;
        private BigDecimal totalAmount;
        private String remark;
        private LocalDateTime createTime;

        public static PrescriptionItemVO fromEntity(PrescriptionItem item) {
            if (item == null) {
                return null;
            }
            PrescriptionItemVO vo = new PrescriptionItemVO();
            vo.setId(item.getId());
            vo.setPrescriptionId(item.getPrescriptionId());
            vo.setDrugCode(item.getDrugCode());
            vo.setDrugName(item.getDrugName());
            vo.setSpecification(item.getSpecification());
            vo.setDosage(item.getDosage());
            vo.setUsage(item.getUsage());
            vo.setFrequency(item.getFrequency());
            vo.setDuration(item.getDuration());
            vo.setQuantity(item.getQuantity());
            vo.setUnitPrice(item.getUnitPrice());
            vo.setTotalAmount(item.getTotalAmount());
            vo.setRemark(item.getRemark());
            vo.setCreateTime(item.getCreateTime());
            return vo;
        }
    }
}