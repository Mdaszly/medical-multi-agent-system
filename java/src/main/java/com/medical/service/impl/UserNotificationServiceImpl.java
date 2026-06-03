package com.medical.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.medical.common.ErrorCode;
import com.medical.exception.ThrowUtils;
import com.medical.mapper.UserNotificationMapper;
import com.medical.model.entity.UserNotification;
import com.medical.model.vo.UserNotificationVO;
import com.medical.service.UserNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserNotificationServiceImpl implements UserNotificationService {

    private static final String BIZ_TYPE_APPOINTMENT = "APPOINTMENT";
    private static final String BIZ_TYPE_BILL = "BILL";
    private static final int READ_UNREAD = 0;
    private static final int READ_DONE = 1;
    private static final int DEFAULT_LIMIT = 50;

    private final UserNotificationMapper userNotificationMapper;

    @Override
    public List<UserNotificationVO> listForUser(Long userId, int limit) {
        ThrowUtils.throwIf(userId == null || userId <= 0, ErrorCode.PARAM_ERROR, "用户ID无效");
        int size = limit > 0 ? Math.min(limit, 100) : DEFAULT_LIMIT;
        LambdaQueryWrapper<UserNotification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserNotification::getUserId, userId)
                .orderByDesc(UserNotification::getCreateTime)
                .last("LIMIT " + size);
        return userNotificationMapper.selectList(wrapper).stream()
                .map(UserNotificationVO::fromEntity)
                .toList();
    }

    @Override
    public void markRead(Long notificationId, Long userId) {
        ThrowUtils.throwIf(notificationId == null || notificationId <= 0, ErrorCode.PARAM_ERROR, "通知ID无效");
        ThrowUtils.throwIf(userId == null || userId <= 0, ErrorCode.PARAM_ERROR, "用户ID无效");

        LambdaUpdateWrapper<UserNotification> update = new LambdaUpdateWrapper<>();
        update.eq(UserNotification::getId, notificationId)
                .eq(UserNotification::getUserId, userId)
                .eq(UserNotification::getReadStatus, READ_UNREAD)
                .set(UserNotification::getReadStatus, READ_DONE);
        int rows = userNotificationMapper.update(null, update);
        ThrowUtils.throwIf(rows == 0, ErrorCode.PARAM_ERROR, "通知不存在或已读");
    }

    @Override
    public long countUnread(Long userId) {
        ThrowUtils.throwIf(userId == null || userId <= 0, ErrorCode.PARAM_ERROR, "用户ID无效");
        LambdaQueryWrapper<UserNotification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserNotification::getUserId, userId)
                .eq(UserNotification::getReadStatus, READ_UNREAD);
        return userNotificationMapper.selectCount(wrapper);
    }

    @Override
    public void saveAppointmentNotification(Long userId, String title, String content,
                                          Long appointmentId, String eventType) {
        if (userId == null || userId <= 0 || !StringUtils.hasText(title)) {
            return;
        }
        UserNotification notification = new UserNotification();
        notification.setUserId(userId);
        notification.setTitle(title);
        notification.setContent(content != null ? content : "");
        notification.setBizType(BIZ_TYPE_APPOINTMENT);
        notification.setBizId(appointmentId);
        notification.setEventType(eventType);
        notification.setReadStatus(READ_UNREAD);
        notification.setCreateTime(LocalDateTime.now());
        userNotificationMapper.insert(notification);
    }

    @Override
    public void saveBillNotification(Long userId, String title, String content,
                                     Long billId, Long appointmentId, String eventType) {
        if (userId == null || userId <= 0 || !StringUtils.hasText(title)) {
            return;
        }
        UserNotification notification = new UserNotification();
        notification.setUserId(userId);
        notification.setTitle(title);
        notification.setContent(content != null ? content : "");
        notification.setBizType(BIZ_TYPE_BILL);
        notification.setBizId(billId);
        notification.setEventType(eventType);
        notification.setReadStatus(READ_UNREAD);
        notification.setCreateTime(LocalDateTime.now());
        userNotificationMapper.insert(notification);
    }
}
