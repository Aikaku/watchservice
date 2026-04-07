package com.watchserviceagent.watchservice_agent.storage;

import com.watchserviceagent.watchservice_agent.common.ApiResponse;
import com.watchserviceagent.watchservice_agent.common.util.OwnerKeyUtil;
import com.watchserviceagent.watchservice_agent.storage.dto.*;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/logs")
@RequiredArgsConstructor
@Slf4j
public class LogController {

    private final LogService logService;

    @GetMapping("/recent")
    public ApiResponse<List<LogResponse>> getRecentLogs(
            @RequestParam(name = "limit", defaultValue = "50") int limit,
            HttpSession session) {
        if (limit <= 0) limit = 50;
        else if (limit > 1000) limit = 1000;
        String ownerKey = OwnerKeyUtil.getOrCreate(session);
        List<LogResponse> logs = logService.getRecentLogs(ownerKey, limit);
        log.info("[LogController] GET /logs/recent limit={} -> {}", limit, logs.size());
        return ApiResponse.ok(logs);
    }

    @GetMapping
    public ApiResponse<LogPageResponse> getLogs(
            @RequestParam(name = "page", required = false) Integer page,
            @RequestParam(name = "size", required = false) Integer size,
            @RequestParam(name = "from", required = false) String from,
            @RequestParam(name = "to", required = false) String to,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "aiLabel", required = false) String aiLabel,
            @RequestParam(name = "eventType", required = false) String eventType,
            @RequestParam(name = "sort", required = false) String sort,
            HttpSession session
    ) {
        String ownerKey = OwnerKeyUtil.getOrCreate(session);
        return ApiResponse.ok(
                logService.getLogs(ownerKey, page, size, from, to, keyword, aiLabel, eventType, sort));
    }

    @GetMapping("/{id}")
    public ApiResponse<LogResponse> getLog(@PathVariable("id") long id, HttpSession session) {
        String ownerKey = OwnerKeyUtil.getOrCreate(session);
        return ApiResponse.ok(logService.getLogById(ownerKey, id));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteLog(@PathVariable("id") long id, HttpSession session) {
        String ownerKey = OwnerKeyUtil.getOrCreate(session);
        logService.deleteOne(ownerKey, id);
        return ApiResponse.ok();
    }

    @PostMapping("/delete")
    public ApiResponse<LogDeleteResponse> deleteLogs(
            @RequestBody LogDeleteRequest req, HttpSession session) {
        String ownerKey = OwnerKeyUtil.getOrCreate(session);
        int deleted = logService.deleteMany(ownerKey, req.getIds());
        return ApiResponse.ok(LogDeleteResponse.builder().deletedCount(deleted).build());
    }

    /** exportLogs: CSV/JSON 혼용 응답이므로 ApiResponse 래퍼 미적용 */
    @PostMapping("/export")
    public ResponseEntity<?> exportLogs(@RequestBody LogExportRequest req, HttpSession session) {
        String ownerKey = OwnerKeyUtil.getOrCreate(session);
        LogService.ExportResult result = logService.exportLogs(ownerKey, req);

        if (result.isJson()) {
            return ResponseEntity.ok(result.getJsonItems());
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, "text/csv; charset=UTF-8")
                .body(result.getCsvText());
    }

    @GetMapping("/top-files")
    public ApiResponse<List<Map<String, Object>>> getTopFiles(
            @RequestParam(name = "limit", defaultValue = "10") int limit,
            HttpSession session) {
        String ownerKey = OwnerKeyUtil.getOrCreate(session);
        return ApiResponse.ok(logService.getTopFiles(ownerKey, limit));
    }

    @GetMapping("/extension-stats")
    public ApiResponse<List<Map<String, Object>>> getExtensionStats(
            @RequestParam(name = "limit", defaultValue = "20") int limit,
            HttpSession session) {
        String ownerKey = OwnerKeyUtil.getOrCreate(session);
        return ApiResponse.ok(logService.getExtensionStats(ownerKey, limit));
    }
}
