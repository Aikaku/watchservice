package com.watchserviceagent.watchservice_agent.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 클래스 이름 : AdminAuthService
 * 기능 : 환경변수 / application.yml 에 정의된 관리자 ID/PW와 비교하여 인증을 수행한다.
 */
@Service
@RequiredArgsConstructor
public class AdminAuthService {

    @Value("${admin.username:admin}")
    private String adminUsername;

    @Value("${admin.password:change-me}")
    private String adminPassword;

    /**
     * 함수 이름 : authenticate
     * 기능 : 전달받은 username/password가 관리자 자격 증명과 일치하는지 확인한다.
     */
    public boolean authenticate(String username, String password) {
        if (username == null || password == null) return false;
        return adminUsername.equals(username) && adminPassword.equals(password);
    }
}

