package com.aegispulse.infra.web.config;

import com.aegispulse.infra.web.trace.RequestStructuredLoggingInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 공통 HTTP 인터셉터 등록 설정.
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcLoggingConfig implements WebMvcConfigurer {

    private final RequestStructuredLoggingInterceptor requestStructuredLoggingInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 모든 요청에 접근 로그를 남겨 traceId 기반 상관관계를 보장한다.
        registry.addInterceptor(requestStructuredLoggingInterceptor);
    }
}
