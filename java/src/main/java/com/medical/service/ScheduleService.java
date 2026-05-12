package com.medical.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.medical.model.dto.schedule.ScheduleAddRequest;
import com.medical.model.dto.schedule.ScheduleQueryRequest;
import com.medical.model.dto.schedule.ScheduleUpdateRequest;
import com.medical.model.entity.Schedule;
import com.medical.model.vo.ScheduleVO;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface ScheduleService {

    ScheduleVO getScheduleById(Long id);

    Schedule getScheduleEntityById(Long id);

    IPage<ScheduleVO> listSchedulePage(long current, long pageSize, ScheduleQueryRequest request);

    List<ScheduleVO> listScheduleByDoctor(Long doctorId, LocalDate startDate, LocalDate endDate);

    List<ScheduleVO> listScheduleByDepartment(String department, LocalDate scheduleDate);

    List<ScheduleVO> listOnDutyDoctors(LocalDate scheduleDate, String shiftType);

    ScheduleVO addSchedule(ScheduleAddRequest request);

    ScheduleVO updateSchedule(Long id, ScheduleUpdateRequest request);

    void deleteSchedule(Long id);

    void batchAddSchedules(List<ScheduleAddRequest> requests);

    void updateDoctorOnDutyStatus(Long doctorId);

    boolean checkScheduleConflict(Long doctorId, LocalDate scheduleDate, String shiftType);

    Map<String, Object> getDoctorLoadBalance(String department);
}
