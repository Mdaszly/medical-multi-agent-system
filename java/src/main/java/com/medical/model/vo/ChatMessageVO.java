package com.medical.model.vo;

import com.medical.model.entity.ChatMessage;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChatMessageVO {

    private Long id;
    private String sessionId;
    private String role;
    private String content;
    private String agentType;
    private String riskLevel;
    private String metadataJson;
    private LocalDateTime createTime;

    public static ChatMessageVO fromEntity(ChatMessage message) {
        ChatMessageVO vo = new ChatMessageVO();
        vo.setId(message.getId());
        vo.setSessionId(message.getSessionId());
        vo.setRole(message.getRole());
        vo.setContent(message.getContent());
        vo.setAgentType(message.getAgentType());
        vo.setRiskLevel(message.getRiskLevel());
        vo.setMetadataJson(message.getMetadataJson());
        vo.setCreateTime(message.getCreateTime());
        return vo;
    }
}
