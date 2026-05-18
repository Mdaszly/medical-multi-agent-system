package com.medical.service;

import com.medical.model.entity.ChatMessage;
import com.medical.model.entity.ChatSession;
import com.medical.model.vo.ChatMessageVO;
import com.medical.model.vo.ChatSessionHistoryVO;
import com.medical.model.vo.ChatSessionVO;

import java.util.List;

public interface ChatSessionService {

    ChatSessionVO createSession(Long userId, String scene, String title);

    ChatSession getSessionForUser(String sessionId, Long userId);

    void updateAsync(String sessionId, String title, Long userId);

    ChatSessionHistoryVO queryHistory(Long userId);

    void deleteSession(String sessionId, Long userId);

    void updateTitle(String sessionId, String title, Long userId);

    ChatMessageVO saveMessage(String sessionId, Long userId, String role, String content,
                              String agentType, String riskLevel, String metadataJson);

    List<ChatMessageVO> listMessages(String sessionId, Long userId, int limit);
}
