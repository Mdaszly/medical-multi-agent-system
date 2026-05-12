package com.medical.model.dto.schedule;

import com.medical.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
public class ScheduleQueryRequest extends PageRequest {

    private Long doctorId;

    private String doctorName;

    private String department;

    private LocalDate scheduleDate;

    private String shiftType;

    private Integer status;

    private Integer onDutyStatus;

    private LocalDate startDate;

    private LocalDate endDate;
}
