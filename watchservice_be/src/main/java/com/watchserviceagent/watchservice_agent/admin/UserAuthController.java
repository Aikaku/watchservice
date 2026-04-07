package com.watchserviceagent.watchservice_agent.admin;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 클래스 이름 : UserAuthController
 * 기능 : 일반 사용자 로그인/로그아웃/세션 확인 API를 제공한다.
 *        USER_AUTH_ENABLED=false(기본)이면 /api/user/check가 항상 인증 완료를 반환한다.
 */
@RestController
@RequiredArgsConstructor
public class UserAuthController {

    private final UserAuthService userAuthService;

    /** 세션 유효성 확인. 인증 비활성 시 항상 200 반환 */
    @GetMapping("/api/user/check")
    public ResponseEntity<?> check(HttpServletRequest request) {
        if (!userAuthService.isAuthEnabled()) {
            return ResponseEntity.ok(Map.of("authenticated", true, "authEnabled", false));
        }
        HttpSession session = request.getSession(false);
        boolean authed = (session != null) && Boolean.TRUE.equals(session.getAttribute(UserAuthInterceptor.SESSION_KEY));
        if (authed) {
            return ResponseEntity.ok(Map.of("authenticated", true, "authEnabled", true));
        }
        return ResponseEntity.status(401).body(Map.of("authenticated", false, "authEnabled", true));
    }

    @PostMapping("/api/user/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req, HttpServletRequest request) {
        if (!userAuthService.isAuthEnabled()) {
            return ResponseEntity.ok(Map.of("result", "ok", "authEnabled", false));
        }
        if (userAuthService.authenticate(req.getPassword())) {
            HttpSession session = request.getSession(true);
            session.setAttribute(UserAuthInterceptor.SESSION_KEY, Boolean.TRUE);
            return ResponseEntity.ok(Map.of("result", "ok", "authEnabled", true));
        }
        return ResponseEntity.status(401).body(Map.of("error", "invalid_password"));
    }

    @PostMapping("/api/user/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.removeAttribute(UserAuthInterceptor.SESSION_KEY);
        }
        return ResponseEntity.ok(Map.of("result", "ok"));
    }

    @Getter
    @Setter
    public static class LoginRequest {
        private String password;
    }
}
