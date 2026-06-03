package com.medical.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_notification")
public class UserNotification {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String title;

    private String content;

    private String bizType;

    private Long bizId;

    private String eventType;

    /** 0-未读，1-已读 */
    private Integer readStatus;

    private LocalDateTime createTime;
}
