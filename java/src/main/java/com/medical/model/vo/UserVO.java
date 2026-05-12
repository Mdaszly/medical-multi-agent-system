package com.medical.model.vo;

import com.medical.model.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Schema(description = "用户信息响应")
public class UserVO {

    @Schema(description = "用户ID", example = "1")
    private Long id;

    @Schema(description = "用户账号（已脱敏）", example = "zh***an")
    private String userAccount;

    @Schema(description = "用户昵称", example = "张三")
    private String userName;

    @Schema(description = "用户角色", example = "user")
    private String userRole;

    @Schema(description = "手机号（已脱敏）", example = "138****5678")
    private String phone;

    @Schema(description = "邮箱（已脱敏）", example = "z***@example.com")
    private String email;

    @Schema(description = "性别 0-未知 1-男 2-女", example = "1")
    private Integer gender;

    @Schema(description = "年龄", example = "25")
    private Integer age;

    @Schema(description = "注册时间", example = "2026-05-12T10:30:00")
    private LocalDateTime createTime;

    public static UserVO fromEntity(User user) {
        if (user == null) {
            return null;
        }
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUserAccount(desensitizeAccount(user.getUserAccount()));
        vo.setUserName(user.getUserName());
        vo.setUserRole(user.getUserRole());
        vo.setPhone(desensitizePhone(user.getPhone()));
        vo.setEmail(desensitizeEmail(user.getEmail()));
        vo.setGender(user.getGender());
        vo.setAge(calculateAge(user.getBirthDate()));
        vo.setCreateTime(user.getCreateTime());
        return vo;
    }

    private static String desensitizePhone(String phone) {
        if (phone == null || phone.length() < 11) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }

    private static String desensitizeEmail(String email) {
        if (email == null || !email.contains("@")) {
            return email;
        }
        int atIndex = email.indexOf("@");
        if (atIndex < 2) {
            return email;
        }
        return email.substring(0, 1) + "***" + email.substring(atIndex);
    }

    private static String desensitizeAccount(String account) {
        if (account == null || account.length() < 4) {
            return account;
        }
        return account.substring(0, 2) + "***" + account.substring(account.length() - 1);
    }

    private static Integer calculateAge(LocalDate birthDate) {
        if (birthDate == null) {
            return null;
        }
        return LocalDate.now().getYear() - birthDate.getYear();
    }
}
