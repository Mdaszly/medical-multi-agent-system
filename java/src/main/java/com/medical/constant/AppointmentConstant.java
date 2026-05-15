package com.medical.constant;

public final class AppointmentConstant {

    private AppointmentConstant() {
    }

    public static final Integer APPOINTMENT_STATUS_PENDING = 0;
    public static final Integer APPOINTMENT_STATUS_CHECKED_IN = 1;
    public static final Integer APPOINTMENT_STATUS_IN_CONSULTATION = 2;
    public static final Integer APPOINTMENT_STATUS_COMPLETED = 3;
    public static final Integer APPOINTMENT_STATUS_CANCELLED = 4;
    public static final Integer APPOINTMENT_STATUS_EXPIRED = 5;
    public static final Integer APPOINTMENT_STATUS_SETTLED = 6;

    public static final String SLOT_STATUS_AVAILABLE = "AVAILABLE";
    public static final String SLOT_STATUS_LOCKED = "LOCKED";
    public static final String SLOT_STATUS_SOLD_OUT = "SOLD_OUT";

    public static final String APPOINTMENT_NO_PREFIX = "APT";
    public static final String LOCK_APPOINTMENT_SLOT = "lock:appointment:slot:%d";
}
