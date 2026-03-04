package com.watchserviceagent.watchservice_agent.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 클래스 이름 : AdminWebConfig
 * 기능 : 관리자 인증 인터셉터를 스프링 MVC에 등록한다.
 */
@Configuration
@RequiredArgsConstructor
public class AdminWebConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new AdminAuthInterceptor())
                .addPathPatterns("/api/admin/**");
    }
}

