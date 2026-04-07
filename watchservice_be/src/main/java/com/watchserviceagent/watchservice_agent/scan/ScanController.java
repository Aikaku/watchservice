package com.watchserviceagent.watchservice_agent.scan;

import com.watchserviceagent.watchservice_agent.common.ApiResponse;
import com.watchserviceagent.watchservice_agent.common.util.OwnerKeyUtil;
import com.watchserviceagent.watchservice_agent.scan.dto.ScanProgressResponse;
import com.watchserviceagent.watchservice_agent.scan.dto.ScanStartRequest;
import com.watchserviceagent.watchservice_agent.scan.dto.ScanStartResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/scan")
@RequiredArgsConstructor
@Slf4j
public class ScanController {

    private final ScanService scanService;

    @PostMapping("/start")
    public ApiResponse<ScanStartResponse> start(
            @RequestBody ScanStartRequest req, HttpSession session) {
        String ownerKey = OwnerKeyUtil.getOrCreate(session);
        boolean auto = (req.getAutoStartWatcher() == null) ? true : req.getAutoStartWatcher();
        String id = scanService.startScan(ownerKey, req.getPaths(), auto);
        log.info("[ScanController] start scanId={} autoStartWatcher={}", id, auto);
        return ApiResponse.ok(ScanStartResponse.builder().scanId(id).build());
    }

    @PostMapping("/{scanId}/pause")
    public ApiResponse<ScanProgressResponse> pause(@PathVariable String scanId) {
        scanService.pause(scanId);
        return ApiResponse.ok(scanService.getProgress(scanId));
    }

    @GetMapping("/{scanId}/progress")
    public ApiResponse<ScanProgressResponse> progress(@PathVariable String scanId) {
        return ApiResponse.ok(scanService.getProgress(scanId));
    }
}
