package com.medical.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AuthRegisterVO {

    private Long id;

    private String userAccount;

    private String userName;

    private String userRole;

    private LocalDateTime createTime;
}