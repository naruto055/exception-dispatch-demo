# 验证记录

日期：2026-11-01（Asia/Shanghai）

- Windows，Java 17.0.12，Maven Wrapper 3.9.9，Spring Boot 4.1.2
- 执行 .\mvnw.cmd -B -ntp clean verify：BUILD SUCCESS
- 7 项测试全部通过，0 失败、0 错误、0 跳过
- 测试覆盖实际 MVC 校验、分发、响应与日志行为
- 使用 java -Dfile.encoding=UTF-8 -jar target/exception-dispatch-demo-0.0.1-SNAPSHOT.jar --server.port=0 --server.address=127.0.0.1 启动打包产物，以随机空闲端口避免干扰其他项目
- 以下请求均通过 HTTP 状态、Content-Type、错误码及中文提示断言；未知异常响应无内部诊断文本，服务端日志包含异常类型及堆栈
- 验证后停止本次启动的服务进程

| 请求路径 | HTTP 状态 | code | message |
| --- | --- | --- | --- |
| /demo/business?type=captcha | 200 | 1001 | 验证码错误 |
| /demo/business?type=password | 200 | 1002 | 账号或密码错误 |
| /demo/locked | 200 | 1003 | 账号已锁定，请联系管理员 |
| /demo/validation | 200 | 4001 | 请求参数不合法 |
| /demo/unexpected | 200 | 5000 | 系统繁忙，请稍后重试 |
| /demo/captcha | 200 | 1001 | 验证码错误 |
