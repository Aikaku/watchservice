package com.watchserviceagent.watchservice_agent.common.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Slf4j
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${cors.allowed-origins:http://localhost:3000,http://localhost:5173}")
    private String[] allowedOrigins;

    public WebConfig() {
        log.info("[WebConfig] 활성화");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
        log.info("[WebConfig] CORS allowedOrigins={}", (Object) allowedOrigins);
    }

    /** React SPA 라우팅: /api/**, /actuator/**, /static/** 외 경로를 index.html로 포워딩 */
    @Override
    public void addViewControllers(@NonNull ViewControllerRegistry registry) {
        // 단일 세그먼트 (점 없는 경로): /notifications, /logs 등
        registry.addViewController("/{path:[^\\.]*}").setViewName("forward:/index.html");
        // 멀티 세그먼트 React 라우트 명시적 처리 (/static/** 오염 방지)
        registry.addViewController("/notifications/**").setViewName("forward:/index.html");
        registry.addViewController("/logs/**").setViewName("forward:/index.html");
        registry.addViewController("/settings/**").setViewName("forward:/index.html");
        registry.addViewController("/notice/**").setViewName("forward:/index.html");
        registry.addViewController("/admin/**").setViewName("forward:/index.html");
    }

    /** 정적 리소스(JS, CSS, 이미지 등)는 static 폴더에서 직접 서빙 */
    @Override
    public void addResourceHandlers(@NonNull ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/static/");
        registry.addResourceHandler("/*.js", "/*.css", "/*.ico", "/*.png", "/*.json")
                .addResourceLocations("classpath:/static/");
    }
}
