package com.medical.model.vo;

import com.medical.model.entity.Appointment;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class AppointmentVO {

    private Long id;

    private String appointmentNo;

    private Long userId;

    private String userName;

    private Long doctorId;

    private String doctorName;

    private String department;

    private Long scheduleId;

    private LocalDate scheduleDate;

    private String shiftType;

    private String timeSlot;

    private BigDecimal consultationFee;

    private Integer status;

    private String statusText;

    private LocalDateTime checkInTime;

    private Boolean checkInStatus;

    private LocalDateTime cancelTime;

    private String cancelReason;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    public static AppointmentVO fromEntity(Appointment appointment) {
        if (appointment == null) {
            return null;
        }
        AppointmentVO vo = new AppointmentVO();
        vo.setId(appointment.getId());
        vo.setAppointmentNo(appointment.getAppointmentNo());
        vo.setUserId(appointment.getUserId());
        vo.setUserName(appointment.getUserName());
        vo.setDoctorId(appointment.getDoctorId());
        vo.setDoctorName(appointment.getDoctorName());
        vo.setDepartment(appointment.getDepartment());
        vo.setScheduleId(appointment.getScheduleId());
        vo.setScheduleDate(appointment.getScheduleDate());
        vo.setShiftType(appointment.getShiftType());
        vo.setTimeSlot(appointment.getTimeSlot());
        vo.setConsultationFee(appointment.getConsultationFee());
        vo.setStatus(appointment.getStatus());
        vo.setStatusText(getStatusText(appointment.getStatus()));
        vo.setCheckInTime(appointment.getCheckInTime());
        vo.setCheckInStatus(appointment.getCheckInStatus());
        vo.setCancelTime(appointment.getCancelTime());
        vo.setCancelReason(appointment.getCancelReason());
        vo.setRemark(appointment.getRemark());
        vo.setCreateTime(appointment.getCreateTime());
        vo.setUpdateTime(appointment.getUpdateTime());
        return vo;
    }

    private static String getStatusText(Integer status) {
        if (status == null) {
            return "未知";
        }
        return switch (status) {
            case 0 -> "待就诊";
            case 1 -> "已签到";
            case 2 -> "诊疗中";
            case 3 -> "已完成";
            case 4 -> "已取消";
            case 5 -> "已过期";
            case 6 -> "已结算";
            default -> "未知";
        };
    }
}
