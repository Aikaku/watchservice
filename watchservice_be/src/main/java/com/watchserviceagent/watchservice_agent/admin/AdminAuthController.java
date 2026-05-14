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

    /*
     * 함수 이름 : login
     * 기능 : 관리자 로그인 요청을 처리한다. 인증 성공 시 세션에 ADMIN_AUTH 속성을 설정한다.
     * 매개변수 : req - 로그인 요청 (username, password), request - HTTP 요청 객체
     * 반환값 : 성공 시 {"result":"ok"}, 실패 시 401 {"error":"invalid_credentials"}
     * 작성 날짜 : 2026/03/08
     * 작성자 : 시스템
     */
    @PostMapping("/api/admin/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req, HttpServletRequest request) {
        if (adminAuthService.authenticate(req.getUsername(), req.getPassword())) {
            HttpSession session = request.getSession(true);
            session.setAttribute(AdminAuthInterceptor.SESSION_KEY, Boolean.TRUE);
            return ResponseEntity.ok(Map.of("result", "ok"));
        }
        return ResponseEntity.status(401).body(Map.of("error", "invalid_credentials"));
    }

    /*
     * 함수 이름 : check
     * 기능 : 현재 세션의 관리자 인증 상태를 확인한다.
     * 매개변수 : request - HTTP 요청 객체
     * 반환값 : 인증된 경우 {"authenticated":true}, 미인증 시 401 {"authenticated":false}
     * 작성 날짜 : 2026/03/08
     * 작성자 : 시스템
     */
    @GetMapping("/api/admin/check")
    public ResponseEntity<?> check(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        boolean authed = (session != null) && Boolean.TRUE.equals(session.getAttribute(AdminAuthInterceptor.SESSION_KEY));
        if (authed) {
            return ResponseEntity.ok(Map.of("authenticated", true));
        }
        return ResponseEntity.status(401).body(Map.of("authenticated", false));
    }

    /*
     * 함수 이름 : logout
     * 기능 : 관리자 세션을 무효화하여 로그아웃 처리한다.
     * 매개변수 : request - HTTP 요청 객체
     * 반환값 : {"result":"ok"}
     * 작성 날짜 : 2026/03/08
     * 작성자 : 시스템
     */
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

