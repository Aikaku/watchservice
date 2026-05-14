package com.watchserviceagent.watchservice_agent.scan.domain;

/**
 * 클래스 이름 : ScanStatus
 * 기능 : 즉시 검사 작업의 상태를 나타내는 열거형.
 * 작성 날짜 : 2026/03/04
 * 작성자 : 이상혁
 */
public enum ScanStatus {
    RUNNING,
    PAUSED,
    DONE,
    ERROR
}
