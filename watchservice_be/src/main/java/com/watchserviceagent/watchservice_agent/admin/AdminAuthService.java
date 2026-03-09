package com.watchserviceagent.watchservice_agent.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 클래스 이름 : AdminAuthService
 * 기능 : 환경변수 / application.yml 에 정의된 관리자 ID/PW와 비교하여 인증을 수행한다.
 *
 * 비밀번호 검증 방식:
 *   - admin.password 값이 "$2a$" 또는 "$2b$"로 시작하면 BCrypt 해시로 간주하여 BCrypt 검증
 *   - 그 외에는 평문 비교 (기존 배포 환경 호환)
 *
 * BCrypt 해시 생성 예시 (Java):
 *   new BCryptPasswordEncoder().encode("yourPassword")
 * 또는 CLI:
 *   htpasswd -bnBC 10 "" yourPassword | tr -d ':\n'
 */
@Service
@RequiredArgsConstructor
public class AdminAuthService {

    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();

    @Value("${admin.username:admin}")
    private String adminUsername;

    @Value("${admin.password:change-me}")
    private String adminPassword;

    /**
     * 함수 이름 : authenticate
     * 기능 : 전달받은 username/password가 관리자 자격 증명과 일치하는지 확인한다.
     *        admin.password가 BCrypt 해시이면 BCrypt로 검증, 평문이면 직접 비교한다.
     */
    public boolean authenticate(String username, String password) {
        if (username == null || password == null) return false;
        if (!adminUsername.equals(username)) return false;

        if (adminPassword.startsWith("$2a$") || adminPassword.startsWith("$2b$")) {
            return ENCODER.matches(password, adminPassword);
        }
        return adminPassword.equals(password);
    }
}

