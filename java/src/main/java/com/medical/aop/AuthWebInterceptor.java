package com.medical.aop;

import cn.dev33.satoken.stp.StpUtil;
import com.medical.common.ErrorCode;
import com.medical.constant.UserConstant;
import com.medical.exception.BusinessException;
import com.medical.model.entity.User;
import com.medical.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthWebInterceptor implements HandlerInterceptor {

    private final UserService userService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String requestUri = request.getRequestURI();
        String method = request.getMethod();
        
        log.debug("Request intercepted: {} {}", method, requestUri);
        
        if (isPublicPath(requestUri)) {
            return true;
        }
        
        StpUtil.checkLogin();
        
        Long userId = StpUtil.getLoginIdAsLong();
        User user = userService.getUserEntityById(userId);
        String role = user.getUserRole();
        
        return checkRolePermission(requestUri, role, userId);
    }

    private boolean isPublicPath(String requestUri) {
        return requestUri.startsWith("/api/auth/") ||
               requestUri.startsWith("/api/health/") ||
               requestUri.startsWith("/swagger/") ||
               requestUri.startsWith("/v3/api-docs/") ||
               requestUri.startsWith("/webjars/");
    }

    private boolean checkRolePermission(String requestUri, String role, Long userId) {
        if (requestUri.startsWith("/api/user/") && !UserConstant.USER_ROLE.equals(role)) {
            log.warn("Role permission denied: userId={}, role={}, uri={}", userId, role, requestUri);
            throw new BusinessException(ErrorCode.NO_AUTH, "无权限访问患者端");
        }
        if (requestUri.startsWith("/api/doctor/") && !UserConstant.DOCTOR_ROLE.equals(role)) {
            log.warn("Role permission denied: userId={}, role={}, uri={}", userId, role, requestUri);
            throw new BusinessException(ErrorCode.NO_AUTH, "无权限访问医生端");
        }
        if (requestUri.startsWith("/api/admin/") && !UserConstant.ADMIN_ROLE.equals(role)) {
            log.warn("Role permission denied: userId={}, role={}, uri={}", userId, role, requestUri);
            throw new BusinessException(ErrorCode.NO_AUTH, "无权限访问管理端");
        }
        return true;
    }
}