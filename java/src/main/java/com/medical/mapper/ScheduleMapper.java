package com.medical.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.medical.model.entity.Schedule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface ScheduleMapper extends BaseMapper<Schedule> {

    List<Schedule> selectByDoctorIdAndDateRange(
            @Param("doctorId") Long doctorId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    List<Schedule> selectByDepartmentAndDate(
            @Param("department") String department,
            @Param("scheduleDate") LocalDate scheduleDate);

    List<Schedule> selectOnDutyDoctors(
            @Param("scheduleDate") LocalDate scheduleDate,
            @Param("shiftType") String shiftType);

    int countByDoctorIdAndDateAndShift(
            @Param("doctorId") Long doctorId,
            @Param("scheduleDate") LocalDate scheduleDate,
            @Param("shiftType") String shiftType);

    int updateCurrentAppointments(@Param("id") Long id, @Param("delta") Integer delta);
}
