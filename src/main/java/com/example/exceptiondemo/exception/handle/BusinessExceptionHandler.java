package com.example.exceptiondemo.exception.handle;

import com.example.exceptiondemo.response.ApiResponse;
import com.example.exceptiondemo.exception.ExceptionHandlerStrategy;
import com.example.exceptiondemo.exception.BusinessException;
import org.springframework.stereotype.Component;

/**
 * 多种业务错误共用同一个转换策略
 *
 * <p>验证码、密码和普通账号锁定只改变错误码与提示，不需要各自实现一份转换逻辑
 * Component 使处理器成为 Spring Bean，分发器才能按注册类型获取它
 */
@Component
public class BusinessExceptionHandler implements ExceptionHandlerStrategy {
    /**
     * 声明业务处理器接收 BusinessException 及其子类，与 handle 中的类型转换保持一致
     *
     * @return 业务异常类型，允许账号锁定等业务子类复用此处理器
     */
    @Override
    public Class<? extends Exception> supportedType() {
        return BusinessException.class;
    }

    /**
     * 从业务异常中保留错误码和可公开提示
     *
     * @param exception 业务异常或其子类，由调用方遵守支持类型约定
     * @return 保留原有业务错误语义的统一响应
     */
    @Override
    public ApiResponse handle(Exception exception) {
        // 启动校验确保注册范围属于业务异常，调用方仍需遵守 supportedType 声明
        BusinessException business = (BusinessException) exception;
        return new ApiResponse(business.getError().getCode(), business.getError().getMessage());
    }
}
