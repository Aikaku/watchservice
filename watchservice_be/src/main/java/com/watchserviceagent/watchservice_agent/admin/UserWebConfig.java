package com.watchserviceagent.watchservice_agent.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 클래스 이름 : UserWebConfig
 * 기능 : 일반 사용자 인증 인터셉터를 스프링 MVC에 등록한다.
 *        USER_AUTH_ENABLED=true 일 때 사용자 API 경로를 보호한다.
 */
@Configuration
@RequiredArgsConstructor
public class UserWebConfig implements WebMvcConfigurer {

    private final UserAuthService userAuthService;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new UserAuthInterceptor(userAuthService))
                .addPathPatterns(
                        "/settings/**",
                        "/watcher/**",
                        "/scan/**",
                        "/logs/**",
                        "/notifications/**",
                        "/alerts/**",
                        "/dashboard/**",
                        "/api/feedback"
                );
    }
}
