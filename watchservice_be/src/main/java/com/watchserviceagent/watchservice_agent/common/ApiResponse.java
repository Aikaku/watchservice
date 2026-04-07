package com.watchserviceagent.watchservice_agent.common;

import lombok.Getter;

/**
 * 클래스 이름 : ApiResponse
 * 기능 : 모든 REST API 응답을 { success, data, message } 형태로 통일한다.
 *        프론트의 HttpClient.js가 data 필드를 자동 unwrap하므로 기존 코드 변경 불필요.
 */
@Getter
public class ApiResponse<T> {

    private final boolean success;
    private final T data;
    private final String message;

    private ApiResponse(boolean success, T data, String message) {
        this.success = success;
        this.data = data;
        this.message = message;
    }

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null);
    }

    public static ApiResponse<Void> ok() {
        return new ApiResponse<>(true, null, null);
    }
}
