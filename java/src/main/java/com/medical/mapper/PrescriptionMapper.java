package com.medical.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.medical.model.entity.Prescription;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PrescriptionMapper extends BaseMapper<Prescription> {

    List<Prescription> selectByUserId(@Param("userId") Long userId);

    List<Prescription> selectByDoctorId(@Param("doctorId") Long doctorId);

    List<Prescription> selectByAppointmentId(@Param("appointmentId") Long appointmentId);

    Prescription selectByPrescriptionNo(@Param("prescriptionNo") String prescriptionNo);
}