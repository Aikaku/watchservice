package com.watchserviceagent.watchservice_agent.scan.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * 클래스 이름 : ScanProgressResponse
 * 기능 : 스캔 진행 상태(상태, 퍼센트, 스캔 수, 전체 수, 현재 경로, 메시지)를 담는 응답 DTO.
 * 작성 날짜 : 2026/03/04
 * 작성자 : 이상혁
 */
@Getter
@Builder
public class ScanProgressResponse {
    private String status;      // RUNNING / PAUSED / DONE / ERROR
    private int percent;        // 0~100
    private long scanned;       // scanned file count
    private long total;         // total file count
    private String currentPath; // now processing
    private String message;     // optional
}
