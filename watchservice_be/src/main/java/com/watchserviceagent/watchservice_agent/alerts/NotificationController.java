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

    @GetMapping("/{id}")
    public ApiResponse<NotificationResponse> getNotification(
            @PathVariable("id") long id, HttpSession session) {
        String ownerKey = OwnerKeyUtil.getOrCreate(session);
        return ApiResponse.ok(notificationService.getNotificationById(ownerKey, id));
    }

    @GetMapping("/stats")
    public ApiResponse<Map<String, Object>> getStats(HttpSession session) {
        String ownerKey = OwnerKeyUtil.getOrCreate(session);
        return ApiResponse.ok(notificationService.getStats(ownerKey));
    }

    @PatchMapping("/{id}/false-positive")
    public ApiResponse<Void> markFalsePositive(
            @PathVariable("id") long id, HttpSession session) {
        String ownerKey = OwnerKeyUtil.getOrCreate(session);
        notificationService.markFalsePositive(ownerKey, id);
        return ApiResponse.ok();
    }
}
