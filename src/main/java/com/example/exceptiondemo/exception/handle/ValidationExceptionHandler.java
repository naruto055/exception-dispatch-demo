package com.example.exceptiondemo.exception.handle;

import com.example.exceptiondemo.response.ApiResponse;
import com.example.exceptiondemo.exception.ExceptionHandlerStrategy;

import org.springframework.stereotype.Component;
/** 参数校验失败转换策略 */
@Component
public class ValidationExceptionHandler implements ExceptionHandlerStrategy {
    /**
     * 声明请求体校验异常类型，供启动阶段检查注册关系
     *
     * @return MVC 请求体校验异常类型
     */
    @Override
    public Class<? extends Exception> supportedType() {
        return org.springframework.web.bind.MethodArgumentNotValidException.class;
    }

    /** 返回统一参数提示，避免回显提交的数据 */
    @Override
    public ApiResponse handle(Exception exception) {
        return new ApiResponse(4001, "请求参数不合法");
    }
}
