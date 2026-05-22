-- ============================================================
-- 为已有的医生排班生成号源
-- ============================================================

-- 1. 先查看当前排班情况
SELECT id, doctor_name, shift_type, schedule_date FROM schedule WHERE is_delete = 0;

-- ============================================================

-- 2. 为"早班(morning)"排班生成时段
INSERT INTO appointment_slot (schedule_id, doctor_id, doctor_name, time_slot, time_start, time_end, max_slots, available_slots, locked_slots, status, version, create_time, update_time)
SELECT 
    s.id AS schedule_id,
    s.doctor_id,
    s.doctor_name,
    times.time_slot,
    times.time_start,
    times.time_end,
    5, 5, 0, 'AVAILABLE', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM schedule s
CROSS JOIN (
    VALUES 
        ('08:00-08:30', '08:00', '08:30'),
        ('08:30-09:00', '08:30', '09:00'),
        ('09:00-09:30', '09:00', '09:30'),
        ('09:30-10:00', '09:30', '10:00'),
        ('10:00-10:30', '10:00', '10:30'),
        ('10:30-11:00', '10:30', '11:00'),
        ('11:00-11:30', '11:00', '11:30'),
        ('11:30-12:00', '11:30', '12:00')
) AS times(time_slot, time_start, time_end)
WHERE s.shift_type = 'morning'
AND s.is_delete = 0
AND NOT EXISTS (
    SELECT 1 FROM appointment_slot sl 
    WHERE sl.schedule_id = s.id AND sl.time_slot = times.time_slot
);

-- ============================================================

-- 3. 为"中班(afternoon)"排班生成时段
INSERT INTO appointment_slot (schedule_id, doctor_id, doctor_name, time_slot, time_start, time_end, max_slots, available_slots, locked_slots, status, version, create_time, update_time)
SELECT 
    s.id AS schedule_id,
    s.doctor_id,
    s.doctor_name,
    times.time_slot,
    times.time_start,
    times.time_end,
    5, 5, 0, 'AVAILABLE', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM schedule s
CROSS JOIN (
    VALUES 
        ('14:00-14:30', '14:00', '14:30'),
        ('14:30-15:00', '14:30', '15:00'),
        ('15:00-15:30', '15:00', '15:30'),
        ('15:30-16:00', '15:30', '16:00'),
        ('16:00-16:30', '16:00', '16:30'),
        ('16:30-17:00', '16:30', '17:00'),
        ('17:00-17:30', '17:00', '17:30'),
        ('17:30-18:00', '17:30', '18:00')
) AS times(time_slot, time_start, time_end)
WHERE s.shift_type = 'afternoon'
AND s.is_delete = 0
AND NOT EXISTS (
    SELECT 1 FROM appointment_slot sl 
    WHERE sl.schedule_id = s.id AND sl.time_slot = times.time_slot
);

-- ============================================================

-- 4. 为"晚班(evening)"排班生成时段
INSERT INTO appointment_slot (schedule_id, doctor_id, doctor_name, time_slot, time_start, time_end, max_slots, available_slots, locked_slots, status, version, create_time, update_time)
SELECT 
    s.id AS schedule_id,
    s.doctor_id,
    s.doctor_name,
    times.time_slot,
    times.time_start,
    times.time_end,
    5, 5, 0, 'AVAILABLE', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM schedule s
CROSS JOIN (
    VALUES 
        ('20:00-20:30', '20:00', '20:30'),
        ('20:30-21:00', '20:30', '21:00'),
        ('21:00-21:30', '21:00', '21:30'),
        ('21:30-22:00', '21:30', '22:00'),
        ('22:00-22:30', '22:00', '22:30'),
        ('22:30-23:00', '22:30', '23:00'),
        ('23:00-23:30', '23:00', '23:30'),
        ('23:30-00:00', '23:30', '00:00')
) AS times(time_slot, time_start, time_end)
WHERE s.shift_type = 'evening'
AND s.is_delete = 0
AND NOT EXISTS (
    SELECT 1 FROM appointment_slot sl 
    WHERE sl.schedule_id = s.id AND sl.time_slot = times.time_slot
);

-- ============================================================

-- 5. 查看生成结果
SELECT 
    s.shift_type,
    s.doctor_name,
    s.schedule_date,
    COUNT(sl.id) AS slot_count
FROM schedule s
LEFT JOIN appointment_slot sl ON s.id = sl.schedule_id
WHERE s.is_delete = 0
GROUP BY s.shift_type, s.doctor_name, s.schedule_date
ORDER BY s.schedule_date;

-- ============================================================
