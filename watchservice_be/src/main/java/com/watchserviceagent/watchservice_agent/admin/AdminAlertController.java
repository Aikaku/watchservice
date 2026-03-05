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

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAlert(@PathVariable("id") long id) {
        int deleted = notificationRepository.deleteByIdAdmin(id);
        if (deleted <= 0) return ResponseEntity.notFound().build();
        return ResponseEntity.noContent().build();
    }

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

    private String normalizeLevel(String level) {
        if (level == null) return null;
        String v = level.trim().toUpperCase(Locale.ROOT);
        if (v.isBlank() || v.equals("ALL")) return null;
        if (v.equals("DANGER") || v.equals("WARNING") || v.equals("SAFE")) return v;
        return null;
    }

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
