package com.watchserviceagent.watchservice_agent.scan.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * 클래스 이름 : ScanStartResponse
 * 기능 : 스캔 시작 응답 DTO. 발급된 scanId를 포함한다.
 * 작성 날짜 : 2026/03/04
 * 작성자 : 이상혁
 */
@Getter
@Builder
public class ScanStartResponse {
    private String scanId;
}
