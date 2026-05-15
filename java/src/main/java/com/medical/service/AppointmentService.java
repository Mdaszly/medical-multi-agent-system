package com.medical.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.medical.model.dto.appointment.AppointmentAddRequest;
import com.medical.model.dto.appointment.AppointmentCancelRequest;
import com.medical.model.dto.appointment.AppointmentQueryRequest;
import com.medical.model.entity.Appointment;
import com.medical.model.vo.AppointmentSlotVO;
import com.medical.model.vo.AppointmentVO;

import java.time.LocalDate;
import java.util.List;

public interface AppointmentService {

    AppointmentVO createAppointment(AppointmentAddRequest request);

    void cancelAppointment(AppointmentCancelRequest request);

    AppointmentVO getAppointmentById(Long id);

    Appointment getAppointmentEntityById(Long id);

    IPage<AppointmentVO> listAppointmentPage(long current, long pageSize, AppointmentQueryRequest request);

    List<AppointmentVO> listAppointmentByUser(Long userId, LocalDate startDate, LocalDate endDate);

    List<AppointmentVO> listAppointmentByDoctor(Long doctorId, LocalDate startDate, LocalDate endDate);

    List<AppointmentSlotVO> getAppointmentSlotsBySchedule(Long scheduleId);

    void checkInAppointment(Long appointmentId);

    void updateAppointmentStatus(Long appointmentId, Integer status);
}
