package com.example.exceptiondemo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.*;
/** 固定场景只为展示分发，不实现真实认证业务 */
@RestController
@RequestMapping("/demo")
public class DemoController {
    /** 对应文章中的验证码失败接口 */
    @GetMapping("/captcha")
    public ApiResponse captcha() {
        throw new BusinessException(BusinessErrorCode.CAPTCHA_INVALID);
    }
    /** 使用不同错误码触发同一种业务异常 */
    @GetMapping("/business")
    public ApiResponse business(@RequestParam String type) {
        throw new BusinessException(switch (type) {
            case "captcha" -> BusinessErrorCode.CAPTCHA_INVALID;
            case "password" -> BusinessErrorCode.PASSWORD_INVALID;
            default -> throw new IllegalArgumentException("不支持的演示类型");
        });
    }
    /** 触发同时匹配子类、父类和默认规则的异常 */
    @GetMapping("/locked")
    public ApiResponse locked() { throw new AccountLockedException(); }
    /** 由 MVC 参数校验真实触发异常 */
    @PostMapping("/validation")
    public ApiResponse validation(@Valid @RequestBody NameRequest input) {
        return new ApiResponse(0, "成功");
    }
    /** 触发未登记异常，诊断文本不得出现在响应中 */
    @GetMapping("/unexpected")
    public ApiResponse unexpected() {
        throw new IllegalStateException("internal-diagnostic-marker");
    }
    /** 演示参数校验的最小请求体 */
    public record NameRequest(@NotBlank String name) {}
}
