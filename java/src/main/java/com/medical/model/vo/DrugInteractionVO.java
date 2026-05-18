package com.medical.model.vo;

import lombok.Data;

@Data
public class DrugInteractionVO {

    private String drugA;
    private String drugB;
    private String severity;
    private String description;
    private String recommendation;
}
