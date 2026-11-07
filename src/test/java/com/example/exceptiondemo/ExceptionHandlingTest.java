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

/** 同时验证实际 MVC 链路和核心分发决策 */
@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
class ExceptionHandlingTest {
    @Autowired MockMvc mvc;
    @Autowired ExceptionDispatcher dispatcher;
    @Autowired ApplicationContext context;

    /** 两个业务错误共用策略，响应保留各自语义 */
    @Test
    void businessErrorsShareHandler() throws Exception {
        assertThat(dispatcher.select(new BusinessException(BusinessErrorCode.CAPTCHA_INVALID)))
                .isEqualTo(ExceptionHandlerRegistry.BUSINESS);
        verifyGet("/demo/business?type=captcha", 1001, "验证码错误");
        verifyGet("/demo/business?type=password", 1002, "账号或密码错误");
    }

    /** 子类同时命中三条规则，数字最小的规则胜出 */
    @Test
    void specificRuleWinsOverParentAndFallback() throws Exception {
        AccountLockedException exception = new AccountLockedException();
        assertThat(Arrays.stream(ExceptionHandlerRegistry.values())
                .filter(rule -> rule.getExceptionType().isAssignableFrom(exception.getClass())).count()).isEqualTo(3);
        assertThat(dispatcher.select(exception)).isEqualTo(ExceptionHandlerRegistry.LOCKED);
        verifyGet("/demo/locked", 1003, "账号已锁定，请联系管理员");
    }

    /** 无专用规则的子类仍由父类处理器接收 */
    @Test
    void unregisteredSubclassUsesParent() {
        BusinessException exception = new BusinessException(BusinessErrorCode.CAPTCHA_INVALID) {};
        assertThat(dispatcher.select(exception)).isEqualTo(ExceptionHandlerRegistry.BUSINESS);
        assertThat(dispatcher.dispatch(exception)).isEqualTo(new ApiResponse(1001, "验证码错误"));
    }

    /** 外层异常决定路由，不解包内部业务异常 */
    @Test
    void wrappedBusinessExceptionUsesFallback() {
        Exception exception = new IllegalStateException(new BusinessException(BusinessErrorCode.CAPTCHA_INVALID));
        assertThat(dispatcher.select(exception)).isEqualTo(ExceptionHandlerRegistry.DEFAULT);
        assertThat(dispatcher.dispatch(exception)).isEqualTo(new ApiResponse(5000, "系统繁忙，请稍后重试"));
    }

    /** 校验由 MVC 参数绑定触发，合法输入正常返回 */
    @Test
    void validationUsesFrameworkPipeline() throws Exception {
        mvc.perform(post("/demo/validation").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"\"}"))
                .andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json("{\"code\":4001,\"message\":\"请求参数不合法\"}"));
        mvc.perform(post("/demo/validation").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Ada\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(0));
    }

    /** 未知错误对外脱敏，服务端保留路径和完整堆栈 */
    @Test
    void unknownErrorIsRedactedAndLogged(CapturedOutput output) throws Exception {
        verifyGet("/demo/unexpected", 5000, "系统繁忙，请稍后重试");
        assertThat(output.getAll()).contains("/demo/unexpected", "java.lang.IllegalStateException: internal-diagnostic-marker",
                "DemoController.unexpected");
    }

    /** 注册配置错误应在测试阶段暴露，避免请求时失败或路由歧义 */
    @Test
    void registryHasUniquePrioritiesFallbackAndBeans() {
        var rules = Arrays.asList(ExceptionHandlerRegistry.values());
        assertThat(rules).extracting(ExceptionHandlerRegistry::getPriority).doesNotHaveDuplicates();
        assertThat(rules).filteredOn(rule -> rule.getExceptionType() == Exception.class).hasSize(1);
        assertThat(ExceptionHandlerRegistry.DEFAULT.getPriority()).isEqualTo(Integer.MAX_VALUE);
        rules.forEach(rule -> assertThat(context.getBean(rule.getHandlerType())).isNotNull());
    }

    /** 共同检查响应协议及内部信息不泄露 */
    private void verifyGet(String path, int code, String message) throws Exception {
        mvc.perform(get(path)).andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value(code)).andExpect(jsonPath("$.message").value(message))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(content().string(not(containsString("internal-diagnostic-marker"))))
                .andExpect(content().string(not(containsString("stackTrace"))));
    }
}
