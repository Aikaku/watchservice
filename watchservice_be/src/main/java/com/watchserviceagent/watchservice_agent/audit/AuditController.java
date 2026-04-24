package com.watchserviceagent.watchservice_agent.audit;

import com.watchserviceagent.watchservice_agent.common.ApiResponse;
import com.watchserviceagent.watchservice_agent.common.util.OwnerKeyUtil;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 클래스 이름 : AuditController
 * 기능 : 파일 권한 감사 결과 조회 및 수동 감사 실행 엔드포인트를 제공한다.
 * 작성 날짜 : 2026/04/24
 * 작성자 : 시스템
 */
@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
@Slf4j
public class AuditController {

    private final FilePermissionAuditService auditService;
    private final AuditRepository auditRepository;

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    /**
     * 함수 이름 : getAuditResults
     * 기능 : 최신 감사 결과 목록을 반환한다.
     */
    @GetMapping
    public ApiResponse<Map<String, Object>> getAuditResults(HttpSession session) {
        String ownerKey = OwnerKeyUtil.getOrCreate(session);
        List<AuditResult> results = auditRepository.findByOwner(ownerKey);
        Instant lastRun = auditService.getLastAuditTime(ownerKey);

        List<Map<String, String>> items = results.stream()
                .map(r -> Map.of(
                        "filePath",    r.getFilePath(),
                        "permissions", r.getPermissions() != null ? r.getPermissions() : "",
                        "issue",       r.getIssue() != null ? r.getIssue() : "",
                        "auditedAt",   FMT.format(r.getAuditedAt())
                ))
                .collect(Collectors.toList());

        return ApiResponse.ok(Map.of(
                "items",     items,
                "total",     items.size(),
                "lastRunAt", lastRun != null ? FMT.format(lastRun) : ""
        ));
    }

    /**
     * 함수 이름 : runAudit
     * 기능 : 수동으로 즉시 감사를 실행한다.
     */
    @PostMapping("/run")
    public ApiResponse<Map<String, Object>> runAudit(HttpSession session) {
        String ownerKey = OwnerKeyUtil.getOrCreate(session);
        log.info("[AuditController] 수동 감사 실행 ownerKey={}", ownerKey);
        int found = auditService.runAuditForOwner(ownerKey);
        return ApiResponse.ok(Map.of("found", found));
    }
}
