package com.medical.task;

import com.medical.common.RedisCacheUtil;
import com.medical.constant.RedisKeyConstant;
import com.medical.constant.ScheduleConstant;
import com.medical.constant.UserConstant;
import com.medical.mapper.ScheduleMapper;
import com.medical.model.entity.Schedule;
import com.medical.model.vo.ScheduleVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class CacheWarmupTask {

    private final ScheduleMapper scheduleMapper;
    private final RedisCacheUtil redisCacheUtil;

    @Scheduled(cron = "0 0 5 * * ?")
    public void warmupScheduleCache() {
        log.info("【定时任务】缓存预热开始...");
        long startTime = System.currentTimeMillis();

        try {
            LocalDate today = LocalDate.now();
            LocalDate endDate = today.plusDays(7);

            LambdaQueryWrapper<Schedule> wrapper = new LambdaQueryWrapper<>();
            wrapper.ge(Schedule::getScheduleDate, today)
                   .le(Schedule::getScheduleDate, endDate)
                   .eq(Schedule::getStatus, ScheduleConstant.STATUS_AVAILABLE)
                   .eq(Schedule::getIsDelete, UserConstant.NOT_DELETED);

            List<Schedule> schedules = scheduleMapper.selectList(wrapper);

            int successCount = 0;
            int failCount = 0;

            for (Schedule schedule : schedules) {
                try {
                    String scheduleKey = String.format(RedisKeyConstant.SCHEDULE_ID, schedule.getId());
                    ScheduleVO vo = ScheduleVO.fromEntity(schedule);
                    redisCacheUtil.set(scheduleKey, vo, RedisKeyConstant.TTL_24_HOURS);

                    String deptDateKey = String.format(RedisKeyConstant.SCHEDULE_DEPT_DATE,
                            schedule.getDepartment(), schedule.getScheduleDate());
                    redisCacheUtil.delete(deptDateKey);

                    successCount++;
                } catch (Exception e) {
                    log.error("预热排班缓存失败: id={}", schedule.getId(), e);
                    failCount++;
                }
            }

            long duration = System.currentTimeMillis() - startTime;
            log.info("【定时任务】缓存预热完成，耗时{}ms，成功{}条，失败{}条", duration, successCount, failCount);

        } catch (Exception e) {
            log.error("【定时任务】缓存预热任务执行失败", e);
        }
    }

    @Scheduled(cron = "0 5 0 * * ?")
    public void resetAppointmentSlots() {
        log.info("【定时任务】号源重置开始...");

        LocalDate yesterday = LocalDate.now().minusDays(1);

        LambdaQueryWrapper<Schedule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Schedule::getScheduleDate, yesterday)
               .eq(Schedule::getIsDelete, UserConstant.NOT_DELETED);

        List<Schedule> expiredSchedules = scheduleMapper.selectList(wrapper);

        for (Schedule schedule : expiredSchedules) {
            String slotsKey = String.format(RedisKeyConstant.SCHEDULE_SLOTS, schedule.getId());
            redisCacheUtil.set(slotsKey, schedule.getMaxAppointments(), RedisKeyConstant.TTL_24_HOURS);
            log.debug("号源重置: scheduleId={}, slots={}", schedule.getId(), schedule.getMaxAppointments());
        }

        log.info("【定时任务】号源重置完成，处理{}条记录", expiredSchedules.size());
    }

    @Scheduled(fixedRate = 5 * 60 * 1000)
    public void syncDoctorStatusCache() {
        log.debug("【定时任务】医生状态缓存同步开始...");

        LocalDate today = LocalDate.now();

        LambdaQueryWrapper<Schedule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Schedule::getScheduleDate, today)
               .eq(Schedule::getStatus, ScheduleConstant.STATUS_AVAILABLE)
               .eq(Schedule::getIsDelete, UserConstant.NOT_DELETED);

        List<Schedule> todaySchedules = scheduleMapper.selectList(wrapper);

        for (Schedule schedule : todaySchedules) {
            String statusKey = String.format(RedisKeyConstant.DOCTOR_STATUS, schedule.getDoctorId());
            Integer currentStatus = redisCacheUtil.get(statusKey, Integer.class);
            
            if (currentStatus == null || !currentStatus.equals(schedule.getOnDutyStatus())) {
                redisCacheUtil.set(statusKey, schedule.getOnDutyStatus(), RedisKeyConstant.TTL_5_MINUTES);
            }
        }

        log.debug("【定时任务】医生状态缓存同步完成");
    }
}