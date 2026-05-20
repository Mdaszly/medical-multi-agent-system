package com.medical.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.medical.annotation.AuthCheck;
import com.medical.common.BaseResponse;
import com.medical.common.ResultUtils;
import com.medical.constant.UserConstant;
import com.medical.model.dto.consult.ChatMessageSaveRequest;
import com.medical.model.dto.consult.ChatSessionCreateRequest;
import com.medical.model.dto.consult.ChatSessionTitleUpdateRequest;
import com.medical.model.dto.consult.DeleteSessionRequest;
import com.medical.model.vo.ChatMessageVO;
import com.medical.model.vo.ChatSessionHistoryVO;
import com.medical.model.vo.ChatSessionVO;
import com.medical.service.ChatSessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/consult/sessions")
@RequiredArgsConstructor
@Tag(name = "线上问诊会话", description = "问诊会话与消息管理")
public class ChatSessionController {

    private final ChatSessionService chatSessionService;

    @PostMapping
    @Operation(summary = "创建会话")
    @AuthCheck(mustRoles = {UserConstant.USER_ROLE, UserConstant.DOCTOR_ROLE, UserConstant.ADMIN_ROLE})
    public BaseResponse<ChatSessionVO> createSession(@RequestBody(required = false) ChatSessionCreateRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        String scene = request != null ? request.getScene() : null;
        String title = request != null ? request.getTitle() : null;
        return ResultUtils.success(chatSessionService.createSession(userId, scene, title));
    }

    @GetMapping("/history")
    @Operation(summary = "查询历史会话")
    @AuthCheck(mustRoles = {UserConstant.USER_ROLE, UserConstant.DOCTOR_ROLE, UserConstant.ADMIN_ROLE})
    public BaseResponse<ChatSessionHistoryVO> queryHistory() {
        Long userId = StpUtil.getLoginIdAsLong();
        return ResultUtils.success(chatSessionService.queryHistory(userId));
    }

    @PutMapping("/title")
    @Operation(summary = "更新会话标题")
    @AuthCheck(mustRoles = {UserConstant.USER_ROLE, UserConstant.DOCTOR_ROLE, UserConstant.ADMIN_ROLE})
    public BaseResponse<Boolean> updateTitle(@RequestBody ChatSessionTitleUpdateRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        chatSessionService.updateTitle(request.getSessionId(), request.getTitle(), userId);
        return ResultUtils.success(true);
    }

    @PostMapping("/delete")
    @Operation(summary = "删除会话")
    @AuthCheck(mustRoles = {UserConstant.USER_ROLE, UserConstant.DOCTOR_ROLE, UserConstant.ADMIN_ROLE})
    public BaseResponse<Boolean> deleteSession(@RequestBody DeleteSessionRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        chatSessionService.deleteSession(request.getSessionId(), userId);
        return ResultUtils.success(true);
    }

    @GetMapping("/{sessionId}/messages")
    @Operation(summary = "查询会话消息")
    @AuthCheck(mustRoles = {UserConstant.USER_ROLE, UserConstant.DOCTOR_ROLE, UserConstant.ADMIN_ROLE})
    public BaseResponse<List<ChatMessageVO>> listMessages(
            @PathVariable String sessionId,
            @RequestParam(defaultValue = "100") int limit) {
        Long userId = StpUtil.getLoginIdAsLong();
        return ResultUtils.success(chatSessionService.listMessages(sessionId, userId, limit));
    }

    @PostMapping("/messages")
    @Operation(summary = "保存消息")
    @AuthCheck(mustRoles = {UserConstant.USER_ROLE, UserConstant.DOCTOR_ROLE, UserConstant.ADMIN_ROLE})
    public BaseResponse<ChatMessageVO> saveMessage(@RequestBody ChatMessageSaveRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        return ResultUtils.success(chatSessionService.saveMessage(
                request.getSessionId(),
                userId,
                request.getRole(),
                request.getContent(),
                request.getAgentType(),
                request.getRiskLevel(),
                request.getMetadataJson()
        ));
    }
}
