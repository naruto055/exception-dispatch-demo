# 异常统一处理与分发示例

从网关异常处理实现提取设计思想，用普通 Spring MVC 演示迁移。项目独立运行，不依赖原项目、数据库或外部服务，不实现真实登录系统。

## 环境与运行

- Java 17（本机验证使用 17.0.12）
- Spring Boot 4.1.2
- Maven Wrapper 固定 Maven 3.9.9
- 依赖：Web MVC、Validation、Web MVC Test，使用 Spring Boot 默认 JSON 支持

版本依据：[Spring Boot](https://spring.io/projects/spring-boot/)、[系统要求](https://docs.spring.io/spring-boot/system-requirements.html)。

在仓库根目录使用 PowerShell：

~~~powershell
.\mvnw.cmd clean verify
.\mvnw.cmd spring-boot:run
~~~

Linux / macOS：

~~~sh
sh mvnw clean verify
sh mvnw spring-boot:run
~~~

默认端口 8080。首次执行需要联网下载 Maven 和依赖。

## 处理链路

~~~text
DemoController 抛出异常
  → GlobalExceptionAdvice 接收 MVC 异常
  → ExceptionDispatcher 查询 ExceptionHandlerRegistry
  → 按异常类型匹配，取最小优先级
  → 从 Spring 容器获取 ExceptionHandlerStrategy
  → 处理器生成 ApiResponse
  → MVC 序列化为 HTTP 200 + JSON
~~~

| 组件 | 职责 |
| --- | --- |
| BusinessErrorCode / BusinessException | 稳定错误码与业务失败上下文 |
| ExceptionHandlerRegistry | 异常类型、策略类型、优先级注册表 |
| ExceptionDispatcher | 类型匹配与策略委派 |
| ExceptionHandlerStrategy 及实现 | 异常转换为响应 |
| GlobalExceptionAdvice | 框架接入、未知异常日志、HTTP 输出 |
| ApiResponse | code、message 对外契约 |

验证码和密码错误都由 BusinessExceptionHandler 转换。错误码不绑定处理器。
AccountLockedExceptionHandler 的专用提示用于展示子类覆盖；实际项目仅在行为有差异时拆分处理器。

## 实际请求

PowerShell：

~~~powershell
Invoke-RestMethod 'http://localhost:8080/demo/business?type=captcha'
Invoke-RestMethod 'http://localhost:8080/demo/business?type=password'
Invoke-RestMethod 'http://localhost:8080/demo/locked'
Invoke-RestMethod 'http://localhost:8080/demo/validation' -Method Post -ContentType 'application/json' -Body '{"name":""}'
Invoke-RestMethod 'http://localhost:8080/demo/unexpected'
~~~

常见 shell：

~~~sh
curl -i 'http://localhost:8080/demo/business?type=captcha'
curl -i 'http://localhost:8080/demo/business?type=password'
curl -i 'http://localhost:8080/demo/locked'
curl -i -H 'Content-Type: application/json' -d '{"name":""}' 'http://localhost:8080/demo/validation'
curl -i 'http://localhost:8080/demo/unexpected'
~~~

| 场景 | 规则 | JSON |
| --- | --- | --- |
| 验证码 | BUSINESS | {"code":1001,"message":"验证码错误"} |
| 密码 | BUSINESS | {"code":1002,"message":"账号或密码错误"} |
| 锁定 | LOCKED | {"code":1003,"message":"账号已锁定，请联系管理员"} |
| 空 name | VALIDATION | {"code":4001,"message":"请求参数不合法"} |
| 未知异常 | DEFAULT | {"code":5000,"message":"系统繁忙，请稍后重试"} |

校验接口传入 {"name":"Ada"} 返回 {"code":0,"message":"成功"}。
文章中的 GET /demo/captcha 也可直接运行，与验证码场景响应一致。

所有上述响应为 HTTP 200，调用方及错误统计必须解析 code。统一响应格式与统一 HTTP 状态是独立选择，这只是演示沿用的项目协议。

## 匹配与优先级

匹配方向是 registeredType.isAssignableFrom(actualException.getClass())，因此注册父类可以接收其子类。
AccountLockedException 同时匹配 LOCKED（5）、BUSINESS（10）、DEFAULT（最大整数），选择 LOCKED。
DEFAULT 故意放在枚举开头，证明不会按声明顺序分发。

这是项目显式规定的数字优先级，不会自动计算继承距离，也不等于 Spring 的 @Order。
Demo 只有一个 Advice。分发器按传入异常的直接类型匹配，不递归 cause；外层未知异常包裹业务异常时进入默认规则。
注册表测试禁止重复优先级，并检查默认规则、策略 Bean 是否存在。重复优先级会产生歧义；缺失 Bean 或兜底会使请求处理失败。

接口统一接收 Exception，业务策略中的向下转型依据注册表的匹配约束；修改映射时必须一并验证类型兼容。

## 如何扩展

1. 仅增加业务错误语义：在 BusinessErrorCode 增加定义，业务位置抛 BusinessException，共用已有策略
2. 确有不同转换行为：按需定义异常类型，实现 ExceptionHandlerStrategy 并注册为 @Component
3. 在枚举登记异常类型、处理器类型、唯一优先级
4. 增加路由和请求测试，确认与父类和默认规则的关系

异常种类少时直接写多个 @ExceptionHandler 更简单。需要集中查看规则、独立维护策略并显式控制匹配顺序时，此示例的分发层才有价值。

## 边界与网关迁移

原网关的响应式 ErrorWebExceptionHandler 接入换成 MVC Advice；保留注册表与策略分工，不复制网关请求对象、原项目工具库或 HTTP 写入代码。
示例用 Exception 兜底，与原项目 Throwable 不同，不承诺处理 JVM 严重错误。
这里只统一 MVC 请求处理链路，不覆盖 Servlet Filter、独立线程、定时任务、消息消费等全部执行环境。
请求格式错误等未登记异常当前进入 DEFAULT；参数示例只覆盖 @Valid 请求体校验，不声称所有客户端错误都已细分。

未知异常在 Advice 中记录一次路径和完整堆栈，对外只返回固定提示。业务失败不默认打印故障堆栈。
响应已提交、序列化失败、连接中断或处理器自身失败时，不保证还能写出统一 JSON，不做递归兜底。
异常处理策略不执行重试、补偿或业务状态修改。

## 验证

ExceptionHandlingTest 使用 Spring Boot 上下文和 MockMvc 验证真实 MVC 链路。
覆盖共享业务策略、多个候选优先级、未登记子类、cause 边界、真实参数校验、默认响应脱敏和日志堆栈、注册配置。
测试结果及实际 HTTP 请求验证记录见 VERIFICATION.md。

此仓库当前是本地示例，尚未发布远程地址。后续发布后将真实链接补入文章；不得用示例 URL 冒充仓库地址。
