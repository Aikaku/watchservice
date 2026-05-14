package com.watchserviceagent.watchservice_agent.admin;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

/**
 * 클래스 이름 : UserAuthInterceptor
 * 기능 : 일반 사용자 API 경로에 대해 세션 기반 인증을 검증한다.
 *        USER_AUTH_ENABLED=false(기본)이면 모든 요청을 통과시킨다.
 */
@Slf4j
@RequiredArgsConstructor
public class UserAuthInterceptor implements HandlerInterceptor {

    public static final String SESSION_KEY = "USER_AUTH";

    private final UserAuthService userAuthService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {

        // 인증 비활성화 시 항상 통과
        if (!userAuthService.isAuthEnabled()) {
            return true;
        }

        // 사용자 인증 엔드포인트 자체는 통과
        String uri = request.getRequestURI();
        if (uri.startsWith("/api/user/")) {
            return true;
        }

        HttpSession session = request.getSession(false);
        boolean authed = (session != null) && Boolean.TRUE.equals(session.getAttribute(SESSION_KEY));

        if (authed) {
            return true;
        }

        sendJsonUnauthorized(response);
        return false;
    }

    /*
     * 함수 이름 : sendJsonUnauthorized
     * 기능 : 401 Unauthorized 응답과 함께 JSON 오류 메시지를 반환한다.
     * 매개변수 : response - HTTP 응답 객체
     * 반환값 : 없음
     * 작성 날짜 : 2026/03/08
     * 작성자 : 시스템
     */
    private void sendJsonUnauthorized(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"error\":\"user_auth_required\"}");
    }
}
