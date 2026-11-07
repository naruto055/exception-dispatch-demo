package com.example.exceptiondemo.exception.num;

/**
 * 业务错误定义与处理策略解耦
 *
 * <p>枚举常量集中管理对外语义，新增同类业务失败通常只需增加错误定义
 * 不引用处理器类，使业务代码表达失败时无需了解服务端的响应转换实现
 */
public enum BusinessErrorCode {
    CAPTCHA_INVALID(1001, "验证码错误"),
    PASSWORD_INVALID(1002, "账号或密码错误"),
    ACCOUNT_LOCKED(1003, "账号已锁定，请联系管理员");
    private final int code;
    private final String message;

    /**
     * 初始化稳定的业务错误定义
     *
     * @param code    调用方用于识别业务结果的稳定数字编码
     * @param message 可以直接返回给调用方的提示
     */
    BusinessErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    /**
     * 获取业务错误码
     *
     * @return 业务结果标识，不参与处理器优先级比较
     */
    public int getCode() {
        return code;
    }

    /**
     * 获取可公开提示
     *
     * @return 与业务错误对应的用户提示
     */
    public String getMessage() {
        return message;
    }
}
