package com.watchserviceagent.watchservice_agent.alerts;

import com.watchserviceagent.watchservice_agent.alerts.dto.NotificationPageResponse;
import com.watchserviceagent.watchservice_agent.alerts.dto.NotificationResponse;
import com.watchserviceagent.watchservice_agent.common.ApiResponse;
import com.watchserviceagent.watchservice_agent.common.util.OwnerKeyUtil;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /*
     * 함수 이름 : getNotifications
     * 기능 : 현재 세션의 ownerKey에 해당하는 알림 목록을 페이지네이션, 필터링, 정렬하여 조회한다.
     * 매개변수 : page - 페이지 번호, size - 페이지 크기, from - 시작 날짜, to - 종료 날짜, level - 위험도 필터, keyword - 검색 키워드, sort - 정렬 기준, session - HTTP 세션
     * 반환값 : ApiResponse<NotificationPageResponse> - 페이지네이션된 알림 목록
     * 작성 날짜 : 2026/03/08
     * 작성자 : 시스템
     */
    @GetMapping
    public ApiResponse<NotificationPageResponse> getNotifications(
            @RequestParam(name = "page", required = false) Integer page,
            @RequestParam(name = "size", required = false) Integer size,
            @RequestParam(name = "from", required = false) String from,
            @RequestParam(name = "to", required = false) String to,
            @RequestParam(name = "level", required = false) String level,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "sort", required = false) String sort,
            HttpSession session
    ) {
        String ownerKey = OwnerKeyUtil.getOrCreate(session);
        return ApiResponse.ok(
                notificationService.getNotifications(ownerKey, page, size, from, to, level, keyword, sort));
    }

    /*
     * 함수 이름 : getNotification
     * 기능 : 특정 ID의 알림 상세 정보를 조회한다.
     * 매개변수 : id - 조회할 알림 ID, session - HTTP 세션
     * 반환값 : ApiResponse<NotificationResponse> - 알림 상세 정보
     * 작성 날짜 : 2026/03/08
     * 작성자 : 시스템
     */
    @GetMapping("/{id}")
    public ApiResponse<NotificationResponse> getNotification(
            @PathVariable("id") long id, HttpSession session) {
        String ownerKey = OwnerKeyUtil.getOrCreate(session);
        return ApiResponse.ok(notificationService.getNotificationById(ownerKey, id));
    }

    /*
     * 함수 이름 : getStats
     * 기능 : 현재 세션의 ownerKey에 해당하는 알림 통계를 조회한다.
     * 매개변수 : session - HTTP 세션
     * 반환값 : ApiResponse<Map<String, Object>> - 알림 통계 데이터
     * 작성 날짜 : 2026/03/08
     * 작성자 : 시스템
     */
    @GetMapping("/stats")
    public ApiResponse<Map<String, Object>> getStats(HttpSession session) {
        String ownerKey = OwnerKeyUtil.getOrCreate(session);
        return ApiResponse.ok(notificationService.getStats(ownerKey));
    }

    /*
     * 함수 이름 : markFalsePositive
     * 기능 : 특정 알림을 오탐으로 표시하고 해당 경로를 예외 규칙에 추가한다.
     * 매개변수 : id - 오탐으로 표시할 알림 ID, session - HTTP 세션
     * 반환값 : ApiResponse<Void> - 처리 결과
     * 작성 날짜 : 2026/03/08
     * 작성자 : 시스템
     */
    @PatchMapping("/{id}/false-positive")
    public ApiResponse<Void> markFalsePositive(
            @PathVariable("id") long id, HttpSession session) {
        String ownerKey = OwnerKeyUtil.getOrCreate(session);
        notificationService.markFalsePositive(ownerKey, id);
        return ApiResponse.ok();
    }
}
