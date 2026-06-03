package com.medical.model.vo;

import com.medical.model.entity.UserNotification;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserNotificationVO {

    private Long id;
    private Long userId;
    private String title;
    private String content;
    private String bizType;
    private Long bizId;
    private String eventType;
    private Integer readStatus;
    private LocalDateTime createTime;

    public static UserNotificationVO fromEntity(UserNotification entity) {
        if (entity == null) {
            return null;
        }
        UserNotificationVO vo = new UserNotificationVO();
        vo.setId(entity.getId());
        vo.setUserId(entity.getUserId());
        vo.setTitle(entity.getTitle());
        vo.setContent(entity.getContent());
        vo.setBizType(entity.getBizType());
        vo.setBizId(entity.getBizId());
        vo.setEventType(entity.getEventType());
        vo.setReadStatus(entity.getReadStatus());
        vo.setCreateTime(entity.getCreateTime());
        return vo;
    }
}
