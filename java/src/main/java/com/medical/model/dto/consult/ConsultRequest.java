package com.medical.model.dto.consult;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Map;

@Data
public class ConsultRequest {

    private String sessionId;

    private String scene;

    @NotBlank(message = "问诊内容不能为空")
    @Size(min = 2, message = "问诊内容至少2个字符")
    private String question;

    private Map<String, Object> patientContext;
}
