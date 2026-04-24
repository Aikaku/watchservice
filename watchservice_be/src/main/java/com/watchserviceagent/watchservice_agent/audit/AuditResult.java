package com.watchserviceagent.watchservice_agent.audit;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

/**
 * 클래스 이름 : AuditResult
 * 기능 : 파일 권한 감사 결과 엔티티. 위험 권한이 발견된 파일 경로와 권한 정보를 담는다.
 * 작성 날짜 : 2026/04/24
 * 작성자 : 시스템
 */
@Getter
@Builder
public class AuditResult {
    private Long id;
    private String ownerKey;
    private String filePath;
    private String permissions;   // 예: rwxr-xrwx
    private String issue;         // 예: others-write, others-execute
    private Instant auditedAt;
}
