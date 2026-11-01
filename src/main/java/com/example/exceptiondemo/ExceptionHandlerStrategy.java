package com.example.exceptiondemo;

/** 异常转换策略，不负责写出 HTTP 响应 */
public interface ExceptionHandlerStrategy {
    /** 将已匹配的异常转换为对外响应 */
    ApiResponse handle(Exception exception);
}
