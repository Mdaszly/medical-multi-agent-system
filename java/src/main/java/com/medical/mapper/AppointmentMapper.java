package com.medical.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.medical.model.entity.Appointment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface AppointmentMapper extends BaseMapper<Appointment> {

    List<Appointment> selectByUserIdAndDateRange(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    List<Appointment> selectByDoctorIdAndDateRange(
            @Param("doctorId") Long doctorId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    int countByUserIdAndScheduleIdAndTimeSlot(
            @Param("userId") Long userId,
            @Param("scheduleId") Long scheduleId,
            @Param("timeSlot") String timeSlot);

    List<Appointment> selectByScheduleId(
            @Param("scheduleId") Long scheduleId);
}
