package com.medical;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableAspectJAutoProxy(exposeProxy = true)
public class ClinicalDecisionApplication {

    public static void main(String[] args) {
        SpringApplication.run(ClinicalDecisionApplication.class, args);
    }
}
