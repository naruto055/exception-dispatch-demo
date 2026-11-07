package com.example.exceptiondemo;

import java.util.Arrays;

import com.example.exceptiondemo.exception.AccountLockedException;
import com.example.exceptiondemo.exception.BusinessException;
import com.example.exceptiondemo.exception.ExceptionDispatcher;
import com.example.exceptiondemo.exception.num.ExceptionHandlerRegistry;
import com.example.exceptiondemo.exception.num.BusinessErrorCode;
import com.example.exceptiondemo.response.ApiResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 同时验证实际 MVC 链路和核心分发决策
 *
 * <p>SpringBootTest 加载应用上下文，MockMvc 在进程内执行 MVC 请求，不需要监听真实端口
 * 直接调用分发器检查规则选择，HTTP 断言检查参数校验、Advice 和 JSON 序列化是否连通
 * OutputCaptureExtension 捕获日志输出，用于检查服务端是否保留诊断信息
 */
@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
class ExceptionHandlingTest {
    @Autowired
    MockMvc mvc;
    @Autowired
    ExceptionDispatcher dispatcher;
    @Autowired
    ApplicationContext context;

    /**
     * 两个业务错误共用策略，响应保留各自语义
     *
     * @throws Exception MVC 请求执行失败时抛出
     */
    @Test
    void businessErrorsShareHandler() throws Exception {
        assertThat(dispatcher.select(new BusinessException(BusinessErrorCode.CAPTCHA_INVALID)))
                .isEqualTo(ExceptionHandlerRegistry.BUSINESS);
        verifyGet("/demo/business?type=captcha", 1001, "验证码错误");
        verifyGet("/demo/business?type=password", 1002, "账号或密码错误");
    }

    /**
     * 普通账号锁定沿用业务处理器，不依赖专用异常子类
     *
     * @throws Exception MVC 请求执行失败时抛出
     */
    @Test
    void ordinaryAccountLockUsesBusinessHandler() throws Exception {
        var result = mvc.perform(get("/demo/business?type=locked"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json("{\"code\":1003,\"message\":\"账号已锁定，请联系管理员\"}"))
                .andReturn();
        // 响应与专用锁定规则相同，需要验证请求实际抛出的类型和选中的规则
        assertThat(result.getResolvedException()).isExactlyInstanceOf(BusinessException.class);
        assertThat(dispatcher.select(result.getResolvedException())).isEqualTo(ExceptionHandlerRegistry.BUSINESS);
    }

    /**
     * 子类同时命中三条规则，数字最小的规则胜出
     *
     * @throws Exception MVC 请求执行失败时抛出
     */
    @Test
    void specificRuleWinsOverParentAndFallback() throws Exception {
        AccountLockedException exception = new AccountLockedException();
        // 先证明有多个候选，再断言最终规则，避免测试退化为只验证单一匹配
        assertThat(Arrays.stream(ExceptionHandlerRegistry.values())
                .filter(rule -> rule.getExceptionType().isAssignableFrom(exception.getClass())).count()).isEqualTo(3);
        assertThat(dispatcher.select(exception)).isEqualTo(ExceptionHandlerRegistry.LOCKED);
        verifyGet("/demo/locked", 1003, "账号已锁定，请联系管理员");
    }

    /**
     * 无专用规则的子类仍由父类处理器接收
     */
    @Test
    void unregisteredSubclassUsesParent() {
        // 空的大括号创建匿名子类，用它验证父类匹配，不必增加生产异常类
        BusinessException exception = new BusinessException(BusinessErrorCode.CAPTCHA_INVALID) {
        };
        assertThat(dispatcher.select(exception)).isEqualTo(ExceptionHandlerRegistry.BUSINESS);
        assertThat(dispatcher.dispatch(exception)).isEqualTo(new ApiResponse(1001, "验证码错误"));
    }

    /**
     * 外层异常决定路由，不解包内部业务异常
     */
    @Test
    void wrappedBusinessExceptionUsesFallback() {
        // cause 中虽有业务异常，实际传给 select 的类型仍然是 IllegalStateException
        Exception exception = new IllegalStateException(new BusinessException(BusinessErrorCode.CAPTCHA_INVALID));
        assertThat(dispatcher.select(exception)).isEqualTo(ExceptionHandlerRegistry.DEFAULT);
        assertThat(dispatcher.dispatch(exception)).isEqualTo(new ApiResponse(5000, "系统繁忙，请稍后重试"));
    }

    /**
     * MVC 校验失败返回注解提示，合法输入正常返回
     *
     * @throws Exception MVC 请求执行失败时抛出
     */
    @Test
    void validationUsesFrameworkPipeline() throws Exception {
        // 发送 JSON 触发真正的 MVC 校验，不能用手工调用 Controller 方法替代这一验证
        mvc.perform(post("/demo/validation").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"\"}"))
                .andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json("{\"code\":4001,\"message\":\"姓名不能为空\"}"));
        mvc.perform(post("/demo/validation").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Ada\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(0));
    }

    /**
     * 未知错误对外脱敏，服务端保留路径和完整堆栈
     *
     * @param output 测试扩展捕获的标准输出与错误输出
     * @throws Exception MVC 请求执行失败时抛出
     */
    @Test
    void unknownErrorIsRedactedAndLogged(CapturedOutput output) throws Exception {
        verifyGet("/demo/unexpected", 5000, "系统繁忙，请稍后重试");
        assertThat(output.getAll()).contains("/demo/unexpected", "java.lang.IllegalStateException: internal-diagnostic-marker",
                "DemoController.unexpected");
    }

    /**
     * 注册配置错误应在测试阶段暴露，避免请求时失败或路由歧义
     *
     * <p>这里验证优先级、兜底和 Bean，启动阶段的类型兼容性由独立上下文测试验证
     */
    @Test
    void registryHasUniquePrioritiesFallbackAndBeans() {
        var rules = Arrays.asList(ExceptionHandlerRegistry.values());
        assertThat(rules).extracting(ExceptionHandlerRegistry::getPriority).doesNotHaveDuplicates();
        assertThat(rules).filteredOn(rule -> rule.getExceptionType() == Exception.class).hasSize(1);
        assertThat(ExceptionHandlerRegistry.DEFAULT.getPriority()).isEqualTo(Integer.MAX_VALUE);
        rules.forEach(rule -> assertThat(context.getBean(rule.getHandlerType())).isNotNull());
    }

    /**
     * 共同检查响应协议及内部信息不泄露
     *
     * @param path    包含查询参数的请求路径
     * @param code    预期的应用层结果码
     * @param message 预期的对外提示
     * @throws Exception MVC 请求执行失败时抛出
     */
    private void verifyGet(String path, int code, String message) throws Exception {
        // 同时断言 HTTP 状态和业务 code，避免把 HTTP 200 当成业务成功的充分条件
        mvc.perform(get(path)).andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value(code)).andExpect(jsonPath("$.message").value(message))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(content().string(not(containsString("internal-diagnostic-marker"))))
                .andExpect(content().string(not(containsString("stackTrace"))));
    }
}
