package com.example.exceptiondemo;

import org.springframework.stereotype.Component;
/** 参数校验失败转换策略 */
@Component
public class ValidationExceptionHandler implements ExceptionHandlerStrategy {
    /** 返回统一参数提示，避免回显提交的数据 */
    @Override
    public ApiResponse handle(Exception exception) {
        return new ApiResponse(4001, "请求参数不合法");
    }
}
