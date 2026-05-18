package com.medical.service;

import com.medical.constant.ConsultConstant;
import com.medical.mapper.ChatMessageMapper;
import com.medical.model.entity.ChatSession;
import com.medical.model.vo.ChatSessionVO;
import com.medical.service.impl.ChatSessionServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ChatSessionServiceTest {

    @Mock
    private ChatMessageMapper chatMessageMapper;

    @Spy
    @InjectMocks
    private ChatSessionServiceImpl chatSessionService;

    @Test
    @DisplayName("创建会话应生成 sessionId")
    void createSession() {
        doReturn(true).when(chatSessionService).save(any(ChatSession.class));

        ChatSessionVO vo = chatSessionService.createSession(100L, ConsultConstant.SCENE_TRIAGE, "头痛问诊");

        assertNotNull(vo.getSessionId());
        assertEquals(ConsultConstant.SCENE_TRIAGE, vo.getScene());
        assertEquals("头痛问诊", vo.getTitle());
        verify(chatSessionService).save(any(ChatSession.class));
    }
}
