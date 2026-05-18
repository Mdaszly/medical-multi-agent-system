package com.medical.model.dto.consult;

import lombok.Data;

@Data
public class ChatMessageSaveRequest {

    private String sessionId;
    private String role;
    private String content;
    private String agentType;
    private String riskLevel;
    private String metadataJson;
}
