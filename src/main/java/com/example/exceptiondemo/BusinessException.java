package com.example.exceptiondemo;

/** 携带业务错误定义的异常载体 */
public class BusinessException extends RuntimeException {
    private final BusinessErrorCode error;
    /** 使用错误定义构造异常 */
    public BusinessException(BusinessErrorCode error) {
        super(error.getMessage());
        this.error = error;
    }
    /** 获取业务错误定义 */
    public BusinessErrorCode getError() { return error; }
}
