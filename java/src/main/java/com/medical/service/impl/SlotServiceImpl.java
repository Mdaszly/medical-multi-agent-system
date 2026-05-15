package com.medical.service.impl;

import com.medical.common.ErrorCode;
import com.medical.exception.ThrowUtils;
import com.medical.constant.AppointmentConstant;
import com.medical.mapper.AppointmentSlotMapper;
import com.medical.mapper.ScheduleMapper;
import com.medical.model.dto.slot.SlotAddRequest;
import com.medical.model.dto.slot.SlotBatchAddRequest;
import com.medical.model.entity.AppointmentSlot;
import com.medical.model.entity.Schedule;
import com.medical.model.vo.AppointmentSlotVO;
import com.medical.service.SlotService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SlotServiceImpl implements SlotService {

    private final AppointmentSlotMapper slotMapper;
    private final ScheduleMapper scheduleMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AppointmentSlotVO addSlot(SlotAddRequest request) {
        log.info("添加号源: scheduleId={}, timeSlot={}", request.getScheduleId(), request.getTimeSlot());

        validateScheduleExists(request.getScheduleId());

        AppointmentSlot slot = new AppointmentSlot();
        slot.setScheduleId(request.getScheduleId());
        slot.setTimeSlot(request.getTimeSlot());
        slot.setTimeStart(request.getTimeStart());
        slot.setTimeEnd(request.getTimeEnd());
        slot.setMaxSlots(request.getMaxSlots() != null ? request.getMaxSlots() : 5);
        slot.setAvailableSlots(slot.getMaxSlots());
        slot.setLockedSlots(0);
        slot.setStatus(AppointmentConstant.SLOT_STATUS_AVAILABLE);
        slot.setVersion(0);

        slotMapper.insert(slot);
        log.info("号源添加成功: id={}", slot.getId());

        return AppointmentSlotVO.fromEntity(slot);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchAddSlots(SlotBatchAddRequest request) {
        log.info("批量添加号源: scheduleId={}, count={}", request.getScheduleId(),
                request.getSlots() != null ? request.getSlots().size() : 0);

        validateScheduleExists(request.getScheduleId());

        ThrowUtils.throwIf(request.getSlots() == null || request.getSlots().isEmpty(), ErrorCode.PARAM_ERROR, "无号源数据");

        List<AppointmentSlot> slots = new ArrayList<>();
        for (SlotAddRequest slotRequest : request.getSlots()) {
            AppointmentSlot slot = new AppointmentSlot();
            slot.setScheduleId(request.getScheduleId());
            slot.setTimeSlot(slotRequest.getTimeSlot());
            slot.setTimeStart(slotRequest.getTimeStart());
            slot.setTimeEnd(slotRequest.getTimeEnd());
            slot.setMaxSlots(slotRequest.getMaxSlots() != null ? slotRequest.getMaxSlots() : 5);
            slot.setAvailableSlots(slot.getMaxSlots());
            slot.setLockedSlots(0);
            slot.setStatus(AppointmentConstant.SLOT_STATUS_AVAILABLE);
            slot.setVersion(0);
            slots.add(slot);
        }

        for (AppointmentSlot slot : slots) {
            slotMapper.insert(slot);
        }

        log.info("批量号源添加成功: count={}", slots.size());
    }

    @Override
    public AppointmentSlotVO getSlotById(Long id) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAM_ERROR, "号源ID无效");
        AppointmentSlot slot = slotMapper.selectById(id);
        ThrowUtils.throwIf(slot == null, ErrorCode.PARAM_ERROR, "号源不存在");
        return AppointmentSlotVO.fromEntity(slot);
    }

    @Override
    public List<AppointmentSlotVO> getSlotsBySchedule(Long scheduleId) {
        ThrowUtils.throwIf(scheduleId == null || scheduleId <= 0, ErrorCode.PARAM_ERROR, "排班ID无效");
        List<AppointmentSlot> slots = slotMapper.selectByScheduleId(scheduleId);
        return slots.stream()
                .map(AppointmentSlotVO::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSlot(Long id, SlotAddRequest request) {
        log.info("更新号源: id={}", id);
        AppointmentSlot slot = slotMapper.selectById(id);
        ThrowUtils.throwIf(slot == null, ErrorCode.PARAM_ERROR, "号源不存在");

        if (StringUtils.hasText(request.getTimeSlot())) {
            slot.setTimeSlot(request.getTimeSlot());
        }
        if (StringUtils.hasText(request.getTimeStart())) {
            slot.setTimeStart(request.getTimeStart());
        }
        if (StringUtils.hasText(request.getTimeEnd())) {
            slot.setTimeEnd(request.getTimeEnd());
        }
        if (request.getMaxSlots() != null) {
            int diff = request.getMaxSlots() - slot.getMaxSlots();
            slot.setMaxSlots(request.getMaxSlots());
            slot.setAvailableSlots(slot.getAvailableSlots() + diff);
        }

        slotMapper.updateById(slot);
        log.info("号源更新成功: id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSlot(Long id) {
        log.info("删除号源: id={}", id);
        AppointmentSlot slot = slotMapper.selectById(id);
        ThrowUtils.throwIf(slot == null, ErrorCode.PARAM_ERROR, "号源不存在");
        slotMapper.deleteById(id);
        log.info("号源删除成功: id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void generateDefaultSlots(Long scheduleId) {
        log.info("生成默认号源: scheduleId={}", scheduleId);
        Schedule schedule = scheduleMapper.selectById(scheduleId);
        ThrowUtils.throwIf(schedule == null, ErrorCode.PARAM_ERROR, "排班不存在");

        List<AppointmentSlot> slots = new ArrayList<>();
        String[] times = {"08:00", "08:30", "09:00", "09:30", "10:00", "10:30",
                "11:00", "11:30", "14:00", "14:30", "15:00", "15:30",
                "16:00", "16:30", "17:00", "17:30"};

        for (int i = 0; i < times.length; i++) {
            String timeStart = times[i];
            String timeEnd = times[i + 1];
            String timeSlot = timeStart + "-" + timeEnd;

            AppointmentSlot slot = new AppointmentSlot();
            slot.setScheduleId(scheduleId);
            slot.setTimeSlot(timeSlot);
            slot.setTimeStart(timeStart);
            slot.setTimeEnd(timeEnd);
            slot.setMaxSlots(5);
            slot.setAvailableSlots(5);
            slot.setLockedSlots(0);
            slot.setStatus(AppointmentConstant.SLOT_STATUS_AVAILABLE);
            slot.setVersion(0);
            slots.add(slot);

            if (i == times.length - 2) {
                break;
            }
        }

        for (AppointmentSlot slot : slots) {
            slotMapper.insert(slot);
        }

        log.info("默认号源生成成功: count={}", slots.size());
    }

    private void validateScheduleExists(Long scheduleId) {
        ThrowUtils.throwIf(scheduleId == null || scheduleId <= 0,
                ErrorCode.PARAM_ERROR, "排班ID无效");
        Schedule schedule = scheduleMapper.selectById(scheduleId);
        ThrowUtils.throwIf(schedule == null,
                ErrorCode.PARAM_ERROR, "排班不存在");
    }
}
