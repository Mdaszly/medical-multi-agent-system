package com.medical;

import com.medical.common.RedisCacheUtil;
import com.medical.constant.RedisKeyConstant;
import com.medical.model.vo.ScheduleVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class RedisCacheTest {

    @Autowired
    private RedisCacheUtil redisCacheUtil;

    private static final String TEST_KEY = "test:schedule:1";

    @BeforeEach
    void setUp() {
        redisCacheUtil.delete(TEST_KEY);
    }

    @Test
    @DisplayName("测试Redis缓存写入和读取")
    void testSetAndGet() {
        ScheduleVO scheduleVO = new ScheduleVO();
        scheduleVO.setId(1L);
        scheduleVO.setDoctorId(100L);
        scheduleVO.setDoctorName("张三");
        scheduleVO.setDepartment("内科");

        redisCacheUtil.set(TEST_KEY, scheduleVO, Duration.ofMinutes(5));

        ScheduleVO cached = redisCacheUtil.get(TEST_KEY, ScheduleVO.class);
        
        assertNotNull(cached);
        assertEquals(1L, cached.getId());
        assertEquals("张三", cached.getDoctorName());
        assertEquals("内科", cached.getDepartment());
    }

    @Test
    @DisplayName("测试Redis缓存不存在时返回null")
    void testGetNotExistKey() {
        ScheduleVO cached = redisCacheUtil.get("not:exist:key", ScheduleVO.class);
        assertNull(cached);
    }

    @Test
    @DisplayName("测试Redis缓存删除")
    void testDelete() {
        ScheduleVO scheduleVO = new ScheduleVO();
        scheduleVO.setId(1L);
        
        redisCacheUtil.set(TEST_KEY, scheduleVO, Duration.ofMinutes(5));
        assertTrue(redisCacheUtil.hasKey(TEST_KEY));
        
        redisCacheUtil.delete(TEST_KEY);
        assertFalse(redisCacheUtil.hasKey(TEST_KEY));
    }

    @Test
    @DisplayName("测试Redis计数器递增")
    void testIncrement() {
        String counterKey = "test:counter";
        redisCacheUtil.delete(counterKey);
        
        Long result1 = redisCacheUtil.increment(counterKey);
        assertEquals(1L, result1);
        
        Long result2 = redisCacheUtil.increment(counterKey);
        assertEquals(2L, result2);
        
        redisCacheUtil.delete(counterKey);
    }

    @Test
    @DisplayName("测试Redis计数器递减")
    void testDecrement() {
        String counterKey = "test:counter";
        redisCacheUtil.set(counterKey, 5, Duration.ofMinutes(5));
        
        Long result = redisCacheUtil.decrement(counterKey);
        assertEquals(4L, result);
        
        redisCacheUtil.delete(counterKey);
    }

    @Test
    @DisplayName("测试Redis分布式锁")
    void testSetIfAbsent() {
        String lockKey = "test:lock";
        
        Boolean result1 = redisCacheUtil.setIfAbsent(lockKey, "locked", Duration.ofMinutes(5));
        assertTrue(result1);
        
        Boolean result2 = redisCacheUtil.setIfAbsent(lockKey, "locked", Duration.ofMinutes(5));
        assertFalse(result2);
        
        redisCacheUtil.delete(lockKey);
    }

    @Test
    @DisplayName("测试Key常量格式")
    void testKeyConstantFormat() {
        String scheduleKey = String.format(RedisKeyConstant.SCHEDULE_ID, 123L);
        assertEquals("schedule:123", scheduleKey);
        
        String deptDateKey = String.format(RedisKeyConstant.SCHEDULE_DEPT_DATE, "内科", "2026-05-13");
        assertEquals("schedule:dept:内科:date:2026-05-13", deptDateKey);
        
        String doctorStatusKey = String.format(RedisKeyConstant.DOCTOR_STATUS, 100L);
        assertEquals("doctor:status:100", doctorStatusKey);
    }
}