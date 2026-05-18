package com.medical.model.vo;

import com.medical.model.entity.ChatSession;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChatSessionVO {

    private String sessionId;
    private String title;
    private String scene;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public static ChatSessionVO fromEntity(ChatSession session) {
        ChatSessionVO vo = new ChatSessionVO();
        vo.setSessionId(session.getSessionId());
        vo.setTitle(session.getTitle());
        vo.setScene(session.getScene());
        vo.setCreateTime(session.getCreateTime());
        vo.setUpdateTime(session.getUpdateTime());
        return vo;
    }
}
