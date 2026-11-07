package com.example.exceptiondemo.exception;

import com.example.exceptiondemo.exception.num.BusinessErrorCode;

/**
 * 携带业务错误定义的异常载体
 *
 * <p>继承 RuntimeException，业务方法无需逐层声明 throws，也不必就地拼装响应
 * 错误定义表达失败原因，实际交给哪个处理器由异常类型和注册规则决定
 */
public class BusinessException extends RuntimeException {
    private final BusinessErrorCode error;

    /**
     * 使用错误定义构造异常
     *
     * @param error 非空的业务错误定义，其提示允许对外公开
     */
    public BusinessException(BusinessErrorCode error) {
        // 父类保存异常消息，error 保留结构化错误码，两者用途不同
        super(error.getMessage());
        this.error = error;
    }

    /**
     * 获取业务错误定义
     *
     * @return 供业务处理器读取的错误码与提示
     */
    public BusinessErrorCode getError() {
        return error;
    }
}
