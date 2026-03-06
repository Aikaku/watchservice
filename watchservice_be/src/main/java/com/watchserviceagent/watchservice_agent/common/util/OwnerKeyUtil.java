package com.watchserviceagent.watchservice_agent.common.util;

import jakarta.servlet.http.HttpSession;

import java.util.UUID;

/**
 * HTTP 세션 기반 ownerKey 유틸리티.
 * 세션에 ownerKey가 없으면 새 UUID를 발급하여 세션에 저장한다.
 * 컨트롤러에서 HttpSession을 주입받아 사용한다.
 */
public class OwnerKeyUtil {

    private static final String SESSION_KEY = "OWNER_KEY";

    public static String getOrCreate(HttpSession session) {
        String ownerKey = (String) session.getAttribute(SESSION_KEY);
        if (ownerKey == null || ownerKey.isBlank()) {
            ownerKey = UUID.randomUUID().toString();
            session.setAttribute(SESSION_KEY, ownerKey);
        }
        return ownerKey;
    }

    private OwnerKeyUtil() {}
}
