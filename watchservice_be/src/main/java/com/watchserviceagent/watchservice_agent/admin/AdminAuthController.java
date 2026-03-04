package com.watchserviceagent.watchservice_agent.admin;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 클래스 이름 : AdminAuthController
 * 기능 : 관리자 로그인/로그아웃 API를 제공한다.
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class AdminAuthController {

    private final AdminAuthService adminAuthService;

    @PostMapping("/api/admin/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req, HttpServletRequest request) {
        if (adminAuthService.authenticate(req.getUsername(), req.getPassword())) {
            HttpSession session = request.getSession(true);
            session.setAttribute(AdminAuthInterceptor.SESSION_KEY, Boolean.TRUE);
            return ResponseEntity.ok(Map.of("result", "ok"));
        }
        return ResponseEntity.status(401).body(Map.of("error", "invalid_credentials"));
    }

    @PostMapping("/api/admin/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return ResponseEntity.ok(Map.of("result", "ok"));
    }

    @Getter
    @Setter
    public static class LoginRequest {
        private String username;
        private String password;
    }
}

