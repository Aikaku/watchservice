package com.watchserviceagent.watchservice_agent.alerts;

import com.watchserviceagent.watchservice_agent.alerts.dto.AlertPageResponse;
import com.watchserviceagent.watchservice_agent.alerts.dto.AlertStatsResponse;
import com.watchserviceagent.watchservice_agent.common.ApiResponse;
import com.watchserviceagent.watchservice_agent.common.util.OwnerKeyUtil;
import com.watchserviceagent.watchservice_agent.storage.dto.LogResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/alerts")
@RequiredArgsConstructor
public class AlertController {

    private final AlertService alertService;

    @GetMapping
    public ApiResponse<AlertPageResponse> getAlerts(
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
                alertService.getAlerts(ownerKey, page, size, from, to, level, keyword, sort));
    }

    @GetMapping("/{id}")
    public ApiResponse<LogResponse> getAlert(@PathVariable("id") long id, HttpSession session) {
        String ownerKey = OwnerKeyUtil.getOrCreate(session);
        return ApiResponse.ok(alertService.getAlertById(ownerKey, id));
    }

    @GetMapping("/stats")
    public ApiResponse<AlertStatsResponse> stats(
            @RequestParam(name = "range", defaultValue = "daily") String range,
            @RequestParam(name = "from", required = false) String from,
            @RequestParam(name = "to", required = false) String to,
            HttpSession session
    ) {
        String ownerKey = OwnerKeyUtil.getOrCreate(session);
        return ApiResponse.ok(alertService.getStats(ownerKey, range, from, to));
    }
}
