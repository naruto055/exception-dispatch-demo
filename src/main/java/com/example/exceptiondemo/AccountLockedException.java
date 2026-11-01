package com.example.exceptiondemo;

/** 用于演示子类规则优先于父类规则 */
public class AccountLockedException extends BusinessException {
    /** 创建账号锁定异常 */
    public AccountLockedException() { super(BusinessErrorCode.ACCOUNT_LOCKED); }
}
