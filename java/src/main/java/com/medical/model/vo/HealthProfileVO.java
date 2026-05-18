package com.medical.model.vo;

import com.medical.model.entity.HealthProfile;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class HealthProfileVO {

    private Long id;

    private Long userId;

    private String userName;

    private String chronicDiseases;

    private String allergyHistory;

    private String medicationHistory;

    private String familyHistory;

    private String surgicalHistory;

    private String vaccinationHistory;

    private String physicalExam;

    private BigDecimal height;

    private BigDecimal weight;

    private String bloodType;

    private String bloodPressure;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    public static HealthProfileVO fromEntity(HealthProfile entity) {
        if (entity == null) {
            return null;
        }
        HealthProfileVO vo = new HealthProfileVO();
        vo.setId(entity.getId());
        vo.setUserId(entity.getUserId());
        vo.setUserName(entity.getUserName());
        vo.setChronicDiseases(entity.getChronicDiseases());
        vo.setAllergyHistory(entity.getAllergyHistory());
        vo.setMedicationHistory(entity.getMedicationHistory());
        vo.setFamilyHistory(entity.getFamilyHistory());
        vo.setSurgicalHistory(entity.getSurgicalHistory());
        vo.setVaccinationHistory(entity.getVaccinationHistory());
        vo.setPhysicalExam(entity.getPhysicalExam());
        vo.setHeight(entity.getHeight());
        vo.setWeight(entity.getWeight());
        vo.setBloodType(entity.getBloodType());
        vo.setBloodPressure(entity.getBloodPressure());
        vo.setRemark(entity.getRemark());
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateTime(entity.getUpdateTime());
        return vo;
    }
}