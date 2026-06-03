package com.medical.messaging.config;

/**
 * RabbitMQ 预约领域事件拓扑常量。
 *
 * <p>路由示意（Topic 交换机 {@link #APPOINTMENT_EXCHANGE}）：
 * <pre>
 *   appointment.created / cancelled / checked-in / expired / reminder / settled / bill-unpaid
 *        ├─► appointment.notification  （用户/医生站内通知）
 *        └─► appointment.audit         （绑定 appointment.#，全量审计）
 *
 *   appointment.expired
 *        └─► appointment.slot-restore  （仅过期：异步回补 available_slots）
 *
 *   消费失败 ──► DLX {@link #APPOINTMENT_DLX} ──► appointment.dlq
 * </pre>
 *
 * <p>与用户主动「取消」不同：取消在 {@code AppointmentServiceImpl} 内同步 {@code increaseAvailableSlots}，
 * 过期回补走本模块 {@code AppointmentSlotRestoreConsumer}，避免定时任务事务内做过多 IO。
 */
public final class RabbitMqTopology {

    public static final String APPOINTMENT_EXCHANGE = "medical.appointment.topic";
    public static final String APPOINTMENT_DLX = "medical.appointment.dlx";

    /** 写 {@code user_notification}，覆盖 7 类事件的用户/医生通知 */
    public static final String QUEUE_NOTIFICATION = "appointment.notification";
    /** 仅消费 expired，将号源 available_slots +1（带上限，见 Mapper） */
    public static final String QUEUE_SLOT_RESTORE = "appointment.slot-restore";
    /** 绑定 {@link #RK_AUDIT_ALL}，落库 {@code appointment_event_audit} */
    public static final String QUEUE_AUDIT = "appointment.audit";
    public static final String QUEUE_DLQ = "appointment.dlq";

    public static final String RK_CREATED = "appointment.created";
    public static final String RK_CANCELLED = "appointment.cancelled";
    public static final String RK_CHECKED_IN = "appointment.checked-in";
    public static final String RK_EXPIRED = "appointment.expired";
    public static final String RK_REMINDER = "appointment.reminder";
    public static final String RK_SETTLED = "appointment.settled";
    public static final String RK_BILL_UNPAID = "appointment.bill-unpaid";
    public static final String RK_AUDIT_ALL = "appointment.#";
    public static final String RK_DLQ = "appointment.dlq";

    private RabbitMqTopology() {
    }
}
