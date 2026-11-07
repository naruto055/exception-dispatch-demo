package com.example.exceptiondemo;

import com.example.exceptiondemo.exception.AccountLockedException;
import com.example.exceptiondemo.exception.ExceptionDispatcher;
import com.example.exceptiondemo.exception.handle.AccountLockedExceptionHandler;
import com.example.exceptiondemo.exception.handle.BusinessExceptionHandler;
import com.example.exceptiondemo.exception.handle.DefaultExceptionHandler;
import com.example.exceptiondemo.exception.handle.ValidationExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.web.bind.MethodArgumentNotValidException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

/**
 * 用独立 Spring 上下文验证启动校验，错误配置不会影响现有 MVC 集成测试
 *
 * <p>测试只替换业务处理器的支持类型声明，模拟注册双方的类型关系发生变化
 * 不修改生产枚举，也不调用校验方法，确保校验确实由 Spring 初始化生命周期触发
 */
class ExceptionRegistryValidationTest {
    // 只注册启动校验需要的组件，避免为配置失败场景启动整个 Web 应用
    // runner 只保存配置，每次 run 都创建并关闭独立上下文，不会污染其他测试
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withBean(ExceptionDispatcher.class)
            .withBean(DefaultExceptionHandler.class)
            .withBean(AccountLockedExceptionHandler.class)
            .withBean(ValidationExceptionHandler.class);

    /**
     * 当前真实注册表与处理器声明一致，应允许上下文正常启动
     *
     * <p>使用真实处理器作为基线，确保启动校验不会拒绝当前合法配置
     */
    @Test
    void matchingTypesAllowStartup() {
        runner.withBean(BusinessExceptionHandler.class).run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(ExceptionDispatcher.class);
        });
    }

    /**
     * 支持父类的处理器可以接收注册的子类，防止校验被误写为类型必须相等
     *
     * <p>注册的是 BusinessException，声明可接收 Exception 时仍满足类型兼容关系
     */
    @Test
    void broaderSupportedTypeAllowsStartup() {
        withBusinessSupportedType(Exception.class).run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(ExceptionDispatcher.class);
        });
    }

    /**
     * 业务规则交给只支持参数校验异常的处理器时，应在启动阶段拒绝配置
     *
     * <p>模拟处理器声明与 BUSINESS 规则没有继承关系，错误应在请求到达之前暴露
     */
    @Test
    void unrelatedSupportedTypeRejectsStartup() {
        withBusinessSupportedType(MethodArgumentNotValidException.class).run(context -> {
            // Spring 会包装初始化异常，因此检查最内层原因及可定位错误映射的信息
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure()).hasRootCauseInstanceOf(IllegalStateException.class)
                    .rootCause().hasMessageContaining("BUSINESS")
                    .hasMessageContaining("注册类型=com.example.exceptiondemo.exception.BusinessException")
                    .hasMessageContaining("处理器=" + BusinessExceptionHandler.class.getName())
                    .hasMessageContaining("支持类型=" + MethodArgumentNotValidException.class.getName());
        });
    }

    /**
     * 只支持业务子类的处理器不能接收整个业务异常范围，防止 isAssignableFrom 方向写反
     *
     * <p>AccountLockedException 属于 BusinessException，但普通业务异常未必是账号锁定异常
     */
    @Test
    void narrowerSupportedTypeRejectsStartup() {
        withBusinessSupportedType(AccountLockedException.class).run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure()).hasRootCauseInstanceOf(IllegalStateException.class)
                    .rootCause().hasMessageContaining("BUSINESS")
                    .hasMessageContaining("支持类型=" + AccountLockedException.class.getName());
        });
    }

    /**
     * 构造带指定类型声明的测试处理器，隔离启动兼容性校验与业务转换逻辑
     *
     * @param supportedType 本次测试中业务处理器声明的支持类型
     * @return 注册了测试处理器的独立上下文运行器
     */
    private ApplicationContextRunner withBusinessSupportedType(Class<? extends Exception> supportedType) {
        BusinessExceptionHandler handler = mock(BusinessExceptionHandler.class);
        // doReturn 避免 Class 通配符捕获导致的 thenReturn 泛型约束，模拟值仍由方法参数限定
        // 只模拟类型声明，不执行 handle，测试不会把模拟声明当作真实转换能力的证明
        doReturn(supportedType).when(handler).supportedType();
        return runner.withBean(BusinessExceptionHandler.class, () -> handler);
    }
}
