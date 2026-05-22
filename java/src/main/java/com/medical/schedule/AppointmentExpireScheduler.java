package com.medical.schedule;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.medical.constant.AppointmentConstant;
import com.medical.mapper.AppointmentMapper;
import com.medical.model.entity.Appointment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 将就诊日已过的「待就诊」预约标记为已过期。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AppointmentExpireScheduler {

    private final AppointmentMapper appointmentMapper;

    @Scheduled(cron = "0 0 1 * * ?")
    public void expirePastPendingAppointments() {
        LocalDate today = LocalDate.now();
        LambdaQueryWrapper<Appointment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Appointment::getStatus, AppointmentConstant.APPOINTMENT_STATUS_PENDING)
                .lt(Appointment::getScheduleDate, today);

        long count = appointmentMapper.selectCount(wrapper);
        if (count == 0) {
            return;
        }

        LambdaUpdateWrapper<Appointment> update = new LambdaUpdateWrapper<>();
        update.eq(Appointment::getStatus, AppointmentConstant.APPOINTMENT_STATUS_PENDING)
                .lt(Appointment::getScheduleDate, today)
                .set(Appointment::getStatus, AppointmentConstant.APPOINTMENT_STATUS_EXPIRED)
                .set(Appointment::getUpdateTime, LocalDateTime.now());

        int updated = appointmentMapper.update(null, update);
        log.info("预约过期任务完成: 更新 {} 条待就诊记录为已过期", updated);
    }
}
