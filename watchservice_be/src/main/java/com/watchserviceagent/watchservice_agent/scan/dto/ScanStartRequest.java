package com.watchserviceagent.watchservice_agent.scan.dto;

import lombok.Getter;
import lombok.ToString;

import java.util.List;

/**
 * 클래스 이름 : ScanStartRequest
 * 기능 : 스캔 시작 요청 DTO. 스캔할 경로 목록과 watcher 자동 시작 여부를 포함한다.
 * 작성 날짜 : 2026/03/04
 * 작성자 : 이상혁
 */
@Getter
@ToString
public class ScanStartRequest {

    // POST /scan/start  Body: { paths:[...] }
    private List<String> paths;

    // default true (null이면 true로 처리)
    private Boolean autoStartWatcher;
}
