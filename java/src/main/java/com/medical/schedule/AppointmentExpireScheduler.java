package com.medical.schedule;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.medical.constant.AppointmentConstant;
import com.medical.mapper.AppointmentMapper;
import com.medical.messaging.appointment.AppointmentEventBridge;
import com.medical.model.entity.Appointment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 将就诊日已过的「待就诊」预约标记为已过期，并发布领域事件（号源回补由 MQ Consumer 处理）。
 *
 * <p>每天 01:00 运行。仅当 {@link com.medical.mapper.AppointmentMapper#updateStatusIf} 成功（仍为 PENDING）
 * 才 {@code publishExpired}，避免与签到并发或多实例重复发事件。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AppointmentExpireScheduler {

    private final AppointmentMapper appointmentMapper;
    private final AppointmentEventBridge appointmentEventBridge;

    @Scheduled(cron = "0 0 1 * * ?")
    @Transactional(rollbackFor = Exception.class)
    public void expirePastPendingAppointments() {
        LocalDate today = LocalDate.now();
        LambdaQueryWrapper<Appointment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Appointment::getStatus, AppointmentConstant.APPOINTMENT_STATUS_PENDING)
                .lt(Appointment::getScheduleDate, today);

        List<Appointment> toExpire = appointmentMapper.selectList(wrapper);
        if (toExpire.isEmpty()) {
            return;
        }

        int updated = 0;
        for (Appointment appointment : toExpire) {
            Integer previousStatus = appointment.getStatus();
            int rows = appointmentMapper.updateStatusIf(
                    appointment.getId(),
                    AppointmentConstant.APPOINTMENT_STATUS_PENDING,
                    AppointmentConstant.APPOINTMENT_STATUS_EXPIRED);
            // 已被签到/取消或另一实例处理过 → 不再发 expired（稳定 eventId 亦防双倍 slot-restore）
            if (rows != 1) {
                log.debug("Skip expire publish, status no longer pending: appointmentId={}", appointment.getId());
                continue;
            }
            appointment.setStatus(AppointmentConstant.APPOINTMENT_STATUS_EXPIRED);
            appointment.setUpdateTime(LocalDateTime.now());
            appointmentEventBridge.publishExpired(appointment, previousStatus);
            updated++;
        }
        if (updated > 0) {
            log.info("预约过期任务完成: 更新 {} 条待就诊记录为已过期", updated);
        }
    }
}
