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
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 权限校验拦截器
 *
 * <p>功能说明：
 * 对带有 @AuthCheck 注解的方法进行权限校验
 *
 * <p>校验逻辑：
 * 1. 检查用户是否登录
 * 2. 获取用户角色
 * 3. 根据注解配置进行权限校验
 *
 * <p>权限校验规则：
 * - 如果设置了 mustRoles（多角色），则只要匹配其中一个角色即可
 * - 如果设置了 adminAccess=true，则admin角色可以访问任何接口
 * - 如果设置了 mustRole（旧版单角色），则只有该角色能访问
 * - 如果都没有设置，则只要登录就能访问
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuthInterceptor {

    private final UserMapper userMapper;
    private final DoctorMapper doctorMapper;

    @Around("@annotation(authCheck)")
    public Object doInterceptor(ProceedingJoinPoint joinPoint, AuthCheck authCheck) throws Throwable {
        StpUtil.checkLogin();

        Long loginId = StpUtil.getLoginIdAsLong();
        String currentRole = null;

        // 优先检查是否为医生（医生账号在user表和doctor表都存在）
        Doctor doctor = doctorMapper.selectById(loginId);
        if (doctor != null) {
            currentRole = UserConstant.DOCTOR_ROLE;
            log.info("Authenticated Doctor: id={}, role={}", loginId, currentRole);
        } else {
            User user = userMapper.selectById(loginId);
            if (user != null) {
                currentRole = user.getUserRole();
                log.info("Authenticated User: id={}, role={}", loginId, currentRole);
            }
        }

        if (currentRole == null) {
            log.warn("User not found: loginId={}", loginId);
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        // 权限校验
        if (!hasPermission(authCheck, currentRole)) {
            log.warn("Permission denied: userId={}, currentRole={}, authCheck={}",
                    loginId, currentRole, buildAuthCheckInfo(authCheck));
            throw new BusinessException(ErrorCode.NO_PERMISSION);
        }

        return joinPoint.proceed();
    }

    /**
     * 检查是否有权限访问
     *
     * @param authCheck 权限注解配置
     * @param currentRole 当前用户角色
     * @return 是否有权限
     */
    private boolean hasPermission(AuthCheck authCheck, String currentRole) {
        // 规则0：管理员特权访问（优先于所有其他规则）
        if (UserConstant.ADMIN_ROLE.equals(currentRole)) {
            log.debug("Admin access granted: role={}", currentRole);
            return true;
        }

        // 规则1：多角色校验（优先）
        String[] mustRoles = authCheck.mustRoles();
        if (mustRoles != null && mustRoles.length > 0) {
            for (String role : mustRoles) {
                if (role.equals(currentRole)) {
                    log.debug("Role matched: currentRole={}, matchedRole={}", currentRole, role);
                    return true;
                }
            }
            log.debug("No role matched: currentRole={}, requiredRoles={}", currentRole, Arrays.toString(mustRoles));
            return false;
        }

        // 规则2：单角色校验（兼容旧版）
        String mustRole = authCheck.mustRole();
        if (StringUtils.hasText(mustRole)) {
            boolean matched = mustRole.equals(currentRole);
            log.debug("Single role check: currentRole={}, requiredRole={}, matched={}",
                    currentRole, mustRole, matched);
            return matched;
        }

        // 规则3：没有配置任何角色限制，只要登录就能访问
        log.debug("No role restriction configured, access granted");
        return true;
    }

    /**
     * 构建权限检查信息（用于日志）
     */
    private String buildAuthCheckInfo(AuthCheck authCheck) {
        StringBuilder sb = new StringBuilder();
        sb.append("AuthCheck(");

        if (StringUtils.hasText(authCheck.mustRole())) {
            sb.append("mustRole=").append(authCheck.mustRole());
        }

        if (authCheck.mustRoles() != null && authCheck.mustRoles().length > 0) {
            sb.append("mustRoles=").append(Arrays.toString(authCheck.mustRoles()));
        }

        if (authCheck.adminAccess()) {
            sb.append("adminAccess=true");
        }

        sb.append(")");
        return sb.toString();
    }
}
