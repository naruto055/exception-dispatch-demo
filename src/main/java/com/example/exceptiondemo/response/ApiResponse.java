package com.example.exceptiondemo.response;

/**
 * 对外响应仅包含稳定错误码和可公开提示
 *
 * <p>record 自动提供构造器、访问器和按组件值比较的 equals，便于响应断言
 * MVC 消息转换器负责生成 JSON，业务处理器只需要创建这个结果对象
 *
 * @param code    应用层结果码，与 HTTP 状态码是两个独立概念
 * @param message 面向调用方的提示，不应包含内部堆栈或敏感数据
 */
public record ApiResponse(int code, String message) {
}
