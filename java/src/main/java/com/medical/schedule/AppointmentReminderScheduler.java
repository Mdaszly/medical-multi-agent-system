package com.medical.schedule;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.medical.common.RedisCacheUtil;
import com.medical.constant.AppointmentConstant;
import com.medical.constant.RedisKeyConstant;
import com.medical.mapper.AppointmentMapper;
import com.medical.messaging.appointment.AppointmentEventBridge;
import com.medical.messaging.config.MessagingProperties;
import com.medical.model.entity.Appointment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * 扫描即将开始的待就诊预约，在就诊前 N 小时发送提醒领域事件。
 *
 * <p>每 15 分钟扫描 today/tomorrow 的 PENDING 预约。
 * 成功消费后由 {@link com.medical.messaging.appointment.consumer.AppointmentNotificationConsumer} 写
 * {@link com.medical.constant.RedisKeyConstant#MQ_REMINDER_SENT}；此处 {@code hasKey} 避免重复 publish。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AppointmentReminderScheduler {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private final AppointmentMapper appointmentMapper;
    private final AppointmentEventBridge appointmentEventBridge;
    private final MessagingProperties messagingProperties;
    private final RedisCacheUtil redisCacheUtil;

    @Scheduled(cron = "0 */15 * * * ?")
    @Transactional(rollbackFor = Exception.class)
    public void sendUpcomingReminders() {
        if (!messagingProperties.isEnabled()) {
            return;
        }

        int hoursBefore = messagingProperties.getAppointment().getReminderHoursBefore();
        if (hoursBefore <= 0) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();
        LocalDate tomorrow = today.plusDays(1);

        LambdaQueryWrapper<Appointment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Appointment::getStatus, AppointmentConstant.APPOINTMENT_STATUS_PENDING)
                .in(Appointment::getScheduleDate, today, tomorrow);

        List<Appointment> candidates = appointmentMapper.selectList(wrapper);
        int sent = 0;
        for (Appointment appointment : candidates) {
            if (shouldSendReminder(appointment, now, hoursBefore)
                    && !isReminderAlreadySent(appointment.getId())) {
                appointmentEventBridge.publishReminder(appointment);
                sent++;
            }
        }
        if (sent > 0) {
            log.info("就诊提醒任务完成: 发送 {} 条提醒事件", sent);
        }
    }

    static boolean shouldSendReminder(Appointment appointment, LocalDateTime now, int hoursBefore) {
        LocalDateTime start = parseAppointmentStart(appointment);
        if (start == null || !start.isAfter(now)) {
            return false;
        }
        LocalDateTime remindAt = start.minusHours(hoursBefore);
        return !now.isBefore(remindAt);
    }

    static LocalDateTime parseAppointmentStart(Appointment appointment) {
        if (appointment == null || appointment.getScheduleDate() == null
                || !StringUtils.hasText(appointment.getTimeSlot())) {
            return null;
        }
        String slot = appointment.getTimeSlot().trim();
        String startText = slot.contains("-") ? slot.substring(0, slot.indexOf('-')).trim() : slot;
        try {
            LocalTime startTime = LocalTime.parse(startText, TIME_FORMAT);
            return LocalDateTime.of(appointment.getScheduleDate(), startTime);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private boolean isReminderAlreadySent(Long appointmentId) {
        if (appointmentId == null) {
            return false;
        }
        String key = String.format(RedisKeyConstant.MQ_REMINDER_SENT, appointmentId);
        return Boolean.TRUE.equals(redisCacheUtil.hasKey(key));
    }
}
