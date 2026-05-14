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

    /*
     * 함수 이름 : check
     * 기능 : 현재 사용자 세션의 인증 상태를 확인한다. 인증 기능 비활성화 시 항상 200을 반환한다.
     * 매개변수 : request - HTTP 요청 객체
     * 반환값 : 인증 상태 및 인증 활성화 여부를 담은 JSON 응답
     * 작성 날짜 : 2026/03/08
     * 작성자 : 시스템
     */
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

    /*
     * 함수 이름 : login
     * 기능 : 사용자 로그인 요청을 처리한다. 인증 성공 시 세션에 USER_AUTH 속성을 설정한다.
     * 매개변수 : req - 로그인 요청 (password), request - HTTP 요청 객체
     * 반환값 : 성공 시 {"result":"ok"}, 실패 시 401 {"error":"invalid_password"}
     * 작성 날짜 : 2026/03/08
     * 작성자 : 시스템
     */
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

    /*
     * 함수 이름 : logout
     * 기능 : 사용자 세션에서 인증 속성을 제거하여 로그아웃 처리한다.
     * 매개변수 : request - HTTP 요청 객체
     * 반환값 : {"result":"ok"}
     * 작성 날짜 : 2026/03/08
     * 작성자 : 시스템
     */
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
