package com.example.exceptiondemo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
/** 示例应用入口 */
@SpringBootApplication
public class DemoApplication {
    /** 启动独立的 MVC 服务 */
    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }
}
