-- ==============================================
-- 医疗中心平台 - 极简版数据库建表语句
-- 数据库类型：PostgreSQL 13+
-- 版本：V2.0（极简核心版）
-- 说明：仅保留核心必要功能，角色通过枚举类管理
-- ==============================================

-- ------------------------------
-- 1. 用户表（患者+管理员共用）
-- user_role枚举值: user-普通患者, admin-管理员, doctor-医生
-- ------------------------------
DROP TABLE IF EXISTS "user";
CREATE TABLE IF NOT EXISTS "user" (
    id                  BIGSERIAL PRIMARY KEY,
    user_account        VARCHAR(50) NOT NULL,
    user_password       VARCHAR(100) NOT NULL,
    salt                VARCHAR(20) NOT NULL,
    user_name           VARCHAR(50) NOT NULL,
    user_role           VARCHAR(20) NOT NULL DEFAULT 'user',
    phone               VARCHAR(20),
    email               VARCHAR(100),
    gender              SMALLINT DEFAULT 0,
    birth_date          DATE,
    user_status         SMALLINT DEFAULT 1,
    -- 医生专属字段
    department          VARCHAR(50),
    title               VARCHAR(50),
    license_no          VARCHAR(50),
    consultation_fee    NUMERIC(10,2) DEFAULT 0.00,
    description         TEXT,
    -- 通用字段
    create_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_delete           SMALLINT DEFAULT 0
);

CREATE UNIQUE INDEX uk_user_account ON "user"(user_account);
CREATE UNIQUE INDEX uk_user_phone ON "user"(phone);
CREATE UNIQUE INDEX uk_user_email ON "user"(email);
CREATE INDEX idx_user_role ON "user"(user_role);
CREATE INDEX idx_user_status ON "user"(user_status);

-- ------------------------------
-- 2. 排班表
-- ------------------------------
DROP TABLE IF EXISTS schedule;
CREATE TABLE IF NOT EXISTS schedule (
    id                  BIGSERIAL PRIMARY KEY,
    doctor_id           BIGINT NOT NULL,
    doctor_name         VARCHAR(50) NOT NULL,
    department          VARCHAR(50) NOT NULL,
    schedule_date       DATE NOT NULL,
    shift_type          VARCHAR(20) NOT NULL,
    shift_name          VARCHAR(50),
    time_range          VARCHAR(50),
    time_start          VARCHAR(10),
    time_end            VARCHAR(10),
    max_appointments    INTEGER DEFAULT 20,
    current_appointments INTEGER DEFAULT 0,
    status              SMALLINT DEFAULT 1,
    on_duty_status      SMALLINT DEFAULT 0,
    description         TEXT,
    create_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_delete           SMALLINT DEFAULT 0
);

CREATE UNIQUE INDEX uk_schedule_doctor_date_shift ON schedule(doctor_id, schedule_date, shift_type) WHERE is_delete = 0;
CREATE INDEX idx_schedule_doctor_id ON schedule(doctor_id);
CREATE INDEX idx_schedule_date ON schedule(schedule_date);
CREATE INDEX idx_schedule_department ON schedule(department);
CREATE INDEX idx_schedule_status ON schedule(status);

-- 创建 doctor 表
CREATE TABLE IF NOT EXISTS doctor (
                                      id                  BIGSERIAL PRIMARY KEY,
                                      doctor_no           VARCHAR(50) NOT NULL,
                                      doctor_name         VARCHAR(50) NOT NULL,
                                      department          VARCHAR(50) NOT NULL,
                                      title               VARCHAR(50) NOT NULL,
                                      license_no          VARCHAR(50) NOT NULL,
                                      consultation_fee    NUMERIC(10,2) DEFAULT 0.00,
                                      description         TEXT,
                                      phone               VARCHAR(20),
                                      email               VARCHAR(100),
                                      work_status         SMALLINT DEFAULT 0,
                                      gender              SMALLINT DEFAULT 0,
                                      user_account        VARCHAR(50) NOT NULL,
                                      user_password       VARCHAR(100) NOT NULL,
                                      salt                VARCHAR(20) NOT NULL,
                                      create_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                      update_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                      is_delete           SMALLINT DEFAULT 0
);

CREATE UNIQUE INDEX uk_doctor_no ON doctor(doctor_no);
CREATE UNIQUE INDEX uk_doctor_license_no ON doctor(license_no);
CREATE UNIQUE INDEX uk_doctor_user_account ON doctor(user_account);
CREATE INDEX idx_doctor_department ON doctor(department);
CREATE INDEX idx_doctor_work_status ON doctor(work_status);


-- ------------------------------
-- 3. 挂号预约表
-- ------------------------------
DROP TABLE IF EXISTS appointment;
CREATE TABLE IF NOT EXISTS appointment (
    id                  BIGSERIAL PRIMARY KEY,
    appointment_no      VARCHAR(50) NOT NULL,
    user_id             BIGINT NOT NULL,
    user_name           VARCHAR(50) NOT NULL,
    doctor_id           BIGINT NOT NULL,
    doctor_name         VARCHAR(50) NOT NULL,
    department          VARCHAR(50) NOT NULL,
    schedule_date       DATE NOT NULL,
    period              VARCHAR(10),
    time_slot           VARCHAR(20) NOT NULL,
    status              SMALLINT DEFAULT 0,
    consultation_fee    NUMERIC(10,2) DEFAULT 0.00,
    paid_amount         NUMERIC(10,2) DEFAULT 0.00,
    payment_status      SMALLINT DEFAULT 0,
    create_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX uk_appointment_no ON appointment(appointment_no);
CREATE INDEX idx_appointment_user_id ON appointment(user_id);
CREATE INDEX idx_appointment_doctor_id ON appointment(doctor_id);
CREATE INDEX idx_appointment_status ON appointment(status);

-- ------------------------------
-- 4. 处方表
-- ------------------------------
DROP TABLE IF EXISTS prescription;
CREATE TABLE IF NOT EXISTS prescription (
    id                  BIGSERIAL PRIMARY KEY,
    prescription_no     VARCHAR(50) NOT NULL,
    appointment_id      BIGINT NOT NULL,
    user_id             BIGINT NOT NULL,
    user_name           VARCHAR(50) NOT NULL,
    doctor_id           BIGINT NOT NULL,
    doctor_name         VARCHAR(50) NOT NULL,
    diagnosis           TEXT,
    drugs               TEXT NOT NULL,
    total_amount        NUMERIC(10,2) DEFAULT 0.00,
    status              SMALLINT DEFAULT 0,
    create_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX uk_prescription_no ON prescription(prescription_no);
CREATE INDEX idx_prescription_appointment_id ON prescription(appointment_id);

-- ------------------------------
-- 5. 支付记录表
-- ------------------------------
DROP TABLE IF EXISTS payment;
CREATE TABLE IF NOT EXISTS payment (
    id                  BIGSERIAL PRIMARY KEY,
    payment_no          VARCHAR(50) NOT NULL,
    appointment_id      BIGINT,
    prescription_id     BIGINT,
    user_id             BIGINT NOT NULL,
    user_name           VARCHAR(50) NOT NULL,
    amount              NUMERIC(10,2) NOT NULL,
    payment_type        VARCHAR(20),
    status              SMALLINT DEFAULT 0,
    transaction_id      VARCHAR(100),
    create_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX uk_payment_no ON payment(payment_no);
CREATE INDEX idx_payment_user_id ON payment(user_id);
CREATE INDEX idx_payment_status ON payment(status);

-- ------------------------------
-- 6. 健康档案表
-- ------------------------------
DROP TABLE IF EXISTS health_profile;
CREATE TABLE IF NOT EXISTS health_profile (
    id                  BIGSERIAL PRIMARY KEY,
    user_id             BIGINT NOT NULL,
    user_name           VARCHAR(50) NOT NULL,
    chronic_diseases    TEXT,
    allergy_history     TEXT,
    medication_history  TEXT,
    family_history      TEXT,
    update_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_health_user_id ON health_profile(user_id);

-- ------------------------------
-- 7. 登录记录表（极简版）
-- ------------------------------
DROP TABLE IF EXISTS login_record;
CREATE TABLE IF NOT EXISTS login_record (
    id                  BIGSERIAL PRIMARY KEY,
    user_id             BIGINT NOT NULL,
    user_account        VARCHAR(50) NOT NULL,
    login_time          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_login_user_id ON login_record(user_id);

-- ------------------------------
-- 初始化数据
-- ------------------------------

-- 默认超级管理员账号
-- 账号: admin
-- 密码: Admin@123456
INSERT INTO "user" (user_account, user_password, salt, user_name, user_role, user_status, create_time, update_time, is_delete) VALUES
('admin', 'e10adc3949ba59abbe56e057f20f883e', 'adminSalt2026', '系统管理员', 'admin', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)
ON CONFLICT (user_account) DO NOTHING;

-- ------------------------------
-- 字段说明
-- ------------------------------
-- user表：
--   user_role: user-普通患者, admin-管理员, doctor-医生
--   user_status: 0-禁用, 1-正常
--   gender: 0-未知, 1-男, 2-女
--   is_delete: 0-未删除, 1-已删除

-- appointment表：
--   status: 0-待就诊, 1-已就诊, 2-已取消, 3-已过期
--   payment_status: 0-未支付, 1-已支付

-- prescription表：
--   status: 0-待审核, 1-已审核, 2-已发药, 3-已完成

-- payment表：
--   status: 0-待支付, 1-已支付, 2-已退款

-- schedule表：
--   status: 0-休息, 1-可预约, 2-已满