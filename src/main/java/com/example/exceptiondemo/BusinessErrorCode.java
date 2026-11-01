package com.example.exceptiondemo;

/** 业务错误定义与处理策略解耦 */
public enum BusinessErrorCode {
    CAPTCHA_INVALID(1001, "验证码错误"),
    PASSWORD_INVALID(1002, "账号或密码错误"),
    ACCOUNT_LOCKED(1003, "账号已锁定，请联系管理员");
    private final int code;
    private final String message;
    /** 初始化稳定的业务错误定义 */
    BusinessErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
    /** 获取业务错误码 */
    public int getCode() { return code; }
    /** 获取可公开提示 */
    public String getMessage() { return message; }
}
