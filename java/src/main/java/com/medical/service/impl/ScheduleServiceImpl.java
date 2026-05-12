package com.medical.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.medical.common.ErrorCode;
import com.medical.constant.ScheduleConstant;
import com.medical.constant.UserConstant;
import com.medical.exception.BusinessException;
import com.medical.exception.ThrowUtils;
import com.medical.mapper.DoctorMapper;
import com.medical.mapper.ScheduleMapper;
import com.medical.model.dto.schedule.ScheduleAddRequest;
import com.medical.model.dto.schedule.ScheduleQueryRequest;
import com.medical.model.dto.schedule.ScheduleUpdateRequest;
import com.medical.model.entity.Doctor;
import com.medical.model.entity.Schedule;
import com.medical.model.vo.ScheduleVO;
import com.medical.service.ScheduleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduleServiceImpl implements ScheduleService {

    private final ScheduleMapper scheduleMapper;
    private final DoctorMapper doctorMapper;

    @Override
    public ScheduleVO getScheduleById(Long id) {
        Schedule schedule = getScheduleEntityById(id);
        return ScheduleVO.fromEntity(schedule);
    }

    @Override
    public Schedule getScheduleEntityById(Long id) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAM_ERROR, "排班ID无效");
        Schedule schedule = scheduleMapper.selectById(id);
        ThrowUtils.throwIf(schedule == null, ErrorCode.USER_NOT_FOUND);
        return schedule;
    }

    @Override
    public IPage<ScheduleVO> listSchedulePage(long current, long pageSize, ScheduleQueryRequest request) {
        log.info("List schedule page: current={}, pageSize={}, request={}", current, pageSize, request);

        Page<Schedule> page = new Page<>(current, pageSize);
        LambdaQueryWrapper<Schedule> wrapper = new LambdaQueryWrapper<>();

        if (request != null) {
            if (request.getDoctorId() != null) {
                wrapper.eq(Schedule::getDoctorId, request.getDoctorId());
            }
            if (StringUtils.hasText(request.getDoctorName())) {
                wrapper.like(Schedule::getDoctorName, request.getDoctorName());
            }
            if (StringUtils.hasText(request.getDepartment())) {
                wrapper.eq(Schedule::getDepartment, request.getDepartment());
            }
            if (request.getScheduleDate() != null) {
                wrapper.eq(Schedule::getScheduleDate, request.getScheduleDate());
            }
            if (StringUtils.hasText(request.getShiftType())) {
                wrapper.eq(Schedule::getShiftType, request.getShiftType());
            }
            if (request.getStatus() != null) {
                wrapper.eq(Schedule::getStatus, request.getStatus());
            }
            if (request.getOnDutyStatus() != null) {
                wrapper.eq(Schedule::getOnDutyStatus, request.getOnDutyStatus());
            }
            if (request.getStartDate() != null) {
                wrapper.ge(Schedule::getScheduleDate, request.getStartDate());
            }
            if (request.getEndDate() != null) {
                wrapper.le(Schedule::getScheduleDate, request.getEndDate());
            }
        }

        wrapper.eq(Schedule::getIsDelete, UserConstant.NOT_DELETED)
               .orderByDesc(Schedule::getScheduleDate)
               .orderByAsc(Schedule::getShiftType);

        IPage<Schedule> schedulePage = scheduleMapper.selectPage(page, wrapper);

        return schedulePage.convert(ScheduleVO::fromEntity);
    }

    @Override
    public List<ScheduleVO> listScheduleByDoctor(Long doctorId, LocalDate startDate, LocalDate endDate) {
        log.info("List schedule by doctor: doctorId={}, startDate={}, endDate={}", doctorId, startDate, endDate);

        ThrowUtils.throwIf(doctorId == null || doctorId <= 0, ErrorCode.PARAM_ERROR, "医生ID无效");

        List<Schedule> schedules = scheduleMapper.selectByDoctorIdAndDateRange(doctorId, startDate, endDate);

        return schedules.stream()
                .map(ScheduleVO::fromEntity)
                .sorted(Comparator.comparing(ScheduleVO::getScheduleDate)
                        .thenComparing(s -> ScheduleConstant.getShiftOrder(s.getShiftType())))
                .collect(Collectors.toList());
    }

    @Override
    public List<ScheduleVO> listScheduleByDepartment(String department, LocalDate scheduleDate) {
        log.info("List schedule by department: department={}, scheduleDate={}", department, scheduleDate);

        ThrowUtils.throwIf(!StringUtils.hasText(department), ErrorCode.PARAM_ERROR, "科室不能为空");
        ThrowUtils.throwIf(scheduleDate == null, ErrorCode.PARAM_ERROR, "排班日期不能为空");

        List<Schedule> schedules = scheduleMapper.selectByDepartmentAndDate(department, scheduleDate);

        return schedules.stream()
                .map(ScheduleVO::fromEntity)
                .sorted(Comparator.comparing(ScheduleVO::getShiftType,
                        Comparator.comparingInt(ScheduleConstant::getShiftOrder)))
                .collect(Collectors.toList());
    }

    @Override
    public List<ScheduleVO> listOnDutyDoctors(LocalDate scheduleDate, String shiftType) {
        log.info("List on duty doctors: scheduleDate={}, shiftType={}", scheduleDate, shiftType);

        ThrowUtils.throwIf(scheduleDate == null, ErrorCode.PARAM_ERROR, "排班日期不能为空");

        List<Schedule> schedules = scheduleMapper.selectOnDutyDoctors(scheduleDate, shiftType);

        return schedules.stream()
                .map(ScheduleVO::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ScheduleVO addSchedule(ScheduleAddRequest request) {
        log.info("Add schedule: doctorId={}, scheduleDate={}, shiftType={}",
                request.getDoctorId(), request.getScheduleDate(), request.getShiftType());

        validateScheduleRequest(request);

        if (checkScheduleConflict(request.getDoctorId(), request.getScheduleDate(), request.getShiftType())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "医生在该时段已有排班，无法重复排班");
        }

        Doctor doctor = doctorMapper.selectById(request.getDoctorId());
        ThrowUtils.throwIf(doctor == null, ErrorCode.USER_NOT_FOUND, "医生不存在");

        Schedule schedule = buildSchedule(request, doctor);

        int result = scheduleMapper.insert(schedule);
        ThrowUtils.throwIf(result == 0, ErrorCode.SYSTEM_ERROR, "添加排班失败");

        updateDoctorOnDutyStatus(request.getDoctorId());

        log.info("Schedule added successfully: id={}", schedule.getId());
        return ScheduleVO.fromEntity(schedule);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ScheduleVO updateSchedule(Long id, ScheduleUpdateRequest request) {
        log.info("Update schedule: id={}, request={}", id, request);

        Schedule schedule = getScheduleEntityById(id);

        Long oldDoctorId = schedule.getDoctorId();

        if (request.getDoctorId() != null && !request.getDoctorId().equals(oldDoctorId)) {
            Doctor doctor = doctorMapper.selectById(request.getDoctorId());
            ThrowUtils.throwIf(doctor == null, ErrorCode.USER_NOT_FOUND, "医生不存在");

            if (checkScheduleConflict(request.getDoctorId(),
                    request.getScheduleDate() != null ? request.getScheduleDate() : schedule.getScheduleDate(),
                    request.getShiftType() != null ? request.getShiftType() : schedule.getShiftType())) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "医生在该时段已有排班");
            }

            schedule.setDoctorId(request.getDoctorId());
            schedule.setDoctorName(doctor.getDoctorName());
            schedule.setDepartment(doctor.getDepartment());
        }

        if (request.getScheduleDate() != null) {
            schedule.setScheduleDate(request.getScheduleDate());
        }
        if (request.getShiftType() != null) {
            schedule.setShiftType(request.getShiftType());
            schedule.setShiftName(ScheduleConstant.getShiftDescription(request.getShiftType()));
            schedule.setTimeRange(ScheduleConstant.getShiftTimeRange(request.getShiftType()));
            
            String[] timeParts = schedule.getTimeRange().split("-");
            schedule.setTimeStart(timeParts[0]);
            schedule.setTimeEnd(timeParts[1]);
        }
        if (request.getMaxAppointments() != null) {
            schedule.setMaxAppointments(request.getMaxAppointments());
        }
        if (request.getStatus() != null) {
            schedule.setStatus(request.getStatus());
        }
        if (request.getDescription() != null) {
            schedule.setDescription(request.getDescription());
        }

        schedule.setUpdateTime(LocalDateTime.now());

        int result = scheduleMapper.updateById(schedule);
        ThrowUtils.throwIf(result == 0, ErrorCode.SYSTEM_ERROR, "更新排班失败");

        updateDoctorOnDutyStatus(oldDoctorId);
        if (request.getDoctorId() != null && !request.getDoctorId().equals(oldDoctorId)) {
            updateDoctorOnDutyStatus(request.getDoctorId());
        }

        log.info("Schedule updated successfully: id={}", id);
        return ScheduleVO.fromEntity(schedule);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSchedule(Long id) {
        log.info("Delete schedule: id={}", id);

        Schedule schedule = getScheduleEntityById(id);
        Long doctorId = schedule.getDoctorId();

        schedule.setIsDelete(UserConstant.IS_DELETED);
        schedule.setUpdateTime(LocalDateTime.now());

        int result = scheduleMapper.updateById(schedule);
        ThrowUtils.throwIf(result == 0, ErrorCode.SYSTEM_ERROR, "删除排班失败");

        updateDoctorOnDutyStatus(doctorId);

        log.info("Schedule deleted successfully: id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchAddSchedules(List<ScheduleAddRequest> requests) {
        log.info("Batch add schedules: count={}", requests.size());

        ThrowUtils.throwIf(requests == null || requests.isEmpty(), ErrorCode.PARAM_ERROR, "排班列表不能为空");

        Set<Long> doctorIds = new HashSet<>();

        for (ScheduleAddRequest request : requests) {
            validateScheduleRequest(request);

            if (checkScheduleConflict(request.getDoctorId(), request.getScheduleDate(), request.getShiftType())) {
                throw new BusinessException(ErrorCode.PARAM_ERROR,
                        String.format("医生ID:%d 在 %s %s 已有排班",
                                request.getDoctorId(), request.getScheduleDate(), request.getShiftType()));
            }

            Doctor doctor = doctorMapper.selectById(request.getDoctorId());
            ThrowUtils.throwIf(doctor == null, ErrorCode.USER_NOT_FOUND, "医生不存在: " + request.getDoctorId());

            Schedule schedule = buildSchedule(request, doctor);
            scheduleMapper.insert(schedule);
            doctorIds.add(request.getDoctorId());
        }

        for (Long doctorId : doctorIds) {
            updateDoctorOnDutyStatus(doctorId);
        }

        log.info("Batch schedules added successfully: count={}", requests.size());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateDoctorOnDutyStatus(Long doctorId) {
        ThrowUtils.throwIf(doctorId == null || doctorId <= 0, ErrorCode.PARAM_ERROR, "医生ID无效");

        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();

        LambdaQueryWrapper<Schedule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Schedule::getDoctorId, doctorId)
               .eq(Schedule::getStatus, ScheduleConstant.STATUS_AVAILABLE)
               .eq(Schedule::getIsDelete, UserConstant.NOT_DELETED);

        List<Schedule> todaySchedules = scheduleMapper.selectList(wrapper);

        Integer onDutyStatus = ScheduleConstant.OFF_DUTY;

        for (Schedule schedule : todaySchedules) {
            if (isOnDutyNow(schedule, now)) {
                onDutyStatus = ScheduleConstant.ON_DUTY;
                break;
            }
        }

        Doctor doctor = doctorMapper.selectById(doctorId);
        if (doctor != null && !onDutyStatus.equals(doctor.getWorkStatus())) {
            doctor.setWorkStatus(onDutyStatus);
            doctor.setUpdateTime(LocalDateTime.now());
            doctorMapper.updateById(doctor);
            log.info("Doctor on duty status updated: doctorId={}, status={}", doctorId, onDutyStatus);
        }
    }

    @Override
    public boolean checkScheduleConflict(Long doctorId, LocalDate scheduleDate, String shiftType) {
        ThrowUtils.throwIf(doctorId == null || doctorId <= 0, ErrorCode.PARAM_ERROR, "医生ID无效");
        ThrowUtils.throwIf(scheduleDate == null, ErrorCode.PARAM_ERROR, "排班日期不能为空");
        ThrowUtils.throwIf(!StringUtils.hasText(shiftType), ErrorCode.PARAM_ERROR, "班次类型不能为空");

        int count = scheduleMapper.countByDoctorIdAndDateAndShift(doctorId, scheduleDate, shiftType);

        return count > 0;
    }

    @Override
    public Map<String, Object> getDoctorLoadBalance(String department) {
        log.info("Get doctor load balance: department={}", department);

        Map<String, Object> result = new HashMap<>();

        LambdaQueryWrapper<Doctor> doctorWrapper = new LambdaQueryWrapper<>();
        doctorWrapper.eq(Doctor::getDepartment, department)
                     .eq(Doctor::getWorkStatus, UserConstant.DOCTOR_STATUS_ONLINE)
                     .eq(Doctor::getIsDelete, UserConstant.NOT_DELETED);

        List<Doctor> doctors = doctorMapper.selectList(doctorWrapper);

        List<Map<String, Object>> doctorLoads = new ArrayList<>();

        for (Doctor doctor : doctors) {
            Map<String, Object> loadInfo = new HashMap<>();
            loadInfo.put("doctorId", doctor.getId());
            loadInfo.put("doctorName", doctor.getDoctorName());

            LocalDate today = LocalDate.now();
            LocalDate endDate = today.plusDays(7);

            List<Schedule> schedules = scheduleMapper.selectByDoctorIdAndDateRange(doctor.getId(), today, endDate);

            int totalAppointments = schedules.stream()
                    .mapToInt(Schedule::getCurrentAppointments)
                    .sum();

            int totalMaxAppointments = schedules.stream()
                    .mapToInt(Schedule::getMaxAppointments)
                    .sum();

            double loadRate = totalMaxAppointments > 0 ?
                    (double) totalAppointments / totalMaxAppointments * 100 : 0;

            loadInfo.put("totalSchedules", schedules.size());
            loadInfo.put("totalAppointments", totalAppointments);
            loadInfo.put("totalMaxAppointments", totalMaxAppointments);
            loadInfo.put("loadRate", String.format("%.1f%%", loadRate));

            doctorLoads.add(loadInfo);
        }

        doctorLoads.sort(Comparator.comparingDouble(m -> {
            String rate = (String) m.get("loadRate");
            return Double.parseDouble(rate.replace("%", ""));
        }));

        result.put("department", department);
        result.put("totalDoctors", doctors.size());
        result.put("doctorLoads", doctorLoads);

        if (!doctorLoads.isEmpty()) {
            Map<String, Object> mostAvailable = doctorLoads.get(0);
            result.put("mostAvailableDoctor", mostAvailable);
        }

        return result;
    }

    private void validateScheduleRequest(ScheduleAddRequest request) {
        ThrowUtils.throwIf(request.getDoctorId() == null || request.getDoctorId() <= 0,
                ErrorCode.PARAM_ERROR, "医生ID无效");
        ThrowUtils.throwIf(request.getScheduleDate() == null,
                ErrorCode.PARAM_ERROR, "排班日期不能为空");
        ThrowUtils.throwIf(!StringUtils.hasText(request.getShiftType()),
                ErrorCode.PARAM_ERROR, "班次类型不能为空");

        if (!Arrays.asList(ScheduleConstant.SHIFT_MORNING,
                ScheduleConstant.SHIFT_AFTERNOON,
                ScheduleConstant.SHIFT_EVENING).contains(request.getShiftType())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "无效的班次类型");
        }

        if (request.getMaxAppointments() != null && request.getMaxAppointments() <= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "最大预约数必须大于0");
        }
    }

    private Schedule buildSchedule(ScheduleAddRequest request, Doctor doctor) {
        Schedule schedule = new Schedule();
        schedule.setDoctorId(request.getDoctorId());
        schedule.setDoctorName(doctor.getDoctorName());
        schedule.setDepartment(doctor.getDepartment());
        schedule.setScheduleDate(request.getScheduleDate());
        schedule.setShiftType(request.getShiftType());
        schedule.setShiftName(ScheduleConstant.getShiftDescription(request.getShiftType()));
        schedule.setTimeRange(ScheduleConstant.getShiftTimeRange(request.getShiftType()));

        String[] timeParts = schedule.getTimeRange().split("-");
        schedule.setTimeStart(timeParts[0]);
        schedule.setTimeEnd(timeParts[1]);

        schedule.setMaxAppointments(request.getMaxAppointments() != null ?
                request.getMaxAppointments() : 20);
        schedule.setCurrentAppointments(0);
        schedule.setStatus(ScheduleConstant.STATUS_AVAILABLE);

        schedule.setOnDutyStatus(isOnDutyNow(schedule, LocalTime.now()) ?
                ScheduleConstant.ON_DUTY : ScheduleConstant.OFF_DUTY);

        schedule.setDescription(request.getDescription());

        return schedule;
    }

    private boolean isOnDutyNow(Schedule schedule, LocalTime now) {
        if (schedule.getScheduleDate() == null || !schedule.getScheduleDate().equals(LocalDate.now())) {
            return false;
        }

        String timeStart = schedule.getTimeStart();
        String timeEnd = schedule.getTimeEnd();

        if (!StringUtils.hasText(timeStart) || !StringUtils.hasText(timeEnd)) {
            return false;
        }

        LocalTime start = LocalTime.parse(timeStart);
        LocalTime end = LocalTime.parse(timeEnd);

        if (end.isBefore(start)) {
            return now.isAfter(start) || now.isBefore(end);
        } else {
            return !now.isBefore(start) && !now.isAfter(end);
        }
    }
}
