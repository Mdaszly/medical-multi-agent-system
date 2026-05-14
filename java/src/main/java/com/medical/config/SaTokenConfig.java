package com.medical.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Slf4j
@Configuration
public class SaTokenConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor(handle -> {
            SaRouter.match("/api/**")
                    .notMatch("/api/auth/register", "/api/auth/login", "/api/auth/logout")
                    // ========================================
                    // TEMPORARY: Exclude temp endpoints - [TEMP-20260513]
                    // DELETE BY: 2026-06-13
                    // ========================================
                    .notMatch("/api/temp/**")
                    .check(r -> StpUtil.checkLogin());
        })).addPathPatterns("/**");
        
        log.info("SaToken interceptor registered successfully");
    }
}
