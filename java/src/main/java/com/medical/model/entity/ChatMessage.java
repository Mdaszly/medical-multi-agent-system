package com.medical.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("chat_message")
public class ChatMessage {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String sessionId;

    private String role;

    private String content;

    private String agentType;

    private String riskLevel;

    private String metadataJson;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
