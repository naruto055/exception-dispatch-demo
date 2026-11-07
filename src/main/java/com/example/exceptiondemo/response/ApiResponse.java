package com.example.exceptiondemo.response;

/** 对外响应仅包含稳定错误码和可公开提示 */
public record ApiResponse(int code, String message) {}
