package com.medical.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "medical.ai")
public class MedicalAiProperties {

    /**
     * legacy: 仅使用 ClinicalPipeline
     * enhanced: 启用 MedicalPipeline 与线上问诊接口
     */
    private String chatType = "enhanced";
}
