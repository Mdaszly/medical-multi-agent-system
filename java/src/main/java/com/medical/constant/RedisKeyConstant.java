package com.medical.constant;

import java.time.Duration;

/**
 * Redis键常量类
 *
 * <p>定义所有Redis缓存键的前缀和过期时间配置。
 * 使用常量可以避免硬编码，提高代码的可维护性。
 *
 * <p>Redis键命名规范：
 * - 使用冒号":"分隔层级
 * - 格式：{模块}:{类型}:{标识}
 * - 例如：doctor:dept:内科
 */
public final class RedisKeyConstant {

    private RedisKeyConstant() {
        // 私有构造函数，防止实例化
    }

    /**
     * 用户缓存相关
     */
    public static final String USER_CACHE_PREFIX = "user:cache:";
    public static final String USER_INFO = "user:info:%d";
    public static final Duration USER_CACHE_TTL = Duration.ofMinutes(30);

    /**
     * 账单缓存相关
     */
    public static final String BILL_ID = "bill:id:%d";
    public static final String BILL_NO = "bill:no:%s";
    public static final String BILL_USER_LIST = "bill:user:%d";
    public static final Duration BILL_CACHE_TTL = Duration.ofMinutes(30);

    /**
     * 排班相关Key常量
     *
     * <p>SCHEDULE_ID: 单个排班信息缓存
     * SCHEDULE_DEPT_DATE: 按科室和日期查询排班列表
     * SCHEDULE_DOCTOR_DATE: 按医生和日期查询排班列表
     * SCHEDULE_SLOTS: 号源剩余数量缓存
     */
    public static final String SCHEDULE_ID = "schedule:%d";
    public static final String SCHEDULE_DEPT_DATE = "schedule:dept:%s:date:%s";
    public static final String SCHEDULE_DOCTOR_DATE = "schedule:doctor:%d:date:%s";
    public static final String SCHEDULE_SLOTS = "schedule:slots:%d";

    /**
     * 医生相关Key常量
     *
     * <p>DOCTOR_STATUS: 医生在岗状态缓存
     * DOCTOR_ID: 单个医生信息缓存
     * DOCTOR_DEPT_LIST: 按科室查询医生列表缓存
     * DOCTOR_DEPT_EMPTY: 空结果缓存（防止缓存穿透）
     * DOCTOR_LOCK_PREFIX: 分布式锁前缀
     */
    public static final String DOCTOR_STATUS = "doctor:status:%d";
    public static final String DOCTOR_ID = "doctor:%d";
    public static final String DOCTOR_DEPT_LIST = "doctor:dept:%s";
    public static final String DOCTOR_DEPT_EMPTY = "doctor:dept:empty:%s";
    public static final String DOCTOR_LOCK_PREFIX = "lock:doctor:dept:%s";

    /**
     * 医生缓存TTL配置
     *
     * <p>DOCTOR_CACHE_TTL: 基准过期时间（30分钟）
     * DOCTOR_CACHE_TTL_MIN: 最小过期时间（25分钟）
     * DOCTOR_CACHE_TTL_MAX: 最大过期时间（35分钟）
     * DOCTOR_EMPTY_CACHE_TTL: 空结果缓存过期时间（5分钟）
     *
     * <p>使用随机TTL范围可以防止缓存雪崩。
     */
    public static final Duration DOCTOR_CACHE_TTL = Duration.ofMinutes(30);
    public static final Duration DOCTOR_CACHE_TTL_MIN = Duration.ofMinutes(25);
    public static final Duration DOCTOR_CACHE_TTL_MAX = Duration.ofMinutes(35);
    public static final Duration DOCTOR_EMPTY_CACHE_TTL = Duration.ofMinutes(5);

    /**
     * 锁相关Key常量
     */
    public static final String LOCK_SCHEDULE = "lock:schedule:doctor:%d:date:%s:shift:%s";
    public static final String LOCK_PAYMENT = "lock:payment:user:%d:payment:%d";
    public static final String LOCK_REFRESH = "lock:refresh:%s";

    /**
     * 通用TTL配置
     */
    public static final Duration TTL_1_HOUR = Duration.ofHours(1);
    public static final Duration TTL_2_HOURS = Duration.ofHours(2);
    public static final Duration TTL_24_HOURS = Duration.ofHours(24);
    public static final Duration TTL_5_MINUTES = Duration.ofMinutes(5);
    public static final Duration TTL_30_MINUTES = Duration.ofMinutes(30);

    /**
     * MQ 消费幂等键：{@code mq:processed:{eventId}}，TTL {@link #MQ_PROCESSED_TTL}。
     * slot/audit 消费者会在 eventId 前加 {@code slot:}/{@code audit:} 前缀避免与 notification 冲突。
     */
    public static final String MQ_PROCESSED = "mq:processed:%s";
    public static final Duration MQ_PROCESSED_TTL = Duration.ofDays(7);

    /**
     * 就诊提醒已成功写入通知表后标记；Scheduler 用 hasKey 减少重复 publish。
     */
    public static final String MQ_REMINDER_SENT = "mq:reminder:sent:%d";
    public static final Duration MQ_REMINDER_SENT_TTL = Duration.ofDays(3);

    /**
     * 构建用户缓存Key
     *
     * @param userId 用户ID
     * @return 完整的缓存Key
     */
    public static String buildUserCacheKey(Long userId) {
        return USER_CACHE_PREFIX + userId;
    }
}