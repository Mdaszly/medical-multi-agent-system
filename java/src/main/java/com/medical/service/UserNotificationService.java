package com.medical.service;

import com.medical.model.vo.UserNotificationVO;

import java.util.List;

public interface UserNotificationService {

    List<UserNotificationVO> listForUser(Long userId, int limit);

    void markRead(Long notificationId, Long userId);

    long countUnread(Long userId);

    void saveAppointmentNotification(Long userId, String title, String content,
                                     Long appointmentId, String eventType);

    void saveBillNotification(Long userId, String title, String content,
                              Long billId, Long appointmentId, String eventType);
}
