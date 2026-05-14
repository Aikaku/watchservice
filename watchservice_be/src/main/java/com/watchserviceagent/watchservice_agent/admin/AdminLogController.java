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

    /*
     * 함수 이름 : getLogs
     * 기능 : 관리자 전용 로그 목록을 페이지네이션, 필터링, 정렬하여 조회한다.
     * 매개변수 : page - 페이지 번호, size - 페이지 크기, from - 시작 날짜, to - 종료 날짜, keyword - 검색 키워드, aiLabel - AI 라벨 필터, eventType - 이벤트 타입 필터, sort - 정렬 기준
     * 반환값 : LogPageResponse - 페이지네이션된 로그 목록
     * 작성 날짜 : 2026/03/08
     * 작성자 : 시스템
     */
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

    /*
     * 함수 이름 : deleteLog
     * 기능 : 특정 ID의 로그를 관리자 권한으로 삭제한다.
     * 매개변수 : id - 삭제할 로그 ID
     * 반환값 : 204 No Content (성공), 404 Not Found (없을 때)
     * 작성 날짜 : 2026/03/08
     * 작성자 : 시스템
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLog(@PathVariable("id") long id) {
        int deleted = logRepository.deleteByIdAdmin(id);
        if (deleted <= 0) return ResponseEntity.notFound().build();
        return ResponseEntity.noContent().build();
    }

    /*
     * 함수 이름 : deleteLogs
     * 기능 : 여러 로그를 관리자 권한으로 일괄 삭제한다.
     * 매개변수 : req - 삭제할 로그 ID 목록을 담은 요청 객체
     * 반환값 : LogDeleteResponse - 삭제된 로그 개수
     * 작성 날짜 : 2026/03/08
     * 작성자 : 시스템
     */
    @PostMapping("/delete")
    public LogDeleteResponse deleteLogs(@RequestBody LogDeleteRequest req) {
        int deleted = 0;
        if (req.getIds() != null && !req.getIds().isEmpty()) {
            deleted = logRepository.deleteByIdsAdmin(req.getIds());
        }
        return LogDeleteResponse.builder().deletedCount(deleted).build();
    }

    /*
     * 함수 이름 : exportLogs
     * 기능 : 필터 조건에 맞는 로그를 CSV 또는 JSON 형식으로 내보낸다. 최대 20000건까지 지원한다.
     * 매개변수 : from - 시작 날짜, to - 종료 날짜, keyword - 검색 키워드, aiLabel - AI 라벨 필터, eventType - 이벤트 타입 필터, format - 출력 형식 (CSV/JSON)
     * 반환값 : CSV 또는 JSON 형식의 로그 데이터
     * 작성 날짜 : 2026/03/08
     * 작성자 : 시스템
     */
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

    /*
     * 함수 이름 : toCsv
     * 기능 : 로그 목록을 CSV 형식의 문자열로 변환한다.
     * 매개변수 : logs - 변환할 로그 목록
     * 반환값 : CSV 형식 문자열
     * 작성 날짜 : 2026/03/08
     * 작성자 : 시스템
     */
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

    /*
     * 함수 이름 : csvEsc
     * 기능 : CSV 특수문자(쉼표, 큰따옴표, 줄바꿈)가 포함된 문자열을 이스케이프 처리한다.
     * 매개변수 : s - 이스케이프할 문자열
     * 반환값 : 이스케이프 처리된 문자열
     * 작성 날짜 : 2026/03/08
     * 작성자 : 시스템
     */
    private String csvEsc(String s) {
        if (s == null) return "";
        boolean needQuote = s.contains(",") || s.contains("\"") || s.contains("\n");
        String v = s.replace("\"", "\"\"");
        return needQuote ? ("\"" + v + "\"") : v;
    }

    /*
     * 함수 이름 : parseSortParam
     * 기능 : sort 파라미터 문자열을 필드명과 정렬 방향으로 파싱한다.
     * 매개변수 : sort - "field,ASC" 또는 "field,DESC" 형식의 정렬 문자열
     * 반환값 : String[] - [정렬필드, 정렬방향] 배열
     * 작성 날짜 : 2026/03/08
     * 작성자 : 시스템
     */
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

    /*
     * 함수 이름 : parseEpochStart
     * 기능 : 시작 날짜 문자열을 epoch 밀리초로 변환한다. 날짜(YYYY-MM-DD) 또는 날짜시간(YYYY-MM-DD HH:mm:ss) 형식 지원.
     * 매개변수 : from - 시작 날짜 문자열
     * 반환값 : Long - epoch 밀리초, 파싱 실패 시 null
     * 작성 날짜 : 2026/03/08
     * 작성자 : 시스템
     */
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

    /*
     * 함수 이름 : parseEpochEnd
     * 기능 : 종료 날짜 문자열을 epoch 밀리초로 변환한다. 날짜(YYYY-MM-DD) 형식은 다음날 자정으로 계산된다.
     * 매개변수 : to - 종료 날짜 문자열
     * 반환값 : Long - epoch 밀리초, 파싱 실패 시 null
     * 작성 날짜 : 2026/03/08
     * 작성자 : 시스템
     */
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
