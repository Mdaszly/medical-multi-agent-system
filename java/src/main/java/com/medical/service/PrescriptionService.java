package com.medical.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.medical.model.dto.prescription.PrescriptionAddRequest;
import com.medical.model.dto.prescription.PrescriptionQueryRequest;
import com.medical.model.dto.prescription.PrescriptionStatusUpdateRequest;
import com.medical.model.dto.prescription.PrescriptionUpdateRequest;
import com.medical.model.vo.PrescriptionVO;

import java.util.List;

public interface PrescriptionService {

    PrescriptionVO createPrescription(PrescriptionAddRequest request);

    PrescriptionVO getPrescriptionById(Long id);

    PrescriptionVO getPrescriptionByNo(String prescriptionNo);

    IPage<PrescriptionVO> listPrescriptionPage(long current, long pageSize, PrescriptionQueryRequest request);

    List<PrescriptionVO> listPrescriptionByUser(Long userId);

    List<PrescriptionVO> listPrescriptionByDoctor(Long doctorId);

    List<PrescriptionVO> listPrescriptionByAppointment(Long appointmentId);

    /**
     * 查询待发药处方列表（药师专用）
     *
     * @return 待发药处方列表
     */
    List<PrescriptionVO> listPendingDispensePrescriptions();

    void updatePrescriptionStatus(PrescriptionStatusUpdateRequest request);

    void dispensePrescription(Long prescriptionId);

    void cancelPrescription(Long prescriptionId);

    /**
     * 修改处方
     *
     * @param request 修改请求
     * @return 修改后的处方VO
     */
    PrescriptionVO updatePrescription(PrescriptionUpdateRequest request);
}
