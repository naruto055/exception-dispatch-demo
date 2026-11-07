package com.example.exceptiondemo.exception.num;

import com.example.exceptiondemo.exception.AccountLockedException;
import com.example.exceptiondemo.exception.BusinessException;
import com.example.exceptiondemo.exception.ExceptionHandlerStrategy;
import com.example.exceptiondemo.exception.handle.AccountLockedExceptionHandler;
import com.example.exceptiondemo.exception.handle.BusinessExceptionHandler;
import com.example.exceptiondemo.exception.handle.DefaultExceptionHandler;
import com.example.exceptiondemo.exception.handle.ValidationExceptionHandler;
import org.springframework.web.bind.MethodArgumentNotValidException;

/**
 * 集中登记异常类型、策略 Bean 类型与项目优先级
 *
 * <p>这里登记的是异常类型到处理器的关系，BusinessErrorCode 登记的是错误码到业务含义的关系
 * Class 对象描述类型，不是异常或处理器实例，具体处理器由 Spring 容器提供
 */
public enum ExceptionHandlerRegistry {
    // 故意先声明兜底规则，选择顺序由优先级决定
    DEFAULT(Exception.class, DefaultExceptionHandler.class, Integer.MAX_VALUE),
    BUSINESS(BusinessException.class, BusinessExceptionHandler.class, 10),
    LOCKED(AccountLockedException.class, AccountLockedExceptionHandler.class, 5),
    VALIDATION(MethodArgumentNotValidException.class, ValidationExceptionHandler.class, 20);

    private final Class<? extends Exception> exceptionType;
    // 泛型限制处理器必须实现策略接口，但无法单独证明它接受上面登记的异常类型
    private final Class<? extends ExceptionHandlerStrategy> handlerType;
    private final int priority;

    /**
     * 登记一条类型映射
     *
     * @param exceptionType 可匹配该类型及其子类的异常规则
     * @param handlerType   在 Spring 中注册的具体处理器类型
     * @param priority      数字越小越优先，项目通过测试约束优先级不重复
     */
    ExceptionHandlerRegistry(Class<? extends Exception> exceptionType, Class<? extends ExceptionHandlerStrategy> handlerType, int priority) {
        this.exceptionType = exceptionType;
        this.handlerType = handlerType;
        this.priority = priority;
    }

    /**
     * 获取匹配类型
     *
     * @return 分发器用于继承关系判断的注册类型
     */
    public Class<? extends Exception> getExceptionType() {
        return exceptionType;
    }

    /**
     * 获取 Spring 策略类型
     *
     * @return 用于从容器获取处理器 Bean 的类型
     */
    public Class<? extends ExceptionHandlerStrategy> getHandlerType() {
        return handlerType;
    }

    /**
     * 获取规则优先级，数字越小越先选中
     *
     * @return 项目内部的选择顺序，不代表 Spring Advice 的执行顺序
     */
    public int getPriority() {
        return priority;
    }
}
