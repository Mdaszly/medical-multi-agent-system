package com.medical.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.medical.model.dto.doctor.DoctorQueryRequest;
import com.medical.model.dto.doctor.DoctorUpdateRequest;
import com.medical.model.entity.Doctor;
import com.medical.model.vo.DoctorVO;

import java.util.List;

public interface DoctorService {

    DoctorVO getDoctorById(Long id);

    Doctor getDoctorEntityById(Long id);

    DoctorVO getCurrentDoctor();

    IPage<DoctorVO> listDoctorPage(long current, long pageSize, DoctorQueryRequest request);

    List<DoctorVO> listDoctorByDepartment(String department);

    DoctorVO updateDoctor(Long id, DoctorUpdateRequest request);

    DoctorVO updateProfile(Long id, DoctorUpdateRequest request);

    void deleteDoctor(Long id);

    void disableDoctor(Long id);

    void enableDoctor(Long id);

    DoctorVO addDoctor(DoctorUpdateRequest request);
}