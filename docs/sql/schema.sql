-- =============================================
-- 医院医疗平台数据库初始化脚本
-- =============================================

-- ------------------------------
-- 1. 用户表
-- ------------------------------
DROP TABLE IF EXISTS "user";
CREATE TABLE IF NOT EXISTS "user" (
    id                  BIGSERIAL PRIMARY KEY,
    user_account        VARCHAR(50) NOT NULL,                      -- 用户账号
    user_password       VARCHAR(100) NOT NULL,                     -- 用户密码（加密存储）
    salt                VARCHAR(32),                               -- 加密盐值
    user_name           VARCHAR(50) NOT NULL,                      -- 用户姓名
    user_role           VARCHAR(20) NOT NULL DEFAULT 'user',       -- 用户角色：admin-管理员, doctor-医生, user-患者, pharmacist-药房人员
    phone               VARCHAR(20),                               -- 手机号
    email               VARCHAR(100),                              -- 邮箱
    gender              SMALLINT DEFAULT 0,                        -- 性别：0-未知, 1-男, 2-女
    birth_date          DATE,                                      -- 出生日期
    user_status         SMALLINT DEFAULT 1,                        -- 状态：0-禁用, 1-启用
    create_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_delete           SMALLINT DEFAULT 0                         -- 是否删除：0-未删除, 1-已删除
);

-- 用户表索引
CREATE UNIQUE INDEX uk_user_account ON "user"(user_account);
CREATE INDEX idx_user_role ON "user"(user_role);
CREATE INDEX idx_user_status ON "user"(user_status);
CREATE INDEX idx_user_is_delete ON "user"(is_delete);

-- ------------------------------
-- 2. 医生表
-- ------------------------------
DROP TABLE IF EXISTS doctor;
CREATE TABLE IF NOT EXISTS doctor (
    id                  BIGSERIAL PRIMARY KEY,
    user_id             BIGINT NOT NULL,                           -- 关联用户ID
    doctor_name         VARCHAR(50) NOT NULL,                      -- 医生姓名
    department          VARCHAR(50) NOT NULL,                      -- 科室
    title               VARCHAR(50),                               -- 职称
    specialty           VARCHAR(100),                              -- 专业领域
    work_status         SMALLINT DEFAULT 1,                        -- 工作状态：0-离线, 1-在线, 2-已离职
    on_duty_status      SMALLINT DEFAULT 0,                        -- 在岗状态：0-休息, 1-在岗
    phone               VARCHAR(20),                               -- 联系电话
    email               VARCHAR(100),                              -- 邮箱
    avatar_url          VARCHAR(255),                              -- 头像URL
    introduction        TEXT,                                      -- 个人简介
    consultation_fee    NUMERIC(10,2) DEFAULT 50.00,               -- 诊疗费
    create_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_delete           SMALLINT DEFAULT 0                         -- 是否删除
);

-- 医生表索引
CREATE UNIQUE INDEX uk_doctor_user_id ON doctor(user_id);
CREATE INDEX idx_doctor_department ON doctor(department);
CREATE INDEX idx_doctor_work_status ON doctor(work_status);
CREATE INDEX idx_doctor_on_duty_status ON doctor(on_duty_status);
CREATE INDEX idx_doctor_is_delete ON doctor(is_delete);

-- ------------------------------
-- 3. 排班表
-- ------------------------------
DROP TABLE IF EXISTS schedule;
CREATE TABLE IF NOT EXISTS schedule (
    id                  BIGSERIAL PRIMARY KEY,
    doctor_id           BIGINT NOT NULL,                           -- 医生ID
    schedule_date       DATE NOT NULL,                             -- 排班日期
    shift_type          VARCHAR(20) NOT NULL,                      -- 班次类型：morning-早班, afternoon-中班, evening-晚班
    start_time          TIME,                                      -- 开始时间
    end_time            TIME,                                      -- 结束时间
    status              SMALLINT DEFAULT 1,                        -- 状态：0-取消, 1-正常
    on_duty_status      SMALLINT DEFAULT 0,                        -- 当前在岗状态：0-休息, 1-在岗
    create_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 排班表索引
CREATE UNIQUE INDEX uk_schedule_doctor_date_shift ON schedule(doctor_id, schedule_date, shift_type);
CREATE INDEX idx_schedule_doctor_id ON schedule(doctor_id);
CREATE INDEX idx_schedule_date ON schedule(schedule_date);
CREATE INDEX idx_schedule_status ON schedule(status);
CREATE INDEX idx_schedule_on_duty_status ON schedule(on_duty_status);

-- ------------------------------
-- 4. 预约表
-- ------------------------------
DROP TABLE IF EXISTS appointment;
CREATE TABLE IF NOT EXISTS appointment (
    id                  BIGSERIAL PRIMARY KEY,
    appointment_no      VARCHAR(50) NOT NULL,                      -- 预约编号
    user_id             BIGINT NOT NULL,                           -- 用户ID
    user_name           VARCHAR(50) NOT NULL,                      -- 用户姓名（冗余）
    doctor_id           BIGINT NOT NULL,                           -- 医生ID
    doctor_name         VARCHAR(50) NOT NULL,                      -- 医生姓名（冗余）
    department          VARCHAR(50) NOT NULL,                      -- 科室
    schedule_id         BIGINT NOT NULL,                           -- 排班ID
    schedule_date       DATE NOT NULL,                             -- 预约日期
    shift_type          VARCHAR(20) NOT NULL,                      -- 班次类型
    time_slot           VARCHAR(50) NOT NULL,                      -- 时间段
    consultation_fee    NUMERIC(10,2) NOT NULL,                   -- 诊疗费用
    status              SMALLINT DEFAULT 0,                        -- 状态：0-待确认, 1-已签到, 2-就诊中, 3-已完成, 4-已取消, 5-已过期, 6-已结算
    check_in_time       TIMESTAMP,                                 -- 签到时间
    check_in_status     VARCHAR(20) DEFAULT 'UNCHECKED',          -- 签到状态：UNCHECKED-未签到, CHECKED-已签到
    remark              TEXT,                                      -- 备注
    create_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version             BIGINT DEFAULT 0                           -- 乐观锁版本号
);

-- 预约表索引
CREATE UNIQUE INDEX uk_appointment_no ON appointment(appointment_no);
CREATE UNIQUE INDEX uk_appointment_user_schedule_slot ON appointment(user_id, schedule_id, time_slot);
CREATE INDEX idx_appointment_user_id ON appointment(user_id);
CREATE INDEX idx_appointment_doctor_id ON appointment(doctor_id);
CREATE INDEX idx_appointment_schedule_id ON appointment(schedule_id);
CREATE INDEX idx_appointment_status ON appointment(status);
CREATE INDEX idx_appointment_schedule_date ON appointment(schedule_date);

-- ------------------------------
-- 5. 预约号源表
-- ------------------------------
DROP TABLE IF EXISTS appointment_slot;
CREATE TABLE IF NOT EXISTS appointment_slot (
    id                  BIGSERIAL PRIMARY KEY,
    schedule_id         BIGINT NOT NULL,                           -- 排班ID
    time_slot           VARCHAR(50) NOT NULL,                      -- 时间段
    total_count         INTEGER DEFAULT 10,                        -- 总号源数
    available_count     INTEGER DEFAULT 10,                        -- 可用号源数
    status              VARCHAR(20) DEFAULT 'AVAILABLE',           -- 状态：AVAILABLE-可预约, LOCKED-锁定中, SOLD_OUT-已售罄
    create_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version             BIGINT DEFAULT 0                           -- 乐观锁版本号
);

-- 号源表索引
CREATE UNIQUE INDEX uk_slot_schedule_time ON appointment_slot(schedule_id, time_slot);
CREATE INDEX idx_slot_schedule_id ON appointment_slot(schedule_id);
CREATE INDEX idx_slot_status ON appointment_slot(status);

-- ------------------------------
-- 6. 费用项表（用于延迟统一结算）
-- ------------------------------
DROP TABLE IF EXISTS fee_item;
CREATE TABLE IF NOT EXISTS fee_item (
    id                  BIGSERIAL PRIMARY KEY,
    fee_item_no         VARCHAR(50) NOT NULL,                     -- 费用项编号
    user_id             BIGINT NOT NULL,                          -- 用户ID
    appointment_id      BIGINT,                                   -- 关联预约ID
    prescription_id     BIGINT,                                   -- 关联处方ID
    bill_id             BIGINT,                                   -- 关联账单ID（结算后填充）
    item_type           VARCHAR(20) NOT NULL,                     -- 费用类型：REGISTRATION-挂号费, CONSULTATION-诊疗费, PRESCRIPTION-处方费, EXAMINATION-检查费, TREATMENT-治疗费
    item_name           VARCHAR(100) NOT NULL,                    -- 费用项名称
    item_code           VARCHAR(50),                              -- 费用项编码（HIS系统编码）
    quantity            NUMERIC(10,2) NOT NULL DEFAULT 1,         -- 数量
    unit_price          NUMERIC(10,2) NOT NULL,                   -- 单价
    total_amount        NUMERIC(10,2) NOT NULL,                   -- 总金额
    discount_amount     NUMERIC(10,2) DEFAULT 0.00,              -- 优惠金额
    actual_amount       NUMERIC(10,2) NOT NULL,                   -- 实际金额
    insurance_amount    NUMERIC(10,2) DEFAULT 0.00,               -- 医保报销金额
    self_pay_amount     NUMERIC(10,2) DEFAULT 0.00,               -- 自付金额
    status              VARCHAR(20) DEFAULT 'UNPAID',             -- 状态：UNPAID-未支付, PAID-已支付, REFUNDED-已退款
    settle_flag         BOOLEAN DEFAULT FALSE,                    -- 是否已结算
    create_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 费用项表索引
CREATE UNIQUE INDEX uk_fee_item_no ON fee_item(fee_item_no);
CREATE INDEX idx_fee_item_user_id ON fee_item(user_id);
CREATE INDEX idx_fee_item_appointment_id ON fee_item(appointment_id);
CREATE INDEX idx_fee_item_bill_id ON fee_item(bill_id);
CREATE INDEX idx_fee_item_status ON fee_item(status);

-- ------------------------------
-- 7. 账单表（统一结算）
-- ------------------------------
DROP TABLE IF EXISTS bill;
CREATE TABLE IF NOT EXISTS bill (
    id                  BIGSERIAL PRIMARY KEY,
    bill_no             VARCHAR(50) NOT NULL,                     -- 账单编号
    user_id             BIGINT NOT NULL,                          -- 用户ID
    appointment_id      BIGINT,                                   -- 关联预约ID
    total_amount        NUMERIC(10,2) NOT NULL,                   -- 总金额
    discount_amount     NUMERIC(10,2) DEFAULT 0.00,              -- 优惠金额
    insurance_amount    NUMERIC(10,2) DEFAULT 0.00,               -- 医保报销金额
    self_pay_amount     NUMERIC(10,2) NOT NULL,                   -- 自付金额
    paid_amount         NUMERIC(10,2) DEFAULT 0.00,               -- 已支付金额
    status              VARCHAR(20) DEFAULT 'UNPAID',             -- 状态：UNPAID-未支付, PAID-已支付, CANCELLED-已取消, REFUNDED-已退款
    pay_time            TIMESTAMP,                                -- 支付时间
    remark              TEXT,                                     -- 备注
    create_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 账单表索引
CREATE UNIQUE INDEX uk_bill_no ON bill(bill_no);
CREATE INDEX idx_bill_user_id ON bill(user_id);
CREATE INDEX idx_bill_appointment_id ON bill(appointment_id);
CREATE INDEX idx_bill_status ON bill(status);

-- ------------------------------
-- 8. 支付记录表
-- ------------------------------
DROP TABLE IF EXISTS payment;
CREATE TABLE IF NOT EXISTS payment (
    id                  BIGSERIAL PRIMARY KEY,
    payment_no          VARCHAR(50) NOT NULL,                     -- 支付编号
    bill_id             BIGINT NOT NULL,                          -- 账单ID
    user_id             BIGINT NOT NULL,                          -- 用户ID
    user_name           VARCHAR(50) NOT NULL,                     -- 用户姓名
    amount              NUMERIC(10,2) NOT NULL,                   -- 支付金额
    payment_type        VARCHAR(20),                              -- 支付方式：WECHAT-微信支付, ALIPAY-支付宝, CASH-现金, INSURANCE-医保支付
    third_party_no      VARCHAR(100),                             -- 第三方支付流水号
    status              SMALLINT DEFAULT 0,                       -- 状态：0-待支付, 1-已支付, 2-已取消, 3-已退款
    pay_time            TIMESTAMP,                                -- 支付时间
    callback_data       JSONB,                                    -- 回调数据（JSON格式）
    create_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 支付记录表索引
CREATE UNIQUE INDEX uk_payment_no ON payment(payment_no);
CREATE INDEX idx_payment_bill_id ON payment(bill_id);
CREATE INDEX idx_payment_user_id ON payment(user_id);
CREATE INDEX idx_payment_status ON payment(status);

-- ------------------------------
-- 9. 处方表
-- ------------------------------
DROP TABLE IF EXISTS prescription;
CREATE TABLE IF NOT EXISTS prescription (
    id                  BIGSERIAL PRIMARY KEY,
    prescription_no     VARCHAR(50) NOT NULL,                     -- 处方编号
    appointment_id      BIGINT NOT NULL,                          -- 预约ID
    user_id             BIGINT NOT NULL,                          -- 用户ID
    user_name           VARCHAR(50) NOT NULL,                     -- 用户姓名
    doctor_id           BIGINT NOT NULL,                          -- 医生ID
    doctor_name         VARCHAR(50) NOT NULL,                     -- 医生姓名
    department          VARCHAR(50) NOT NULL,                     -- 科室
    diagnosis           TEXT,                                     -- 诊断结果
    drugs               TEXT NOT NULL,                            -- 药品信息（JSON格式）
    total_amount        NUMERIC(10,2) DEFAULT 0.00,               -- 处方总金额
    status              SMALLINT DEFAULT 0,                       -- 状态：0-待审核, 1-已审核, 2-已发药, 3-已完成, 4-已取消
    audit_time          TIMESTAMP,                                -- 审核时间
    audit_user_id       BIGINT,                                   -- 审核人ID
    audit_remark        VARCHAR(200),                             -- 审核备注
    dispense_time       TIMESTAMP,                                -- 发药时间
    remark              TEXT,                                     -- 备注
    create_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 处方表索引
CREATE UNIQUE INDEX uk_prescription_no ON prescription(prescription_no);
CREATE INDEX idx_prescription_appointment_id ON prescription(appointment_id);
CREATE INDEX idx_prescription_user_id ON prescription(user_id);
CREATE INDEX idx_prescription_doctor_id ON prescription(doctor_id);
CREATE INDEX idx_prescription_status ON prescription(status);

-- ------------------------------
-- 10. 处方明细表
-- ------------------------------
DROP TABLE IF EXISTS prescription_item;
CREATE TABLE IF NOT EXISTS prescription_item (
    id                  BIGSERIAL PRIMARY KEY,
    prescription_id     BIGINT NOT NULL,                          -- 处方ID
    drug_code           VARCHAR(50) NOT NULL,                     -- 药品编码
    drug_name           VARCHAR(100) NOT NULL,                    -- 药品名称
    specification       VARCHAR(100),                              -- 规格
    dosage              VARCHAR(100),                              -- 剂量
    usage               VARCHAR(100),                              -- 用法
    frequency           VARCHAR(50),                               -- 频次
    duration            VARCHAR(50),                               -- 用药时长
    quantity            NUMERIC(10,2) NOT NULL,                   -- 数量
    unit_price          NUMERIC(10,2) NOT NULL,                   -- 单价
    total_amount        NUMERIC(10,2) NOT NULL,                   -- 小计
    remark              VARCHAR(200),                             -- 备注
    create_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 处方明细表索引
CREATE INDEX idx_prescription_item_prescription_id ON prescription_item(prescription_id);
CREATE INDEX idx_prescription_item_drug_code ON prescription_item(drug_code);

-- ------------------------------
-- 11. 药品信息表（新增）
-- ------------------------------
DROP TABLE IF EXISTS drug_info;
CREATE TABLE IF NOT EXISTS drug_info (
    id                  BIGSERIAL PRIMARY KEY,
    drug_code           VARCHAR(50) NOT NULL,                     -- 药品编码（国家标准码）
    drug_name           VARCHAR(100) NOT NULL,                    -- 药品名称
    generic_name        VARCHAR(100),                             -- 通用名
    trade_name          VARCHAR(100),                             -- 商品名
    specification       VARCHAR(100) NOT NULL,                    -- 规格
    unit                VARCHAR(20) NOT NULL,                     -- 最小单位：盒、瓶、片等
    manufacturer        VARCHAR(100),                             -- 生产厂家
    category            VARCHAR(50),                              -- 药品类别
    category_code       VARCHAR(20),                              -- 类别编码
    dosage_form         VARCHAR(50),                              -- 剂型
    prescription_flag   BOOLEAN DEFAULT true,                     -- 是否处方药
    status              SMALLINT DEFAULT 1,                       -- 状态：0-停用, 1-启用
    create_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    remark              TEXT                                      -- 备注
);

-- 药品信息表索引
CREATE UNIQUE INDEX uk_drug_code ON drug_info(drug_code);
CREATE INDEX idx_drug_category_code ON drug_info(category_code);
CREATE INDEX idx_drug_status ON drug_info(status);
CREATE INDEX idx_drug_name ON drug_info(drug_name);

-- ------------------------------
-- 12. 药品价格表（新增）
-- ------------------------------
DROP TABLE IF EXISTS drug_price;
CREATE TABLE IF NOT EXISTS drug_price (
    id                  BIGSERIAL PRIMARY KEY,
    drug_id             BIGINT NOT NULL,                          -- 关联药品ID
    price_type          VARCHAR(20) NOT NULL,                     -- 价格类型：RETAIL-零售价, INSURANCE-医保价, WHOLESALE-批发价
    price               NUMERIC(10,2) NOT NULL,                   -- 价格
    effective_date      DATE NOT NULL,                            -- 生效日期
    expire_date         DATE,                                     -- 失效日期（空表示当前有效）
    create_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_user_id      BIGINT NOT NULL                           -- 创建人ID
);

-- 药品价格表索引
CREATE UNIQUE INDEX uk_drug_price_effective ON drug_price(drug_id, price_type) WHERE expire_date IS NULL;
CREATE INDEX idx_drug_price_drug_id ON drug_price(drug_id);
CREATE INDEX idx_drug_price_price_type ON drug_price(price_type);

-- ------------------------------
-- 13. 健康档案表
-- ------------------------------
DROP TABLE IF EXISTS health_profile;
CREATE TABLE IF NOT EXISTS health_profile (
    id                  BIGSERIAL PRIMARY KEY,
    user_id             BIGINT NOT NULL,                          -- 用户ID
    user_name           VARCHAR(50) NOT NULL,                     -- 用户姓名
    chronic_diseases    TEXT,                                     -- 慢性病史
    allergy_history     TEXT,                                     -- 过敏史
    medication_history  TEXT,                                     -- 用药史
    family_history      TEXT,                                     -- 家族病史
    remark              TEXT,                                     -- 备注
    update_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 健康档案表索引
CREATE UNIQUE INDEX uk_health_profile_user_id ON health_profile(user_id);

-- ------------------------------
-- 14. 审计日志表
-- ------------------------------
DROP TABLE IF EXISTS audit_log;
CREATE TABLE IF NOT EXISTS audit_log (
    id                  BIGSERIAL PRIMARY KEY,
    user_id             BIGINT,                                   -- 操作人ID
    user_name           VARCHAR(50),                              -- 操作人姓名
    module              VARCHAR(50),                              -- 操作模块
    operation           VARCHAR(50),                              -- 操作类型：ADD-新增, UPDATE-更新, DELETE-删除, QUERY-查询
    target_type         VARCHAR(50),                              -- 目标类型
    target_id           VARCHAR(100),                             -- 目标ID
    before_data         TEXT,                                     -- 操作前数据（JSON）
    after_data          TEXT,                                     -- 操作后数据（JSON）
    ip_address          VARCHAR(50),                              -- IP地址
    user_agent          TEXT,                                     -- 用户代理
    result              VARCHAR(20) DEFAULT 'SUCCESS',            -- 操作结果：SUCCESS-成功, FAIL-失败
    error_message       TEXT,                                     -- 错误信息
    create_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 审计日志表索引
CREATE INDEX idx_audit_user_id ON audit_log(user_id);
CREATE INDEX idx_audit_module ON audit_log(module);
CREATE INDEX idx_audit_operation ON audit_log(operation);
CREATE INDEX idx_audit_create_time ON audit_log(create_time);

-- ------------------------------
-- 初始化数据
-- ------------------------------

-- 初始化管理员用户
INSERT INTO "user" (user_account, user_password, salt, user_name, user_role, phone, email, user_status)
VALUES 
('admin', 'e10adc3949ba59abbe56e057f20f883e', 'admin_salt', '系统管理员', 'admin', '13800138000', 'admin@hospital.com', 1),
('pharmacist', 'e10adc3949ba59abbe56e057f20f883e', 'pharmacist_salt', '药房管理员', 'pharmacist', '13900139000', 'pharmacist@hospital.com', 1)
ON CONFLICT (user_account) DO NOTHING;

-- 初始化药品数据（示例）
INSERT INTO drug_info (drug_code, drug_name, generic_name, trade_name, specification, unit, manufacturer, category, category_code, dosage_form)
VALUES 
('C01AD01', '阿莫西林胶囊', '阿莫西林', '阿莫仙', '0.5g*24粒', '盒', '华北制药股份有限公司', '抗生素', 'ANTIBIOTIC', '胶囊剂'),
('C03AA01', '感冒灵颗粒', '感冒灵', '999', '10g*9袋', '盒', '华润三九医药股份有限公司', '感冒药', 'COLD', '颗粒剂'),
('N02BE01', '布洛芬缓释胶囊', '布洛芬', '芬必得', '0.4g*20粒', '盒', '中美天津史克制药有限公司', '解热镇痛药', 'ANALGESIC', '胶囊剂'),
('R03BA02', '沙丁胺醇气雾剂', '沙丁胺醇', '万托林', '100μg*200喷', '瓶', '葛兰素史克(中国)投资有限公司', '呼吸系统药', 'RESPIRATORY', '气雾剂'),
('A02BC01', '奥美拉唑肠溶胶囊', '奥美拉唑', '洛赛克', '20mg*14粒', '盒', '阿斯利康制药有限公司', '消化系统药', 'DIGESTIVE', '胶囊剂')
ON CONFLICT (drug_code) DO NOTHING;

-- 初始化药品价格数据（示例）
INSERT INTO drug_price (drug_id, price_type, price, effective_date, create_user_id)
SELECT id, 'RETAIL', 28.00, '2024-01-01', 1 FROM drug_info WHERE drug_code = 'C01AD01'
ON CONFLICT (drug_id, price_type) WHERE expire_date IS NULL DO NOTHING;

INSERT INTO drug_price (drug_id, price_type, price, effective_date, create_user_id)
SELECT id, 'INSURANCE', 18.00, '2024-01-01', 1 FROM drug_info WHERE drug_code = 'C01AD01'
ON CONFLICT (drug_id, price_type) WHERE expire_date IS NULL DO NOTHING;

INSERT INTO drug_price (drug_id, price_type, price, effective_date, create_user_id)
SELECT id, 'RETAIL', 15.00, '2024-01-01', 1 FROM drug_info WHERE drug_code = 'C03AA01'
ON CONFLICT (drug_id, price_type) WHERE expire_date IS NULL DO NOTHING;

INSERT INTO drug_price (drug_id, price_type, price, effective_date, create_user_id)
SELECT id, 'INSURANCE', 10.00, '2024-01-01', 1 FROM drug_info WHERE drug_code = 'C03AA01'
ON CONFLICT (drug_id, price_type) WHERE expire_date IS NULL DO NOTHING;

INSERT INTO drug_price (drug_id, price_type, price, effective_date, create_user_id)
SELECT id, 'RETAIL', 35.00, '2024-01-01', 1 FROM drug_info WHERE drug_code = 'N02BE01'
ON CONFLICT (drug_id, price_type) WHERE expire_date IS NULL DO NOTHING;

INSERT INTO drug_price (drug_id, price_type, price, effective_date, create_user_id)
SELECT id, 'RETAIL', 58.00, '2024-01-01', 1 FROM drug_info WHERE drug_code = 'R03BA02'
ON CONFLICT (drug_id, price_type) WHERE expire_date IS NULL DO NOTHING;

INSERT INTO drug_price (drug_id, price_type, price, effective_date, create_user_id)
SELECT id, 'RETAIL', 42.00, '2024-01-01', 1 FROM drug_info WHERE drug_code = 'A02BC01'
ON CONFLICT (drug_id, price_type) WHERE expire_date IS NULL DO NOTHING;

-- 1. 添加冗余字段（无外键约束）
ALTER TABLE appointment_slot
    ADD COLUMN doctor_id BIGINT DEFAULT NULL,
    ADD COLUMN doctor_name VARCHAR(100) DEFAULT NULL;

-- 2. 添加注释
COMMENT ON COLUMN appointment_slot.doctor_id IS '医生ID（冗余字段，无外键）';
COMMENT ON COLUMN appointment_slot.doctor_name IS '医生姓名（冗余字段）';

-- 3. 回填现有数据
UPDATE appointment_slot a
SET doctor_id = s.doctor_id,
    doctor_name = s.doctor_name
FROM schedule s
WHERE a.schedule_id = s.id;

-- 4. 验证
SELECT a.id, a.schedule_id, a.doctor_id, a.doctor_name, a.time_slot
FROM appointment_slot a
LIMIT 10;

-- 给 payment 表添加缺失的字段
ALTER TABLE payment
    ADD COLUMN callback_data TEXT DEFAULT NULL;

-- 1. 给 payment 表添加版本号（乐观锁）
ALTER TABLE payment ADD COLUMN version INT DEFAULT 0;

-- 2. 给 bill 表添加 version 字段
ALTER TABLE bill ADD COLUMN version INT DEFAULT 0;

-- 3. 为已存在的记录设置初始版本号
UPDATE payment SET version = 0 WHERE version IS NULL OR version = 0;
UPDATE bill SET version = 0 WHERE version IS NULL OR version = 0;

-- 4. 确保字段不为NULL
ALTER TABLE payment ALTER COLUMN version SET DEFAULT 0;
ALTER TABLE payment ALTER COLUMN version SET NOT NULL;
ALTER TABLE bill ALTER COLUMN version SET DEFAULT 0;
ALTER TABLE bill ALTER COLUMN version SET NOT NULL;


-- 5. 验证
SELECT id, version, status FROM payment LIMIT 5;
SELECT id, version, status FROM bill LIMIT 5;



-- =============================================
-- 脚本结束
-- =============================================