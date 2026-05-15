package com.medical.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.medical.model.entity.AppointmentSlot;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface AppointmentSlotMapper extends BaseMapper<AppointmentSlot> {

    List<AppointmentSlot> selectByScheduleId(
            @Param("scheduleId") Long scheduleId);

    @Update("UPDATE appointment_slot SET available_slots = available_slots - 1, version = version + 1 " +
            "WHERE id = #{id} AND available_slots > 0 AND version = #{version}")
    int decreaseAvailableSlotsWithLock(
            @Param("id") Long id,
            @Param("version") Integer version);

    @Update("UPDATE appointment_slot SET available_slots = available_slots + 1 " +
            "WHERE id = #{id}")
    int increaseAvailableSlots(
            @Param("id") Long id);
}
