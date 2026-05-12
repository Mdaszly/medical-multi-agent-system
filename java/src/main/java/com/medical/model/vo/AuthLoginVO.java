package com.medical.model.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class AuthLoginVO {

    private Long id;

    private String token;

    private String userAccount;

    private String userName;

    private String userRole;

    private LocalDateTime expireTime;

    private RoleInfo roleInfo;

    @Data
    public static class RoleInfo {
        private String department;
        private String title;
        private BigDecimal consultationFee;
        private List<String> permissions;
    }
}