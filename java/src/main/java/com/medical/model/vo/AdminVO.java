package com.medical.model.vo;

import com.medical.model.entity.User;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdminVO {

    private Long id;

    private String userAccount;

    private String userName;

    private String phone;

    private String email;

    private String userRole;

    private Integer userStatus;

    private LocalDateTime lastLoginTime;

    private LocalDateTime createTime;

    public static AdminVO fromEntity(User user) {
        if (user == null) {
            return null;
        }
        AdminVO vo = new AdminVO();
        vo.setId(user.getId());
        vo.setUserAccount(user.getUserAccount());
        vo.setUserName(user.getUserName());
        vo.setPhone(user.getPhone());
        vo.setEmail(user.getEmail());
        vo.setUserRole(user.getUserRole());
        vo.setUserStatus(user.getUserStatus());
        vo.setLastLoginTime(user.getLastLoginTime());
        vo.setCreateTime(user.getCreateTime());
        return vo;
    }
}