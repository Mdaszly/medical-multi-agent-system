package com.medical.aop;

import cn.dev33.satoken.stp.StpUtil;
import com.medical.annotation.AuthCheck;
import com.medical.common.ErrorCode;
import com.medical.constant.UserConstant;
import com.medical.exception.BusinessException;
import com.medical.mapper.DoctorMapper;
import com.medical.mapper.UserMapper;
import com.medical.model.entity.Doctor;
import com.medical.model.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuthInterceptor {

    private final UserMapper userMapper;
    private final DoctorMapper doctorMapper;

    @Around("@annotation(authCheck)")
    public Object doInterceptor(ProceedingJoinPoint joinPoint, AuthCheck authCheck) throws Throwable {
        String mustRole = authCheck.mustRole();
        StpUtil.checkLogin();

        Long loginId = StpUtil.getLoginIdAsLong();
        String currentRole = null;

        // 先查询User表
        User user = userMapper.selectById(loginId);
        if (user != null) {
            currentRole = user.getUserRole();
            log.info("Authenticated User: id={}, role={}", loginId, currentRole);
        } else {
            // 再查询Doctor表
            Doctor doctor = doctorMapper.selectById(loginId);
            if (doctor != null) {
                currentRole = UserConstant.DOCTOR_ROLE;
                log.info("Authenticated Doctor: id={}, role={}", loginId, currentRole);
            }
        }

        if (currentRole == null) {
            log.warn("User not found: loginId={}", loginId);
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        if (mustRole != null && !mustRole.isEmpty()) {
            if (!mustRole.equals(currentRole)) {
                log.warn("Role mismatch: userId={}, requiredRole={}, actualRole={}",
                        loginId, mustRole, currentRole);
                throw new BusinessException(ErrorCode.NO_PERMISSION);
            }
        }
        return joinPoint.proceed();
    }
}
