package com.medical.model.vo;

import com.medical.model.entity.Doctor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class DoctorVO {

    private Long id;

    private String doctorNo;

    private String doctorName;

    private String department;

    private String title;

    private String specialty;

    private String phone;

    private String email;

    private String hospitalName;

    private BigDecimal consultationFee;

    private String description;

    private Integer workStatus;

    private LocalDateTime createTime;

    public static DoctorVO fromEntity(Doctor doctor) {
        if (doctor == null) {
            return null;
        }
        DoctorVO vo = new DoctorVO();
        vo.setId(doctor.getId());
        vo.setDoctorNo(doctor.getDoctorNo());
        vo.setDoctorName(doctor.getDoctorName());
        vo.setDepartment(doctor.getDepartment());
        vo.setTitle(doctor.getTitle());
        vo.setSpecialty(doctor.getSpecialty());
        vo.setPhone(doctor.getPhone());
        vo.setEmail(doctor.getEmail());
        vo.setHospitalName(doctor.getHospitalName());
        vo.setConsultationFee(doctor.getConsultationFee());
        vo.setDescription(doctor.getDescription());
        vo.setWorkStatus(doctor.getWorkStatus());
        vo.setCreateTime(doctor.getCreateTime());
        return vo;
    }
}