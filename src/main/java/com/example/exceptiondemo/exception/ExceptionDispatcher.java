package com.example.exceptiondemo.exception;

import com.example.exceptiondemo.exception.num.ExceptionHandlerRegistry;
import com.example.exceptiondemo.response.ApiResponse;

import java.util.Arrays;
import java.util.Comparator;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
/** 按异常类型与优先级分发，不依赖 Servlet 请求 */
@Component
public class ExceptionDispatcher {
    private final ApplicationContext context;
    /** 注入处理器所在的 Spring 容器 */
    public ExceptionDispatcher(ApplicationContext context) { this.context = context; }
    /** 选择传入异常直接类型匹配的最高优先级规则 */
    public ExceptionHandlerRegistry select(Exception exception) {
        // 方向必须是注册类型接收实际类型，不递归查找 cause
        return Arrays.stream(ExceptionHandlerRegistry.values())
                .filter(rule -> rule.getExceptionType().isAssignableFrom(exception.getClass()))
                .min(Comparator.comparingInt(ExceptionHandlerRegistry::getPriority))
                .orElseThrow(() -> new IllegalStateException("缺少默认异常规则"));
    }
    /** 根据选定规则获取 Bean 并委派转换 */
    public ApiResponse dispatch(Exception exception) {
        return context.getBean(select(exception).getHandlerType()).handle(exception);
    }
}
