package com.example.exceptiondemo.exception.handle;

import com.example.exceptiondemo.response.ApiResponse;
import com.example.exceptiondemo.exception.ExceptionHandlerStrategy;
import com.example.exceptiondemo.exception.AccountLockedException;
import com.example.exceptiondemo.exception.num.BusinessErrorCode;
import org.springframework.stereotype.Component;

/**
 * 演示账号锁定子类的专用规则覆盖父类规则
 *
 * <p>当前响应与普通账号锁定相同，专用处理器用于展示优先级选择
 * 实际业务仅在转换行为需要独立维护时拆分，不应因多一个错误码就增加处理器
 */
@Component
public class AccountLockedExceptionHandler implements ExceptionHandlerStrategy {
    /**
     * 将专用锁定提示限定于账号锁定异常，避免普通业务错误被错误转换为锁定结果
     *
     * @return 账号锁定异常类型，包括它的子类
     */
    @Override
    public Class<? extends Exception> supportedType() {
        return AccountLockedException.class;
    }

    /**
     * 返回账号恢复指引
     *
     * @param exception 已匹配的账号锁定异常，本示例不读取额外上下文
     * @return 锁定错误码与固定恢复提示，不修改账号状态
     */
    @Override
    public ApiResponse handle(Exception exception) {
        return new ApiResponse(BusinessErrorCode.ACCOUNT_LOCKED.getCode(), "账号已锁定，请联系管理员");
    }
}
