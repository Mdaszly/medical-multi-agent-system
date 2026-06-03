package com.medical.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.medical.model.entity.Appointment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

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

    /**
     * 条件更新状态（CAS）。返回 0 表示当前 status 已不是 expectedStatus，调用方不应再发 MQ。
     * 用于过期任务与签到并发互斥，避免 blind {@code updateById} 覆盖。
     */
    @Update("UPDATE appointment SET status = #{newStatus}, update_time = NOW() "
            + "WHERE id = #{id} AND status = #{expectedStatus}")
    int updateStatusIf(@Param("id") Long id,
                       @Param("expectedStatus") Integer expectedStatus,
                       @Param("newStatus") Integer newStatus);

    /** 待就诊 → 已签到，原子写入 check_in_time / check_in_status */
    @Update("UPDATE appointment SET status = #{newStatus}, check_in_time = NOW(), "
            + "check_in_status = TRUE, update_time = NOW() "
            + "WHERE id = #{id} AND status = #{expectedStatus}")
    int checkInIfPending(@Param("id") Long id,
                         @Param("expectedStatus") Integer expectedStatus,
                         @Param("newStatus") Integer newStatus);
}
