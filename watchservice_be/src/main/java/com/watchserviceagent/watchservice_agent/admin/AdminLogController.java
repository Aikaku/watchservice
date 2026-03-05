package com.watchserviceagent.watchservice_agent.admin;

import com.watchserviceagent.watchservice_agent.storage.LogRepository;
import com.watchserviceagent.watchservice_agent.storage.domain.Log;
import com.watchserviceagent.watchservice_agent.storage.dto.LogDeleteRequest;
import com.watchserviceagent.watchservice_agent.storage.dto.LogDeleteResponse;
import com.watchserviceagent.watchservice_agent.storage.dto.LogPageResponse;
import com.watchserviceagent.watchservice_agent.storage.dto.LogResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;

/**
 * 클래스 이름 : AdminLogController
 * 기능 : 관리자 전용 로그 관리 API. 모든 owner_key의 로그를 조회·삭제·내보내기한다.
 * 경로 : /api/admin/logs  (AdminAuthInterceptor에 의해 보호됨)
 */
@RestController
@RequestMapping("/api/admin/logs")
@RequiredArgsConstructor
public class AdminLogController {

    private final LogRepository logRepository;

    private static final int MAX_PAGE_SIZE = 1000;
    private static final int EXPORT_LIMIT = 20000;
    private static final DateTimeFormatter DATE_TIME_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @GetMapping
    public LogPageResponse getLogs(
            @RequestParam(name = "page", required = false) Integer page,
            @RequestParam(name = "size", required = false) Integer size,
            @RequestParam(name = "from", required = false) String from,
            @RequestParam(name = "to", required = false) String to,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "aiLabel", required = false) String aiLabel,
            @RequestParam(name = "eventType", required = false) String eventType,
            @RequestParam(name = "sort", required = false) String sort
    ) {
        int p = (page == null || page < 1) ? 1 : page;
        int s = (size == null || size < 1) ? 50 : Math.min(size, MAX_PAGE_SIZE);

        String[] sortParts = parseSortParam(sort);
        Long fromEpoch = parseEpochStart(from);
        Long toEpoch = parseEpochEnd(to);

        long total = logRepository.countLogsAdmin(fromEpoch, toEpoch, keyword, aiLabel, eventType);
        int offset = (p - 1) * s;
        List<Log> rows = logRepository.findLogsAdmin(
                fromEpoch, toEpoch, keyword, aiLabel, eventType,
                sortParts[0], sortParts[1], offset, s);

        List<LogResponse> items = rows.stream().map(LogResponse::from).toList();
        return LogPageResponse.builder().items(items).page(p).size(s).total(total).build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLog(@PathVariable("id") long id) {
        int deleted = logRepository.deleteByIdAdmin(id);
        if (deleted <= 0) return ResponseEntity.notFound().build();
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/delete")
    public LogDeleteResponse deleteLogs(@RequestBody LogDeleteRequest req) {
        int deleted = 0;
        if (req.getIds() != null && !req.getIds().isEmpty()) {
            deleted = logRepository.deleteByIdsAdmin(req.getIds());
        }
        return LogDeleteResponse.builder().deletedCount(deleted).build();
    }

    @PostMapping("/export")
    public ResponseEntity<?> exportLogs(
            @RequestParam(name = "from", required = false) String from,
            @RequestParam(name = "to", required = false) String to,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "aiLabel", required = false) String aiLabel,
            @RequestParam(name = "eventType", required = false) String eventType,
            @RequestParam(name = "format", defaultValue = "CSV") String format
    ) {
        Long fromEpoch = parseEpochStart(from);
        Long toEpoch = parseEpochEnd(to);

        List<Log> logs = logRepository.findLogsAdmin(
                fromEpoch, toEpoch, keyword, aiLabel, eventType,
                "collectedAt", "DESC", 0, EXPORT_LIMIT);

        String fmt = format.trim().toUpperCase(Locale.ROOT);
        if ("JSON".equals(fmt)) {
            List<LogResponse> items = logs.stream().map(LogResponse::from).toList();
            return ResponseEntity.ok(items);
        }

        String csv = toCsv(logs);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, "text/csv; charset=UTF-8")
                .body(csv);
    }

    private String toCsv(List<Log> logs) {
        StringBuilder sb = new StringBuilder();
        sb.append("id,ownerKey,collectedAt,eventType,path,exists,size,aiLabel,aiScore\n");
        for (Log log : logs) {
            LogResponse r = LogResponse.from(log);
            sb.append(r.getId()).append(",");
            sb.append(csvEsc(log.getOwnerKey())).append(",");
            sb.append(csvEsc(r.getCollectedAt())).append(",");
            sb.append(csvEsc(r.getEventType())).append(",");
            sb.append(csvEsc(r.getPath())).append(",");
            sb.append(r.isExists()).append(",");
            sb.append(r.getSize()).append(",");
            sb.append(csvEsc(r.getAiLabel())).append(",");
            sb.append(r.getAiScore() == null ? "" : r.getAiScore());
            sb.append("\n");
        }
        return sb.toString();
    }

    private String csvEsc(String s) {
        if (s == null) return "";
        boolean needQuote = s.contains(",") || s.contains("\"") || s.contains("\n");
        String v = s.replace("\"", "\"\"");
        return needQuote ? ("\"" + v + "\"") : v;
    }

    private String[] parseSortParam(String sort) {
        String field = "collectedAt";
        String dir = "DESC";
        if (sort != null && !sort.isBlank()) {
            String[] parts = sort.split(",");
            if (parts.length >= 1 && !parts[0].isBlank()) field = parts[0].trim();
            if (parts.length >= 2 && !parts[1].isBlank()) dir = parts[1].trim().toUpperCase(Locale.ROOT);
        }
        if (!dir.equals("ASC") && !dir.equals("DESC")) dir = "DESC";
        return new String[]{field, dir};
    }

    private Long parseEpochStart(String from) {
        if (from == null || from.isBlank()) return null;
        try {
            return LocalDate.parse(from.trim()).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        } catch (DateTimeParseException ignore) {}
        try {
            return LocalDateTime.parse(from.trim(), DATE_TIME_FMT).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        } catch (DateTimeParseException ignore) {}
        return null;
    }

    private Long parseEpochEnd(String to) {
        if (to == null || to.isBlank()) return null;
        try {
            return LocalDate.parse(to.trim()).plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        } catch (DateTimeParseException ignore) {}
        try {
            return LocalDateTime.parse(to.trim(), DATE_TIME_FMT).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        } catch (DateTimeParseException ignore) {}
        return null;
    }
}
