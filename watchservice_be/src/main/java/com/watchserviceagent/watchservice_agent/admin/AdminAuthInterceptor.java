package com.watchserviceagent.watchservice_agent.admin;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

/**
 * 클래스 이름 : AdminAuthInterceptor
 * 기능 : /api/admin/** 요청에 대해 세션 기반 관리자 인증을 검증한다.
 *        인증 실패 시 401 JSON 을 반환한다.
 */
@Slf4j
public class AdminAuthInterceptor implements HandlerInterceptor {

    public static final String SESSION_KEY = "ADMIN_AUTH";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {

        String uri = request.getRequestURI();

        // 로그인 엔드포인트는 통과
        if (uri.startsWith("/api/admin/login")) {
            return true;
        }

        // /api/admin/** 에 대해서만 검사
        if (!uri.startsWith("/api/admin/")) {
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

    private void sendJsonUnauthorized(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"error\":\"admin_auth_required\"}");
    }
}

