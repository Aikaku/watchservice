package com.watchserviceagent.watchservice_agent.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * 클래스 이름 : SecurityConfig
 * 기능 : Spring Security 기본 설정.
 *        인증·인가는 기존 AdminAuthInterceptor가 담당하므로 모든 요청을 허용하고,
 *        Spring Security의 보안 응답 헤더(XSS, 클릭재킹 방어 등)만 활성화한다.
 *
 * 보안 헤더 (자동 적용):
 *   - X-Content-Type-Options: nosniff
 *   - X-Frame-Options: DENY  (클릭재킹 방어)
 *   - Cache-Control: no-cache, no-store
 *   - Strict-Transport-Security (HTTPS 환경에서 자동 활성화)
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // REST API + SPA 구조이므로 CSRF 비활성화
            .csrf(csrf -> csrf.disable())
            // 기본 폼 로그인·HTTP Basic 비활성화 (커스텀 AdminAuthInterceptor 사용)
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable())
            // 모든 요청 허용 (admin 보호는 AdminAuthInterceptor가 담당)
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
            // 보안 헤더 유지 (기본값: X-Frame-Options DENY, X-Content-Type-Options nosniff 등)
            .headers(headers -> headers
                .frameOptions(frame -> frame.deny())
            );

        return http.build();
    }
}
