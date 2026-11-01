package com.example.exceptiondemo;

import org.springframework.stereotype.Component;
/** 多种业务错误共用同一个转换策略 */
@Component
public class BusinessExceptionHandler implements ExceptionHandlerStrategy {
    /** 从业务异常中保留错误码和可公开提示 */
    @Override
    public ApiResponse handle(Exception exception) {
        // 注册表保证该策略只接收业务异常及其子类
        BusinessException business = (BusinessException) exception;
        return new ApiResponse(business.getError().getCode(), business.getError().getMessage());
    }
}
