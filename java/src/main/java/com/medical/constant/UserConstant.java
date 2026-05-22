package com.medical.constant;

public class UserConstant {

    private UserConstant() {
    }

    public static final String USER_ROLE = "user";
    public static final String ADMIN_ROLE = "admin";
    public static final String DOCTOR_ROLE = "doctor";
    public static final String PHARMACIST_ROLE = "pharmacist";

    /** Sa-Token Session：登录主体类型 */
    public static final String SESSION_LOGIN_TYPE = "loginType";
    /** Sa-Token Session：当前角色 */
    public static final String SESSION_USER_ROLE = "userRole";
    /** Sa-Token Session：医生表主键（与 loginId 可能不同） */
    public static final String SESSION_DOCTOR_ID = "doctorId";
    /** 登录主体：user 表 */
    public static final String LOGIN_TYPE_USER = "user";
    /** 登录主体：doctor 表 */
    public static final String LOGIN_TYPE_DOCTOR = "doctor";

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

    public static final String ADMIN_TYPE_FORMAL = "FORMAL";
    public static final String ADMIN_TYPE_TEMPORARY = "TEMPORARY";
}