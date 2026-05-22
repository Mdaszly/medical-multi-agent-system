package com.medical.model.vo;

import lombok.Data;

import java.time.LocalDate;

@Data
public class DepartmentDateStatusVO {

    private LocalDate scheduleDate;

    private String weekDayLabel;

    private Integer dayOfMonth;

    private boolean hasAvailable;

    private boolean allFull;
}
