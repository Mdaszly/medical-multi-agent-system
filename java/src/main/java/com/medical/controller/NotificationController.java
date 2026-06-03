package com.medical.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.medical.annotation.AuthCheck;
import com.medical.common.BaseResponse;
import com.medical.common.ResultUtils;
import com.medical.constant.UserConstant;
import com.medical.model.vo.UserNotificationVO;
import com.medical.service.UserNotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/notification")
@RequiredArgsConstructor
@Tag(name = "用户通知", description = "站内通知（预约等领域事件）")
public class NotificationController {

    private final UserNotificationService userNotificationService;

    @GetMapping("/list")
    @Operation(summary = "当前用户通知列表")
    @AuthCheck(mustRoles = {UserConstant.USER_ROLE, UserConstant.DOCTOR_ROLE, UserConstant.ADMIN_ROLE})
    public BaseResponse<List<UserNotificationVO>> listNotifications(
            @Parameter(description = "条数上限，默认50，最大100")
            @RequestParam(value = "limit", required = false, defaultValue = "50") int limit) {
        Long userId = StpUtil.getLoginIdAsLong();
        return ResultUtils.success(userNotificationService.listForUser(userId, limit));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "当前用户未读通知数")
    @AuthCheck(mustRoles = {UserConstant.USER_ROLE, UserConstant.DOCTOR_ROLE, UserConstant.ADMIN_ROLE})
    public BaseResponse<Long> unreadCount() {
        Long userId = StpUtil.getLoginIdAsLong();
        return ResultUtils.success(userNotificationService.countUnread(userId));
    }

    @PutMapping("/{id}/read")
    @Operation(summary = "标记通知已读")
    @AuthCheck(mustRoles = {UserConstant.USER_ROLE, UserConstant.DOCTOR_ROLE, UserConstant.ADMIN_ROLE})
    public BaseResponse<Void> markRead(@PathVariable("id") Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        userNotificationService.markRead(id, userId);
        return ResultUtils.success(null);
    }
}
