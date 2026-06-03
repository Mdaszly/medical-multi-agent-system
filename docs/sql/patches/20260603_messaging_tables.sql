-- 增量脚本：已有库追加 Phase 1 消息相关表（勿对空库执行，空库请用 docs/sql/schema.sql）
-- 用法: psql -U postgres -d clinical_decision -f docs/sql/patches/20260603_messaging_tables.sql

CREATE TABLE IF NOT EXISTS user_notification (
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

CREATE INDEX IF NOT EXISTS idx_user_notification_user_id ON user_notification(user_id);
CREATE INDEX IF NOT EXISTS idx_user_notification_user_read ON user_notification(user_id, read_status);
CREATE INDEX IF NOT EXISTS idx_user_notification_create_time ON user_notification(create_time DESC);

CREATE TABLE IF NOT EXISTS appointment_event_audit (
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

CREATE UNIQUE INDEX IF NOT EXISTS uk_appointment_event_audit_event_id ON appointment_event_audit(event_id);
CREATE INDEX IF NOT EXISTS idx_appointment_event_audit_appointment_id ON appointment_event_audit(appointment_id);
CREATE INDEX IF NOT EXISTS idx_appointment_event_audit_create_time ON appointment_event_audit(create_time DESC);
