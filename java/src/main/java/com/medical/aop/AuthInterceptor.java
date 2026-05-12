package com.medical.aop;

import cn.dev33.satoken.stp.StpUtil;
import com.medical.annotation.AuthCheck;
import com.medical.common.ErrorCode;
import com.medical.exception.BusinessException;
import com.medical.mapper.UserMapper;
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

    @Around("@annotation(authCheck)")
    public Object doInterceptor(ProceedingJoinPoint joinPoint, AuthCheck authCheck) throws Throwable {
        String mustRole = authCheck.mustRole();
        StpUtil.checkLogin();

        Long userId = StpUtil.getLoginIdAsLong();
        User user = userMapper.selectById(userId);

        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        if (mustRole != null && !mustRole.isEmpty()) {
            String userRole = user.getUserRole();
            if (!mustRole.equals(userRole)) {
                log.warn("User role mismatch: userId={}, requiredRole={}, actualRole={}",
                        userId, mustRole, userRole);
                throw new BusinessException(ErrorCode.NO_PERMISSION);
            }
        }
        return joinPoint.proceed();
    }
}
