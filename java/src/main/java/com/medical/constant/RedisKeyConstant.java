package com.medical.constant;

import java.time.Duration;

public final class RedisKeyConstant {

    private RedisKeyConstant() {}

    /**
     * 用户缓存相关
     */
    public static final String USER_CACHE_PREFIX = "user:cache:";
    public static final Duration USER_CACHE_TTL = Duration.ofMinutes(30);

    /**
     * 排班相关Key常量
     */
    public static final String SCHEDULE_ID = "schedule:%d";
    public static final String SCHEDULE_DEPT_DATE = "schedule:dept:%s:date:%s";
    public static final String SCHEDULE_DOCTOR_DATE = "schedule:doctor:%d:date:%s";
    public static final String SCHEDULE_SLOTS = "schedule:slots:%d";

    /**
     * 医生相关Key常量
     */
    public static final String DOCTOR_STATUS = "doctor:status:%d";
    public static final String DOCTOR_ID = "doctor:%d";

    /**
     * 锁相关Key常量
     */
    public static final String LOCK_SCHEDULE = "lock:schedule:doctor:%d:date:%s:shift:%s";

    /**
     * 通用TTL配置
     */
    public static final Duration TTL_1_HOUR = Duration.ofHours(1);
    public static final Duration TTL_2_HOURS = Duration.ofHours(2);
    public static final Duration TTL_24_HOURS = Duration.ofHours(24);
    public static final Duration TTL_5_MINUTES = Duration.ofMinutes(5);
    public static final Duration TTL_30_MINUTES = Duration.ofMinutes(30);

    /**
     * 构建用户缓存Key
     */
    public static String buildUserCacheKey(Long userId) {
        return USER_CACHE_PREFIX + userId;
    }
}