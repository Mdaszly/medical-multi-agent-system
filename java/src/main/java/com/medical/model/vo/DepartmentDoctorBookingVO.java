package com.medical.model.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class DepartmentDoctorBookingVO {

    private Long doctorId;

    private String doctorName;

    private String title;

    private String specialty;

    private String description;

    private BigDecimal consultationFee;

    private Integer morningRemaining;

    private Integer afternoonRemaining;

    private Integer eveningRemaining;

    private Integer totalRemaining;

    private boolean bookable;

    private boolean hasSchedule;
}
