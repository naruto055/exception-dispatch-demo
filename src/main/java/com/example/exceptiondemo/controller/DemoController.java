package com.example.exceptiondemo.controller;

import com.example.exceptiondemo.exception.AccountLockedException;
import com.example.exceptiondemo.response.ApiResponse;
import com.example.exceptiondemo.exception.num.BusinessErrorCode;
import com.example.exceptiondemo.exception.BusinessException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.*;

/**
 * 固定场景只为展示分发，不实现真实认证业务
 *
 * <p>控制器只表达成功或抛出异常，失败响应由 Advice 和处理器生成
 * 类上的路径与方法上的路径组合，例如 /demo 与 /business 组成完整接口路径
 */
@RestController
@RequestMapping("/demo")
public class DemoController {
    /**
     * 对应文章中的验证码失败接口
     *
     * @return 正常返回类型，本演示通过抛出业务异常结束执行
     */
    @GetMapping("/captcha")
    public ApiResponse captcha() {
        throw new BusinessException(BusinessErrorCode.CAPTCHA_INVALID);
    }

    /**
     * 使用不同错误码触发同一种业务异常
     *
     * @param type 业务失败类型
     * @return 正常返回类型，本演示始终抛出异常
     * @throws IllegalArgumentException type 不属于本演示支持的业务失败类型
     */
    @GetMapping("/business")
    public ApiResponse business(@RequestParam String type) {
        // 先求值 switch 再构造业务异常，default 抛出的异常不会被自动包装为 BusinessException
        throw new BusinessException(switch (type) {
            case "captcha" -> BusinessErrorCode.CAPTCHA_INVALID;
            case "password" -> BusinessErrorCode.PASSWORD_INVALID;
            case "locked" -> BusinessErrorCode.ACCOUNT_LOCKED;
            default -> throw new IllegalArgumentException("不支持的演示类型");
        });
    }

    /**
     * 触发同时匹配子类、父类和默认规则的异常
     *
     * @return 正常返回类型，本演示通过抛出账号锁定子类异常结束执行
     */
    @GetMapping("/locked")
    public ApiResponse locked() {
        throw new AccountLockedException();
    }

    /**
     * 由 MVC 参数校验真实触发异常
     *
     * <p>RequestBody 将 JSON 绑定为对象，Valid 触发约束校验，失败时不会执行方法体
     *
     * @param input 已通过请求体校验的姓名参数
     * @return 校验通过后的成功响应
     */
    @PostMapping("/validation")
    public ApiResponse validation(@Valid @RequestBody NameRequest input) {
        return new ApiResponse(0, "成功");
    }

    /**
     * 触发未登记异常，诊断文本不得出现在响应中
     *
     * @return 正常返回类型，本演示通过抛出未知异常结束执行
     */
    @GetMapping("/unexpected")
    public ApiResponse unexpected() {
        throw new IllegalStateException("internal-diagnostic-marker");
    }

    /**
     * 演示参数校验的最小请求体
     *
     * <p>NotBlank 拒绝 null、空字符串和仅含空白的姓名，message 提供可返回前端的提示
     *
     * @param name 待校验的姓名，约束由 MVC 参数校验触发
     */
    public record NameRequest(@NotBlank(message = "姓名不能为空") String name) {
    }
}
