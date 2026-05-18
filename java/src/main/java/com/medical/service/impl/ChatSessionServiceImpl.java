package com.medical.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.medical.common.ErrorCode;
import com.medical.constant.ConsultConstant;
import com.medical.exception.ThrowUtils;
import com.medical.mapper.ChatMessageMapper;
import com.medical.mapper.ChatSessionMapper;
import com.medical.model.entity.ChatMessage;
import com.medical.model.entity.ChatSession;
import com.medical.model.vo.ChatMessageVO;
import com.medical.model.vo.ChatSessionHistoryVO;
import com.medical.model.vo.ChatSessionVO;
import com.medical.service.ChatSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatSessionServiceImpl extends ServiceImpl<ChatSessionMapper, ChatSession>
        implements ChatSessionService {

    private final ChatMessageMapper chatMessageMapper;

    @Override
    public ChatSessionVO createSession(Long userId, String scene, String title) {
        ThrowUtils.throwIf(userId == null || userId <= 0, ErrorCode.PARAM_ERROR, "用户ID无效");

        String resolvedScene = StringUtils.hasText(scene) ? scene : ConsultConstant.SCENE_CONSULTATION;
        String resolvedTitle = StringUtils.hasText(title) ? title : ConsultConstant.DEFAULT_SESSION_TITLE;

        ChatSession session = new ChatSession();
        session.setSessionId(UUID.randomUUID().toString());
        session.setUserId(userId);
        session.setScene(resolvedScene);
        session.setTitle(resolvedTitle);
        save(session);
        return ChatSessionVO.fromEntity(session);
    }

    @Override
    public ChatSession getSessionForUser(String sessionId, Long userId) {
        ChatSession session = getOwnedSession(sessionId, userId);
        ThrowUtils.throwIf(session == null, ErrorCode.PARAM_ERROR, "会话不存在或无权访问");
        return session;
    }

    @Async
    @Override
    public void updateAsync(String sessionId, String title, Long userId) {
        if (!StringUtils.hasText(sessionId) || userId == null) {
            return;
        }
        ChatSession session = lambdaQuery()
                .eq(ChatSession::getSessionId, sessionId)
                .eq(ChatSession::getUserId, userId)
                .one();
        if (session == null) {
            return;
        }
        if (!StringUtils.hasText(session.getTitle()) && StringUtils.hasText(title)) {
            String trimmed = title.length() > 100 ? title.substring(0, 100) : title;
            session.setTitle(trimmed);
        }
        session.setUpdateTime(LocalDateTime.now());
        updateById(session);
        log.debug("Session title updated async: {}", sessionId);
    }

    @Override
    public ChatSessionHistoryVO queryHistory(Long userId) {
        ThrowUtils.throwIf(userId == null || userId <= 0, ErrorCode.PARAM_ERROR, "用户ID无效");

        List<ChatSession> sessions = lambdaQuery()
                .eq(ChatSession::getUserId, userId)
                .orderByDesc(ChatSession::getUpdateTime)
                .last("LIMIT " + ConsultConstant.HISTORY_MAX_SIZE)
                .list();

        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        LocalDateTime thirtyDaysAgo = startOfToday.minusDays(30);
        LocalDateTime oneYearAgo = startOfToday.minusYears(1);

        ChatSessionHistoryVO history = new ChatSessionHistoryVO();
        for (ChatSession session : sessions) {
            ChatSessionVO vo = ChatSessionVO.fromEntity(session);
            LocalDateTime updateTime = session.getUpdateTime() != null
                    ? session.getUpdateTime() : session.getCreateTime();
            if (updateTime == null) {
                history.getOlderThanYear().add(vo);
                continue;
            }
            if (!updateTime.isBefore(startOfToday)) {
                history.getToday().add(vo);
            } else if (!updateTime.isBefore(thirtyDaysAgo)) {
                history.getLast30Days().add(vo);
            } else if (!updateTime.isBefore(oneYearAgo)) {
                history.getLastYear().add(vo);
            } else {
                history.getOlderThanYear().add(vo);
            }
        }
        return history;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSession(String sessionId, Long userId) {
        ChatSession session = getOwnedSession(sessionId, userId);
        ThrowUtils.throwIf(session == null, ErrorCode.PARAM_ERROR, "会话不存在或无权访问");
        chatMessageMapper.deleteBySessionId(sessionId);
        removeById(session.getId());
    }

    @Override
    public void updateTitle(String sessionId, String title, Long userId) {
        ThrowUtils.throwIf(!StringUtils.hasText(title), ErrorCode.PARAM_ERROR, "标题不能为空");
        ChatSession session = getOwnedSession(sessionId, userId);
        ThrowUtils.throwIf(session == null, ErrorCode.PARAM_ERROR, "会话不存在或无权访问");
        session.setTitle(title.length() > 100 ? title.substring(0, 100) : title);
        session.setUpdateTime(LocalDateTime.now());
        updateById(session);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatMessageVO saveMessage(String sessionId, Long userId, String role, String content,
                                     String agentType, String riskLevel, String metadataJson) {
        ThrowUtils.throwIf(!StringUtils.hasText(sessionId), ErrorCode.PARAM_ERROR, "会话ID不能为空");
        ThrowUtils.throwIf(!StringUtils.hasText(role), ErrorCode.PARAM_ERROR, "消息角色不能为空");
        ThrowUtils.throwIf(!StringUtils.hasText(content), ErrorCode.PARAM_ERROR, "消息内容不能为空");

        ChatSession session = getOwnedSession(sessionId, userId);
        ThrowUtils.throwIf(session == null, ErrorCode.PARAM_ERROR, "会话不存在或无权访问");

        ChatMessage message = new ChatMessage();
        message.setSessionId(sessionId);
        message.setRole(role);
        message.setContent(content);
        message.setAgentType(agentType);
        message.setRiskLevel(riskLevel);
        message.setMetadataJson(metadataJson);
        chatMessageMapper.insert(message);

        session.setUpdateTime(LocalDateTime.now());
        updateById(session);

        return ChatMessageVO.fromEntity(message);
    }

    @Override
    public List<ChatMessageVO> listMessages(String sessionId, Long userId, int limit) {
        getSessionForUser(sessionId, userId);
        int resolvedLimit = limit <= 0 ? 100 : Math.min(limit, 200);

        List<ChatMessage> messages = chatMessageMapper.selectList(
                new LambdaQueryWrapper<ChatMessage>()
                        .eq(ChatMessage::getSessionId, sessionId)
                        .orderByAsc(ChatMessage::getCreateTime)
                        .last("LIMIT " + resolvedLimit)
        );
        return messages.stream().map(ChatMessageVO::fromEntity).collect(Collectors.toList());
    }

    private ChatSession getOwnedSession(String sessionId, Long userId) {
        if (!StringUtils.hasText(sessionId) || userId == null) {
            return null;
        }
        return lambdaQuery()
                .eq(ChatSession::getSessionId, sessionId)
                .eq(ChatSession::getUserId, userId)
                .one();
    }
}
