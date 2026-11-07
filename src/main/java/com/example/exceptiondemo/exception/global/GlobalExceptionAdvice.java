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
/** MVC 统一入口负责日志和 HTTP 输出，分发规则独立维护 */
@RestControllerAdvice
public class GlobalExceptionAdvice {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionAdvice.class);
    private final ExceptionDispatcher dispatcher;
    /** 注入项目异常分发器 */
    public GlobalExceptionAdvice(ExceptionDispatcher dispatcher) { this.dispatcher = dispatcher; }
    /** 接收 MVC 请求处理链路中的异常并统一输出 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse> handle(Exception exception, HttpServletRequest request) {
        // 未知异常只在入口记录一次完整堆栈，不向客户端暴露细节
        if (dispatcher.select(exception) == ExceptionHandlerRegistry.DEFAULT) {
            log.error("请求处理失败，路径=" + request.getRequestURI(), exception);
        }
        return ResponseEntity.ok(dispatcher.dispatch(exception));
    }
}
