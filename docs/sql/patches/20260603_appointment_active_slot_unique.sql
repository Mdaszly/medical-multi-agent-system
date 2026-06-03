-- 唯一约束仅限制「有效预约」（待就诊/已签到/诊疗中），允许取消/过期/已结算后同号源再次预约
DROP INDEX IF EXISTS uk_appointment_user_schedule_slot;

CREATE UNIQUE INDEX uk_appointment_user_schedule_slot
    ON appointment (user_id, schedule_id, time_slot)
    WHERE status IN (0, 1, 2);
