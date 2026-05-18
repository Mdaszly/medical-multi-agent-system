package com.medical.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.medical.annotation.AuthCheck;
import com.medical.common.BaseResponse;
import com.medical.common.ResultUtils;
import com.medical.constant.UserConstant;
import com.medical.model.dto.consult.ConsultRequest;
import com.medical.model.vo.ConsultVO;
import com.medical.service.ConsultOrchestrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/v1/consult")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "medical.ai", name = "chat-type", havingValue = "enhanced")
@Tag(name = "线上问诊", description = "AI 问诊接口")
public class ConsultController {

    private final ConsultOrchestrationService consultOrchestrationService;

    @PostMapping
    @Operation(summary = "提交问诊", description = "路由 Agent + 专业 Agent 同步推理")
    @AuthCheck(mustRoles = {UserConstant.USER_ROLE, UserConstant.DOCTOR_ROLE, UserConstant.ADMIN_ROLE})
    public BaseResponse<ConsultVO> consult(@Valid @RequestBody ConsultRequest request) throws Exception {
        Long userId = StpUtil.getLoginIdAsLong();
        return ResultUtils.success(consultOrchestrationService.consultSync(request, userId));
    }

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "流式问诊", description = "百炼 SSE 流式输出，事件：chunk / done / error")
    @AuthCheck(mustRoles = {UserConstant.USER_ROLE, UserConstant.DOCTOR_ROLE, UserConstant.ADMIN_ROLE})
    public SseEmitter consultStream(@Valid @RequestBody ConsultRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        return consultOrchestrationService.consultStream(request, userId);
    }
}
