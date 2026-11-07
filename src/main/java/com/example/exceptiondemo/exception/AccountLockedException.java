package com.example.exceptiondemo.exception;

import com.example.exceptiondemo.exception.num.BusinessErrorCode;

/**
 * 用于演示子类规则优先于父类规则
 *
 * <p>能同时匹配锁定规则、业务父类规则和默认规则，最终选择仍取决于数字优先级
 * 普通账号锁定也可以直接使用 BusinessException，此子类用于展示专用规则的机制
 */
public class AccountLockedException extends BusinessException {
    /**
     * 创建账号锁定异常
     *
     * <p>复用已有错误定义，继承关系只改变可匹配的规则，不会自动创建新的错误码
     */
    public AccountLockedException() {
        super(BusinessErrorCode.ACCOUNT_LOCKED);
    }
}
