package com.medical;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

//1. Spring Boot主启动类，作为多Agent临床决策系统的入口
//2. @SpringBootApplication注解自动完成组件扫描、自动配置和启动Spring上下文
@SpringBootApplication
public class ClinicalDecisionApplication {

    //3. 程序入口方法，启动Spring Boot应用
    //4. SpringApplication.run()会自动扫描com.medical包下的所有@Component类
    //5. 初始化顺序：配置加载 → Bean实例化 → Tomcat启动(端口8080)
    public static void main(String[] args) {
        SpringApplication.run(ClinicalDecisionApplication.class, args);
    }
}
