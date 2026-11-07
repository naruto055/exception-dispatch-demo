package com.example.exceptiondemo.exception.handle;

import com.example.exceptiondemo.response.ApiResponse;
import com.example.exceptiondemo.exception.ExceptionHandlerStrategy;

import org.springframework.stereotype.Component;
/** 未登记异常的默认转换策略 */
@Component
public class DefaultExceptionHandler implements ExceptionHandlerStrategy {
    /** 内部诊断信息只保留在服务端日志 */
    @Override
    public ApiResponse handle(Exception exception) {
        return new ApiResponse(5000, "系统繁忙，请稍后重试");
    }
}
