package com.medical.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("com.medical.mapper")
public class MyBatisPlusScanConfig {
}
