package com.example.exceptiondemo.exception.num;

import com.example.exceptiondemo.exception.AccountLockedException;
import com.example.exceptiondemo.exception.BusinessException;
import com.example.exceptiondemo.exception.ExceptionHandlerStrategy;
import com.example.exceptiondemo.exception.handle.AccountLockedExceptionHandler;
import com.example.exceptiondemo.exception.handle.BusinessExceptionHandler;
import com.example.exceptiondemo.exception.handle.DefaultExceptionHandler;
import com.example.exceptiondemo.exception.handle.ValidationExceptionHandler;

import org.springframework.web.bind.MethodArgumentNotValidException;
/** 集中登记异常类型、策略 Bean 类型与项目优先级 */
public enum ExceptionHandlerRegistry {
    // 故意先声明兜底规则，选择顺序由优先级决定
    DEFAULT(Exception.class, DefaultExceptionHandler.class, Integer.MAX_VALUE),
    BUSINESS(BusinessException.class, BusinessExceptionHandler.class, 10),
    LOCKED(AccountLockedException.class, AccountLockedExceptionHandler.class, 5),
    VALIDATION(MethodArgumentNotValidException.class, ValidationExceptionHandler.class, 20);
    private final Class<? extends Exception> exceptionType;
    private final Class<? extends ExceptionHandlerStrategy> handlerType;
    private final int priority;
    /** 登记一条类型映射 */
    ExceptionHandlerRegistry(Class<? extends Exception> exceptionType,
            Class<? extends ExceptionHandlerStrategy> handlerType, int priority) {
        this.exceptionType = exceptionType;
        this.handlerType = handlerType;
        this.priority = priority;
    }
    /** 获取匹配类型 */
    public Class<? extends Exception> getExceptionType() { return exceptionType; }
    /** 获取 Spring 策略类型 */
    public Class<? extends ExceptionHandlerStrategy> getHandlerType() { return handlerType; }
    /** 获取规则优先级，数字越小越先选中 */
    public int getPriority() { return priority; }
}
