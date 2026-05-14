package com.watchserviceagent.watchservice_agent.admin;

import com.watchserviceagent.watchservice_agent.alerts.NotificationRepository;
import com.watchserviceagent.watchservice_agent.alerts.domain.Notification;
import com.watchserviceagent.watchservice_agent.alerts.dto.NotificationPageResponse;
import com.watchserviceagent.watchservice_agent.alerts.dto.NotificationResponse;
import lombok.RequiredArgsConstructor;
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
 * 클래스 이름 : AdminAlertController
 * 기능 : 관리자 전용 알림(notification) 관리 API. 모든 owner_key의 알림을 조회·삭제한다.
 * 경로 : /api/admin/alerts  (AdminAuthInterceptor에 의해 보호됨)
 */
@RestController
@RequestMapping("/api/admin/alerts")
@RequiredArgsConstructor
public class AdminAlertController {

    private final NotificationRepository notificationRepository;

    private static final int MAX_PAGE_SIZE = 1000;
    private static final DateTimeFormatter DATE_TIME_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    /*
     * 함수 이름 : getAlerts
     * 기능 : 관리자 전용 알림 목록을 페이지네이션, 필터링, 정렬하여 조회한다.
     * 매개변수 : page - 페이지 번호, size - 페이지 크기, from - 시작 날짜, to - 종료 날짜, level - 위험도 필터, keyword - 검색 키워드, sort - 정렬 기준
     * 반환값 : NotificationPageResponse - 페이지네이션된 알림 목록
     * 작성 날짜 : 2026/03/08
     * 작성자 : 이상혁
     */
    @GetMapping
    public NotificationPageResponse getAlerts(
            @RequestParam(name = "page", required = false) Integer page,
            @RequestParam(name = "size", required = false) Integer size,
            @RequestParam(name = "from", required = false) String from,
            @RequestParam(name = "to", required = false) String to,
            @RequestParam(name = "level", required = false) String level,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "sort", required = false) String sort
    ) {
        int p = (page == null || page < 1) ? 1 : page;
        int s = (size == null || size < 1) ? 50 : Math.min(size, MAX_PAGE_SIZE);

        String[] sortParts = parseSortParam(sort);
        Long fromEpoch = parseEpochStart(from);
        Long toEpoch = parseEpochEnd(to);
        String lv = normalizeLevel(level);

        long total = notificationRepository.countNotificationsAdmin(fromEpoch, toEpoch, keyword, lv);
        int offset = (p - 1) * s;
        List<Notification> rows = notificationRepository.findNotificationsAdmin(
                fromEpoch, toEpoch, keyword, lv, sortParts[0], sortParts[1], offset, s);

        List<NotificationResponse> items = rows.stream().map(this::toResponse).toList();
        return NotificationPageResponse.builder().items(items).total(total).page(p).size(s).build();
    }

    /*
     * 함수 이름 : deleteAlert
     * 기능 : 특정 ID의 알림을 관리자 권한으로 삭제한다.
     * 매개변수 : id - 삭제할 알림 ID
     * 반환값 : 204 No Content (성공), 404 Not Found (없을 때)
     * 작성 날짜 : 2026/03/08
     * 작성자 : 이상혁
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAlert(@PathVariable("id") long id) {
        int deleted = notificationRepository.deleteByIdAdmin(id);
        if (deleted <= 0) return ResponseEntity.notFound().build();
        return ResponseEntity.noContent().build();
    }

    /*
     * 함수 이름 : toResponse
     * 기능 : Notification 엔티티를 NotificationResponse DTO로 변환한다.
     * 매개변수 : n - 알림 엔티티
     * 반환값 : NotificationResponse - 변환된 응답 DTO
     * 작성 날짜 : 2026/03/08
     * 작성자 : 이상혁
     */
    private NotificationResponse toResponse(Notification n) {
        return NotificationResponse.builder()
                .id(n.getId())
                .windowStart(DATE_TIME_FORMATTER.format(n.getWindowStart()))
                .windowEnd(DATE_TIME_FORMATTER.format(n.getWindowEnd()))
                .createdAt(DATE_TIME_FORMATTER.format(n.getCreatedAt()))
                .aiLabel(n.getAiLabel())
                .aiScore(n.getAiScore())
                .topFamily(n.getTopFamily())
                .aiDetail(n.getAiDetail())
                .guidance(n.getGuidance())
                .affectedFilesCount(n.getAffectedFilesCount())
                .affectedPaths(n.getAffectedPaths())
                .build();
    }

    /*
     * 함수 이름 : normalizeLevel
     * 기능 : 위험도 레벨 문자열을 정규화한다. ALL 또는 빈값이면 null 반환, 유효하지 않은 값이면 null 반환.
     * 매개변수 : level - 위험도 문자열 (DANGER, WARNING, SAFE, ALL 등)
     * 반환값 : 정규화된 레벨 문자열 또는 null
     * 작성 날짜 : 2026/03/08
     * 작성자 : 이상혁
     */
    private String normalizeLevel(String level) {
        if (level == null) return null;
        String v = level.trim().toUpperCase(Locale.ROOT);
        if (v.isBlank() || v.equals("ALL")) return null;
        if (v.equals("DANGER") || v.equals("WARNING") || v.equals("SAFE")) return v;
        return null;
    }

    /*
     * 함수 이름 : parseSortParam
     * 기능 : sort 파라미터 문자열을 필드명과 정렬 방향으로 파싱한다.
     * 매개변수 : sort - "field,ASC" 또는 "field,DESC" 형식의 정렬 문자열
     * 반환값 : String[] - [정렬필드, 정렬방향] 배열
     * 작성 날짜 : 2026/03/08
     * 작성자 : 이상혁
     */
    private String[] parseSortParam(String sort) {
        String field = "createdAt";
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
