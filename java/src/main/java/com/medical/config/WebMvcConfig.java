package com.medical.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addFormatterForFieldType(LocalDate.class, new LocalDateFormatter());
        registry.addFormatterForFieldType(LocalDateTime.class, new LocalDateTimeFormatter());
        log.info("日期格式化配置已注册: yyyy-MM-dd");
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        log.info("WebMvcConfig initialized - interceptors registered");
    }

    private static class LocalDateFormatter implements org.springframework.format.Formatter<LocalDate> {
        @Override
        public LocalDate parse(String text, java.util.Locale locale) {
            if (text == null || text.trim().isEmpty()) {
                return null;
            }
            text = text.trim();
            return LocalDate.parse(text, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        }

        @Override
        public String print(LocalDate object, java.util.Locale locale) {
            return object != null ? object.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) : "";
        }
    }

    private static class LocalDateTimeFormatter implements org.springframework.format.Formatter<LocalDateTime> {
        @Override
        public LocalDateTime parse(String text, java.util.Locale locale) {
            if (text == null || text.trim().isEmpty()) {
                return null;
            }
            text = text.trim();
            return LocalDateTime.parse(text, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        }

        @Override
        public String print(LocalDateTime object, java.util.Locale locale) {
            return object != null ? object.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : "";
        }
    }
}
