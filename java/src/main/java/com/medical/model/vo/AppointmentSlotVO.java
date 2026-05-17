package com.medical.model.vo;

import com.medical.model.entity.AppointmentSlot;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AppointmentSlotVO {

    private Long id;

    private Long scheduleId;

    private Long doctorId;

    private String doctorName;

    private String timeSlot;

    private String timeStart;

    private String timeEnd;

    private Integer maxSlots;

    private Integer availableSlots;

    private Integer lockedSlots;

    private String status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    public static AppointmentSlotVO fromEntity(AppointmentSlot slot) {
        if (slot == null) {
            return null;
        }
        AppointmentSlotVO vo = new AppointmentSlotVO();
        vo.setId(slot.getId());
        vo.setScheduleId(slot.getScheduleId());
        vo.setDoctorId(slot.getDoctorId());
        vo.setDoctorName(slot.getDoctorName());
        vo.setTimeSlot(slot.getTimeSlot());
        vo.setTimeStart(slot.getTimeStart());
        vo.setTimeEnd(slot.getTimeEnd());
        vo.setMaxSlots(slot.getMaxSlots());
        vo.setAvailableSlots(slot.getAvailableSlots());
        vo.setLockedSlots(slot.getLockedSlots());
        vo.setStatus(slot.getStatus());
        vo.setCreateTime(slot.getCreateTime());
        vo.setUpdateTime(slot.getUpdateTime());
        return vo;
    }
}
