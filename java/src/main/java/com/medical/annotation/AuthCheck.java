package com.medical.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 权限校验注解
 *
 * <p>功能说明：
 * 用于标注在Controller方法上，表示该接口需要权限校验
 *
 * <p>使用方式：
 * <pre>
 * // 单角色校验（只有user角色能访问）
 * {@code @AuthCheck(mustRole = "user")}
 *
 * // 多角色校验（user或admin角色都能访问）
 * {@code @AuthCheck(mustRoles = {"user", "admin"})}
 *
 * // 不设置角色（只要登录就能访问）
 * {@code @AuthCheck}
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuthCheck {

    /**
     * 单角色校验（已过时，建议使用 mustRoles）
     * 如果设置了 mustRoles，则忽略此属性
     */
    @Deprecated
    String mustRole() default "";

    /**
     * 多角色校验
     * 设置允许访问的角色列表，只要匹配其中一个角色即可访问
     * 例如：{"user", "admin"} 表示 user 或 admin 角色都能访问
     */
    String[] mustRoles() default {};

    /**
     * 是否允许管理员访问
     * 如果为true，则admin角色可以访问任何接口
     */
    boolean adminAccess() default false;
}
