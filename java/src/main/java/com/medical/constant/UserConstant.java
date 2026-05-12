package com.medical.constant;

public class UserConstant {

    private UserConstant() {
    }

    public static final String USER_ROLE = "user";
    public static final String ADMIN_ROLE = "admin";
    public static final String DOCTOR_ROLE = "doctor";

    public static final Integer STATUS_NORMAL = 1;
    public static final Integer STATUS_DISABLED = 0;

    public static final Integer NOT_DELETED = 0;
    public static final Integer IS_DELETED = 1;

    public static final Integer GENDER_UNKNOWN = 0;
    public static final Integer GENDER_MALE = 1;
    public static final Integer GENDER_FEMALE = 2;

    public static final Integer LOGIN_SUCCESS = 1;
    public static final Integer LOGIN_FAILED = 0;

    public static final Integer MAX_LOGIN_RETRY = 5;
    public static final Long LOCK_DURATION_MINUTES = 30L;

    public static final Integer DOCTOR_STATUS_ONLINE = 1;
    public static final Integer DOCTOR_STATUS_OFFLINE = 0;
    public static final Integer DOCTOR_STATUS_LEFT = 2;
}