package com.medical.model.dto.slot;

import lombok.Data;

import java.time.LocalDate;

@Data
public class SlotAddRequest {

    private Long scheduleId;

    private String timeSlot;

    private String timeStart;

    private String timeEnd;

    private Integer maxSlots;
}
