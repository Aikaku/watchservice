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

/**
 * 클래스 이름 : ScanController
 * 기능 : 즉시 검사(스캔) 시작·일시정지·진행률 조회 REST 컨트롤러.
 * 작성 날짜 : 2026/03/04
 * 작성자 : 이상혁
 */
@RestController
@RequestMapping("/scan")
@RequiredArgsConstructor
@Slf4j
public class ScanController {

    private final ScanService scanService;

    /*
     * 함수 이름 : start
     * 기능 : 즉시 검사를 시작한다. 스캔 ID를 발급하고 비동기 스캔 작업을 실행한다.
     * 매개변수 : ScanStartRequest req - 스캔할 경로 목록 및 watcher 자동 시작 여부, HttpSession session - 세션
     * 반환값 : ApiResponse<ScanStartResponse> - 발급된 scanId
     * 작성 날짜 : 2026/03/04
     * 작성자 : 이상혁
     */
    @PostMapping("/start")
    public ApiResponse<ScanStartResponse> start(
            @RequestBody ScanStartRequest req, HttpSession session) {
        String ownerKey = OwnerKeyUtil.getOrCreate(session);
        boolean auto = (req.getAutoStartWatcher() == null) ? true : req.getAutoStartWatcher();
        String id = scanService.startScan(ownerKey, req.getPaths(), auto);
        log.info("[ScanController] start scanId={} autoStartWatcher={}", id, auto);
        return ApiResponse.ok(ScanStartResponse.builder().scanId(id).build());
    }

    /*
     * 함수 이름 : pause
     * 기능 : 진행 중인 스캔을 일시정지(중단) 처리하고 현재 진행률을 반환한다.
     * 매개변수 : String scanId - 스캔 ID
     * 반환값 : ApiResponse<ScanProgressResponse> - 중단 후 진행 상태
     * 작성 날짜 : 2026/03/04
     * 작성자 : 이상혁
     */
    @PostMapping("/{scanId}/pause")
    public ApiResponse<ScanProgressResponse> pause(@PathVariable String scanId) {
        scanService.pause(scanId);
        return ApiResponse.ok(scanService.getProgress(scanId));
    }

    /*
     * 함수 이름 : progress
     * 기능 : 스캔 진행률(퍼센트, 스캔 수, 현재 경로 등)을 조회한다.
     * 매개변수 : String scanId - 스캔 ID
     * 반환값 : ApiResponse<ScanProgressResponse>
     * 작성 날짜 : 2026/03/04
     * 작성자 : 이상혁
     */
    @GetMapping("/{scanId}/progress")
    public ApiResponse<ScanProgressResponse> progress(@PathVariable String scanId) {
        return ApiResponse.ok(scanService.getProgress(scanId));
    }
}
