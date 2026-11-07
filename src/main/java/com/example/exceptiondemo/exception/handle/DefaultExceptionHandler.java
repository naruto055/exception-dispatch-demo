package com.example.exceptiondemo.exception.handle;

import com.example.exceptiondemo.response.ApiResponse;
import com.example.exceptiondemo.exception.ExceptionHandlerStrategy;
import org.springframework.stereotype.Component;

/**
 * 未登记异常的默认转换策略
 *
 * <p>默认规则在没有更高优先级匹配时被选择，不是其他处理器执行失败后的重试入口
 * 本 Demo 在 Advice 中记录未知异常日志，这里只负责创建安全的响应
 */
@Component
public class DefaultExceptionHandler implements ExceptionHandlerStrategy {
    /**
     * 默认处理器只生成通用响应，可以接收入口范围内的任意 Exception
     *
     * @return Exception 类型，与默认注册规则的覆盖范围一致
     */
    @Override
    public Class<? extends Exception> supportedType() {
        return Exception.class;
    }

    /**
     * 内部诊断信息只保留在服务端日志
     *
     * @param exception 未匹配专用规则的异常，不将其原始消息返回客户端
     * @return 通用系统错误码与可公开提示
     */
    @Override
    public ApiResponse handle(Exception exception) {
        return new ApiResponse(5000, "系统繁忙，请稍后重试");
    }
}
