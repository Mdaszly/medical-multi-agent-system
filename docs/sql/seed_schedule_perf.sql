-- ============================================================
-- 排班压测种子数据：内科 2026-05-29
-- 库：clinical_decision (PostgreSQL)
-- 用途：本地压测 / 演示（Apifox、JMeter）
-- 规模：约 25 名内科医生 + 37~50 条当日排班（非百万级）
-- ============================================================
--
-- 执行前：
--   1. 备份数据库
--   2. 核对表结构：\d schedule  与  \d doctor
--   3. 执行后删除 Redis：schedule:dept:内科:date:2026-05-29
--
-- 压测参数：
--   GET /api/schedule/list/department?department=内科&scheduleDate=2026-05-29
-- ============================================================

BEGIN;

-- ---------- 0. 若 schedule 缺列，按需取消注释（执行前先核对 information_schema）----------
-- ALTER TABLE schedule ADD COLUMN IF NOT EXISTS doctor_name VARCHAR(50);
-- ALTER TABLE schedule ADD COLUMN IF NOT EXISTS department VARCHAR(50);
-- ALTER TABLE schedule ADD COLUMN IF NOT EXISTS shift_name VARCHAR(20);
-- ALTER TABLE schedule ADD COLUMN IF NOT EXISTS time_range VARCHAR(30);
-- ALTER TABLE schedule ADD COLUMN IF NOT EXISTS time_start VARCHAR(10);
-- ALTER TABLE schedule ADD COLUMN IF NOT EXISTS time_end VARCHAR(10);
-- ALTER TABLE schedule ADD COLUMN IF NOT EXISTS max_appointments INT DEFAULT 40;
-- ALTER TABLE schedule ADD COLUMN IF NOT EXISTS current_appointments INT DEFAULT 0;
-- ALTER TABLE schedule ADD COLUMN IF NOT EXISTS on_duty_status SMALLINT DEFAULT 0;
-- ALTER TABLE schedule ADD COLUMN IF NOT EXISTS description VARCHAR(255);
-- ALTER TABLE schedule ADD COLUMN IF NOT EXISTS is_delete SMALLINT DEFAULT 0;
-- CREATE INDEX IF NOT EXISTS idx_schedule_dept_date ON schedule(department, schedule_date);

-- 若表仅有 start_time/end_time 而无 time_start/time_end，请将下方 INSERT 列名改为 start_time/end_time

-- ---------- 1. 修正已有排班（doctor_id=2，李四）----------
UPDATE schedule s
SET doctor_name        = COALESCE(s.doctor_name, d.doctor_name, '李四医生'),
    department         = COALESCE(s.department, d.department, '内科'),
    shift_name         = COALESCE(s.shift_name, '早班'),
    time_range         = COALESCE(s.time_range, '08:00-14:00'),
    time_start         = COALESCE(s.time_start, '08:00'),
    time_end           = COALESCE(s.time_end, '14:00'),
    max_appointments   = COALESCE(s.max_appointments, 40),
    current_appointments = COALESCE(s.current_appointments, 0),
    status             = COALESCE(s.status, 1),
    on_duty_status     = COALESCE(s.on_duty_status, 0),
    is_delete          = COALESCE(s.is_delete, 0),
    update_time        = CURRENT_TIMESTAMP
FROM doctor d
WHERE s.doctor_id = d.id
  AND s.schedule_date = DATE '2026-05-29'
  AND s.shift_type = 'morning';

UPDATE doctor
SET department  = '内科',
    doctor_name = COALESCE(doctor_name, '李四医生'),
    work_status = COALESCE(work_status, 1),
    is_delete   = COALESCE(is_delete, 0)
WHERE id = 2;

-- ---------- 2. 新增 24 名内科医生（user_id 占位 9001~9024，仅压测展示）----------
INSERT INTO doctor (
    user_id, doctor_name, department, title, specialty,
    work_status, on_duty_status, consultation_fee, is_delete,
    create_time, update_time
)
SELECT
    9000 + gs AS user_id,
    '内科医生' || lpad(gs::text, 2, '0') AS doctor_name,
    '内科' AS department,
    CASE (gs % 3)
        WHEN 0 THEN '主任医师'
        WHEN 1 THEN '副主任医师'
        ELSE '主治医师'
    END AS title,
    CASE (gs % 4)
        WHEN 0 THEN '心血管'
        WHEN 1 THEN '呼吸'
        WHEN 2 THEN '消化'
        ELSE '内分泌'
    END AS specialty,
    1 AS work_status,
    0 AS on_duty_status,
    (50.00 + (gs % 5) * 10)::numeric(10, 2) AS consultation_fee,
    0 AS is_delete,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM generate_series(1, 24) AS gs
WHERE NOT EXISTS (
    SELECT 1 FROM doctor d WHERE d.user_id = 9000 + gs
);

-- ---------- 3. 2026-05-29：每位内科医生至少 1 个早班 ----------
INSERT INTO schedule (
    doctor_id, doctor_name, department, schedule_date,
    shift_type, shift_name, time_range, time_start, time_end,
    max_appointments, current_appointments, status, on_duty_status,
    description, is_delete, create_time, update_time
)
SELECT
    d.id,
    d.doctor_name,
    d.department,
    DATE '2026-05-29',
    'morning',
    '早班',
    '08:00-14:00',
    '08:00',
    '14:00',
    40,
    0,
    1,
    0,
    '压测种子-早班',
    0,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM doctor d
WHERE d.department = '内科'
  AND COALESCE(d.is_delete, 0) = 0
  AND NOT EXISTS (
      SELECT 1 FROM schedule s
      WHERE s.doctor_id = d.id
        AND s.schedule_date = DATE '2026-05-29'
        AND s.shift_type = 'morning'
        AND COALESCE(s.is_delete, 0) = 0
  );

-- ---------- 4. 其中 12 人再加下午班 ----------
INSERT INTO schedule (
    doctor_id, doctor_name, department, schedule_date,
    shift_type, shift_name, time_range, time_start, time_end,
    max_appointments, current_appointments, status, on_duty_status,
    description, is_delete, create_time, update_time
)
SELECT
    d.id,
    d.doctor_name,
    d.department,
    DATE '2026-05-29',
    'afternoon',
    '中班',
    '14:00-20:00',
    '14:00',
    '20:00',
    30,
    0,
    1,
    0,
    '压测种子-中班',
    0,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM (
    SELECT id, doctor_name, department
    FROM doctor
    WHERE department = '内科' AND COALESCE(is_delete, 0) = 0
    ORDER BY id
    LIMIT 12
) d
WHERE NOT EXISTS (
    SELECT 1 FROM schedule s
    WHERE s.doctor_id = d.id
      AND s.schedule_date = DATE '2026-05-29'
      AND s.shift_type = 'afternoon'
      AND COALESCE(s.is_delete, 0) = 0
);

-- ---------- 5. 可选：其它日期少量历史 ----------
INSERT INTO schedule (
    doctor_id, doctor_name, department, schedule_date,
    shift_type, shift_name, time_range, time_start, time_end,
    max_appointments, current_appointments, status, on_duty_status,
    description, is_delete, create_time, update_time
)
SELECT
    d.id,
    d.doctor_name,
    d.department,
    DATE '2026-05-26',
    'morning',
    '早班',
    '08:00-14:00',
    '08:00',
    '14:00',
    40,
    0,
    1,
    0,
    '压测种子-历史',
    0,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM doctor d
WHERE d.department = '内科'
  AND COALESCE(d.is_delete, 0) = 0
  AND d.id % 2 = 0
  AND NOT EXISTS (
      SELECT 1 FROM schedule s
      WHERE s.doctor_id = d.id
        AND s.schedule_date = DATE '2026-05-26'
        AND s.shift_type = 'morning'
        AND COALESCE(s.is_delete, 0) = 0
  );

COMMIT;

-- ---------- 6. 校验 ----------
SELECT COUNT(*) AS doctor_neike_cnt
FROM doctor
WHERE department = '内科' AND COALESCE(is_delete, 0) = 0;

SELECT COUNT(*) AS schedule_20260529_cnt
FROM schedule
WHERE department = '内科'
  AND schedule_date = DATE '2026-05-29'
  AND COALESCE(is_delete, 0) = 0
  AND status = 1;

SELECT shift_type, COUNT(*) AS cnt
FROM schedule
WHERE department = '内科'
  AND schedule_date = DATE '2026-05-29'
  AND COALESCE(is_delete, 0) = 0
GROUP BY shift_type
ORDER BY shift_type;

-- ---------- 7. 后续（手动）----------
-- Redis: DEL schedule:dept:内科:date:2026-05-29
-- 号源（若需预约压测）: 执行 docs/sql/generate_slots.sql
