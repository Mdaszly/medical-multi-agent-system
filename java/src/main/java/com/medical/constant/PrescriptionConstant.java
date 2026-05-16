package com.medical.constant;

public final class PrescriptionConstant {

    private PrescriptionConstant() {
    }

    public static final Integer PRESCRIPTION_STATUS_PENDING = 0;
    public static final Integer PRESCRIPTION_STATUS_AUDITED = 1;
    public static final Integer PRESCRIPTION_STATUS_DISPENSED = 2;
    public static final Integer PRESCRIPTION_STATUS_COMPLETED = 3;
    public static final Integer PRESCRIPTION_STATUS_CANCELLED = 4;

    public static final String PRESCRIPTION_NO_PREFIX = "RX";
}