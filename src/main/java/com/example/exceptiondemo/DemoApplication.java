package com.example.exceptiondemo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 示例应用入口
 *
 * <p>启动类位于根包，默认组件扫描会发现子包中的 Controller、Advice 和处理器
 * Spring 容器负责创建这些对象并注入依赖，业务代码不需要手动组装分发链路
 */
@SpringBootApplication
public class DemoApplication {
    /**
     * 启动独立的 MVC 服务
     *
     * @param args 命令行启动参数，可用于覆盖端口等应用配置
     */
    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }
}
