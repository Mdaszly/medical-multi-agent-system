package com.medical.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "medical.ai.graph")
public class MedicalGraphProperties {

    private boolean enabled = true;
    private boolean preEnrich = true;
    private boolean validateIcd = true;
    private int fuzzySymptomLimit = 5;
    private boolean fallbackToStaticIcd = true;
    private boolean bootstrapOnStartup = true;
    private boolean syncToRdbOnStartup = false;

}
