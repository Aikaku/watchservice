package com.watchserviceagent.watchservice_agent.common.util;

import jakarta.servlet.http.HttpSession;

/**
 * ownerKey 유틸리티.
 * 단일 사용자 데스크탑 앱이므로 항상 고정 키 "default"를 반환한다.
 * 이를 통해 서버 재시작 후에도 이메일·감시 폴더·예외 규칙 등 설정이 유지된다.
 */
public class OwnerKeyUtil {

    private static final String DEFAULT_OWNER = "default";

    public static String getOrCreate(HttpSession session) {
        return DEFAULT_OWNER;
    }

    private OwnerKeyUtil() {}
}
