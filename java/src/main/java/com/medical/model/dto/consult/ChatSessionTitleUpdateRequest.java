package com.medical.model.dto.consult;

import lombok.Data;

@Data
public class ChatSessionTitleUpdateRequest {

    private String sessionId;
    private String title;
}
