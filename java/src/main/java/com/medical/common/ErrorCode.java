package com.medical.common;

public enum ErrorCode {

    SUCCESS(0, "success"),
    PARAM_ERROR(40000, "请求参数错误"),
    VERIFICATION_CODE_ERROR(40001, "验证码错误"),
    VERIFICATION_CODE_EXPIRED(40002, "验证码已过期"),
    PASSWORD_FORMAT_ERROR(40003, "密码格式不正确"),
    PASSWORD_NOT_MATCH(40004, "两次密码输入不一致"),
    NOT_LOGIN(40100, "用户未登录"),
    TOKEN_EXPIRED(40101, "Token已过期"),
    TOKEN_INVALID(40102, "Token无效"),
    NO_PERMISSION(40300, "无权限访问"),
    ACCOUNT_DISABLED(40301, "账号已被禁用"),
    ACCOUNT_LOCKED(40302, "账号已被锁定"),
    NO_AUTH(40303, "无权限"),
    USER_NOT_FOUND(40400, "用户不存在"),
    USER_ALREADY_EXISTS(40401, "用户已存在"),
    SYSTEM_ERROR(50000, "系统内部错误");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}