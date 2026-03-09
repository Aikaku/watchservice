package com.watchserviceagent.watchservice_agent.dashboard;

import com.watchserviceagent.watchservice_agent.alerts.AlertService;
import com.watchserviceagent.watchservice_agent.alerts.NotificationService;
import com.watchserviceagent.watchservice_agent.alerts.dto.AlertPageResponse;
import com.watchserviceagent.watchservice_agent.common.util.OwnerKeyUtil;
import com.watchserviceagent.watchservice_agent.dashboard.dto.DashboardSummaryResponse;
import com.watchserviceagent.watchservice_agent.settings.SettingsService;
import com.watchserviceagent.watchservice_agent.settings.dto.WatchedFolderResponse;
import com.watchserviceagent.watchservice_agent.storage.dto.LogResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 클래스 이름 : DashboardController
 * 기능 : 대시보드 요약 정보를 제공하는 REST API 엔드포인트를 제공한다.
 * 작성 날짜 : 2025/12/17
 * 작성자 : 시스템
 */
@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
@Slf4j
public class DashboardController {

    private final AlertService alertService;
    private final SettingsService settingsService;
    private final NotificationService notificationService;

    @GetMapping("/summary")
    public DashboardSummaryResponse getSummary(HttpSession session) {
        String ownerKey = OwnerKeyUtil.getOrCreate(session);

        AlertPageResponse page = alertService.getAlerts(
                ownerKey,
                1, 50,
                null, null,
                "ALL",
                null,
                "collectedAt,desc"
        );

        List<LogResponse> items = page.getItems();
        int danger = 0;
        int warning = 0;

        for (LogResponse r : items) {
            if (r.getAiLabel() == null) continue;
            switch (r.getAiLabel().toUpperCase()) {
                case "DANGER" -> danger++;
                case "WARNING" -> warning++;
            }
        }

        String status;
        String statusLabel;

        if (danger > 0) {
            status = "DANGER";
            statusLabel = "위험";
        } else if (warning > 0) {
            status = "WARNING";
            statusLabel = "주의";
        } else {
            status = "SAFE";
            statusLabel = "안전";
        }

        String lastEventTime = "-";
        if (items != null && !items.isEmpty()) {
            lastEventTime = items.get(0).getCollectedAt();
        }

        String watchedPath = "-";
        try {
            List<WatchedFolderResponse> folders = settingsService.getWatchedFolders(ownerKey);
            if (folders != null && !folders.isEmpty()) watchedPath = folders.get(0).getPath();
        } catch (Exception ignore) {}

        String guidance = null;
        try {
            var latest = notificationService.getLatestNotificationOrNull(ownerKey);
            if (latest != null) guidance = latest.getGuidance();
        } catch (Exception ignore) {}

        DashboardSummaryResponse resp = DashboardSummaryResponse.builder()
                .status(status)
                .statusLabel(statusLabel)
                .lastEventTime(lastEventTime)
                .dangerCount(danger)
                .warningCount(warning)
                .totalCount(items == null ? 0 : items.size())
                .watchedPath(watchedPath)
                .guidance(guidance)
                .build();

        log.info("[DashboardController] /dashboard/summary -> {}", resp);
        return resp;
    }
}
