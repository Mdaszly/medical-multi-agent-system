package com.medical.model.vo;

import com.medical.constant.ScheduleConstant;
import com.medical.model.entity.Schedule;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class ScheduleVO {

    private Long id;

    private Long doctorId;

    private String doctorName;

    private String department;

    private LocalDate scheduleDate;

    private String shiftType;

    private String shiftName;

    private String timeRange;

    private String timeStart;

    private String timeEnd;

    private Integer maxAppointments;

    private Integer currentAppointments;

    private Integer status;

    private Integer onDutyStatus;

    private String description;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    public static ScheduleVO fromEntity(Schedule schedule) {
        if (schedule == null) {
            return null;
        }
        ScheduleVO vo = new ScheduleVO();
        vo.setId(schedule.getId());
        vo.setDoctorId(schedule.getDoctorId());
        vo.setDoctorName(schedule.getDoctorName());
        vo.setDepartment(schedule.getDepartment());
        vo.setScheduleDate(schedule.getScheduleDate());
        vo.setShiftType(schedule.getShiftType());
        vo.setShiftName(schedule.getShiftName());
        vo.setTimeRange(schedule.getTimeRange());
        vo.setTimeStart(schedule.getTimeStart());
        vo.setTimeEnd(schedule.getTimeEnd());
        vo.setMaxAppointments(schedule.getMaxAppointments());
        vo.setCurrentAppointments(schedule.getCurrentAppointments());
        vo.setStatus(schedule.getStatus());
        vo.setOnDutyStatus(schedule.getOnDutyStatus());
        vo.setDescription(schedule.getDescription());
        vo.setCreateTime(schedule.getCreateTime());
        vo.setUpdateTime(schedule.getUpdateTime());
        return vo;
    }
}
