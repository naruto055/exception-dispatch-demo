package com.example.exceptiondemo.exception.handle;

import com.example.exceptiondemo.response.ApiResponse;
import com.example.exceptiondemo.exception.ExceptionHandlerStrategy;
import com.example.exceptiondemo.exception.AccountLockedException;
import com.example.exceptiondemo.exception.num.BusinessErrorCode;

import org.springframework.stereotype.Component;
/** 用专用提示演示子类规则覆盖父类规则 */
@Component
public class AccountLockedExceptionHandler implements ExceptionHandlerStrategy {
    /** 返回账号恢复指引 */
    @Override
    public ApiResponse handle(Exception exception) {
        return new ApiResponse(BusinessErrorCode.ACCOUNT_LOCKED.getCode(), "账号已锁定，请联系管理员");
    }
}
