package com.medical.schedule;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * 联调专用：手动触发就诊提醒扫描（等同 cron 任务，无需等待 15 分钟）。
 * 运行：mvn -Dtest=AppointmentReminderLiveTriggerTest test
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@TestPropertySource(properties = {
        "medical.messaging.enabled=true"
})
class AppointmentReminderLiveTriggerTest {

    @Autowired
    private AppointmentReminderScheduler appointmentReminderScheduler;

    @Test
    void triggerReminderScanNow() {
        appointmentReminderScheduler.sendUpcomingReminders();
    }
}
