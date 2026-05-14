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

    /*
     * 함수 이름 : getAlerts
     * 기능 : 현재 세션의 ownerKey에 해당하는 알림 목록을 페이지네이션, 필터링, 정렬하여 조회한다.
     * 매개변수 : page - 페이지 번호, size - 페이지 크기, from - 시작 날짜, to - 종료 날짜, level - 위험도 필터, keyword - 검색 키워드, sort - 정렬 기준, session - HTTP 세션
     * 반환값 : ApiResponse<AlertPageResponse> - 페이지네이션된 알림 목록
     * 작성 날짜 : 2026/03/08
     * 작성자 : 시스템
     */
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

    /*
     * 함수 이름 : getAlert
     * 기능 : 특정 ID의 알림을 조회한다.
     * 매개변수 : id - 조회할 알림 ID, session - HTTP 세션
     * 반환값 : ApiResponse<LogResponse> - 알림 상세 정보
     * 작성 날짜 : 2026/03/08
     * 작성자 : 시스템
     */
    @GetMapping("/{id}")
    public ApiResponse<LogResponse> getAlert(@PathVariable("id") long id, HttpSession session) {
        String ownerKey = OwnerKeyUtil.getOrCreate(session);
        return ApiResponse.ok(alertService.getAlertById(ownerKey, id));
    }

    /*
     * 함수 이름 : stats
     * 기능 : 알림 통계 데이터를 조회한다.
     * 매개변수 : range - 통계 기간 단위 (daily/weekly/monthly), from - 시작 날짜, to - 종료 날짜, session - HTTP 세션
     * 반환값 : ApiResponse<AlertStatsResponse> - 알림 통계 데이터
     * 작성 날짜 : 2026/03/08
     * 작성자 : 시스템
     */
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
