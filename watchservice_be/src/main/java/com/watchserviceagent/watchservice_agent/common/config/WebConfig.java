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

    /** React SPA 라우팅: /api/**, /actuator/** 외 모든 경로를 index.html로 포워딩 */
    @Override
    public void addViewControllers(@NonNull ViewControllerRegistry registry) {
        // React Router가 처리하는 클라이언트 사이드 경로들을 index.html로 포워딩
        registry.addViewController("/{path:[^\\.]*}").setViewName("forward:/index.html");
        registry.addViewController("/{path:[^\\.]*}/**").setViewName("forward:/index.html");
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
