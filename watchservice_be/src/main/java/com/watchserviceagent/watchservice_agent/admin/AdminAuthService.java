package com.watchserviceagent.watchservice_agent.admin;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 클래스 이름 : AdminAuthService
 * 기능 : 환경변수 / application.yml 에 정의된 관리자 ID/PW와 비교하여 인증을 수행한다.
 *
 * 비밀번호 검증 방식:
 *   - admin.password 값이 "$2a$" 또는 "$2b$"로 시작하면 BCrypt 해시로 간주하여 BCrypt 검증
 *   - 그 외의 평문은 더 이상 허용하지 않음 (보안 강화)
 *
 * BCrypt 해시 생성 예시 (Java):
 *   new BCryptPasswordEncoder().encode("yourPassword")
 * 또는 CLI:
 *   htpasswd -bnBC 10 "" yourPassword | tr -d ':\n'
 */
@Service
@Slf4j
public class AdminAuthService {

    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();
    private static final String DEFAULT_PASSWORD = "123456789";

    @Value("${admin.username:admin}")
    private String adminUsername;

    @Value("${admin.password:123456789}")
    private String adminPassword;

    /**
     * 함수 이름 : init
     * 기능 : 서버 기동 시 기본 비밀번호 사용 여부를 검사하고 경고 로그를 출력한다.
     */
    @PostConstruct
    public void init() {
        if (DEFAULT_PASSWORD.equals(adminPassword)) {
            log.error("==========================================================");
            log.error("[SECURITY] 관리자 비밀번호가 기본값(123456789)입니다.");
            log.error("[SECURITY] 환경변수 ADMIN_PASSWORD를 BCrypt 해시로 설정하세요.");
            log.error("[SECURITY] 예: ADMIN_PASSWORD=$(htpasswd -bnBC 10 '' yourPassword | tr -d ':\\n')");
            log.error("==========================================================");
        }
    }

    /**
     * 함수 이름 : authenticate
     * 기능 : 전달받은 username/password가 관리자 자격 증명과 일치하는지 확인한다.
     *        admin.password는 반드시 BCrypt 해시이어야 하며, 평문 비밀번호는 차단된다.
     */
    public boolean authenticate(String username, String password) {
        if (username == null || password == null) return false;
        if (!adminUsername.equals(username)) return false;

        if (!(adminPassword.startsWith("$2a$") || adminPassword.startsWith("$2b$"))) {
            log.error("[SECURITY] admin.password가 BCrypt 해시가 아닙니다. 로그인이 차단됩니다. " +
                      "환경변수 ADMIN_PASSWORD를 BCrypt 해시로 설정하세요.");
            return false;
        }

        return ENCODER.matches(password, adminPassword);
    }
}

