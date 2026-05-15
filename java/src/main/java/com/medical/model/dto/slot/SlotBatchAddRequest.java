package com.medical.model.dto.slot;

import lombok.Data;

import java.util.List;

@Data
public class SlotBatchAddRequest {

    private Long scheduleId;

    private List<SlotAddRequest> slots;
}
