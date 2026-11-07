package com.example.exceptiondemo.exception;

import com.example.exceptiondemo.response.ApiResponse;

/**
 * 异常转换策略，不负责写出 HTTP 响应
 *
 * <p>所有实现都接收异常并返回 ApiResponse，分发器只依赖这一共同约定
 * 实现类由 Spring 管理，不应把单次请求的异常保存在共享实例字段中
 */
public interface ExceptionHandlerStrategy {
    /**
     * 声明当前处理器能够接收的异常类型，包括该类型的子类
     *
     * <p>分发器在启动时将此声明与注册表中的异常类型比较，提前发现错误映射
     * 此方法不参与请求时的规则选择，分发仍由注册表中的异常类型和优先级决定
     * 实现类必须保证 handle 方法能够处理声明范围内的异常，校验不会分析方法体中的强制类型转换
     *
     * @return 处理器支持的异常父类型或具体类型
     */
    Class<? extends Exception> supportedType();

    /**
     * 将已匹配的异常转换为对外响应
     *
     * <p>这里只转换失败结果，不重新执行业务操作，也不承担重试或事务补偿
     * 直接调用实现类会绕过分发器，调用方仍需遵守其支持类型声明
     *
     * @param exception 已按注册规则匹配、符合 supportedType 声明的异常
     * @return 可公开的统一响应
     */
    ApiResponse handle(Exception exception);
}
