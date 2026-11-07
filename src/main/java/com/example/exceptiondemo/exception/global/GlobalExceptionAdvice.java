package com.example.exceptiondemo.exception.global;

import com.example.exceptiondemo.exception.ExceptionDispatcher;
import com.example.exceptiondemo.exception.num.ExceptionHandlerRegistry;
import com.example.exceptiondemo.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * MVC 统一入口负责日志和 HTTP 输出，分发规则独立维护
 *
 * <p>RestControllerAdvice 将异常处理结果作为响应体交给 MVC 序列化
 * 作用范围是进入 MVC 异常处理机制的请求，不覆盖独立线程、定时任务等执行边界
 */
@RestControllerAdvice
public class GlobalExceptionAdvice {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionAdvice.class);
    private final ExceptionDispatcher dispatcher;

    /**
     * 注入项目异常分发器
     *
     * @param dispatcher 负责选择规则并调用处理器的分发器
     */
    public GlobalExceptionAdvice(ExceptionDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    /**
     * 接收 MVC 请求处理链路中的异常并统一输出
     *
     * <p>Spring 先决定是否调用本方法，本方法内部再按项目注册表选择具体策略
     *
     * @param exception 从 MVC 请求处理链路传播出来的异常
     * @param request   当前请求，仅用于补充服务端日志中的路径信息
     * @return HTTP 200 与统一响应体，调用方还需读取 code 判断业务结果
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse> handle(Exception exception, HttpServletRequest request) {
        // 未知异常只在入口记录一次完整堆栈，不向客户端暴露细节
        if (dispatcher.select(exception) == ExceptionHandlerRegistry.DEFAULT) {
            log.error("请求处理失败，路径=" + request.getRequestURI(), exception);
        }
        // HTTP 200 是本 Demo 的协议约定，不代表响应体中的业务结果一定成功
        return ResponseEntity.ok(dispatcher.dispatch(exception));
    }
}
