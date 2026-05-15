package com.medical.service;

import com.medical.model.dto.slot.SlotAddRequest;
import com.medical.model.dto.slot.SlotBatchAddRequest;
import com.medical.model.entity.AppointmentSlot;
import com.medical.model.vo.AppointmentSlotVO;

import java.util.List;

public interface SlotService {

    AppointmentSlotVO addSlot(SlotAddRequest request);

    void batchAddSlots(SlotBatchAddRequest request);

    AppointmentSlotVO getSlotById(Long id);

    List<AppointmentSlotVO> getSlotsBySchedule(Long scheduleId);

    void updateSlot(Long id, SlotAddRequest request);

    void deleteSlot(Long id);

    void generateDefaultSlots(Long scheduleId);
}
