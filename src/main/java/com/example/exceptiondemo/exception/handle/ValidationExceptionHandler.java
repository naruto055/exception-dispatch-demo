package com.example.exceptiondemo.exception.handle;

import com.example.exceptiondemo.response.ApiResponse;
import com.example.exceptiondemo.exception.ExceptionHandlerStrategy;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.MethodArgumentNotValidException;

/**
 * 参数校验失败转换策略
 *
 * <p>读取 MVC 收集的校验结果，不重新执行校验，也不直接返回异常的诊断消息
 */
@Component
public class ValidationExceptionHandler implements ExceptionHandlerStrategy {
    /**
     * 声明请求体校验异常类型，保证 handle 能读取该异常携带的校验结果
     *
     * @return MVC 请求体校验异常类型，不代表支持所有参数相关异常
     */
    @Override
    public Class<? extends Exception> supportedType() {
        return MethodArgumentNotValidException.class;
    }

    /**
     * 提取校验提示并转换为统一响应
     *
     * @param exception 请求参数校验异常
     * @return 包含校验提示的参数错误响应
     */
    @Override
    public ApiResponse handle(Exception exception) {
        // 具体异常携带 BindingResult，类型转换依赖注册关系和 supportedType 声明一致
        MethodArgumentNotValidException validationException = (MethodArgumentNotValidException) exception;
        // 只返回一条有效校验提示，没有提示时使用通用文案
        String message = validationException.getBindingResult().getAllErrors().stream()
                // getAllErrors 同时包含字段错误和对象级错误，提示可来自校验注解的 message
                .map(error -> error.getDefaultMessage())
                .filter(text -> text != null && !text.isBlank())
                // 第一条指当前错误列表的顺序，不保证等于字段或注解的声明顺序
                .findFirst()
                .orElse("请求参数不合法");
        return new ApiResponse(4001, message);
    }
}
