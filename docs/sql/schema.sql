-- =============================================================================
-- 医院医疗平台 PostgreSQL 初始化脚本（权威版本）
-- =============================================================================
-- 数据库: clinical_decision
-- 对齐来源: 生产库 pg_dump schema-only（2026-05-22）
-- 表数量: 20（已移除 audit_log / audit_logs / clinical_sessions）
--
-- 症状-ICD 数据流:
--   Neo4j（权威） --POST /api/knowledge-graph/sync-to-rdb--> symptom / icd10_code / symptom_icd_rel
--   症状数据勿手写 SQL 灌库，统一走 sync-to-rdb
--
-- 用法（空库）:
--   psql -U postgres -d clinical_decision -f docs/sql/schema.sql
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 0. 清理（按外键依赖倒序）
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS appointment_event_audit;
DROP TABLE IF EXISTS user_notification;
DROP TABLE IF EXISTS symptom_icd_rel;
DROP TABLE IF EXISTS chat_message;
DROP TABLE IF EXISTS chat_session;
DROP TABLE IF EXISTS prescription_item;
DROP TABLE IF EXISTS prescription;
DROP TABLE IF EXISTS payment;
DROP TABLE IF EXISTS fee_item;
DROP TABLE IF EXISTS bill;
DROP TABLE IF EXISTS appointment;
DROP TABLE IF EXISTS appointment_slot;
DROP TABLE IF EXISTS health_profile;
DROP TABLE IF EXISTS drug_price;
DROP TABLE IF EXISTS drug_info;
DROP TABLE IF EXISTS schedule;
DROP TABLE IF EXISTS doctor;
DROP TABLE IF EXISTS symptom;
DROP TABLE IF EXISTS icd10_code;
DROP TABLE IF EXISTS "user";

-- -----------------------------------------------------------------------------
-- 1. 用户表
-- -----------------------------------------------------------------------------
CREATE TABLE "user" (
    id                  BIGSERIAL PRIMARY KEY,
    user_account        VARCHAR(50) NOT NULL,
    user_password       VARCHAR(100) NOT NULL,
    salt                VARCHAR(32),
    user_name           VARCHAR(50) NOT NULL,
    user_role           VARCHAR(20) NOT NULL DEFAULT 'user',
    phone               VARCHAR(20),
    email               VARCHAR(100),
    gender              SMALLINT DEFAULT 0,
    birth_date          DATE,
    user_status         SMALLINT DEFAULT 1,
    create_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_delete           SMALLINT DEFAULT 0,
    admin_account_type  VARCHAR(20) DEFAULT NULL
);

COMMENT ON COLUMN "user".admin_account_type IS '管理员账号类型：FORMAL-正式, TEMPORARY-临时（非 admin 为 NULL）';

CREATE UNIQUE INDEX uk_user_account ON "user"(user_account);
CREATE INDEX idx_user_role ON "user"(user_role);
CREATE INDEX idx_user_status ON "user"(user_status);
CREATE INDEX idx_user_is_delete ON "user"(is_delete);

-- -----------------------------------------------------------------------------
-- 2. 医生表
-- -----------------------------------------------------------------------------
CREATE TABLE doctor (
    id                  BIGSERIAL PRIMARY KEY,
    user_id             BIGINT NOT NULL,
    doctor_name         VARCHAR(50) NOT NULL,
    department          VARCHAR(50) NOT NULL,
    title               VARCHAR(50),
    specialty           VARCHAR(100),
    work_status         SMALLINT DEFAULT 1,
    on_duty_status      SMALLINT DEFAULT 0,
    phone               VARCHAR(20),
    email               VARCHAR(100),
    avatar_url          VARCHAR(255),
    introduction        TEXT,
    consultation_fee    NUMERIC(10,2) DEFAULT 50.00,
    create_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_delete           SMALLINT DEFAULT 0,
    doctor_no           VARCHAR(50),
    license_no          VARCHAR(100),
    hospital_id         BIGINT,
    hospital_name       VARCHAR(100),
    description         TEXT,
    gender              SMALLINT DEFAULT 0,
    user_account        VARCHAR(50),
    user_password       VARCHAR(100),
    salt                VARCHAR(32)
);

CREATE UNIQUE INDEX uk_doctor_user_id ON doctor(user_id);
CREATE INDEX idx_doctor_department ON doctor(department);
CREATE INDEX idx_doctor_work_status ON doctor(work_status);
CREATE INDEX idx_doctor_on_duty_status ON doctor(on_duty_status);
CREATE INDEX idx_doctor_is_delete ON doctor(is_delete);

-- -----------------------------------------------------------------------------
-- 3. 排班表
-- -----------------------------------------------------------------------------
CREATE TABLE schedule (
    id                  BIGSERIAL PRIMARY KEY,
    doctor_id           BIGINT NOT NULL,
    schedule_date       DATE NOT NULL,
    shift_type          VARCHAR(20) NOT NULL,
    start_time          TIME,
    end_time            TIME,
    status              SMALLINT DEFAULT 1,
    on_duty_status      SMALLINT DEFAULT 0,
    create_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    doctor_name         VARCHAR(100),
    department          VARCHAR(50),
    shift_name          VARCHAR(50),
    time_range          VARCHAR(50),
    time_start          VARCHAR(20),
    time_end            VARCHAR(20),
    max_appointments    INTEGER DEFAULT 0,
    current_appointments INTEGER DEFAULT 0,
    description         TEXT,
    is_delete           SMALLINT DEFAULT 0
);

CREATE UNIQUE INDEX uk_schedule_doctor_date_shift ON schedule(doctor_id, schedule_date, shift_type);
CREATE INDEX idx_schedule_doctor_id ON schedule(doctor_id);
CREATE INDEX idx_schedule_date ON schedule(schedule_date);
CREATE INDEX idx_schedule_status ON schedule(status);
CREATE INDEX idx_schedule_on_duty_status ON schedule(on_duty_status);

-- -----------------------------------------------------------------------------
-- 4. 预约号源表
-- -----------------------------------------------------------------------------
CREATE TABLE appointment_slot (
    id                  BIGSERIAL PRIMARY KEY,
    schedule_id         BIGINT NOT NULL,
    time_slot           VARCHAR(50) NOT NULL,
    total_count         INTEGER DEFAULT 10,
    available_count     INTEGER DEFAULT 10,
    status              VARCHAR(20) DEFAULT 'AVAILABLE',
    create_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version             BIGINT DEFAULT 0,
    doctor_id           BIGINT,
    doctor_name         VARCHAR(100) DEFAULT NULL,
    time_start          VARCHAR(20),
    time_end            VARCHAR(20),
    max_slots           INTEGER DEFAULT 10,
    available_slots     INTEGER DEFAULT 10,
    locked_slots        INTEGER DEFAULT 0
);

COMMENT ON COLUMN appointment_slot.doctor_id IS '医生ID（冗余，无外键）';
COMMENT ON COLUMN appointment_slot.doctor_name IS '医生姓名（冗余）';
COMMENT ON COLUMN appointment_slot.total_count IS '遗留字段，业务请用 max_slots';
COMMENT ON COLUMN appointment_slot.available_count IS '遗留字段，业务请用 available_slots';

CREATE UNIQUE INDEX uk_slot_schedule_time ON appointment_slot(schedule_id, time_slot);
CREATE INDEX idx_slot_schedule_id ON appointment_slot(schedule_id);
CREATE INDEX idx_slot_status ON appointment_slot(status);

-- -----------------------------------------------------------------------------
-- 5. 预约表
-- -----------------------------------------------------------------------------
CREATE TABLE appointment (
    id                  BIGSERIAL PRIMARY KEY,
    appointment_no      VARCHAR(50) NOT NULL,
    user_id             BIGINT NOT NULL,
    user_name           VARCHAR(50) NOT NULL,
    doctor_id           BIGINT NOT NULL,
    doctor_name         VARCHAR(50) NOT NULL,
    department          VARCHAR(50) NOT NULL,
    schedule_id         BIGINT NOT NULL,
    schedule_date       DATE NOT NULL,
    shift_type          VARCHAR(20) NOT NULL,
    time_slot           VARCHAR(50) NOT NULL,
    consultation_fee    NUMERIC(10,2) NOT NULL,
    status              SMALLINT DEFAULT 0,
    check_in_time       TIMESTAMP,
    check_in_status     VARCHAR(20) DEFAULT 'UNCHECKED',
    remark              TEXT,
    create_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version             BIGINT DEFAULT 0,
    visit_type          VARCHAR(20),
    chief_complaint     VARCHAR(500),
    prescription_id     BIGINT,
    diagnosis           TEXT
);

COMMENT ON COLUMN appointment.visit_type IS '就诊类型：初诊/复诊';
COMMENT ON COLUMN appointment.chief_complaint IS '患者主诉';
COMMENT ON COLUMN appointment.prescription_id IS '关联处方ID';

CREATE UNIQUE INDEX uk_appointment_no ON appointment(appointment_no);
CREATE UNIQUE INDEX uk_appointment_user_schedule_slot
    ON appointment(user_id, schedule_id, time_slot)
    WHERE status IN (0, 1, 2);
CREATE INDEX idx_appointment_user_id ON appointment(user_id);
CREATE INDEX idx_appointment_doctor_id ON appointment(doctor_id);
CREATE INDEX idx_appointment_schedule_id ON appointment(schedule_id);
CREATE INDEX idx_appointment_status ON appointment(status);
CREATE INDEX idx_appointment_schedule_date ON appointment(schedule_date);

-- -----------------------------------------------------------------------------
-- 6. 费用项表
-- -----------------------------------------------------------------------------
CREATE TABLE fee_item (
    id                  BIGSERIAL PRIMARY KEY,
    fee_item_no         VARCHAR(50) NOT NULL,
    user_id             BIGINT NOT NULL,
    appointment_id      BIGINT,
    prescription_id     BIGINT,
    bill_id             BIGINT,
    item_type           VARCHAR(20) NOT NULL,
    item_name           VARCHAR(100) NOT NULL,
    item_code           VARCHAR(50),
    quantity            NUMERIC(10,2) NOT NULL DEFAULT 1,
    unit_price          NUMERIC(10,2) NOT NULL,
    total_amount        NUMERIC(10,2) NOT NULL,
    discount_amount     NUMERIC(10,2) DEFAULT 0.00,
    actual_amount       NUMERIC(10,2) NOT NULL,
    insurance_amount    NUMERIC(10,2) DEFAULT 0.00,
    self_pay_amount     NUMERIC(10,2) DEFAULT 0.00,
    status              VARCHAR(20) DEFAULT 'UNPAID',
    settle_flag         BOOLEAN DEFAULT FALSE,
    create_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX uk_fee_item_no ON fee_item(fee_item_no);
CREATE INDEX idx_fee_item_user_id ON fee_item(user_id);
CREATE INDEX idx_fee_item_appointment_id ON fee_item(appointment_id);
CREATE INDEX idx_fee_item_bill_id ON fee_item(bill_id);
CREATE INDEX idx_fee_item_status ON fee_item(status);

-- -----------------------------------------------------------------------------
-- 7. 账单表
-- -----------------------------------------------------------------------------
CREATE TABLE bill (
    id                  BIGSERIAL PRIMARY KEY,
    bill_no             VARCHAR(50) NOT NULL,
    user_id             BIGINT NOT NULL,
    appointment_id      BIGINT,
    total_amount        NUMERIC(10,2) NOT NULL,
    discount_amount     NUMERIC(10,2) DEFAULT 0.00,
    insurance_amount    NUMERIC(10,2) DEFAULT 0.00,
    self_pay_amount     NUMERIC(10,2) NOT NULL,
    paid_amount         NUMERIC(10,2) DEFAULT 0.00,
    status              VARCHAR(20) DEFAULT 'UNPAID',
    pay_time            TIMESTAMP,
    remark              TEXT,
    create_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version             INTEGER NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX uk_bill_no ON bill(bill_no);
CREATE INDEX idx_bill_user_id ON bill(user_id);
CREATE INDEX idx_bill_appointment_id ON bill(appointment_id);
CREATE INDEX idx_bill_status ON bill(status);

-- -----------------------------------------------------------------------------
-- 8. 支付记录表
-- -----------------------------------------------------------------------------
CREATE TABLE payment (
    id                  BIGSERIAL PRIMARY KEY,
    payment_no          VARCHAR(50) NOT NULL,
    bill_id             BIGINT NOT NULL,
    user_id             BIGINT NOT NULL,
    user_name           VARCHAR(50) NOT NULL,
    amount              NUMERIC(10,2) NOT NULL,
    payment_type        VARCHAR(20),
    third_party_no      VARCHAR(100),
    status              SMALLINT DEFAULT 0,
    pay_time            TIMESTAMP,
    callback_data       JSONB,
    create_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version             INTEGER NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX uk_payment_no ON payment(payment_no);
CREATE INDEX idx_payment_bill_id ON payment(bill_id);
CREATE INDEX idx_payment_user_id ON payment(user_id);
CREATE INDEX idx_payment_status ON payment(status);

-- -----------------------------------------------------------------------------
-- 9. 处方表
-- -----------------------------------------------------------------------------
CREATE TABLE prescription (
    id                  BIGSERIAL PRIMARY KEY,
    prescription_no     VARCHAR(50) NOT NULL,
    appointment_id      BIGINT NOT NULL,
    user_id             BIGINT NOT NULL,
    user_name           VARCHAR(50) NOT NULL,
    doctor_id           BIGINT NOT NULL,
    doctor_name         VARCHAR(50) NOT NULL,
    department          VARCHAR(50) NOT NULL,
    diagnosis           TEXT,
    drugs               TEXT NOT NULL,
    total_amount        NUMERIC(10,2) DEFAULT 0.00,
    status              SMALLINT DEFAULT 0,
    audit_time          TIMESTAMP,
    audit_user_id       BIGINT,
    audit_remark        VARCHAR(200),
    dispense_time       TIMESTAMP,
    remark              TEXT,
    create_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX uk_prescription_no ON prescription(prescription_no);
CREATE INDEX idx_prescription_appointment_id ON prescription(appointment_id);
CREATE INDEX idx_prescription_user_id ON prescription(user_id);
CREATE INDEX idx_prescription_doctor_id ON prescription(doctor_id);
CREATE INDEX idx_prescription_status ON prescription(status);

-- -----------------------------------------------------------------------------
-- 10. 处方明细表
-- -----------------------------------------------------------------------------
CREATE TABLE prescription_item (
    id                  BIGSERIAL PRIMARY KEY,
    prescription_id     BIGINT NOT NULL,
    drug_code           VARCHAR(50) NOT NULL,
    drug_name           VARCHAR(100) NOT NULL,
    specification       VARCHAR(100),
    dosage              VARCHAR(100),
    usage               VARCHAR(100),
    frequency           VARCHAR(50),
    duration            VARCHAR(50),
    quantity            NUMERIC(10,2) NOT NULL,
    unit_price          NUMERIC(10,2) NOT NULL,
    total_amount        NUMERIC(10,2) NOT NULL,
    remark              VARCHAR(200),
    create_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_prescription_item_prescription_id ON prescription_item(prescription_id);
CREATE INDEX idx_prescription_item_drug_code ON prescription_item(drug_code);

-- -----------------------------------------------------------------------------
-- 11. 药品信息表
-- -----------------------------------------------------------------------------
CREATE TABLE drug_info (
    id                  BIGSERIAL PRIMARY KEY,
    drug_code           VARCHAR(50) NOT NULL,
    drug_name           VARCHAR(100) NOT NULL,
    generic_name        VARCHAR(100),
    trade_name          VARCHAR(100),
    specification       VARCHAR(100) NOT NULL,
    unit                VARCHAR(20) NOT NULL,
    manufacturer        VARCHAR(100),
    category            VARCHAR(50),
    category_code       VARCHAR(20),
    dosage_form         VARCHAR(50),
    prescription_flag   BOOLEAN DEFAULT TRUE,
    status              SMALLINT DEFAULT 1,
    create_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    remark              TEXT
);

CREATE UNIQUE INDEX uk_drug_code ON drug_info(drug_code);
CREATE INDEX idx_drug_category_code ON drug_info(category_code);
CREATE INDEX idx_drug_status ON drug_info(status);
CREATE INDEX idx_drug_name ON drug_info(drug_name);

-- -----------------------------------------------------------------------------
-- 12. 药品价格表
-- -----------------------------------------------------------------------------
CREATE TABLE drug_price (
    id                  BIGSERIAL PRIMARY KEY,
    drug_id             BIGINT NOT NULL,
    price_type          VARCHAR(20) NOT NULL,
    price               NUMERIC(10,2) NOT NULL,
    effective_date      DATE NOT NULL,
    expire_date         DATE,
    create_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_user_id      BIGINT NOT NULL
);

CREATE UNIQUE INDEX uk_drug_price_effective ON drug_price(drug_id, price_type) WHERE expire_date IS NULL;
CREATE INDEX idx_drug_price_drug_id ON drug_price(drug_id);
CREATE INDEX idx_drug_price_price_type ON drug_price(price_type);

-- -----------------------------------------------------------------------------
-- 13. 健康档案表
-- -----------------------------------------------------------------------------
CREATE TABLE health_profile (
    id                  BIGSERIAL PRIMARY KEY,
    user_id             BIGINT NOT NULL,
    user_name           VARCHAR(50) NOT NULL,
    chronic_diseases    TEXT,
    allergy_history     TEXT,
    medication_history  TEXT,
    family_history      TEXT,
    surgical_history    TEXT,
    vaccination_history TEXT,
    physical_exam       TEXT,
    height              NUMERIC(5,2),
    weight              NUMERIC(5,2),
    blood_type          VARCHAR(10),
    blood_pressure      VARCHAR(20),
    remark              TEXT,
    create_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX uk_health_profile_user_id ON health_profile(user_id);

-- -----------------------------------------------------------------------------
-- 14. 线上问诊：会话表 / 消息表
-- -----------------------------------------------------------------------------
CREATE TABLE chat_session (
    id                  BIGSERIAL PRIMARY KEY,
    session_id          VARCHAR(64) NOT NULL,
    user_id             BIGINT NOT NULL,
    title               VARCHAR(200),
    scene               VARCHAR(32) NOT NULL DEFAULT 'consultation',
    create_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_delete           SMALLINT DEFAULT 0
);

CREATE UNIQUE INDEX uk_chat_session_session_id ON chat_session(session_id);
CREATE INDEX idx_chat_session_user_id ON chat_session(user_id);
CREATE INDEX idx_chat_session_update_time ON chat_session(update_time);
CREATE INDEX idx_chat_session_is_delete ON chat_session(is_delete);

CREATE TABLE chat_message (
    id                  BIGSERIAL PRIMARY KEY,
    session_id          VARCHAR(64) NOT NULL,
    role                VARCHAR(20) NOT NULL,
    content             TEXT NOT NULL,
    agent_type          VARCHAR(32),
    risk_level          VARCHAR(32),
    metadata_json       TEXT,
    create_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_chat_message_session_id ON chat_message(session_id);
CREATE INDEX idx_chat_message_create_time ON chat_message(create_time);

-- -----------------------------------------------------------------------------
-- 15. 症状 / ICD-10 / 关联表（RDB 镜像，数据来自 Neo4j 同步）
-- -----------------------------------------------------------------------------
CREATE TABLE icd10_code (
    code                VARCHAR(20) PRIMARY KEY,
    description         VARCHAR(500),
    description_cn      VARCHAR(500),
    chapter_code        VARCHAR(10),
    chapter_name        VARCHAR(100),
    create_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_icd10_desc_cn ON icd10_code(description_cn);
CREATE INDEX idx_icd10_chapter ON icd10_code(chapter_code);

CREATE TABLE symptom (
    id                  BIGSERIAL PRIMARY KEY,
    name                VARCHAR(100) NOT NULL,
    name_pinyin         VARCHAR(200),
    category_code       VARCHAR(10),
    category_name       VARCHAR(50),
    frequency           INTEGER NOT NULL DEFAULT 0,
    status              SMALLINT NOT NULL DEFAULT 1,
    create_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX uk_symptom_name ON symptom(name);
CREATE INDEX idx_symptom_pinyin ON symptom(name_pinyin);
CREATE INDEX idx_symptom_category ON symptom(category_code);
CREATE INDEX idx_symptom_freq_status ON symptom(frequency DESC, status);

CREATE TABLE symptom_icd_rel (
    id                  BIGSERIAL PRIMARY KEY,
    symptom_id          BIGINT NOT NULL REFERENCES symptom(id) ON DELETE RESTRICT,
    icd_code            VARCHAR(20) NOT NULL REFERENCES icd10_code(code) ON DELETE RESTRICT,
    priority            INTEGER NOT NULL DEFAULT 1,
    create_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_symptom_icd UNIQUE (symptom_id, icd_code)
);

CREATE INDEX idx_rel_symptom_id ON symptom_icd_rel(symptom_id);
CREATE INDEX idx_rel_icd_code ON symptom_icd_rel(icd_code);

-- -----------------------------------------------------------------------------
-- 19. 用户通知表（预约等领域事件异步落库）
-- -----------------------------------------------------------------------------
CREATE TABLE user_notification (
    id                  BIGSERIAL PRIMARY KEY,
    user_id             BIGINT NOT NULL,
    title               VARCHAR(100) NOT NULL,
    content             TEXT NOT NULL,
    biz_type            VARCHAR(30) NOT NULL DEFAULT 'APPOINTMENT',
    biz_id              BIGINT,
    event_type          VARCHAR(50),
    read_status         SMALLINT NOT NULL DEFAULT 0,
    create_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE user_notification IS '用户站内通知（由 RabbitMQ 预约事件 Consumer 写入）';
COMMENT ON COLUMN user_notification.read_status IS '0-未读，1-已读';
COMMENT ON COLUMN user_notification.biz_type IS '业务类型，如 APPOINTMENT';
COMMENT ON COLUMN user_notification.event_type IS '领域事件类型，如 APPOINTMENT_CREATED';

CREATE INDEX idx_user_notification_user_id ON user_notification(user_id);
CREATE INDEX idx_user_notification_user_read ON user_notification(user_id, read_status);
CREATE INDEX idx_user_notification_create_time ON user_notification(create_time DESC);

-- -----------------------------------------------------------------------------
-- 20. 预约领域事件审计表
-- -----------------------------------------------------------------------------
CREATE TABLE appointment_event_audit (
    id                  BIGSERIAL PRIMARY KEY,
    event_id            VARCHAR(100) NOT NULL,
    event_type          VARCHAR(50) NOT NULL,
    appointment_id      BIGINT NOT NULL,
    user_id             BIGINT,
    doctor_id           BIGINT,
    previous_status     SMALLINT,
    current_status      SMALLINT,
    source              VARCHAR(20),
    payload_json        TEXT,
    create_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE appointment_event_audit IS '预约领域事件审计（RabbitMQ appointment.audit 队列消费落库）';

CREATE UNIQUE INDEX uk_appointment_event_audit_event_id ON appointment_event_audit(event_id);
CREATE INDEX idx_appointment_event_audit_appointment_id ON appointment_event_audit(appointment_id);
CREATE INDEX idx_appointment_event_audit_create_time ON appointment_event_audit(create_time DESC);

-- =============================================================================
-- 脚本结束
-- =============================================================================
