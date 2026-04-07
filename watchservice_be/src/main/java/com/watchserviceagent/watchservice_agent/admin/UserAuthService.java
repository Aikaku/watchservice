package com.watchserviceagent.watchservice_agent.admin;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 클래스 이름 : UserAuthService
 * 기능 : 일반 사용자 인증을 처리한다. USER_AUTH_ENABLED=true 일 때만 활성화된다.
 *        활성화 시 USER_PASSWORD 환경변수로 설정한 비밀번호로 접속을 제어한다.
 */
@Service
@Slf4j
public class UserAuthService {

    @Value("${user.auth.enabled:false}")
    private boolean authEnabled;

    @Value("${user.password:}")
    private String userPassword;

    @PostConstruct
    public void init() {
        if (authEnabled) {
            if (userPassword == null || userPassword.isBlank()) {
                log.warn("[UserAuthService] USER_AUTH_ENABLED=true 이지만 USER_PASSWORD가 설정되지 않았습니다. 인증을 비활성화합니다.");
                authEnabled = false;
            } else {
                log.info("[UserAuthService] 일반 사용자 인증 활성화됨.");
            }
        }
    }

    public boolean isAuthEnabled() {
        return authEnabled;
    }

    public boolean authenticate(String password) {
        if (!authEnabled) return true;
        if (password == null || password.isBlank()) return false;
        return userPassword.equals(password);
    }
}
