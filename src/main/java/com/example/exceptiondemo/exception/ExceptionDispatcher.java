package com.example.exceptiondemo.exception;

import java.util.Arrays;
import java.util.Comparator;

import com.example.exceptiondemo.exception.num.ExceptionHandlerRegistry;
import com.example.exceptiondemo.response.ApiResponse;
import jakarta.annotation.PostConstruct;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * 按异常类型与优先级分发，不依赖 Servlet 请求
 *
 * <p>启动校验检查注册关系，请求分发选择并执行策略，两者发生在不同阶段
 */
@Component
public class ExceptionDispatcher {
    private final ApplicationContext context;

    /**
     * 注入处理器所在的 Spring 容器
     *
     * @param context 用于获取已注册处理器 Bean 的应用上下文
     */
    public ExceptionDispatcher(ApplicationContext context) {
        this.context = context;
    }

    /**
     * 在 Spring 完成依赖注入后校验异常类型与处理器的对应关系
     *
     * <p>启动时遍历静态注册表，避免错误映射直到首个请求到达才暴露
     * 这里只读取处理器的类型声明，不调用 handle，也不模拟或重试异常处理
     * 优先级唯一性与默认规则存在性仍由现有注册配置测试负责
     *
     * @throws IllegalStateException 处理器声明的支持范围不能覆盖注册的异常类型
     */
    @PostConstruct
    public void validateRegistry() {
        for (ExceptionHandlerRegistry rule : ExceptionHandlerRegistry.values()) {
            // 获取实际的 Spring Bean，缺失 Bean 时也会在启动阶段直接暴露配置错误
            ExceptionHandlerStrategy handler = context.getBean(rule.getHandlerType());
            Class<? extends Exception> supportedType = handler.supportedType();
            // 左边必须是处理器接受的类型，右边是注册规则可能交给它的类型
            // 业务处理器可以接收账号锁定子类，但账号锁定处理器不能覆盖所有业务异常
            if (!supportedType.isAssignableFrom(rule.getExceptionType())) {
                // 错误映射属于配置错误，终止 Bean 初始化，不交给默认处理器掩盖问题
                throw new IllegalStateException("异常注册规则 " + rule.name()
                        + " 不兼容：注册类型=" + rule.getExceptionType().getName()
                        + "，处理器=" + rule.getHandlerType().getName()
                        + "，支持类型=" + supportedType.getName());
            }
        }
    }

    /**
     * 按传入异常的实际类型选择最高优先级规则，包含父类匹配
     *
     * <p>只检查当前异常，不递归查找 cause，也不自动选择继承关系最近的类型
     *
     * @param exception 需要处理的非空异常实例
     * @return 匹配项中优先级数字最小的注册规则
     * @throws IllegalStateException 没有匹配规则，通常表示默认注册配置被破坏
     */
    public ExceptionHandlerRegistry select(Exception exception) {
        // 方向必须是注册类型接收实际类型，不递归查找 cause
        return Arrays.stream(ExceptionHandlerRegistry.values())
                // 相当于判断注册类型的变量能否接收当前异常，例如 Exception 能接收业务异常
                .filter(rule -> rule.getExceptionType().isAssignableFrom(exception.getClass()))
                // 多个规则匹配时只选一个，父类规则优先级更高时也可以胜过子类规则
                .min(Comparator.comparingInt(ExceptionHandlerRegistry::getPriority))
                .orElseThrow(() -> new IllegalStateException("缺少默认异常规则"));
    }

    /**
     * 根据选定规则获取 Bean 并委派转换
     *
     * @param exception 进入分发链路的非空异常实例
     * @return 选中处理器生成的统一响应
     */
    public ApiResponse dispatch(Exception exception) {
        // 从容器获取处理器以保留依赖注入能力，处理器执行失败时不会自动重试默认规则
        return context.getBean(select(exception).getHandlerType()).handle(exception);
    }
}
