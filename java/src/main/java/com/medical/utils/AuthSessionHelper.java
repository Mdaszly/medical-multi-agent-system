package com.medical.utils;

import cn.dev33.satoken.stp.StpUtil;
import com.medical.common.ErrorCode;
import com.medical.constant.UserConstant;
import com.medical.exception.BusinessException;
import com.medical.mapper.DoctorMapper;
import com.medical.mapper.UserMapper;
import com.medical.model.entity.Doctor;
import com.medical.model.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 登录会话绑定与角色解析。
 * <p>user 与 doctor 表主键可能数值相同，禁止仅凭 loginId 查 doctor 表推断角色。
 */
@Component
@RequiredArgsConstructor
public class AuthSessionHelper {

    private final UserMapper userMapper;
    private final DoctorMapper doctorMapper;

    public void loginAsUser(User user) {
        StpUtil.login(user.getId());
        StpUtil.getSession().set(UserConstant.SESSION_LOGIN_TYPE, UserConstant.LOGIN_TYPE_USER);
        StpUtil.getSession().set(UserConstant.SESSION_USER_ROLE, user.getUserRole());
        bindDoctorIdForUser(user.getId(), user.getUserRole());
    }

    public void loginAsDoctor(Doctor doctor) {
        StpUtil.login(doctor.getId());
        StpUtil.getSession().set(UserConstant.SESSION_LOGIN_TYPE, UserConstant.LOGIN_TYPE_DOCTOR);
        StpUtil.getSession().set(UserConstant.SESSION_USER_ROLE, UserConstant.DOCTOR_ROLE);
        StpUtil.getSession().set(UserConstant.SESSION_DOCTOR_ID, doctor.getId());
    }

    /**
     * 解析当前登录医生在 doctor 表的主键。
     * 医生账号可能先匹配 user 表登录（loginId=user.id），预约等业务数据使用 doctor.id。
     */
    public Long getCurrentDoctorId() {
        if (!UserConstant.DOCTOR_ROLE.equals(getCurrentRole())) {
            return null;
        }

        Object cached = StpUtil.getSession().get(UserConstant.SESSION_DOCTOR_ID);
        if (cached instanceof Number) {
            return ((Number) cached).longValue();
        }

        Long loginId = StpUtil.getLoginIdAsLong();
        if (UserConstant.LOGIN_TYPE_DOCTOR.equals(getLoginType())) {
            StpUtil.getSession().set(UserConstant.SESSION_DOCTOR_ID, loginId);
            return loginId;
        }

        Doctor byUserId = doctorMapper.selectByUserId(loginId);
        if (byUserId != null) {
            StpUtil.getSession().set(UserConstant.SESSION_DOCTOR_ID, byUserId.getId());
            return byUserId.getId();
        }

        Doctor byId = doctorMapper.selectById(loginId);
        if (byId != null) {
            StpUtil.getSession().set(UserConstant.SESSION_DOCTOR_ID, byId.getId());
            return byId.getId();
        }

        return null;
    }

    private void bindDoctorIdForUser(Long userId, String userRole) {
        if (!UserConstant.DOCTOR_ROLE.equals(userRole)) {
            return;
        }
        Doctor doctor = doctorMapper.selectByUserId(userId);
        if (doctor != null) {
            StpUtil.getSession().set(UserConstant.SESSION_DOCTOR_ID, doctor.getId());
        }
    }

    public String getCurrentRole() {
        String role = (String) StpUtil.getSession().get(UserConstant.SESSION_USER_ROLE);
        if (StringUtils.hasText(role)) {
            return role;
        }
        return resolveRoleFallback(StpUtil.getLoginIdAsLong());
    }

    public String getLoginType() {
        return (String) StpUtil.getSession().get(UserConstant.SESSION_LOGIN_TYPE);
    }

    private String resolveRoleFallback(Long loginId) {
        String loginType = (String) StpUtil.getSession().get(UserConstant.SESSION_LOGIN_TYPE);

        if (UserConstant.LOGIN_TYPE_DOCTOR.equals(loginType)) {
            Doctor doctor = doctorMapper.selectById(loginId);
            if (doctor != null) {
                return UserConstant.DOCTOR_ROLE;
            }
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        User user = userMapper.selectById(loginId);
        if (user != null) {
            return user.getUserRole();
        }

        Doctor doctor = doctorMapper.selectById(loginId);
        if (doctor != null) {
            return UserConstant.DOCTOR_ROLE;
        }

        throw new BusinessException(ErrorCode.USER_NOT_FOUND);
    }
}
